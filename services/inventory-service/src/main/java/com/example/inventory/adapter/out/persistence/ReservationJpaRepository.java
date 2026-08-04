package com.example.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {
    Optional<ReservationJpaEntity> findByRequestId(UUID requestId);
    List<ReservationJpaEntity> findAllByStatusAndExpiresAtBefore(String status, Instant cutoff);
    List<ReservationJpaEntity> findAllByUserIdAndShowId(UUID userId, UUID showId);
}
