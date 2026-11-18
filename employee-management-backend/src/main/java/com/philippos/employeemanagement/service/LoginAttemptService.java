package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.exception.LoginRateLimitException;
import com.philippos.employeemanagement.util.AccountValidation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Throttles repeated failed logins per username and client address.
 * State is kept in memory, which is sufficient for a single backend instance.
 */
@Service
public class LoginAttemptService {
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.login-rate-limit.lock-duration:15m}") Duration lockDuration,
            Clock clock) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Login maximum attempts must be greater than zero");
        }
        if (lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("Login lock duration must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public String keyFor(String username, String clientAddress) {
        return AccountValidation.normalizeUsername(username) + "|" + clientAddress;
    }

    public void checkAllowed(String key) {
        AttemptState state = attempts.get(key);
        if (state == null || state.lockedUntil() == null) {
            return;
        }
        Instant now = clock.instant();
        if (now.isBefore(state.lockedUntil())) {
            long retryAfterSeconds = Math.max(1,
                    Duration.between(now, state.lockedUntil()).toSeconds());
            throw new LoginRateLimitException(
                    "Too many failed login attempts. Please try again later.", retryAfterSeconds);
        }
        attempts.remove(key, state);
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        attempts.compute(key, (ignored, state) -> {
            AttemptState current = state == null || state.isExpired(now, lockDuration)
                    ? new AttemptState(0, now, null)
                    : state;
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new AttemptState(failures, current.windowStart(), lockedUntil);
        });
        if (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.values().removeIf(state -> state.isExpired(now, lockDuration));
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private record AttemptState(int failures, Instant windowStart, Instant lockedUntil) {
        boolean isExpired(Instant now, Duration window) {
            if (lockedUntil != null) {
                return !now.isBefore(lockedUntil);
            }
            return !now.isBefore(windowStart.plus(window));
        }
    }
}
