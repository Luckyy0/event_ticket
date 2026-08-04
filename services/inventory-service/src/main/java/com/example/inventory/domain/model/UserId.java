package com.example.inventory.domain.model;

import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        if (value == null) throw new IllegalArgumentException("User ID cannot be null");
    }
    @Override
    public String toString() { return value.toString(); }
}
