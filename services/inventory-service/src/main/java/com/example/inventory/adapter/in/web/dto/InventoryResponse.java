package com.example.inventory.adapter.in.web.dto;

import com.example.inventory.domain.model.Inventory;

import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID showId,
        UUID ticketTypeId,
        int totalQuantity,
        int availableQuantity,
        int reservedQuantity,
        int soldQuantity,
        int version
) {
    public static InventoryResponse fromDomain(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getShowId().value(),
                inventory.getTicketTypeId().value(),
                inventory.getQuantity().total(),
                inventory.getQuantity().available(),
                inventory.getQuantity().reserved(),
                inventory.getQuantity().sold(),
                inventory.getVersion()
        );
    }
}
