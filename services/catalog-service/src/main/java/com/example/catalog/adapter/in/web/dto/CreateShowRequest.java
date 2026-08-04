package com.example.catalog.adapter.in.web.dto;

import com.example.catalog.domain.model.SaleWindow;
import com.example.catalog.domain.model.TicketType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreateShowRequest(
        @NotNull(message = "Show start time is required")
        Instant startTime,

        @NotNull(message = "Show end time is required")
        Instant endTime,

        SaleWindowDto saleWindow,

        List<CreateTicketTypeDto> ticketTypes
) {
    public record SaleWindowDto(
            Instant opensAt,
            Instant closesAt
    ) {
        public SaleWindow toDomain() {
            if (opensAt == null || closesAt == null) return null;
            return new SaleWindow(opensAt, closesAt);
        }
    }

    public record CreateTicketTypeDto(
            String name,
            String description,
            BigDecimal price,
            String currency,
            int totalQuantity,
            int sortOrder
    ) {
        public TicketType toDomain() {
            return new TicketType(UUID.randomUUID(), name, description, price, currency, totalQuantity, sortOrder);
        }
    }

    public List<TicketType> toDomainTicketTypes() {
        if (ticketTypes == null) return new ArrayList<>();
        return ticketTypes.stream().map(CreateTicketTypeDto::toDomain).collect(Collectors.toList());
    }
}
