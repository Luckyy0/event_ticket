package com.example.inventory.application.usecase;

import com.example.inventory.application.port.in.ExpireReservationsUseCase;
import com.example.inventory.application.port.out.ClockPort;
import com.example.inventory.application.port.out.InventoryPersistencePort;
import com.example.inventory.application.port.out.OutboxPort;
import com.example.inventory.application.port.out.ReservationPersistencePort;
import com.example.inventory.domain.event.InventoryReservationExpiredEvent;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.Reservation;

import java.time.Instant;
import java.util.List;

public class ExpireReservationsUseCaseImpl implements ExpireReservationsUseCase {

    private final ReservationPersistencePort reservationPersistencePort;
    private final InventoryPersistencePort inventoryPersistencePort;
    private final OutboxPort outboxPort;
    private final ClockPort clockPort;

    public ExpireReservationsUseCaseImpl(
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
    public int expireReservations() {
        Instant now = clockPort.now();
        List<Reservation> expiredReservations = reservationPersistencePort.findExpiredHeldReservations(now);
        int count = 0;

        for (Reservation reservation : expiredReservations) {
            try {
                reservation.expire();

                Inventory inventory = inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(
                        reservation.getShowId(), reservation.getTicketTypeId()
                ).orElse(null);

                if (inventory != null) {
                    inventory.releaseReservation(reservation.getQuantity());
                    inventoryPersistencePort.save(inventory);
                }

                reservationPersistencePort.save(reservation);

                outboxPort.publish(new InventoryReservationExpiredEvent(
                        reservation.getId(),
                        reservation.getTicketTypeId(),
                        reservation.getShowId(),
                        reservation.getUserId(),
                        reservation.getQuantity(),
                        reservation.getExpiration().expiresAt(),
                        now
                ));
                count++;
            } catch (Exception ignored) {
                // Log and continue reconciling other expired reservations
            }
        }
        return count;
    }
}
