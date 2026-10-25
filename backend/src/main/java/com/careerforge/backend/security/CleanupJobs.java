package com.careerforge.backend.security;

import com.careerforge.backend.Repository.NotificationRepository;
import com.careerforge.backend.Repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Component
public class CleanupJobs {
    private static final Logger log = LoggerFactory.getLogger(CleanupJobs.class);

    private final RevokedTokenRepository revokedTokenRepository;
    private final LoginAttemptService loginAttemptService;
    private final NotificationRepository notificationRepository;

    public CleanupJobs(RevokedTokenRepository revokedTokenRepository, LoginAttemptService loginAttemptService,
                       NotificationRepository notificationRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.loginAttemptService = loginAttemptService;
        this.notificationRepository = notificationRepository;
    }

    // runs at startup, then every hour
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void purge() {
        long tokens = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        loginAttemptService.purgeExpired();
        int notifications = notificationRepository.deleteOlderThan(Instant.now().minus(90, ChronoUnit.DAYS));
        if (tokens > 0 || notifications > 0) {
            log.info("Cleanup: removed {} expired tokens and {} old notifications", tokens, notifications);
        }
    }
}