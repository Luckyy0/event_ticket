package com.example.inventory.domain.model;

import java.util.UUID;

public record ReservationId(UUID value) {
    public ReservationId {
        if (value == null) throw new IllegalArgumentException("Reservation ID cannot be null");
    }
    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID());
    }
    @Override
    public String toString() { return value.toString(); }
}
