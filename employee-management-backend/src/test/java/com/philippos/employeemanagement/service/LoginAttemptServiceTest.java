package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.exception.LoginRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private static final Instant START = Instant.parse("2026-01-15T10:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final LoginAttemptService service =
            new LoginAttemptService(3, Duration.ofMinutes(15), clock);

    @Test
    void keyNormalizesUsernameAndIncludesClientAddress() {
        assertEquals("admin|10.0.0.1", service.keyFor(" ADMIN ", "10.0.0.1"));
    }

    @Test
    void locksAfterMaximumFailedAttempts() {
        String key = service.keyFor("admin", "10.0.0.1");
        service.recordFailure(key);
        service.recordFailure(key);
        assertDoesNotThrow(() -> service.checkAllowed(key));

        service.recordFailure(key);

        LoginRateLimitException exception =
                assertThrows(LoginRateLimitException.class, () -> service.checkAllowed(key));
        assertEquals(900, exception.getRetryAfterSeconds());
    }

    @Test
    void lockExpiresAfterLockDuration() {
        String key = service.keyFor("admin", "10.0.0.1");
        for (int attempt = 0; attempt < 3; attempt++) {
            service.recordFailure(key);
        }

        clock.advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> service.checkAllowed(key));
        service.recordFailure(key);
        assertDoesNotThrow(() -> service.checkAllowed(key));
    }

    @Test
    void failuresOutsideWindowDoNotAccumulate() {
        String key = service.keyFor("admin", "10.0.0.1");
        service.recordFailure(key);
        service.recordFailure(key);

        clock.advance(Duration.ofMinutes(16));
        service.recordFailure(key);

        assertDoesNotThrow(() -> service.checkAllowed(key));
    }

    @Test
    void successClearsFailures() {
        String key = service.keyFor("admin", "10.0.0.1");
        service.recordFailure(key);
        service.recordFailure(key);
        service.recordSuccess(key);
        service.recordFailure(key);
        service.recordFailure(key);

        assertDoesNotThrow(() -> service.checkAllowed(key));
    }

    @Test
    void differentClientAddressesAreTrackedSeparately() {
        String blocked = service.keyFor("admin", "10.0.0.1");
        for (int attempt = 0; attempt < 3; attempt++) {
            service.recordFailure(blocked);
        }

        assertThrows(LoginRateLimitException.class, () -> service.checkAllowed(blocked));
        assertDoesNotThrow(() -> service.checkAllowed(service.keyFor("admin", "10.0.0.2")));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new LoginAttemptService(0, Duration.ofMinutes(1), clock));
        assertThrows(IllegalArgumentException.class,
                () -> new LoginAttemptService(3, Duration.ZERO, clock));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
