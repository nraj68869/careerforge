package com.careerforge.backend.dto;

import com.careerforge.backend.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(@NotNull ApplicationStatus status) {
    
}
