package com.careerforge.backend.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, message = "ADMIN_PASSWORD must be at least 10 characters") String password) {
}