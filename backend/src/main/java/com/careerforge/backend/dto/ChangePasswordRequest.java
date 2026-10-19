package com.careerforge.backend.dto;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password needs a letter and a number")
        String newPassword) {}
