package com.careerforge.backend.dto;

import com.careerforge.backend.entity.ApplicationStatus;
import com.careerforge.backend.entity.JobStatus;

import java.time.Instant;
import java.util.List;

public record ApplicationResponse(
        Long id, Long jobId, String jobTitle, String company, JobStatus jobStatus,
        Long studentId, String studentName, String studentEmail,
        ApplicationStatus status, String coverNote, Instant appliedAt, Instant updatedAt,
        List<ApplicationStatus> nextStatuses,   // what a recruiter/officer may move it to
        boolean canWithdraw) {}                 // whether the student may withdraw