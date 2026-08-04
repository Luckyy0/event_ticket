package com.example.catalog.application.usecase;

import com.example.catalog.application.command.CreateEventCommand;
import com.example.catalog.application.port.in.CreateEventUseCase;
import com.example.catalog.application.port.out.EventRepositoryPort;
import com.example.catalog.domain.model.Event;
import org.springframework.stereotype.Service;

@Service
public class CreateEventUseCaseImpl implements CreateEventUseCase {

    private final EventRepositoryPort eventRepository;

    public CreateEventUseCaseImpl(EventRepositoryPort eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Event createEvent(CreateEventCommand command) {
        Event event = Event.create(
                command.name(),
                command.description(),
                command.imageUrl(),
                command.organizerId(),
                command.venue()
        );
        return eventRepository.save(event);
    }
}
