package com.example.catalog.adapter.in.web.dto;

import com.example.catalog.domain.model.Show;
import com.example.catalog.domain.model.ShowStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ShowResponse(
        UUID id,
        UUID eventId,
        Instant startTime,
        Instant endTime,
        ShowStatus status,
        SaleWindowResponse saleWindow,
        List<TicketTypeResponse> ticketTypes
) {
    public static ShowResponse fromDomain(Show show) {
        if (show == null) return null;
        SaleWindowResponse saleWindowRes = show.getSaleWindow() != null
                ? new SaleWindowResponse(show.getSaleWindow().getOpensAt(), show.getSaleWindow().getClosesAt())
                : null;

        List<TicketTypeResponse> ticketResponses = show.getTicketTypes().stream()
                .map(TicketTypeResponse::fromDomain)
                .collect(Collectors.toList());

        return new ShowResponse(
                show.getId(),
                show.getEventId(),
                show.getStartTime(),
                show.getEndTime(),
                show.getStatus(),
                saleWindowRes,
                ticketResponses
        );
    }
}
