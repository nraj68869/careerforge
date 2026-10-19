package com.careerforge.backend.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated 
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties (
    @NotBlank 
    @Size(min = 43, message = "JWT secret must be based of at least 32 random bytes")
    String secret,
    @NotBlank String issuer,
    @Positive long expirationMinutes){

    }

