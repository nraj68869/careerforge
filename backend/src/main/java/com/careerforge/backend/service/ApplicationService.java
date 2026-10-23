package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobApplicationRepository;
import com.careerforge.backend.Repository.JobRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.ApplicationResponse;
import com.careerforge.backend.dto.ApplicationStatsResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApplicationService {
    private static final int MAX_PAGE_SIZE = 50;

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

    public ApplicationService(JobApplicationRepository applicationRepository, JobRepository jobRepository,
                              UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
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
        return toResponse(applicationRepository.save(application), actor);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> mine(AuthUser actor, ApplicationStatus status, int page, int size) {
        return applicationRepository.findAll(spec(actor.id(), null, status, null), pageable(page, size))
                .map(a -> toResponse(a, actor));
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
        return toResponse(applicationRepository.save(application), actor);
    }

    // ================= recruiter / officer / admin =================

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> forJob(AuthUser actor, Long jobId, ApplicationStatus status,
                                            int page, int size) {
        findManageableJob(actor, jobId);                 // throws 404 if it is not yours
        return applicationRepository.findAll(spec(null, jobId, status, null), pageable(page, size))
                .map(a -> toResponse(a, actor));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> all(AuthUser actor, ApplicationStatus status, Long jobId, String q,
                                         int page, int size) {
        return applicationRepository.findAll(spec(null, jobId, status, q), pageable(page, size))
                .map(a -> toResponse(a, actor));
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
        return toResponse(applicationRepository.save(application), actor);
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

    private Pageable pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")));
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

    private ApplicationResponse toResponse(JobApplication a, AuthUser actor) {
        boolean studentView = actor.role() == Role.STUDENT;
        List<ApplicationStatus> next = studentView
                ? List.of()
                : TRANSITIONS.get(a.getStatus()).stream().sorted().toList();
        return new ApplicationResponse(
                a.getId(), a.getJob().getId(), a.getJob().getTitle(), a.getJob().getCompany(),
                a.getJob().getStatus(), a.getStudent().getId(), a.getStudent().getName(),
                a.getStudent().getEmail(), a.getStatus(), a.getCoverNote(), a.getAppliedAt(),
                a.getUpdatedAt(), next, studentView && canWithdraw(a));
    }
}