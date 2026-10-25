package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobApplicationRepository;
import com.careerforge.backend.Repository.JobRepository;
import com.careerforge.backend.Repository.StudentProfileRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.JobRequest;
import com.careerforge.backend.dto.JobResponse;
import com.careerforge.backend.dto.MatchInfo;
import com.careerforge.backend.entity.*;
import com.careerforge.backend.exception.ApiException;
import com.careerforge.backend.exception.UserNotFoundException;
import com.careerforge.backend.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JobService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_RANKED = 300;
    private static final int MAX_JOB_SKILLS = 15;
    private static final Sort NEWEST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;
    private final StudentProfileRepository profileRepository;
    private final MatchingService matchingService;

    public JobService(JobRepository jobRepository, UserRepository userRepository,
                      JobApplicationRepository applicationRepository,
                      StudentProfileRepository profileRepository, MatchingService matchingService) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
        this.matchingService = matchingService;
    }

    // ---------- reading ----------

    /** Students: open jobs only, with match scores. Officers/admins: everything. */
    @Transactional(readOnly = true)
    public Page<JobResponse> browse(AuthUser actor, JobStatus status, JobType type, String q,
                                    String location, String sort, int page, int size) {
        boolean student = actor.role() == Role.STUDENT;
        Specification<Job> spec = buildSpec(null, student ? null : status, student, type, q, location);

        if (!student) {
            return jobRepository.findAll(spec, pageable(page, size)).map(this::toResponse);
        }

        StudentProfile profile = profileRepository.findByUserId(actor.id()).orElse(null);

        if ("match".equalsIgnoreCase(sort)) {
            int safeSize = clampSize(size);
            int safePage = Math.max(page, 0);
            List<Job> jobs = new ArrayList<>(
                    jobRepository.findAll(spec, PageRequest.of(0, MAX_RANKED, NEWEST)).getContent());

            Map<Long, MatchInfo> matches = new HashMap<>();
            for (Job job : jobs) {
                MatchInfo match = matchingService.score(profile, job);
                if (match != null) matches.put(job.getId(), match);
            }
            // best score first; jobs without required skills go last; ties stay newest-first
            jobs.sort(Comparator.comparingInt((Job j) -> matches.containsKey(j.getId()) ? matches.get(j.getId()).score() : -1).reversed());

            int from = (int) Math.min((long) safePage * safeSize, jobs.size());
            int to = Math.min(from + safeSize, jobs.size());
            List<Job> slice = jobs.subList(from, to);
            Map<Long, ApplicationStatus> mine = myStatuses(actor.id(), slice);
            List<JobResponse> content = slice.stream()
                    .map(j -> toResponse(j, mine.get(j.getId()), matches.get(j.getId()))).toList();
            return new PageImpl<>(content, PageRequest.of(safePage, safeSize), jobs.size());
        }

        Page<Job> result = jobRepository.findAll(spec, pageable(page, size));
        Map<Long, ApplicationStatus> mine = myStatuses(actor.id(), result.getContent());
        return result.map(job -> toResponse(job, mine.get(job.getId()), matchingService.score(profile, job)));
    }

    /** The jobs I posted, any status. */
    @Transactional(readOnly = true)
    public Page<JobResponse> mine(AuthUser actor, JobStatus status, JobType type,
                                  String q, String location, int page, int size) {
        Specification<Job> spec = buildSpec(actor.id(), status, false, type, q, location);
        return jobRepository.findAll(spec, pageable(page, size)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse get(AuthUser actor, Long id) {
        Job job = findOrThrow(id);
        if (!canView(actor, job)) throw notFound();
        if (actor.role() != Role.STUDENT) return toResponse(job);

        ApplicationStatus mine = applicationRepository.findByJobIdAndStudentId(id, actor.id())
                .map(JobApplication::getStatus).orElse(null);
        StudentProfile profile = profileRepository.findByUserId(actor.id()).orElse(null);
        return toResponse(job, mine, matchingService.score(profile, job));
    }

    // ---------- writing ----------

    @Transactional
    public JobResponse create(AuthUser actor, JobRequest req) {
        validateSalary(req);
        User poster = userRepository.findById(actor.id())
                .orElseThrow(() -> new UserNotFoundException(actor.id()));
        Job job = new Job();
        job.setPostedBy(poster);
        apply(job, req);
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse update(AuthUser actor, Long id, JobRequest req) {
        Job job = findManageable(actor, id);
        validateSalary(req);
        apply(job, req);
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse setStatus(AuthUser actor, Long id, JobStatus status) {
        Job job = findManageable(actor, id);
        if (status == JobStatus.OPEN && job.getDeadline().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The deadline has passed. Edit the job and choose a new deadline to reopen it.");
        }
        job.setStatus(status);
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public void delete(AuthUser actor, Long id) {
        Job job = findManageable(actor, id);
        if (applicationRepository.existsByJobId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This job has applications. Close it instead of deleting it.");
        }
        jobRepository.delete(job);
    }

    // ---------- the permission rules (one place) ----------

    private boolean isPrivileged(AuthUser actor) {
        return actor.role() == Role.ADMIN || actor.role() == Role.PLACEMENT_OFFICER;
    }

    private boolean isOwner(AuthUser actor, Job job) {
        return job.getPostedBy().getId().equals(actor.id());
    }

    private boolean isAccepting(Job job) {
        return job.getStatus() == JobStatus.OPEN && !job.getDeadline().isBefore(LocalDate.now());
    }

    private boolean canView(AuthUser actor, Job job) {
        if (isPrivileged(actor)) return true;
        if (actor.role() == Role.RECRUITER) return isOwner(actor, job);
        return isAccepting(job);                       // students
    }

    private boolean canManage(AuthUser actor, Job job) {
        return isPrivileged(actor) || (actor.role() == Role.RECRUITER && isOwner(actor, job));
    }

    private Job findManageable(AuthUser actor, Long id) {
        Job job = findOrThrow(id);
        if (!canManage(actor, job)) throw notFound();  // 404, not 403: do not reveal it exists
        return job;
    }

    // ---------- helpers ----------

    private Job findOrThrow(Long id) {
        return jobRepository.findWithPosterById(id).orElseThrow(this::notFound);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Job not found");
    }

    private void validateSalary(JobRequest req) {
        if (req.salaryMin() != null && req.salaryMax() != null && req.salaryMax() < req.salaryMin()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum salary cannot be lower than minimum salary");
        }
    }

    private void apply(Job job, JobRequest req) {
        job.setTitle(req.title().trim());
        job.setCompany(req.company().trim());
        job.setLocation(req.location().trim());
        job.setType(req.type());
        job.setDescription(req.description().trim());
        job.setSalaryMin(req.salaryMin());
        job.setSalaryMax(req.salaryMax());
        job.setDeadline(req.deadline());
        job.getRequiredSkills().clear();
        job.getRequiredSkills().addAll(SkillUtils.clean(req.requiredSkills(), MAX_JOB_SKILLS));
        job.setMinCgpa(req.minCgpa());
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    // The client NEVER chooses the sort field, and the page size is capped.
    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), clampSize(size), NEWEST);
    }

    private Specification<Job> buildSpec(Long ownerId, JobStatus status, boolean onlyAcceptingNow,
                                         JobType type, String q, String location) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (ownerId != null) p.add(cb.equal(root.get("postedBy").get("id"), ownerId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (onlyAcceptingNow) {
                p.add(cb.equal(root.get("status"), JobStatus.OPEN));
                p.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("deadline"), LocalDate.now()));
            }
            if (type != null) p.add(cb.equal(root.get("type"), type));
            if (q != null && !q.isBlank()) {
                String like = "%" + escapeLike(q.trim().toLowerCase()) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), like, '\\'),
                        cb.like(cb.lower(root.<String>get("company")), like, '\\')));
            }
            if (location != null && !location.isBlank()) {
                String like = "%" + escapeLike(location.trim().toLowerCase()) + "%";
                p.add(cb.like(cb.lower(root.<String>get("location")), like, '\\'));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    // a user typing % or _ must not act as a wildcard
    private String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // one query for the whole page: which of these jobs did this student apply to?
    private Map<Long, ApplicationStatus> myStatuses(Long studentId, List<Job> jobs) {
        if (jobs.isEmpty()) return Map.of();
        List<Long> ids = jobs.stream().map(Job::getId).toList();
        return applicationRepository.findByStudentIdAndJobIdIn(studentId, ids).stream()
                .collect(Collectors.toMap(a -> a.getJob().getId(), JobApplication::getStatus));
    }

    private JobResponse toResponse(Job j) {
        return toResponse(j, null, null);
    }

    private JobResponse toResponse(Job j, ApplicationStatus myStatus, MatchInfo match) {
        List<String> skills = j.getRequiredSkills().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        return new JobResponse(j.getId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getType(),
                j.getDescription(), j.getSalaryMin(), j.getSalaryMax(), j.getDeadline(), j.getStatus(),
                isAccepting(j), j.getPostedBy().getId(), j.getPostedBy().getName(), j.getCreatedAt(),
                myStatus, skills, j.getMinCgpa(), match);
    }
}