package com.careerforge.backend.dto;

import com.careerforge.backend.entity.JobType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JobRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String location,
        @NotNull JobType type,
        @NotBlank @Size(min = 20, max = 5000, message = "Description must be 20 to 5000 characters") String description,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @NotNull @FutureOrPresent LocalDate deadline,
        @Size(max = 15, message = "At most 15 required skills")
        List<@Size(max = 40, message = "A skill can have at most 40 characters") String> requiredSkills,
        @DecimalMin(value = "0.0", message = "Minimum CGPA cannot be negative")
        @DecimalMax(value = "10.0", message = "Minimum CGPA cannot be above 10")
        @Digits(integer = 2, fraction = 2, message = "CGPA can have at most 2 decimal places") BigDecimal minCgpa) {}