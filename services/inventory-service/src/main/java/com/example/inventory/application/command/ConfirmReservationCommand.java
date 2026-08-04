package com.example.inventory.application.command;

import com.example.inventory.domain.model.ReservationId;

public record ConfirmReservationCommand(ReservationId reservationId) {
    public ConfirmReservationCommand {
        if (reservationId == null) throw new IllegalArgumentException("ReservationId cannot be null");
    }
}
