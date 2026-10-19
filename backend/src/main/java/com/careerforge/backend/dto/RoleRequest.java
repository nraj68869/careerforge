package com.careerforge.backend.dto;

import com.careerforge.backend.entity.Role;
import jakarta.validation.constraints.NotNull;

public  record RoleRequest(@NotNull Role role) {
}
