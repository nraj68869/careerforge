package com.careerforge.backend.controller;

import com.careerforge.backend.dto.ApplicationResponse;
import com.careerforge.backend.dto.ApplicationStatsResponse;
import com.careerforge.backend.dto.ApplyRequest;
import com.careerforge.backend.dto.UpdateApplicationStatusRequest;
import com.careerforge.backend.entity.ApplicationStatus;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // ---- student ----
    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal AuthUser actor, @PathVariable Long jobId,
                                     @Valid @RequestBody(required = false) ApplyRequest request) {
        return applicationService.apply(actor, jobId, request == null ? null : request.coverNote());
    }

    @GetMapping("/applications/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public Page<ApplicationResponse> mine(@AuthenticationPrincipal AuthUser actor,
                                          @RequestParam(required = false) ApplicationStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return applicationService.mine(actor, status, page, size);
    }

    @PutMapping("/applications/{id}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ApplicationResponse withdraw(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return applicationService.withdraw(actor, id);
    }

    // ---- recruiter (own jobs), officer, admin ----
    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public Page<ApplicationResponse> forJob(@AuthenticationPrincipal AuthUser actor, @PathVariable Long jobId,
                                            @RequestParam(required = false) ApplicationStatus status,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return applicationService.forJob(actor, jobId, status, sort, page, size);
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public ApplicationResponse updateStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        return applicationService.updateStatus(actor, id, request.status());
    }

    // ---- officer, admin ----
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','ADMIN')")
    public Page<ApplicationResponse> all(@AuthenticationPrincipal AuthUser actor,
                                         @RequestParam(required = false) ApplicationStatus status,
                                         @RequestParam(required = false) Long jobId,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return applicationService.all(actor, status, jobId, q, page, size);
    }

    @GetMapping("/applications/stats")
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','ADMIN')")
    public ApplicationStatsResponse stats() {
        return applicationService.stats();
    }

    @GetMapping("/applications/export")
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','ADMIN')")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"careerforge-applications.csv\"")
                .body(applicationService.exportCsv());
    }
}