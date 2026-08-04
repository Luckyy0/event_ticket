package com.example.inventory.domain.model;

import java.util.UUID;

public record ShowId(UUID value) {
    public ShowId {
        if (value == null) throw new IllegalArgumentException("Show ID cannot be null");
    }
    @Override
    public String toString() { return value.toString(); }
}
