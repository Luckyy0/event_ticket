package com.example.inventory.domain.exception;

public class ReservationAlreadyConfirmedException extends RuntimeException {
    public ReservationAlreadyConfirmedException(String message) {
        super(message);
    }
}
