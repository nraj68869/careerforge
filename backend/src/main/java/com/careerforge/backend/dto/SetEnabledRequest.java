package com.careerforge.backend.dto;

import jakarta.validation.constraints.NotNull;
public record SetEnabledRequest(@NotNull Boolean enabled) {
}