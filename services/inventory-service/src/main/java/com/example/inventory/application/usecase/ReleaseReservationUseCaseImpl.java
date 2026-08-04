package com.example.inventory.application.usecase;

import com.example.inventory.application.command.ReleaseReservationCommand;
import com.example.inventory.application.port.in.ReleaseReservationUseCase;
import com.example.inventory.application.port.out.ClockPort;
import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.application.port.out.OutboxPort;
import com.example.inventory.application.port.out.ReservationPersistencePort;
import com.example.inventory.domain.event.InventoryReleasedEvent;
import com.example.inventory.domain.exception.ReservationNotFoundException;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationStatus;

import java.time.Instant;

public class ReleaseReservationUseCaseImpl implements ReleaseReservationUseCase {

    private final ReservationPersistencePort reservationPersistencePort;
    private final InventoryPersistencePort inventoryPersistencePort;
    private final OutboxPort outboxPort;
    private final ClockPort clockPort;

    public ReleaseReservationUseCaseImpl(
            ReservationPersistencePort reservationPersistencePort,
            InventoryPersistencePort inventoryPersistencePort,
            OutboxPort outboxPort,
            ClockPort clockPort
    ) {
        this.reservationPersistencePort = reservationPersistencePort;
        this.inventoryPersistencePort = inventoryPersistencePort;
        this.outboxPort = outboxPort;
        this.clockPort = clockPort;
    }

    @Override
    public void releaseReservation(ReleaseReservationCommand command) {
        Reservation reservation = reservationPersistencePort.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            // Idempotent release
            return;
        }

        Instant now = clockPort.now();
        reservation.release(now);

        Inventory inventory = inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(
                reservation.getShowId(), reservation.getTicketTypeId()
        ).orElseThrow(() -> new IllegalStateException("Inventory not found for reservation: " + reservation.getId()));

        inventory.releaseReservation(reservation.getQuantity());

        inventoryPersistencePort.save(inventory);
        reservationPersistencePort.save(reservation);

        outboxPort.publish(new InventoryReleasedEvent(
                reservation.getId(),
                reservation.getTicketTypeId(),
                reservation.getShowId(),
                reservation.getUserId(),
                reservation.getQuantity(),
                reservation.getReleasedAt(),
                now
        ));
    }
}
