package com.example.catalog.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTicketTypeCommand(
        UUID eventId,
        UUID showId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int totalQuantity,
        int sortOrder
) {}
