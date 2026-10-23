package com.careerforge.backend.dto;

import com.careerforge.backend.entity.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewRecruiterRequest(@NotNull ApprovalStatus status) {}