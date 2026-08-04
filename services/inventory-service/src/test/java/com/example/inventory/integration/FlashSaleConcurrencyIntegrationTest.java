package com.example.inventory.integration;

import com.example.inventory.application.command.ReserveTicketCommand;
import com.example.inventory.application.port.in.ReserveTicketUseCase;
import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FlashSaleConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReserveTicketUseCase reserveTicketUseCase;

    @Autowired
    private InventoryPersistencePort inventoryPersistencePort;

    @Test
    void shouldHandle50ConcurrentReservations_for10AvailableTickets_withoutOverselling() throws InterruptedException {
        // Given: Flash Sale Show with 10 available tickets
        ShowId flashSaleShowId = new ShowId(UUID.randomUUID());
        TicketTypeId flashSaleTicketTypeId = new TicketTypeId(UUID.randomUUID());
        int totalAvailable = 10;
        Inventory flashSaleInventory = Inventory.create(flashSaleShowId, flashSaleTicketTypeId, totalAvailable);
        inventoryPersistencePort.save(flashSaleInventory);

        // When: 50 concurrent threads each try to reserve 1 ticket
        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await(); // Wait for all threads to be ready
                    UserId userId = new UserId(UUID.randomUUID());
                    RequestId requestId = new RequestId(UUID.randomUUID());
                    ReserveTicketCommand command = new ReserveTicketCommand(
                            flashSaleTicketTypeId, flashSaleShowId, userId, 1, requestId
                    );

                    reserveTicketUseCase.reserveTicket(command);
                    successCount.incrementAndGet();
                } catch (InsufficientInventoryException ex) {
                    failCount.incrementAndGet();
                } catch (Exception ex) {
                    // unexpected error
                    ex.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();
        boolean completed = endGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Then: Exactly 10 succeed, exactly 40 fail
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(40);

        // And: Database inventory is in a valid state (0 available, 10 reserved, 0 sold, 10 total)
        Optional<Inventory> finalInventory = inventoryPersistencePort.findByShowIdAndTicketTypeId(flashSaleShowId, flashSaleTicketTypeId);
        assertThat(finalInventory).isPresent();
        assertThat(finalInventory.get().getQuantity().available()).isEqualTo(0);
        assertThat(finalInventory.get().getQuantity().reserved()).isEqualTo(10);
        assertThat(finalInventory.get().getQuantity().sold()).isEqualTo(0);
        assertThat(finalInventory.get().getQuantity().total()).isEqualTo(10);
    }
}
