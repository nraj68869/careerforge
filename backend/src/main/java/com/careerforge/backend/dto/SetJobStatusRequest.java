package com.careerforge.backend.dto;

import jakarta.validation.constraints.NotNull;
import com.careerforge.backend.entity.JobStatus;
public record SetJobStatusRequest(@NotNull JobStatus status) {
}
