package com.careerforge.backend.dto;

import com.careerforge.backend.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id, NotificationType type, String title, String message, String route,
        boolean read, Instant createdAt) {}