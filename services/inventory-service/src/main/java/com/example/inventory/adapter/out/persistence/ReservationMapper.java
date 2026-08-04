package com.example.inventory.adapter.out.persistence;

import com.example.inventory.domain.model.*;

public class ReservationMapper {

    public static Reservation toDomain(ReservationJpaEntity entity) {
        if (entity == null) return null;
        return new Reservation(
                new ReservationId(entity.getId()),
                new TicketTypeId(entity.getTicketTypeId()),
                new ShowId(entity.getShowId()),
                new UserId(entity.getUserId()),
                new RequestId(entity.getRequestId()),
                entity.getQuantity(),
                ReservationStatus.valueOf(entity.getStatus()),
                new ReservationExpiration(entity.getExpiresAt()),
                entity.getCreatedAt(),
                entity.getConfirmedAt(),
                entity.getReleasedAt(),
                entity.getVersion()
        );
    }

    public static ReservationJpaEntity toEntity(Reservation domain) {
        if (domain == null) return null;
        return new ReservationJpaEntity(
                domain.getId().value(),
                domain.getTicketTypeId().value(),
                domain.getShowId().value(),
                domain.getUserId().value(),
                domain.getRequestId().value(),
                domain.getQuantity(),
                domain.getStatus().name(),
                domain.getExpiration().expiresAt(),
                domain.getCreatedAt(),
                domain.getConfirmedAt(),
                domain.getReleasedAt(),
                domain.getVersion()
        );
    }
}
