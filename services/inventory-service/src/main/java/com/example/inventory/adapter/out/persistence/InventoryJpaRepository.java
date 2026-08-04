package com.example.inventory.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, UUID> {

    Optional<InventoryJpaEntity> findByShowIdAndTicketTypeId(UUID showId, UUID ticketTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryJpaEntity i WHERE i.showId = :showId AND i.ticketTypeId = :ticketTypeId")
    Optional<InventoryJpaEntity> findByShowIdAndTicketTypeIdForUpdate(
            @Param("showId") UUID showId,
            @Param("ticketTypeId") UUID ticketTypeId
    );

    List<InventoryJpaEntity> findAllByShowId(UUID showId);

    @Modifying
    @Query(value = """
        UPDATE inventories
        SET available_quantity = available_quantity - :qty,
            reserved_quantity = reserved_quantity + :qty,
            version = version + 1,
            updated_at = NOW()
        WHERE show_id = :showId
          AND ticket_type_id = :ticketTypeId
          AND available_quantity >= :qty
        """, nativeQuery = true)
    int atomicReserve(
            @Param("showId") UUID showId,
            @Param("ticketTypeId") UUID ticketTypeId,
            @Param("qty") int qty
    );
}
