package com.example.inventory.adapter.out.persistence;

import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class InventoryPersistenceAdapter implements InventoryPersistencePort {

    private final InventoryJpaRepository inventoryJpaRepository;

    public InventoryPersistenceAdapter(InventoryJpaRepository inventoryJpaRepository) {
        this.inventoryJpaRepository = inventoryJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Inventory> findByShowIdAndTicketTypeId(ShowId showId, TicketTypeId ticketTypeId) {
        return inventoryJpaRepository.findByShowIdAndTicketTypeId(showId.value(), ticketTypeId.value())
                .map(InventoryMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Inventory> findByShowIdAndTicketTypeIdForUpdate(ShowId showId, TicketTypeId ticketTypeId) {
        return inventoryJpaRepository.findByShowIdAndTicketTypeIdForUpdate(showId.value(), ticketTypeId.value())
                .map(InventoryMapper::toDomain);
    }

    @Override
    @Transactional
    public Inventory save(Inventory inventory) {
        InventoryJpaEntity entity = InventoryMapper.toEntity(inventory);
        InventoryJpaEntity saved = inventoryJpaRepository.save(entity);
        return InventoryMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findAllByShowId(ShowId showId) {
        return inventoryJpaRepository.findAllByShowId(showId.value())
                .stream()
                .map(InventoryMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean atomicReserve(ShowId showId, TicketTypeId ticketTypeId, int quantity) {
        int updated = inventoryJpaRepository.atomicReserve(showId.value(), ticketTypeId.value(), quantity);
        return updated > 0;
    }
}
