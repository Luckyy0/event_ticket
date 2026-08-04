package com.example.catalog.adapter.in.web;

import com.example.catalog.adapter.in.web.dto.*;
import com.example.catalog.application.command.CreateEventCommand;
import com.example.catalog.application.command.CreateShowCommand;
import com.example.catalog.application.command.CreateTicketTypeCommand;
import com.example.catalog.application.command.UpdateEventCommand;
import com.example.catalog.application.port.in.CreateEventUseCase;
import com.example.catalog.application.port.in.ManageEventUseCase;
import com.example.catalog.application.port.in.ManageShowUseCase;
import com.example.catalog.bootstrap.config.SecurityUtils;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.Show;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class OrganizerEventController {

    private final CreateEventUseCase createEventUseCase;
    private final ManageEventUseCase manageEventUseCase;
    private final ManageShowUseCase manageShowUseCase;

    public OrganizerEventController(CreateEventUseCase createEventUseCase, ManageEventUseCase manageEventUseCase, ManageShowUseCase manageShowUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.manageEventUseCase = manageEventUseCase;
        this.manageShowUseCase = manageShowUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        CreateEventCommand command = new CreateEventCommand(
                request.name(),
                request.description(),
                request.imageUrl(),
                currentUserId,
                request.venue().toDomain()
        );
        Event created = createEventUseCase.createEvent(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.fromDomain(created));
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        UpdateEventCommand command = new UpdateEventCommand(
                eventId,
                request.name(),
                request.description(),
                request.imageUrl(),
                currentUserId,
                request.venue().toDomain(),
                isAdmin
        );
        Event updated = manageEventUseCase.updateEvent(command);
        return ResponseEntity.ok(EventResponse.fromDomain(updated));
    }

    @PostMapping("/{eventId}/publish")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> publishEvent(@PathVariable UUID eventId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        manageEventUseCase.publishEvent(eventId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> cancelEvent(@PathVariable UUID eventId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        manageEventUseCase.cancelEvent(eventId, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/shows")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<ShowResponse> addShow(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateShowRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        CreateShowCommand command = new CreateShowCommand(
                eventId,
                request.startTime(),
                request.endTime(),
                request.saleWindow() != null ? request.saleWindow().toDomain() : null,
                request.toDomainTicketTypes()
        );
        Show createdShow = manageShowUseCase.addShow(command, currentUserId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShowResponse.fromDomain(createdShow));
    }

    @PostMapping("/{eventId}/shows/{showId}/ticket-types")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> addTicketType(
            @PathVariable UUID eventId,
            @PathVariable UUID showId,
            @Valid @RequestBody CreateTicketTypeRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        CreateTicketTypeCommand command = new CreateTicketTypeCommand(
                eventId,
                showId,
                request.name(),
                request.description(),
                request.price(),
                request.currency(),
                request.totalQuantity(),
                request.sortOrder()
        );
        manageShowUseCase.addTicketType(command, currentUserId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
