package com.example.inventory.domain.model;

import java.util.UUID;

public class Inventory {

    private final UUID id;
    private final ShowId showId;
    private final TicketTypeId ticketTypeId;
    private InventoryQuantity quantity;
    private int version;

    public Inventory(UUID id, ShowId showId, TicketTypeId ticketTypeId, InventoryQuantity quantity, int version) {
        if (id == null) throw new IllegalArgumentException("Inventory ID cannot be null");
        if (showId == null) throw new IllegalArgumentException("ShowId cannot be null");
        if (ticketTypeId == null) throw new IllegalArgumentException("TicketTypeId cannot be null");
        if (quantity == null) throw new IllegalArgumentException("Quantity cannot be null");

        this.id = id;
        this.showId = showId;
        this.ticketTypeId = ticketTypeId;
        this.quantity = quantity;
        this.version = version;
    }

    public static Inventory create(ShowId showId, TicketTypeId ticketTypeId, int totalQuantity) {
        return new Inventory(UUID.randomUUID(), showId, ticketTypeId, InventoryQuantity.ofInitial(totalQuantity), 0);
    }

    public void reserve(int requestedQuantity) {
        this.quantity = this.quantity.reserve(requestedQuantity);
    }

    public void confirmReservation(int quantityToConfirm) {
        this.quantity = this.quantity.confirm(quantityToConfirm);
    }

    public void releaseReservation(int quantityToRelease) {
        this.quantity = this.quantity.release(quantityToRelease);
    }

    public UUID getId() {
        return id;
    }

    public ShowId getShowId() {
        return showId;
    }

    public TicketTypeId getTicketTypeId() {
        return ticketTypeId;
    }

    public InventoryQuantity getQuantity() {
        return quantity;
    }

    public int getVersion() {
        return version;
    }
}
