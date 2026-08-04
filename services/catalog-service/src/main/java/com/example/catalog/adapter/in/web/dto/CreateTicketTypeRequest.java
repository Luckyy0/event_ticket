package com.example.catalog.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTicketTypeRequest(
        @NotBlank(message = "Ticket type name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        BigDecimal price,

        String currency,

        @Min(value = 1, message = "Quantity must be at least 1")
        int totalQuantity,

        int sortOrder
) {}
