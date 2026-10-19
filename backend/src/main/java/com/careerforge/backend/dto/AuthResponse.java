package com.careerforge.backend.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {}
