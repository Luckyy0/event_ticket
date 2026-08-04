package com.example.inventory.application.port.out;

import com.example.inventory.domain.model.RequestId;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationPersistencePort {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(ReservationId id);
    Optional<Reservation> findByRequestId(RequestId requestId);
    List<Reservation> findExpiredHeldReservations(Instant cutoff);
    boolean existsByRequestId(RequestId requestId);
}
