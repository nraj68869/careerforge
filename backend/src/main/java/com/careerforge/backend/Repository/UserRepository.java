package com.careerforge.backend.Repository;

import com.careerforge.backend.entity.ApprovalStatus;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
    long countByEnabled(boolean enabled);
    List<User> findByRoleAndApprovalStatus(Role role, ApprovalStatus status, Pageable pageable);

    @Query("select u.id from User u where u.role = :role")
    List<Long> findIdsByRole(@Param("role") Role role);
}