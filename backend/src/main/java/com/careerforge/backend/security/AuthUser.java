package com.careerforge.backend.security;

import com.careerforge.backend.entity.Role;
import java.time.Instant;
public record AuthUser(Long id, String email, Role role, String tokenId, Instant tokenExpiresAt) {
    
}
