package com.careerforge.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType type;

    @Column(nullable = false, length = 5000)
    private String description;

    private Integer salaryMin;      // yearly, optional
    private Integer salaryMax;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JobStatus status = JobStatus.OPEN;

    // a real database link to the user who posted it
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posted_by_id", nullable = false)
    private User postedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", length = 40, nullable = false)
    @BatchSize(size = 50)
    private Set<String> requiredSkills = new LinkedHashSet<>();

    @Column(precision = 4, scale = 2)
    private BigDecimal minCgpa;
}
