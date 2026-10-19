package com.careerforge.backend.Repository;

import com.careerforge.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    // load the poster in the SAME query (otherwise one extra query per job row)
    @Override
    @EntityGraph(attributePaths = "postedBy")
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);

    @EntityGraph(attributePaths = "postedBy")
    Optional<Job> findWithPosterById(Long id);

    boolean existsByPostedById(Long userId);
}