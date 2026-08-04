package com.example.inventory.domain.model;

import com.example.inventory.domain.exception.InvalidQuantityException;
import com.example.inventory.domain.exception.InvalidStateTransitionException;
import com.example.inventory.domain.exception.ReservationExpiredException;

import java.time.Instant;

public class Reservation {

    private final ReservationId id;
    private final TicketTypeId ticketTypeId;
    private final ShowId showId;
    private final UserId userId;
    private final RequestId requestId;
    private final int quantity;
    private ReservationStatus status;
    private final ReservationExpiration expiration;
    private final Instant createdAt;
    private Instant confirmedAt;
    private Instant releasedAt;
    private int version;

    public Reservation(
            ReservationId id,
            TicketTypeId ticketTypeId,
            ShowId showId,
            UserId userId,
            RequestId requestId,
            int quantity,
            ReservationStatus status,
            ReservationExpiration expiration,
            Instant createdAt,
            Instant confirmedAt,
            Instant releasedAt,
            int version
    ) {
        if (id == null) throw new IllegalArgumentException("ReservationId cannot be null");
        if (ticketTypeId == null) throw new IllegalArgumentException("TicketTypeId cannot be null");
        if (showId == null) throw new IllegalArgumentException("ShowId cannot be null");
        if (userId == null) throw new IllegalArgumentException("UserId cannot be null");
        if (requestId == null) throw new IllegalArgumentException("RequestId cannot be null");
        if (quantity <= 0) throw new InvalidQuantityException("Quantity must be greater than zero");
        if (status == null) throw new IllegalArgumentException("Status cannot be null");
        if (expiration == null) throw new IllegalArgumentException("Expiration cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("CreatedAt cannot be null");

        this.id = id;
        this.ticketTypeId = ticketTypeId;
        this.showId = showId;
        this.userId = userId;
        this.requestId = requestId;
        this.quantity = quantity;
        this.status = status;
        this.expiration = expiration;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.releasedAt = releasedAt;
        this.version = version;
    }

    public static Reservation create(
            TicketTypeId ticketTypeId,
            ShowId showId,
            UserId userId,
            RequestId requestId,
            int quantity,
            Instant now,
            long holdMinutes
    ) {
        ReservationExpiration exp = ReservationExpiration.ofMinutesFrom(now, holdMinutes);
        return new Reservation(
                ReservationId.generate(),
                ticketTypeId,
                showId,
                userId,
                requestId,
                quantity,
                ReservationStatus.HELD,
                exp,
                now,
                null,
                null,
                0
        );
    }

    public void confirm(Instant now) {
        if (this.status != ReservationStatus.HELD) {
            throw new InvalidStateTransitionException("Cannot transition from " + this.status + " to CONFIRMED");
        }
        if (this.expiration.isExpired(now)) {
            throw new ReservationExpiredException("Cannot confirm expired reservation: " + id);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    public void release(Instant now) {
        if (this.status != ReservationStatus.HELD) {
            throw new InvalidStateTransitionException("Cannot transition from " + this.status + " to RELEASED");
        }
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = now;
    }

    public void expire() {
        if (this.status != ReservationStatus.HELD) {
            throw new InvalidStateTransitionException("Cannot transition from " + this.status + " to EXPIRED");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public void cancel() {
        if (this.status != ReservationStatus.HELD) {
            throw new InvalidStateTransitionException("Cannot transition from " + this.status + " to CANCELLED");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public ReservationId getId() { return id; }
    public TicketTypeId getTicketTypeId() { return ticketTypeId; }
    public ShowId getShowId() { return showId; }
    public UserId getUserId() { return userId; }
    public RequestId getRequestId() { return requestId; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public ReservationExpiration getExpiration() { return expiration; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getReleasedAt() { return releasedAt; }
    public int getVersion() { return version; }
}
