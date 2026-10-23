package com.careerforge.backend.security;

import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.entity.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)   // runs before AdminSeeder
public class SingleAdminGuard implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SingleAdminGuard.class);

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SingleAdminGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long admins = userRepository.countByRole(Role.ADMIN);
        if (admins > 1) {
            throw new IllegalStateException("Found " + admins + " ADMIN accounts, but CareerForge allows exactly one. "
                    + "Run  SELECT id, email FROM users WHERE role = 'ADMIN';  then keep one and demote the rest with  "
                    + "UPDATE users SET role = 'STUDENT' WHERE role = 'ADMIN' AND id <> <ID_TO_KEEP>;  and restart.");
        }

        // PostgreSQL "partial unique index": at most ONE row may have role = 'ADMIN'
        entityManager.createNativeQuery(
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_single_admin ON users (role) WHERE role = 'ADMIN'")
                .executeUpdate();
        log.info("Single-admin rule is enforced by the database");
    }
}