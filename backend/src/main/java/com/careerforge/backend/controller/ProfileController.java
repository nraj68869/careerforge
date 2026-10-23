package com.careerforge.backend.controller;

import com.careerforge.backend.dto.ProfileRequest;
import com.careerforge.backend.dto.ProfileResponse;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

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

    @GetMapping("/students/{studentId}/profile")
    @PreAuthorize("hasAnyRole('RECRUITER','PLACEMENT_OFFICER','ADMIN')")
    public ProfileResponse forStudent(@AuthenticationPrincipal AuthUser actor, @PathVariable Long studentId) {
        return profileService.getForViewer(actor, studentId);
    }
}