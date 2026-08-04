package com.example.catalog.application.command;

import com.example.catalog.domain.model.SaleWindow;
import com.example.catalog.domain.model.TicketType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateShowCommand(
        UUID eventId,
        Instant startTime,
        Instant endTime,
        SaleWindow saleWindow,
        List<TicketType> ticketTypes
) {}
