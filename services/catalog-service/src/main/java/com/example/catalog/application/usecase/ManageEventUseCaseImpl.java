package com.example.catalog.application.usecase;

import com.example.catalog.application.command.UpdateEventCommand;
import com.example.catalog.application.port.in.ManageEventUseCase;
import com.example.catalog.application.port.out.EventRepositoryPort;
import com.example.catalog.domain.exception.EventNotFoundException;
import com.example.catalog.domain.model.Event;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ManageEventUseCaseImpl implements ManageEventUseCase {

    private final EventRepositoryPort eventRepository;

    public ManageEventUseCaseImpl(EventRepositoryPort eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Event updateEvent(UpdateEventCommand command) {
        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        checkOwnershipOrAdmin(event, command.organizerId(), command.isAdmin());

        event.updateDetails(command.name(), command.description(), command.imageUrl(), command.venue());
        return eventRepository.save(event);
    }

    @Override
    public void publishEvent(UUID eventId, UUID currentUserId, boolean isAdmin) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        checkOwnershipOrAdmin(event, currentUserId, isAdmin);

        event.publish();
        eventRepository.save(event);
    }

    @Override
    public void cancelEvent(UUID eventId, UUID currentUserId, boolean isAdmin) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        checkOwnershipOrAdmin(event, currentUserId, isAdmin);

        event.cancel();
        eventRepository.save(event);
    }

    private void checkOwnershipOrAdmin(Event event, UUID currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (currentUserId == null || !event.getOrganizerId().equals(currentUserId)) {
            throw new AccessDeniedException("Access Denied: Current user is not the organizer of this event");
        }
    }
}
