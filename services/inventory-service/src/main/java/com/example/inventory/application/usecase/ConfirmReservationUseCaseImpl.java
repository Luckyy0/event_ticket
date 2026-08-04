package com.example.inventory.application.usecase;

import com.example.inventory.application.command.ConfirmReservationCommand;
import com.example.inventory.application.port.in.ConfirmReservationUseCase;
import com.example.inventory.application.port.out.ClockPort;
import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.application.port.out.OutboxPort;
import com.example.inventory.application.port.out.ReservationPersistencePort;
import com.example.inventory.domain.event.InventoryConfirmedEvent;
import com.example.inventory.domain.exception.ReservationAlreadyConfirmedException;
import com.example.inventory.domain.exception.ReservationNotFoundException;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationStatus;

import java.time.Instant;

public class ConfirmReservationUseCaseImpl implements ConfirmReservationUseCase {

    private final ReservationPersistencePort reservationPersistencePort;
    private final InventoryPersistencePort inventoryPersistencePort;
    private final OutboxPort outboxPort;
    private final ClockPort clockPort;

    public ConfirmReservationUseCaseImpl(
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
    public void confirmReservation(ConfirmReservationCommand command) {
        Reservation reservation = reservationPersistencePort.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            // Idempotent confirmation
            return;
        }

        Instant now = clockPort.now();
        reservation.confirm(now);

        Inventory inventory = inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(
                reservation.getShowId(), reservation.getTicketTypeId()
        ).orElseThrow(() -> new IllegalStateException("Inventory not found for reservation: " + reservation.getId()));

        inventory.confirmReservation(reservation.getQuantity());

        inventoryPersistencePort.save(inventory);
        reservationPersistencePort.save(reservation);

        outboxPort.publish(new InventoryConfirmedEvent(
                reservation.getId(),
                reservation.getTicketTypeId(),
                reservation.getShowId(),
                reservation.getUserId(),
                reservation.getQuantity(),
                reservation.getConfirmedAt(),
                now
        ));
    }
}
