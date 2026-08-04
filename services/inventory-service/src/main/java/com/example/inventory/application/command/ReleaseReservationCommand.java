package com.example.inventory.application.command;

import com.example.inventory.domain.model.ReservationId;

public record ReleaseReservationCommand(ReservationId reservationId) {
    public ReleaseReservationCommand {
        if (reservationId == null) throw new IllegalArgumentException("ReservationId cannot be null");
    }
}
