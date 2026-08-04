package com.example.catalog.adapter.in.web.dto;

import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.EventStatus;
import com.example.catalog.domain.model.Show;
import com.example.catalog.domain.model.TicketType;
import com.example.catalog.domain.model.Venue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record EventResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        UUID organizerId,
        VenueResponse venue,
        EventStatus status,
        List<ShowResponse> shows,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse fromDomain(Event event) {
        if (event == null) return null;
        VenueResponse venueRes = event.getVenue() != null
                ? new VenueResponse(event.getVenue().getName(), event.getVenue().getAddress(), event.getVenue().getCity(), event.getVenue().getCapacity())
                : null;

        List<ShowResponse> showResponses = event.getShows().stream()
                .map(ShowResponse::fromDomain)
                .collect(Collectors.toList());

        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getImageUrl(),
                event.getOrganizerId(),
                venueRes,
                event.getStatus(),
                showResponses,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
