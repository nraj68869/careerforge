package com.careerforge.backend.dto;

import java.util.List;

public record MatchInfo(int score, List<String> matchedSkills, List<String> missingSkills, Boolean cgpaOk) {}