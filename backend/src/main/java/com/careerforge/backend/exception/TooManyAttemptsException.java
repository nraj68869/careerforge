package com.careerforge.backend.exception;

public class TooManyAttemptsException extends RuntimeException {
    private final long retryAfterMinutes;

    public TooManyAttemptsException(long retryAfterMinutes) {
        super("Too many failed attempts. Try again in " + retryAfterMinutes + " minute(s).");
        this.retryAfterMinutes = retryAfterMinutes;
    }

    public long getRetryAfterMinutes() { return retryAfterMinutes; }
}