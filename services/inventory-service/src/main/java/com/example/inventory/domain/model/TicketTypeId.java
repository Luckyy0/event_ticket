package com.example.inventory.domain.model;

import java.util.UUID;

public record TicketTypeId(UUID value) {
    public TicketTypeId {
        if (value == null) throw new IllegalArgumentException("TicketType ID cannot be null");
    }
    @Override
    public String toString() { return value.toString(); }
}
