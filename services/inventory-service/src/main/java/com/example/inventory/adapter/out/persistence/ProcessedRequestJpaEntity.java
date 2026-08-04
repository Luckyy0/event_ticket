package com.example.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_requests")
public class ProcessedRequestJpaEntity {

    @Id
    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus;

    public ProcessedRequestJpaEntity() {}

    public ProcessedRequestJpaEntity(UUID requestId, UUID reservationId, Instant processedAt, String resultStatus) {
        this.requestId = requestId;
        this.reservationId = reservationId;
        this.processedAt = processedAt;
        this.resultStatus = resultStatus;
    }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
}
