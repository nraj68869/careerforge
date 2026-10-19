package com.careerforge.backend.controller;

import com.careerforge.backend.dto.*;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.service.AuthService;
import com.careerforge.backend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser current,
                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(current, request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthUser current) {
        authService.logout(current);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser current) {
        return userService.getUserById(current.id());
    }
}