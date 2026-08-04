package com.example.inventory.domain.exception;

import com.example.inventory.domain.model.ReservationId;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(ReservationId id) {
        super("Reservation not found with ID: " + id);
    }
    public ReservationNotFoundException(String message) {
        super(message);
    }
}
