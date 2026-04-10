package org.urlshortener.urlshortener.filter.ratelimit;

import java.util.Map;

/**
 * Résultat d'une tentative de consommation de rate limit.
 * Immutable value object.
 */
public record RateLimitResult(
        boolean allowed,
        int limit,
        long remaining,
        long resetTime,
        int retryAfterSeconds
) {

    public static RateLimitResult success() {
        return new RateLimitResult(true, 0, 0, 0, 0);
    }

    public static RateLimitResult success(int limit, long remaining, long resetTime) {
        return new RateLimitResult(true, limit, remaining, resetTime, 0);
    }

    public static RateLimitResult exceeded(int limit, long remaining, long resetTime, int retryAfter) {
        return new RateLimitResult(false, limit, remaining, resetTime, retryAfter);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isLimited() {
        return !allowed;
    }

    public Map<String, String> toHeaders() {
        return Map.of(
                "X-RateLimit-Limit", String.valueOf(limit),
                "X-RateLimit-Remaining", String.valueOf(remaining),
                "X-RateLimit-Reset", String.valueOf(resetTime)
        );
    }
}

