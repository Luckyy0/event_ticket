package com.example.inventory.domain.event;

import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;
import com.example.inventory.domain.model.UserId;

import java.time.Instant;

public record InventoryReservationRejectedEvent(
        TicketTypeId ticketTypeId,
        ShowId showId,
        UserId userId,
        int requestedQuantity,
        int availableQuantity,
        String reason,
        Instant occurredAt
) {}
