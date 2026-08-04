package com.example.catalog.domain.model;

import java.time.Instant;

public class SaleWindow {
    private final Instant opensAt;
    private final Instant closesAt;

    public SaleWindow(Instant opensAt, Instant closesAt) {
        if (opensAt == null || closesAt == null) {
            throw new IllegalArgumentException("Sale window opensAt and closesAt are required");
        }
        if (closesAt.isBefore(opensAt) || closesAt.equals(opensAt)) {
            throw new IllegalArgumentException("Sale window closesAt must be after opensAt");
        }
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public Instant getOpensAt() {
        return opensAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }
}
