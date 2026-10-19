package com.careerforge.backend.Repository;

import com.careerforge.backend.entity.RevokedToken;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String>{
    long deleteByExpiresAtBefore(Instant time);
}
