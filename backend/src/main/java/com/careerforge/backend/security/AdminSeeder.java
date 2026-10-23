package com.careerforge.backend.security;

import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Order(2)
@Component
public class AdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties admin;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminProperties admin) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.admin = admin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;                                   // an admin already exists, nothing to do
        }
        String email = admin.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("ADMIN_EMAIL (" + email
                    + ") already belongs to a non-admin account. Put a different ADMIN_EMAIL in .env.");
        }
        userRepository.save(new User(admin.name().trim(), email,
                passwordEncoder.encode(admin.password()), Role.ADMIN));
        log.info("Initial admin account created: {}", email);
    }
}