
package com.careerforge.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        Long studentId, String studentName, String studentEmail,
        String college, String degree, Integer graduationYear, BigDecimal cgpa,
        String about, String resumeUrl, List<String> skills,
        int completeness, Instant updatedAt) {}