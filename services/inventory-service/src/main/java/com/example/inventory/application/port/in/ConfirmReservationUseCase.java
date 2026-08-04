package com.example.inventory.application.port.in;

import com.example.inventory.application.command.ConfirmReservationCommand;

public interface ConfirmReservationUseCase {
    void confirmReservation(ConfirmReservationCommand command);
}
