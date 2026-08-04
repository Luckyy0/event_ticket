package com.example.inventory.application.usecase;

import com.example.inventory.application.command.ReserveTicketCommand;
import com.example.inventory.application.port.in.ReserveTicketUseCase;
import com.example.inventory.application.port.out.*;
import com.example.inventory.domain.event.InventoryReservationRejectedEvent;
import com.example.inventory.domain.event.InventoryReservedEvent;
import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.Reservation;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class ReserveTicketUseCaseImpl implements ReserveTicketUseCase {

    private final InventoryPersistencePort inventoryPersistencePort;
    private final ReservationPersistencePort reservationPersistencePort;
    private final DistributedLockPort distributedLockPort;
    private final OutboxPort outboxPort;
    private final ClockPort clockPort;
    private final long holdMinutes;

    public ReserveTicketUseCaseImpl(
            InventoryPersistencePort inventoryPersistencePort,
            ReservationPersistencePort reservationPersistencePort,
            DistributedLockPort distributedLockPort,
            OutboxPort outboxPort,
            ClockPort clockPort
    ) {
        this(inventoryPersistencePort, reservationPersistencePort, distributedLockPort, outboxPort, clockPort, 15);
    }

    public ReserveTicketUseCaseImpl(
            InventoryPersistencePort inventoryPersistencePort,
            ReservationPersistencePort reservationPersistencePort,
            DistributedLockPort distributedLockPort,
            OutboxPort outboxPort,
            ClockPort clockPort,
            long holdMinutes
    ) {
        this.inventoryPersistencePort = inventoryPersistencePort;
        this.reservationPersistencePort = reservationPersistencePort;
        this.distributedLockPort = distributedLockPort;
        this.outboxPort = outboxPort;
        this.clockPort = clockPort;
        this.holdMinutes = holdMinutes;
    }

    @Override
    public Reservation reserveTicket(ReserveTicketCommand command) {
        // 1. Idempotency Check
        Optional<Reservation> existing = reservationPersistencePort.findByRequestId(command.requestId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Acquire Distributed Lock for Show + TicketType
        String lockKey = "lock:inventory:" + command.showId().value() + ":" + command.ticketTypeId().value();
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = distributedLockPort.acquireLock(lockKey, lockValue, Duration.ofSeconds(5), Duration.ofSeconds(3));

        if (!acquired) {
            throw new InsufficientInventoryException("Could not acquire lock for inventory, high contention. Please retry.");
        }

        try {
            // Re-check idempotency under lock
            Optional<Reservation> doubleCheck = reservationPersistencePort.findByRequestId(command.requestId());
            if (doubleCheck.isPresent()) {
                return doubleCheck.get();
            }

            // 3. Load Inventory
            Inventory inventory = inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(
                    command.showId(), command.ticketTypeId()
            ).orElseThrow(() -> {
                outboxPort.publish(new InventoryReservationRejectedEvent(
                        command.ticketTypeId(), command.showId(), command.userId(),
                        command.quantity(), 0, "Inventory not found", clockPort.now()
                ));
                return new InsufficientInventoryException("Inventory not found for show: "
                        + command.showId() + ", ticketType: " + command.ticketTypeId());
            });

            // 4. Reserve within Domain Aggregate
            try {
                inventory.reserve(command.quantity());
            } catch (InsufficientInventoryException ex) {
                outboxPort.publish(new InventoryReservationRejectedEvent(
                        command.ticketTypeId(), command.showId(), command.userId(),
                        command.quantity(), inventory.getQuantity().available(), ex.getMessage(), clockPort.now()
                ));
                throw ex;
            }

            // 5. Create Reservation
            Instant now = clockPort.now();
            Reservation reservation = Reservation.create(
                    command.ticketTypeId(),
                    command.showId(),
                    command.userId(),
                    command.requestId(),
                    command.quantity(),
                    now,
                    holdMinutes
            );

            // 6. Save Aggregate and Reservation
            inventoryPersistencePort.save(inventory);
            Reservation savedReservation = reservationPersistencePort.save(reservation);

            // 7. Publish Domain Event to Outbox
            outboxPort.publish(new InventoryReservedEvent(
                    savedReservation.getId(),
                    savedReservation.getTicketTypeId(),
                    savedReservation.getShowId(),
                    savedReservation.getUserId(),
                    savedReservation.getQuantity(),
                    savedReservation.getExpiration().expiresAt(),
                    now
            ));

            return savedReservation;
        } finally {
            distributedLockPort.releaseLock(lockKey, lockValue);
        }
    }
}
