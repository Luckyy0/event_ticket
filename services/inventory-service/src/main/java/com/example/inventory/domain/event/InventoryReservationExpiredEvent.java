package com.example.inventory.domain.event;

import com.example.inventory.domain.model.ReservationId;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;
import com.example.inventory.domain.model.UserId;

import java.time.Instant;

public record InventoryReservationExpiredEvent(
        ReservationId reservationId,
        TicketTypeId ticketTypeId,
        ShowId showId,
        UserId userId,
        int quantity,
        Instant expiredAt,
        Instant occurredAt
) {}
