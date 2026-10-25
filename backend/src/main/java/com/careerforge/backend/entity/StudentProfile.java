package com.careerforge.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@BatchSize(size = 50)
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // exactly one profile per student
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 150)
    private String college;

    @Column(length = 100)
    private String degree;

    private Integer graduationYear;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;                 // 0.00 to 10.00

    @Column(length = 500)
    private String about;

    @Column(length = 500)
    private String resumeUrl;

    @Column(length = 64)
    private String resumeFile;

    @Column(length = 120)
    private String resumeOriginalName; 

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill", length = 40, nullable = false)
    private Set<String> skills = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}