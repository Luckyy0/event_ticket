package com.example.inventory.application.port.out;

import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;

import java.util.List;
import java.util.Optional;

public interface InventoryPersistencePort {
    Optional<Inventory> findByShowIdAndTicketTypeId(ShowId showId, TicketTypeId ticketTypeId);
    Optional<Inventory> findByShowIdAndTicketTypeIdForUpdate(ShowId showId, TicketTypeId ticketTypeId);
    Inventory save(Inventory inventory);
    List<Inventory> findAllByShowId(ShowId showId);
    boolean atomicReserve(ShowId showId, TicketTypeId ticketTypeId, int quantity);
}
