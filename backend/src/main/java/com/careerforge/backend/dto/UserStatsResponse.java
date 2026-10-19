package com.careerforge.backend.dto;

public record UserStatsResponse (long total, long students, long recruiters,
        long placementOfficers, long admins, long disabled){
    
}
