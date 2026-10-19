package com.careerforge.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity 
@Table(name = "revoked_tokens")
public class RevokedToken {
    @Id 
    @Column(length = 64)
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RevokedToken(){}

    public RevokedToken(String jti, Instant expiresAt){
        this.jti = jti;
        this.expiresAt = expiresAt;
    }
    public String getJti(){
        return jti;
    }
    public Instant getExpiresAt(){
        return expiresAt;
    }
}
