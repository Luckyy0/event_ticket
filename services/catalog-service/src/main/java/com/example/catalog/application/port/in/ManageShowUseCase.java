package com.example.catalog.application.port.in;

import com.example.catalog.application.command.CreateShowCommand;
import com.example.catalog.application.command.CreateTicketTypeCommand;
import com.example.catalog.domain.model.Show;

import java.util.UUID;

public interface ManageShowUseCase {
    Show addShow(CreateShowCommand command, UUID currentUserId, boolean isAdmin);
    void addTicketType(CreateTicketTypeCommand command, UUID currentUserId, boolean isAdmin);
}
