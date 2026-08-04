package com.example.inventory.application.port.in;

import com.example.inventory.application.command.ReleaseReservationCommand;

public interface ReleaseReservationUseCase {
    void releaseReservation(ReleaseReservationCommand command);
}
