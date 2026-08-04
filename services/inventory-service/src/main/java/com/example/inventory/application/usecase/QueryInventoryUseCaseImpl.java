package com.example.inventory.application.usecase;

import com.example.inventory.application.port.in.QueryInventoryUseCase;
import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;

import java.util.List;

public class QueryInventoryUseCaseImpl implements QueryInventoryUseCase {

    private final InventoryPersistencePort inventoryPersistencePort;

    public QueryInventoryUseCaseImpl(InventoryPersistencePort inventoryPersistencePort) {
        this.inventoryPersistencePort = inventoryPersistencePort;
    }

    @Override
    public Inventory getInventory(ShowId showId, TicketTypeId ticketTypeId) {
        return inventoryPersistencePort.findByShowIdAndTicketTypeId(showId, ticketTypeId)
                .orElseThrow(() -> new InsufficientInventoryException("Inventory not found for show: "
                        + showId + ", ticketType: " + ticketTypeId));
    }

    @Override
    public List<Inventory> getInventoriesByShow(ShowId showId) {
        return inventoryPersistencePort.findAllByShowId(showId);
    }
}
