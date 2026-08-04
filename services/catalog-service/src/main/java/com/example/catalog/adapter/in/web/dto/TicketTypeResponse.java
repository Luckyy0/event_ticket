package com.example.catalog.adapter.in.web.dto;

import com.example.catalog.domain.model.TicketType;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketTypeResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int totalQuantity,
        int sortOrder
) {
    public static TicketTypeResponse fromDomain(TicketType ticketType) {
        if (ticketType == null) return null;
        return new TicketTypeResponse(
                ticketType.getId(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                ticketType.getTotalQuantity(),
                ticketType.getSortOrder()
        );
    }
}
