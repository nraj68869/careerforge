package com.careerforge.backend.dto;

import jakarta.validation.constraints.Size;

public record ApplyRequest(
        @Size(max = 1000, message = "Cover note must be at most 1000 characters") String coverNote) {}