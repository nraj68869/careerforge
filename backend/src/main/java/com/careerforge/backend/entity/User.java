package com.careerforge.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "users")

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, length = 100)
    private String passwordHash;
    
    @Column(nullable = false)
    private boolean enabled = true;

    @Column 
    private Instant passwordChangedAt;

    public User() {
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public Instant getPasswordChangedAt(){
         return passwordChangedAt;
      }
    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(Role role) {
        this.role = role;
    }
    public String getPasswordHash() {
         return passwordHash; 
        }
    public void setPasswordHash(String passwordHash) { 
        this.passwordHash = passwordHash; 
    }
    public boolean isEnabled() {
     return enabled; 
    }
    public void setEnabled(boolean enabled) {
     this.enabled = enabled; 
    }
    public void setPasswordChangedAt(Instant passwordChangedAt){
        this.passwordChangedAt = passwordChangedAt;
    }
    
}
