package com.example.inventory.integration;

import com.example.inventory.adapter.out.persistence.InventoryJpaRepository;
import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.application.port.out.ReservationPersistencePort;
import com.example.inventory.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryPersistencePort inventoryPersistencePort;

    @Autowired
    private ReservationPersistencePort reservationPersistencePort;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @Test
    void shouldSaveAndLoadInventory() {
        ShowId showId = new ShowId(UUID.randomUUID());
        TicketTypeId ticketTypeId = new TicketTypeId(UUID.randomUUID());
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);

        inventoryPersistencePort.save(inventory);

        Optional<Inventory> loaded = inventoryPersistencePort.findByShowIdAndTicketTypeId(showId, ticketTypeId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getQuantity().total()).isEqualTo(100);
        assertThat(loaded.get().getQuantity().available()).isEqualTo(100);
    }

    @Test
    void shouldPerformAtomicReserve() {
        ShowId showId = new ShowId(UUID.randomUUID());
        TicketTypeId ticketTypeId = new TicketTypeId(UUID.randomUUID());
        Inventory inventory = Inventory.create(showId, ticketTypeId, 10);
        inventoryPersistencePort.save(inventory);

        boolean success1 = inventoryPersistencePort.atomicReserve(showId, ticketTypeId, 6);
        assertThat(success1).isTrue();

        boolean success2 = inventoryPersistencePort.atomicReserve(showId, ticketTypeId, 5);
        assertThat(success2).isFalse(); // only 4 left

        Optional<Inventory> updated = inventoryPersistencePort.findByShowIdAndTicketTypeId(showId, ticketTypeId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getQuantity().available()).isEqualTo(4);
        assertThat(updated.get().getQuantity().reserved()).isEqualTo(6);
    }

    @Test
    void shouldSaveAndFindReservationByRequestId() {
        ShowId showId = new ShowId(UUID.randomUUID());
        TicketTypeId ticketTypeId = new TicketTypeId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        RequestId requestId = new RequestId(UUID.randomUUID());

        Reservation reservation = Reservation.create(ticketTypeId, showId, userId, requestId, 2, Instant.now(), 15);
        reservationPersistencePort.save(reservation);

        Optional<Reservation> loaded = reservationPersistencePort.findByRequestId(requestId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(reservation.getId());
        assertThat(loaded.get().getQuantity()).isEqualTo(2);
        assertThat(loaded.get().getStatus()).isEqualTo(ReservationStatus.HELD);
    }
}
