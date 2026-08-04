package com.example.inventory.application.port.in;

import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;

import java.util.List;

public interface QueryInventoryUseCase {
    Inventory getInventory(ShowId showId, TicketTypeId ticketTypeId);
    List<Inventory> getInventoriesByShow(ShowId showId);
}
