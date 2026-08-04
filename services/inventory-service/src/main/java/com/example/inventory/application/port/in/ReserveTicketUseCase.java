package com.example.inventory.application.port.in;

import com.example.inventory.application.command.ReserveTicketCommand;
import com.example.inventory.domain.model.Reservation;

public interface ReserveTicketUseCase {
    Reservation reserveTicket(ReserveTicketCommand command);
}
