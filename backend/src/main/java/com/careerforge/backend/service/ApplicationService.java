package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobApplicationRepository;
import com.careerforge.backend.Repository.JobRepository;
import com.careerforge.backend.Repository.StudentProfileRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.ApplicationResponse;
import com.careerforge.backend.dto.ApplicationStatsResponse;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_RANKED = 500;
    private static final int MAX_EXPORT = 10_000;
    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id"));

    // the ONLY moves a recruiter/officer may make
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS = Map.of(
            ApplicationStatus.APPLIED, Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED),
            ApplicationStatus.SHORTLISTED, Set.of(ApplicationStatus.SELECTED, ApplicationStatus.REJECTED),
            ApplicationStatus.REJECTED, Set.of(ApplicationStatus.SHORTLISTED),
            ApplicationStatus.SELECTED, Set.of(),
            ApplicationStatus.WITHDRAWN, Set.of());

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final MatchingService matchingService;
    private final NotificationService notifications;

    public ApplicationService(JobApplicationRepository applicationRepository, JobRepository jobRepository,
                          UserRepository userRepository, StudentProfileRepository profileRepository,
                          MatchingService matchingService, NotificationService notifications) {
    this.applicationRepository = applicationRepository;
    this.jobRepository = jobRepository;
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.matchingService = matchingService;
    this.notifications = notifications;
}

    // ================= student =================

    @Transactional
    public ApplicationResponse apply(AuthUser actor, Long jobId, String coverNote) {
    Job job = jobRepository.findWithPosterById(jobId).orElseThrow(this::jobNotFound);
    if (!isAccepting(job)) {
        throw new ApiException(HttpStatus.CONFLICT, "This job is no longer accepting applications");
    }

    String note = clean(coverNote);
    Instant now = Instant.now();
    JobApplication application = applicationRepository.findByJobIdAndStudentId(jobId, actor.id()).orElse(null);

    if (application == null) {
        User student = userRepository.findById(actor.id())
                .orElseThrow(() -> new UserNotFoundException(actor.id()));
        application = new JobApplication();
        application.setJob(job);
        application.setStudent(student);
        application.setCoverNote(note);
    } else if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
        // re-applying after a withdrawal re-opens the same record
        application.setStatus(ApplicationStatus.APPLIED);
        application.setCoverNote(note);
        application.setAppliedAt(now);
        application.setUpdatedAt(now);
    } else {
        throw new ApiException(HttpStatus.CONFLICT, "You have already applied to this job");
    }

    JobApplication saved = applicationRepository.save(application);
    notifications.send(job.getPostedBy().getId(), NotificationType.APPLICATION, "New application",
            saved.getStudent().getName() + " applied to " + job.getTitle(), "my-jobs");
    return toResponse(saved, actor, null);
}

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> mine(AuthUser actor, ApplicationStatus status, int page, int size) {
        return applicationRepository.findAll(spec(actor.id(), null, status, null), pageable(page, size))
                .map(a -> toResponse(a, actor, null));
    }

    @Transactional
    public ApplicationResponse withdraw(AuthUser actor, Long id) {
    JobApplication application = applicationRepository.findWithDetailsById(id)
            .orElseThrow(this::applicationNotFound);
    if (!application.getStudent().getId().equals(actor.id())) {
        throw applicationNotFound();                 // not yours: it "does not exist"
    }
    if (!canWithdraw(application)) {
        throw new ApiException(HttpStatus.CONFLICT, "This application can no longer be withdrawn");
    }
    application.setStatus(ApplicationStatus.WITHDRAWN);
    application.setUpdatedAt(Instant.now());

    JobApplication saved = applicationRepository.save(application);
    notifications.send(application.getJob().getPostedBy().getId(), NotificationType.APPLICATION,
            "Application withdrawn",
            application.getStudent().getName() + " withdrew from " + application.getJob().getTitle(), "my-jobs");
    return toResponse(saved, actor, null);
}

    // ================= recruiter / officer / admin =================

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> forJob(AuthUser actor, Long jobId, ApplicationStatus status,
                                            String sort, int page, int size) {
        findManageableJob(actor, jobId);                 // throws 404 if it is not yours
        Specification<JobApplication> spec = spec(null, jobId, status, null);

        if ("match".equalsIgnoreCase(sort)) {
            int safeSize = clampSize(size);
            int safePage = Math.max(page, 0);
            List<JobApplication> all = new ArrayList<>(
                    applicationRepository.findAll(spec, PageRequest.of(0, MAX_RANKED, LATEST)).getContent());
            Map<Long, MatchInfo> matches = matchesFor(all);

            // best candidates first; applicants without a score go last
            all.sort(Comparator.comparingInt((JobApplication a) -> matches.containsKey(a.getId()) ? matches.get(a.getId()).score() : -1).reversed());

            int from = (int) Math.min((long) safePage * safeSize, all.size());
            int to = Math.min(from + safeSize, all.size());
            List<ApplicationResponse> content = all.subList(from, to).stream()
                    .map(a -> toResponse(a, actor, matches.get(a.getId()))).toList();
            return new PageImpl<>(content, PageRequest.of(safePage, safeSize), all.size());
        }

        Page<JobApplication> result = applicationRepository.findAll(spec, pageable(page, size));
        Map<Long, MatchInfo> matches = matchesFor(result.getContent());
        return result.map(a -> toResponse(a, actor, matches.get(a.getId())));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> all(AuthUser actor, ApplicationStatus status, Long jobId, String q,
                                         int page, int size) {
        Page<JobApplication> result = applicationRepository.findAll(spec(null, jobId, status, q), pageable(page, size));
        Map<Long, MatchInfo> matches = matchesFor(result.getContent());
        return result.map(a -> toResponse(a, actor, matches.get(a.getId())));
    }

    @Transactional
    public ApplicationResponse updateStatus(AuthUser actor, Long id, ApplicationStatus next) {
    JobApplication application = applicationRepository.findWithDetailsById(id)
            .orElseThrow(this::applicationNotFound);
    if (!canManageJob(actor, application.getJob())) {
        throw applicationNotFound();
    }
    if (next == ApplicationStatus.WITHDRAWN) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Only the student can withdraw an application");
    }
    if (!TRANSITIONS.get(application.getStatus()).contains(next)) {
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Cannot move an application from " + application.getStatus() + " to " + next);
    }
    application.setStatus(next);
    application.setUpdatedAt(Instant.now());

    JobApplication saved = applicationRepository.save(application);
    notifications.send(application.getStudent().getId(), NotificationType.STATUS, statusTitle(next),
            application.getJob().getCompany() + " · " + application.getJob().getTitle(), "my-applications");
    return toResponse(saved, actor, null);
}

    @Transactional(readOnly = true)
    public ApplicationStatsResponse stats() {
        return new ApplicationStatsResponse(
                applicationRepository.count(),
                applicationRepository.countByStatus(ApplicationStatus.APPLIED),
                applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED),
                applicationRepository.countByStatus(ApplicationStatus.SELECTED),
                applicationRepository.countByStatus(ApplicationStatus.REJECTED),
                applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN),
                applicationRepository.countDistinctStudentsByStatus(ApplicationStatus.SELECTED),
                jobRepository.countByStatus(JobStatus.OPEN));
    }

    // ================= placement report (CSV) =================

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        List<JobApplication> apps = applicationRepository
                .findAll(spec(null, null, null, null), PageRequest.of(0, MAX_EXPORT, LATEST)).getContent();
        Map<Long, MatchInfo> matches = matchesFor(apps);

        StringBuilder csv = new StringBuilder("\uFEFF");    // lets Excel read UTF-8 names correctly
        csv.append("Student,Email,Job,Company,Status,Match %,Applied on,Last update\r\n");
        for (JobApplication a : apps) {
            MatchInfo m = matches.get(a.getId());
            csv.append(cell(a.getStudent().getName())).append(',')
               .append(cell(a.getStudent().getEmail())).append(',')
               .append(cell(a.getJob().getTitle())).append(',')
               .append(cell(a.getJob().getCompany())).append(',')
               .append(cell(a.getStatus().name())).append(',')
               .append(cell(m == null ? "" : String.valueOf(m.score()))).append(',')
               .append(cell(a.getAppliedAt().toString().substring(0, 10))).append(',')
               .append(cell(a.getUpdatedAt().toString().substring(0, 10))).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // quote every cell, and defuse spreadsheet formulas (a cell starting with = + - @ can run code in Excel)
    private String cell(String value) {
        if (value == null) return "\"\"";
        String v = value.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    // ================= permission rules =================

    private boolean canManageJob(AuthUser actor, Job job) {
        if (actor.role() == Role.ADMIN || actor.role() == Role.PLACEMENT_OFFICER) return true;
        return actor.role() == Role.RECRUITER && job.getPostedBy().getId().equals(actor.id());
    }

    private Job findManageableJob(AuthUser actor, Long jobId) {
        Job job = jobRepository.findWithPosterById(jobId).orElseThrow(this::jobNotFound);
        if (!canManageJob(actor, job)) throw jobNotFound();   // 404, not 403
        return job;
    }

    private boolean isAccepting(Job job) {
        return job.getStatus() == JobStatus.OPEN && !job.getDeadline().isBefore(LocalDate.now());
    }

    private boolean canWithdraw(JobApplication a) {
        return a.getStatus() == ApplicationStatus.APPLIED || a.getStatus() == ApplicationStatus.SHORTLISTED;
    }

    // ================= helpers =================

    // one query loads every student's profile for the page; then each pair is scored
    private Map<Long, MatchInfo> matchesFor(List<JobApplication> apps) {
        if (apps.isEmpty()) return Map.of();
        List<Long> studentIds = apps.stream().map(a -> a.getStudent().getId()).distinct().toList();
        Map<Long, StudentProfile> profiles = profileRepository.findByUserIdIn(studentIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        Map<Long, MatchInfo> result = new HashMap<>();
        for (JobApplication a : apps) {
            MatchInfo match = matchingService.score(profiles.get(a.getStudent().getId()), a.getJob());
            if (match != null) result.put(a.getId(), match);
        }
        return result;
    }

    private ApiException jobNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Job not found");
    }

    private ApiException applicationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Application not found");
    }

    private String clean(String note) {
        if (note == null || note.isBlank()) return null;
        return note.trim();
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), clampSize(size), LATEST);
    }

    private Specification<JobApplication> spec(Long studentId, Long jobId, ApplicationStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (studentId != null) p.add(cb.equal(root.get("student").get("id"), studentId));
            if (jobId != null) p.add(cb.equal(root.get("job").get("id"), jobId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (q != null && !q.isBlank()) {
                String like = "%" + escapeLike(q.trim().toLowerCase()) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("student").<String>get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("job").<String>get("title")), like, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private ApplicationResponse toResponse(JobApplication a, AuthUser actor, MatchInfo match) {
        boolean studentView = actor.role() == Role.STUDENT;
        List<ApplicationStatus> next = studentView
                ? List.of()
                : TRANSITIONS.get(a.getStatus()).stream().sorted().toList();
        return new ApplicationResponse(
                a.getId(), a.getJob().getId(), a.getJob().getTitle(), a.getJob().getCompany(),
                a.getJob().getStatus(), a.getStudent().getId(), a.getStudent().getName(),
                a.getStudent().getEmail(), a.getStatus(), a.getCoverNote(), a.getAppliedAt(),
                a.getUpdatedAt(), next, studentView && canWithdraw(a), match);
    }
    private String statusTitle(ApplicationStatus status) {
    return switch (status) {
        case SHORTLISTED -> "You have been shortlisted";
        case SELECTED -> "Congratulations, you were selected!";
        case REJECTED -> "Update on your application";
        default -> "Your application status changed";
    };
}
}