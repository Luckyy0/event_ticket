package com.example.inventory.domain;

import com.example.inventory.domain.exception.InvalidStateTransitionException;
import com.example.inventory.domain.exception.ReservationExpiredException;
import com.example.inventory.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationAggregateTest {

    private final TicketTypeId ticketTypeId = new TicketTypeId(UUID.randomUUID());
    private final ShowId showId = new ShowId(UUID.randomUUID());
    private final UserId userId = new UserId(UUID.randomUUID());
    private final RequestId requestId = new RequestId(UUID.randomUUID());
    private final Instant now = Instant.parse("2026-08-04T12:00:00Z");

    @Test
    void shouldCreateReservation_withStatusHeld() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.getQuantity()).isEqualTo(2);
        assertThat(reservation.getRequestId()).isEqualTo(requestId);
        assertThat(reservation.getExpiration().isExpired(now)).isFalse();
    }

    @Test
    void shouldTransitionToConfirmed_whenStatusIsHeld() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );

        Instant confirmTime = now.plus(5, ChronoUnit.MINUTES);
        reservation.confirm(confirmTime);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isEqualTo(confirmTime);
    }

    @Test
    void shouldRejectConfirmation_whenReservationIsExpired() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );

        Instant pastExpiration = now.plus(16, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> reservation.confirm(pastExpiration))
                .isInstanceOf(ReservationExpiredException.class)
                .hasMessageContaining("Cannot confirm expired reservation");
    }

    @Test
    void shouldTransitionToReleased_whenStatusIsHeld() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );

        Instant releaseTime = now.plus(5, ChronoUnit.MINUTES);
        reservation.release(releaseTime);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.getReleasedAt()).isEqualTo(releaseTime);
    }

    @Test
    void shouldTransitionToExpired_whenStatusIsHeld() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );

        reservation.expire();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void shouldTransitionToCancelled_whenStatusIsHeld() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void shouldRejectTransitionToConfirmed_whenStatusIsReleased() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );
        reservation.release(now.plus(1, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> reservation.confirm(now.plus(2, ChronoUnit.MINUTES)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition from RELEASED to CONFIRMED");
    }

    @Test
    void shouldRejectTransitionToConfirmed_whenStatusIsAlreadyConfirmed() {
        Reservation reservation = Reservation.create(
                ticketTypeId, showId, userId, requestId, 2, now, 15
        );
        reservation.confirm(now.plus(1, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> reservation.confirm(now.plus(2, ChronoUnit.MINUTES)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition from CONFIRMED to CONFIRMED");
    }
}
