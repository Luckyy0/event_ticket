package com.example.inventory.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Version
    private int version;

    public ReservationJpaEntity() {}

    public ReservationJpaEntity(
            UUID id,
            UUID ticketTypeId,
            UUID showId,
            UUID userId,
            UUID requestId,
            int quantity,
            String status,
            Instant expiresAt,
            Instant createdAt,
            Instant confirmedAt,
            Instant releasedAt,
            int version
    ) {
        this.id = id;
        this.ticketTypeId = ticketTypeId;
        this.showId = showId;
        this.userId = userId;
        this.requestId = requestId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.releasedAt = releasedAt;
        this.version = version;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(UUID ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    public UUID getShowId() { return showId; }
    public void setShowId(UUID showId) { this.showId = showId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
