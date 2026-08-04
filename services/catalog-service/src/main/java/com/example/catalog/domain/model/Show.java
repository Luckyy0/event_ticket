package com.example.catalog.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Show {
    private final UUID id;
    private final UUID eventId;
    private final Instant startTime;
    private final Instant endTime;
    private ShowStatus status;
    private final SaleWindow saleWindow;
    private final List<TicketType> ticketTypes;

    public Show(UUID id, UUID eventId, Instant startTime, Instant endTime, ShowStatus status, SaleWindow saleWindow, List<TicketType> ticketTypes) {
        if (id == null) {
            throw new IllegalArgumentException("Show ID is required");
        }
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Show start time is required");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("Show end time is required");
        }
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("Show end time must be after start time");
        }
        if (saleWindow != null && saleWindow.getClosesAt().isAfter(startTime)) {
            throw new IllegalArgumentException("Sale window must close before show start time");
        }

        this.id = id;
        this.eventId = eventId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : ShowStatus.SCHEDULED;
        this.saleWindow = saleWindow;
        this.ticketTypes = ticketTypes != null ? new ArrayList<>(ticketTypes) : new ArrayList<>();
    }

    public static Show create(UUID eventId, Instant startTime, Instant endTime, SaleWindow saleWindow, List<TicketType> ticketTypes) {
        if (startTime == null || startTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Show start time must be in the future");
        }
        return new Show(UUID.randomUUID(), eventId, startTime, endTime, ShowStatus.SCHEDULED, saleWindow, ticketTypes);
    }

    public void addTicketType(TicketType ticketType) {
        if (ticketType == null) {
            throw new IllegalArgumentException("TicketType cannot be null");
        }
        this.ticketTypes.add(ticketType);
    }

    public void markOnSale() {
        this.status = ShowStatus.ON_SALE;
    }

    public void markSoldOut() {
        this.status = ShowStatus.SOLD_OUT;
    }

    public void cancel() {
        this.status = ShowStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public ShowStatus getStatus() {
        return status;
    }

    public SaleWindow getSaleWindow() {
        return saleWindow;
    }

    public List<TicketType> getTicketTypes() {
        return Collections.unmodifiableList(ticketTypes);
    }
}
