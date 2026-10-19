package com.careerforge.backend.security;

import com.careerforge.backend.Repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class CleanupJobs {
    private static final Logger log = LoggerFactory.getLogger(CleanupJobs.class);

    private final RevokedTokenRepository revokedTokenRepository;
    private final LoginAttemptService loginAttemptService;

    public CleanupJobs(RevokedTokenRepository revokedTokenRepository, LoginAttemptService loginAttemptService) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.loginAttemptService = loginAttemptService;
    }

    // runs at startup, then every hour
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void purge() {
        // a token that has expired is dead anyway, so its "cancelled" entry is no longer needed
        long removed = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        loginAttemptService.purgeExpired();
        if (removed > 0) {
            log.info("Cleanup: removed {} expired revoked-token entries", removed);
        }
    }
}