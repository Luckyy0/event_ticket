package com.example.catalog.application.port.in;

import com.example.catalog.application.command.UpdateEventCommand;
import com.example.catalog.domain.model.Event;

import java.util.UUID;

public interface ManageEventUseCase {
    Event updateEvent(UpdateEventCommand command);
    void publishEvent(UUID eventId, UUID currentUserId, boolean isAdmin);
    void cancelEvent(UUID eventId, UUID currentUserId, boolean isAdmin);
}
