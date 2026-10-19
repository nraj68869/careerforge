package com.careerforge.backend.dto;

import jakarta.validation.constraints.*;
public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password needs a letter and a number")
        String password) {}

