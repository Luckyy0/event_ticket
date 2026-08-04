package com.example.inventory.adapter.out.persistence;

import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.InventoryQuantity;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;

import java.time.Instant;

public class InventoryMapper {

    public static Inventory toDomain(InventoryJpaEntity entity) {
        if (entity == null) return null;
        return new Inventory(
                entity.getId(),
                new ShowId(entity.getShowId()),
                new TicketTypeId(entity.getTicketTypeId()),
                new InventoryQuantity(
                        entity.getTotalQuantity(),
                        entity.getAvailableQuantity(),
                        entity.getReservedQuantity(),
                        entity.getSoldQuantity()
                ),
                entity.getVersion()
        );
    }

    public static InventoryJpaEntity toEntity(Inventory domain) {
        if (domain == null) return null;
        Instant now = Instant.now();
        return new InventoryJpaEntity(
                domain.getId(),
                domain.getShowId().value(),
                domain.getTicketTypeId().value(),
                domain.getQuantity().total(),
                domain.getQuantity().available(),
                domain.getQuantity().reserved(),
                domain.getQuantity().sold(),
                domain.getVersion(),
                now,
                now
        );
    }
}
