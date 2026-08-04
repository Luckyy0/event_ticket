package com.example.catalog.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class TicketType {
    private final UUID id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final String currency;
    private final int totalQuantity;
    private final int sortOrder;

    public TicketType(UUID id, String name, String description, BigDecimal price, String currency, int totalQuantity, int sortOrder) {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket type name is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ticket price must be greater than zero");
        }
        if (totalQuantity <= 0) {
            throw new IllegalArgumentException("Total quantity must be greater than zero");
        }

        this.id = id;
        this.name = name.trim();
        this.description = description;
        this.price = price;
        this.currency = (currency != null && !currency.trim().isEmpty()) ? currency.trim() : "VND";
        this.totalQuantity = totalQuantity;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
