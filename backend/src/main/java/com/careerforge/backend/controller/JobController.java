package com.careerforge.backend.controller;

import com.careerforge.backend.dto.JobRequest;
import com.careerforge.backend.dto.JobResponse;
import com.careerforge.backend.dto.SetJobStatusRequest;
import com.careerforge.backend.entity.JobStatus;
import com.careerforge.backend.entity.JobType;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // Students, officers, admins: browse. (Students automatically see only open jobs.)
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','PLACEMENT_OFFICER','ADMIN')")
    public Page<JobResponse> browse(@AuthenticationPrincipal AuthUser actor,
                                    @RequestParam(required = false) JobStatus status,
                                    @RequestParam(required = false) JobType type,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return jobService.browse(actor, status, type, q, location, page, size);
    }

    // Recruiters (and officers/admins): the jobs I posted
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public Page<JobResponse> mine(@AuthenticationPrincipal AuthUser actor,
                                  @RequestParam(required = false) JobStatus status,
                                  @RequestParam(required = false) JobType type,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String location,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return jobService.mine(actor, status, type, q, location, page, size);
    }

    @GetMapping("/{id}")
    public JobResponse get(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return jobService.get(actor, id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@AuthenticationPrincipal AuthUser actor, @Valid @RequestBody JobRequest request) {
        return jobService.create(actor, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public JobResponse update(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                              @Valid @RequestBody JobRequest request) {
        return jobService.update(actor, id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public JobResponse setStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                 @Valid @RequestBody SetJobStatusRequest request) {
        return jobService.setStatus(actor, id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        jobService.delete(actor, id);
    }
}