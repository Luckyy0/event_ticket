package com.example.inventory.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ReservationExpiration(Instant expiresAt) {

    public ReservationExpiration {
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt cannot be null");
        }
    }

    public static ReservationExpiration ofMinutesFrom(Instant startTime, long minutes) {
        if (startTime == null) {
            throw new IllegalArgumentException("StartTime cannot be null");
        }
        return new ReservationExpiration(startTime.plus(minutes, ChronoUnit.MINUTES));
    }

    public boolean isExpired(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        return now.isAfter(expiresAt) || now.equals(expiresAt);
    }

    public Duration remainingDuration(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        if (isExpired(now)) {
            return Duration.ZERO;
        }
        return Duration.between(now, expiresAt);
    }
}
