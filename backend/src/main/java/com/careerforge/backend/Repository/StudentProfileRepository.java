package com.careerforge.backend.Repository;

import com.careerforge.backend.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    List<StudentProfile> findByUserIdIn(Collection<Long> userIds);

    void deleteByUserId(Long userId);
}