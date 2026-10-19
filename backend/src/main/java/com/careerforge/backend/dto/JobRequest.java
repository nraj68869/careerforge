package com.careerforge.backend.dto;

import com.careerforge.backend.entity.JobType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record JobRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String location,
        @NotNull JobType type,
        @NotBlank @Size(min = 20, max = 5000, message = "Description must be 20 to 5000 characters") String description,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @NotNull @FutureOrPresent LocalDate deadline) {}