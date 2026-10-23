package com.careerforge.backend.dto;

public record ApplicationStatsResponse(
        long total, long applied, long shortlisted, long selected, long rejected, long withdrawn,
        long studentsPlaced, long openJobs) {}