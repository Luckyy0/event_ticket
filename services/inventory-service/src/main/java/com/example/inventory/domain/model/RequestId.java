package com.example.inventory.domain.model;

import java.util.UUID;

public record RequestId(UUID value) {
    public RequestId {
        if (value == null) throw new IllegalArgumentException("Request ID cannot be null");
    }
    @Override
    public String toString() { return value.toString(); }
}
