// ProfileRequest.java
package com.careerforge.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProfileRequest(
        @Size(max = 150) String college,
        @Size(max = 100) String degree,
        @Min(value = 2000, message = "Graduation year looks wrong")
        @Max(value = 2100, message = "Graduation year looks wrong") Integer graduationYear,
        @DecimalMin(value = "0.0", message = "CGPA cannot be negative")
        @DecimalMax(value = "10.0", message = "CGPA cannot be above 10")
        @Digits(integer = 2, fraction = 2, message = "CGPA can have at most 2 decimal places") BigDecimal cgpa,
        @Size(max = 500, message = "About must be at most 500 characters") String about,
        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Resume link must start with http:// or https://")
        String resumeUrl,
        @Size(max = 20, message = "At most 20 skills") List<@Size(max = 40, message = "A skill can have at most 40 characters") String> skills) {}