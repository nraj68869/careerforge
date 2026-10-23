package com.careerforge.backend.dto;

import com.careerforge.backend.entity.ApplicationStatus;
import com.careerforge.backend.entity.JobStatus;
import com.careerforge.backend.entity.JobType;

import java.time.Instant;
import java.time.LocalDate;
public record JobResponse(
     Long id, String title, String company, String location, JobType type, String description,
        Integer salaryMin, Integer salaryMax, LocalDate deadline, JobStatus status,
        boolean acceptingApplications, Long postedById, String postedByName, Instant createdAt, ApplicationStatus myApplicationStatus
) {
} 
