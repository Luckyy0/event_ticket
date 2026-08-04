package com.example.catalog.application.port.in;

import com.example.catalog.application.command.CreateEventCommand;
import com.example.catalog.domain.model.Event;

public interface CreateEventUseCase {
    Event createEvent(CreateEventCommand command);
}
