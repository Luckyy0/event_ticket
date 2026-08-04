package com.example.inventory.adapter.out.persistence;

import com.example.inventory.application.port.out.ReservationPersistencePort;
import com.example.inventory.domain.model.RequestId;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationId;
import com.example.inventory.domain.model.ReservationStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class ReservationPersistenceAdapter implements ReservationPersistencePort {

    private final ReservationJpaRepository reservationJpaRepository;
    private final ProcessedRequestJpaRepository processedRequestJpaRepository;

    public ReservationPersistenceAdapter(
            ReservationJpaRepository reservationJpaRepository,
            ProcessedRequestJpaRepository processedRequestJpaRepository
    ) {
        this.reservationJpaRepository = reservationJpaRepository;
        this.processedRequestJpaRepository = processedRequestJpaRepository;
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity = ReservationMapper.toEntity(reservation);
        ReservationJpaEntity saved = reservationJpaRepository.save(entity);

        // Record processed request for idempotency tracking
        if (!processedRequestJpaRepository.existsByRequestId(reservation.getRequestId().value())) {
            processedRequestJpaRepository.save(new ProcessedRequestJpaEntity(
                    reservation.getRequestId().value(),
                    reservation.getId().value(),
                    Instant.now(),
                    reservation.getStatus().name()
            ));
        }

        return ReservationMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findById(ReservationId id) {
        return reservationJpaRepository.findById(id.value())
                .map(ReservationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findByRequestId(RequestId requestId) {
        return reservationJpaRepository.findByRequestId(requestId.value())
                .map(ReservationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findExpiredHeldReservations(Instant cutoff) {
        return reservationJpaRepository.findAllByStatusAndExpiresAtBefore(ReservationStatus.HELD.name(), cutoff)
                .stream()
                .map(ReservationMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRequestId(RequestId requestId) {
        return processedRequestJpaRepository.existsByRequestId(requestId.value());
    }
}
