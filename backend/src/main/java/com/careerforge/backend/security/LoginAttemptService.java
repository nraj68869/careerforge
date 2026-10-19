package com.careerforge.backend.security;

import com.careerforge.backend.exception.TooManyAttemptsException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private record Entry(int failures, Instant windowStart, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    /** Throws if this key is currently locked. */
    public void checkAllowed(String key) {
        Entry entry = attempts.get(key);
        Instant now = Instant.now();
        if (entry != null && entry.lockedUntil() != null && now.isBefore(entry.lockedUntil())) {
            long minutes = Duration.between(now, entry.lockedUntil()).toMinutes() + 1;
            throw new TooManyAttemptsException(minutes);
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, entry) -> {
            if (entry == null || now.isAfter(entry.windowStart().plus(WINDOW))) {
                return new Entry(1, now, null);                       // start a fresh counting window
            }
            int failures = entry.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_TIME) : null;
            return new Entry(failures, entry.windowStart(), lockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    /** Called by the cleanup job so the map does not grow forever. */
    public void purgeExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(e ->
                now.isAfter(e.getValue().windowStart().plus(WINDOW))
                        && (e.getValue().lockedUntil() == null || now.isAfter(e.getValue().lockedUntil())));
    }
}
