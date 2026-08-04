package com.example.inventory.adapter.in.web.dto;

import com.example.inventory.domain.model.Reservation;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        UUID ticketTypeId,
        UUID showId,
        UUID userId,
        UUID requestId,
        int quantity,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant confirmedAt,
        Instant releasedAt
) {
    public static ReservationResponse fromDomain(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId().value(),
                reservation.getTicketTypeId().value(),
                reservation.getShowId().value(),
                reservation.getUserId().value(),
                reservation.getRequestId().value(),
                reservation.getQuantity(),
                reservation.getStatus().name(),
                reservation.getExpiration().expiresAt(),
                reservation.getCreatedAt(),
                reservation.getConfirmedAt(),
                reservation.getReleasedAt()
        );
    }
}
