package com.eventhub.infrastructure.security.iam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing failed authentication attempts and brute-force account lockout.
 * Enforces account lockout for 15 minutes after 5 consecutive failed attempts.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPT = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L; // 15 minutes

    private static class AttemptRecord {
        int attempts;
        long lockedUntil;

        AttemptRecord(int attempts, long lockedUntil) {
            this.attempts = attempts;
            this.lockedUntil = lockedUntil;
        }
    }

    private final Map<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    public synchronized void loginSucceeded(String key) {
        attemptsCache.remove(key.toLowerCase());
        log.info("[IAM-AUDIT] Authentication succeeded for user: {}", key);
    }

    public synchronized void loginFailed(String key) {
        String normalizedKey = key.toLowerCase();
        long now = System.currentTimeMillis();

        AttemptRecord record = attemptsCache.get(normalizedKey);
        if (record == null || now > record.lockedUntil && record.lockedUntil > 0) {
            record = new AttemptRecord(1, 0);
        } else {
            record.attempts++;
        }

        if (record.attempts >= MAX_ATTEMPT) {
            record.lockedUntil = now + LOCK_DURATION_MS;
            log.warn("[IAM-ALERT] Account '{}' is LOCKED due to {} consecutive failed attempts. Locked for 15 minutes.",
                    normalizedKey, record.attempts);
        } else {
            log.warn("[IAM-AUDIT] Failed login attempt {}/{} for user '{}'", record.attempts, MAX_ATTEMPT, normalizedKey);
        }

        attemptsCache.put(normalizedKey, record);
    }

    public synchronized boolean isLocked(String key) {
        String normalizedKey = key.toLowerCase();
        AttemptRecord record = attemptsCache.get(normalizedKey);
        if (record == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (record.lockedUntil > 0) {
            if (now < record.lockedUntil) {
                return true;
            } else {
                // Lock expired
                attemptsCache.remove(normalizedKey);
                return false;
            }
        }
        return false;
    }

    public synchronized long getRemainingLockoutSeconds(String key) {
        String normalizedKey = key.toLowerCase();
        AttemptRecord record = attemptsCache.get(normalizedKey);
        if (record == null || record.lockedUntil == 0) {
            return 0;
        }
        long now = System.currentTimeMillis();
        if (now < record.lockedUntil) {
            return Math.max(1, (record.lockedUntil - now + 999) / 1000);
        }
        return 0;
    }

    public void reset() {
        attemptsCache.clear();
    }
}
