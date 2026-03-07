package com.ron.javainfohunter.api.exception;

/**
 * Exception thrown when rate limit is exceeded
 *
 * Used by @RateLimit annotation to signal that a request has exceeded
 * the configured rate limit threshold.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final int limit;
    private final long windowSeconds;

    public RateLimitExceededException(String key, int limit, long windowSeconds) {
        super(String.format("Rate limit exceeded for key '%s': %d requests per %ds",
            key, limit, windowSeconds));
        this.key = key;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    public String getKey() {
        return key;
    }

    public int getLimit() {
        return limit;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
