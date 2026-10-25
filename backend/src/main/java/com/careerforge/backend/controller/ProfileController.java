package com.careerforge.backend.controller;

import com.careerforge.backend.dto.ProfileRequest;
import com.careerforge.backend.dto.ProfileResponse;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // ---- the student ----
    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ProfileResponse mine(@AuthenticationPrincipal AuthUser actor) {
        return profileService.getMine(actor);
    }

    @PutMapping("/profile/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ProfileResponse save(@AuthenticationPrincipal AuthUser actor, @Valid @RequestBody ProfileRequest request) {
        return profileService.saveMine(actor, request);
    }

    @PostMapping(path = "/profile/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ProfileResponse uploadResume(@AuthenticationPrincipal AuthUser actor,
                                        @RequestParam("file") MultipartFile file) {
        return profileService.uploadResume(actor, file);
    }

    @DeleteMapping("/profile/me/resume")
    @PreAuthorize("hasRole('STUDENT')")
    public ProfileResponse removeResume(@AuthenticationPrincipal AuthUser actor) {
        return profileService.removeResume(actor);
    }

    @GetMapping("/profile/me/resume")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> myResume(@AuthenticationPrincipal AuthUser actor) {
        return pdf(profileService.readMyResume(actor));
    }

    // ---- recruiter (own applicants), officer, admin ----
    @GetMapping("/students/{studentId}/profile")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public ProfileResponse forStudent(@AuthenticationPrincipal AuthUser actor, @PathVariable Long studentId) {
        return profileService.getForViewer(actor, studentId);
    }

    @GetMapping("/students/{studentId}/resume")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public ResponseEntity<byte[]> studentResume(@AuthenticationPrincipal AuthUser actor, @PathVariable Long studentId) {
        return pdf(profileService.readResumeForViewer(actor, studentId));
    }

    private ResponseEntity<byte[]> pdf(byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resume.pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(bytes);
    }
}