package com.example.catalog.domain;

import com.example.catalog.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShowDomainTest {

    private final UUID eventId = UUID.randomUUID();
    private final Instant futureStart = Instant.now().plus(30, ChronoUnit.DAYS);
    private final Instant futureEnd = futureStart.plus(3, ChronoUnit.HOURS);
    private final SaleWindow validSaleWindow = new SaleWindow(
            Instant.now().plus(1, ChronoUnit.DAYS),
            futureStart.minus(1, ChronoUnit.HOURS)
    );

    @Test
    void shouldCreateShow_whenAllFieldsAreValid() {
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "VIP seating", new BigDecimal("2000000"), "VND", 100, 1);
        Show show = Show.create(eventId, futureStart, futureEnd, validSaleWindow, List.of(vip));

        assertThat(show.getId()).isNotNull();
        assertThat(show.getEventId()).isEqualTo(eventId);
        assertThat(show.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
        assertThat(show.getTicketTypes()).hasSize(1);
    }

    @Test
    void shouldRejectShow_whenStartTimeIsInThePast() {
        Instant pastStart = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant pastEnd = pastStart.plus(3, ChronoUnit.HOURS);
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("100000"), "VND", 50, 1);

        assertThatThrownBy(() -> Show.create(eventId, pastStart, pastEnd, validSaleWindow, List.of(vip)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Show start time must be in the future");
    }

    @Test
    void shouldRejectShow_whenEndTimeIsBeforeStartTime() {
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("100000"), "VND", 50, 1);

        assertThatThrownBy(() -> Show.create(eventId, futureEnd, futureStart, validSaleWindow, List.of(vip)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Show end time must be after start time");
    }

    @Test
    void shouldRejectShow_whenSaleWindowClosesAfterShowStarts() {
        SaleWindow invalidWindow = new SaleWindow(
                Instant.now().plus(1, ChronoUnit.DAYS),
                futureStart.plus(1, ChronoUnit.HOURS)
        );
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("100000"), "VND", 50, 1);

        assertThatThrownBy(() -> Show.create(eventId, futureStart, futureEnd, invalidWindow, List.of(vip)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sale window must close before show start time");
    }

    @Test
    void shouldCreateTicketType_whenPriceAndQuantityArePositive() {
        TicketType ticketType = new TicketType(
                UUID.randomUUID(),
                "Standard",
                "Standard admission",
                new BigDecimal("500000"),
                "VND",
                1000,
                2
        );

        assertThat(ticketType.getName()).isEqualTo("Standard");
        assertThat(ticketType.getPrice()).isEqualByComparingTo("500000");
        assertThat(ticketType.getTotalQuantity()).isEqualTo(1000);
        assertThat(ticketType.getCurrency()).isEqualTo("VND");
    }

    @Test
    void shouldRejectTicketType_whenPriceIsZeroOrNegative() {
        assertThatThrownBy(() -> new TicketType(UUID.randomUUID(), "VIP", "desc", BigDecimal.ZERO, "VND", 100, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticket price must be greater than zero");

        assertThatThrownBy(() -> new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("-50000"), "VND", 100, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticket price must be greater than zero");
    }

    @Test
    void shouldRejectTicketType_whenQuantityIsZeroOrNegative() {
        assertThatThrownBy(() -> new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("100000"), "VND", 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total quantity must be greater than zero");
    }

    @Test
    void shouldRejectTicketType_whenNameIsEmpty() {
        assertThatThrownBy(() -> new TicketType(UUID.randomUUID(), "  ", "desc", new BigDecimal("100000"), "VND", 100, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticket type name is required");
    }

    @Test
    void shouldTransitionToOnSale_whenSaleWindowOpens() {
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("100000"), "VND", 50, 1);
        Show show = Show.create(eventId, futureStart, futureEnd, validSaleWindow, List.of(vip));
        show.markOnSale();

        assertThat(show.getStatus()).isEqualTo(ShowStatus.ON_SALE);
    }

    @Test
    void shouldTransitionToSoldOut_whenMarkedSoldOut() {
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "desc", new BigDecimal("100000"), "VND", 50, 1);
        Show show = Show.create(eventId, futureStart, futureEnd, validSaleWindow, List.of(vip));
        show.markSoldOut();

        assertThat(show.getStatus()).isEqualTo(ShowStatus.SOLD_OUT);
    }
}
