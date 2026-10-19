package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.JobRequest;
import com.careerforge.backend.dto.JobResponse;
import com.careerforge.backend.entity.*;
import com.careerforge.backend.exception.ApiException;
import com.careerforge.backend.exception.UserNotFoundException;
import com.careerforge.backend.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {
    private static final int MAX_PAGE_SIZE = 50;

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public JobService(JobRepository jobRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    // ---------- reading ----------

    /** Students: open jobs only. Officers/admins: everything, optional status filter. */
    @Transactional(readOnly = true)
    public Page<JobResponse> browse(AuthUser actor, JobStatus status, JobType type,
                                    String q, String location, int page, int size) {
        boolean student = actor.role() == Role.STUDENT;
        Specification<Job> spec = buildSpec(null, student ? null : status, student, type, q, location);
        return jobRepository.findAll(spec, pageable(page, size)).map(this::toResponse);
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
        return toResponse(job);
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
        jobRepository.delete(findManageable(actor, id));
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
    }

    
    private Pageable pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
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

    private String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private JobResponse toResponse(Job j) {
        return new JobResponse(j.getId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getType(),
                j.getDescription(), j.getSalaryMin(), j.getSalaryMax(), j.getDeadline(), j.getStatus(),
                isAccepting(j), j.getPostedBy().getId(), j.getPostedBy().getName(), j.getCreatedAt());
    }
}