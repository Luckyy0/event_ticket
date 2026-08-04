package com.example.inventory.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveTicketRequest(
        @NotNull(message = "showId is required")
        UUID showId,

        @NotNull(message = "ticketTypeId is required")
        UUID ticketTypeId,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,

        @NotNull(message = "requestId is required for idempotency")
        UUID requestId
) {}
