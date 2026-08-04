package com.example.catalog.application.usecase;

import com.example.catalog.application.command.CreateShowCommand;
import com.example.catalog.application.command.CreateTicketTypeCommand;
import com.example.catalog.application.port.in.ManageShowUseCase;
import com.example.catalog.application.port.out.EventRepositoryPort;
import com.example.catalog.domain.exception.EventNotFoundException;
import com.example.catalog.domain.exception.ShowNotFoundException;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.Show;
import com.example.catalog.domain.model.TicketType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ManageShowUseCaseImpl implements ManageShowUseCase {

    private final EventRepositoryPort eventRepository;

    public ManageShowUseCaseImpl(EventRepositoryPort eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Show addShow(CreateShowCommand command, UUID currentUserId, boolean isAdmin) {
        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        checkOwnershipOrAdmin(event, currentUserId, isAdmin);

        Show show = Show.create(
                command.eventId(),
                command.startTime(),
                command.endTime(),
                command.saleWindow(),
                command.ticketTypes()
        );

        event.addShow(show);
        eventRepository.save(event);
        return show;
    }

    @Override
    public void addTicketType(CreateTicketTypeCommand command, UUID currentUserId, boolean isAdmin) {
        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        checkOwnershipOrAdmin(event, currentUserId, isAdmin);

        Show targetShow = event.getShows().stream()
                .filter(s -> s.getId().equals(command.showId()))
                .findFirst()
                .orElseThrow(() -> new ShowNotFoundException(command.showId()));

        TicketType ticketType = new TicketType(
                UUID.randomUUID(),
                command.name(),
                command.description(),
                command.price(),
                command.currency(),
                command.totalQuantity(),
                command.sortOrder()
        );

        targetShow.addTicketType(ticketType);
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
