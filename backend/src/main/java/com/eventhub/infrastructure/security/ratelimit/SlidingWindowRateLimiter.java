package com.eventhub.infrastructure.security.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe Sliding Window Rate Limiter using epoch millisecond tracking.
 * Accurately bounds requests over rolling temporal windows.
 */
@Component
public class SlidingWindowRateLimiter {

    private final Map<String, ConcurrentLinkedDeque<Long>> requestWindows = new ConcurrentHashMap<>();

    public record RateLimitResult(boolean isAllowed, long retryAfterSeconds) {}

    /**
     * Attempts to acquire execution permission for the specified key within the rolling window.
     *
     * @param key unique identifier (e.g. client IP + endpoint tier)
     * @param maxRequests maximum permitted requests in window
     * @param windowMillis rolling window duration in milliseconds
     * @return RateLimitResult indicating whether request is allowed and seconds until reset
     */
    public RateLimitResult tryAcquire(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        ConcurrentLinkedDeque<Long> timestamps = requestWindows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // Evict expired timestamps outside the rolling window
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return new RateLimitResult(true, 0);
            } else {
                Long oldest = timestamps.peekFirst();
                long waitMillis = (oldest != null) ? (oldest + windowMillis - now) : windowMillis;
                long retryAfterSec = Math.max(1, (waitMillis + 999) / 1000);
                return new RateLimitResult(false, retryAfterSec);
            }
        }
    }

    /**
     * Clears internal state (useful for test resets).
     */
    public void reset() {
        requestWindows.clear();
    }
}
