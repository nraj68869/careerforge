package com.careerforge.backend.Repository;

import com.careerforge.backend.entity.ApplicationStatus;
import com.careerforge.backend.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    // load job, poster and student in the same query (avoids one extra query per row)
    @Override
    @EntityGraph(attributePaths = {"job", "job.postedBy", "student"})
    Page<JobApplication> findAll(Specification<JobApplication> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.postedBy", "student"})
    Optional<JobApplication> findWithDetailsById(Long id);

    Optional<JobApplication> findByJobIdAndStudentId(Long jobId, Long studentId);

    List<JobApplication> findByStudentIdAndJobIdIn(Long studentId, Collection<Long> jobIds);

    boolean existsByJobId(Long jobId);

    boolean existsByStudentId(Long studentId);

    long countByStatus(ApplicationStatus status);

    @Query("select count(distinct a.student.id) from JobApplication a where a.status = :status")
    long countDistinctStudentsByStatus(@Param("status") ApplicationStatus status);

    @Query("select count(a) > 0 from JobApplication a "
                + "where a.student.id = :studentId and a.job.postedBy.id = :recruiterId "
                + "and a.status <> com.careerforge.backend.entity.ApplicationStatus.WITHDRAWN")
    boolean recruiterMayViewStudent(@Param("studentId") Long studentId, @Param("recruiterId") Long recruiterId);
}