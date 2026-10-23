package com.careerforge.backend.controller;

import com.careerforge.backend.dto.*;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ---- ADMIN ONLY ----
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getAllUsers(@PageableDefault(size = 10) Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> searchUser(@RequestParam String name,
                                         @PageableDefault(size = 10) Pageable pageable) {
        return userService.searchUsers(name, pageable);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request,
                                   @AuthenticationPrincipal AuthUser actor) {
        return userService.changeRole(id, request.role(), actor);
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody SetEnabledRequest request,
                                   @AuthenticationPrincipal AuthUser actor) {
        return userService.setEnabled(id, request.enabled(), actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal AuthUser actor) {
        userService.deleteUser(id, actor);
    }
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public UserStatsResponse getStats() {
        return userService.getStats();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> pendingRecruiters() {
        return userService.pendingRecruiters();
    }
    
    @PutMapping("/{id}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse reviewRecruiter(@PathVariable Long id, @Valid @RequestBody ReviewRecruiterRequest request) {
        return userService.reviewRecruiter(id, request.status());
    }
    

    // ---- THE USER THEMSELVES, OR AN ADMIN ----
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id()")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return new ApiResponse<>(true, "User retrieved successfully", userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id()")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        return userService.updateUser(id, request);
    }
}