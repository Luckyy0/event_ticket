package com.example.inventory.application;

import com.example.inventory.application.command.ConfirmReservationCommand;
import com.example.inventory.application.command.ReleaseReservationCommand;
import com.example.inventory.application.command.ReserveTicketCommand;
import com.example.inventory.application.port.out.*;
import com.example.inventory.application.usecase.*;
import com.example.inventory.domain.event.InventoryConfirmedEvent;
import com.example.inventory.domain.event.InventoryReleasedEvent;
import com.example.inventory.domain.event.InventoryReservedEvent;
import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.exception.ReservationExpiredException;
import com.example.inventory.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UseCasesTest {

    private InventoryPersistencePort inventoryPersistencePort;
    private ReservationPersistencePort reservationPersistencePort;
    private DistributedLockPort distributedLockPort;
    private OutboxPort outboxPort;
    private ClockPort clockPort;

    private ReserveTicketUseCaseImpl reserveTicketUseCase;
    private ConfirmReservationUseCaseImpl confirmReservationUseCase;
    private ReleaseReservationUseCaseImpl releaseReservationUseCase;
    private ExpireReservationsUseCaseImpl expireReservationsUseCase;

    private final Instant now = Instant.parse("2026-08-04T12:00:00Z");
    private final ShowId showId = new ShowId(UUID.randomUUID());
    private final TicketTypeId ticketTypeId = new TicketTypeId(UUID.randomUUID());
    private final UserId userId = new UserId(UUID.randomUUID());
    private final RequestId requestId = new RequestId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        inventoryPersistencePort = mock(InventoryPersistencePort.class);
        reservationPersistencePort = mock(ReservationPersistencePort.class);
        distributedLockPort = mock(DistributedLockPort.class);
        outboxPort = mock(OutboxPort.class);
        clockPort = mock(ClockPort.class);

        when(clockPort.now()).thenReturn(now);
        when(distributedLockPort.acquireLock(any(), any(), any(), any())).thenReturn(true);
        when(distributedLockPort.releaseLock(any(), any())).thenReturn(true);

        reserveTicketUseCase = new ReserveTicketUseCaseImpl(
                inventoryPersistencePort, reservationPersistencePort,
                distributedLockPort, outboxPort, clockPort
        );

        confirmReservationUseCase = new ConfirmReservationUseCaseImpl(
                reservationPersistencePort, inventoryPersistencePort,
                outboxPort, clockPort
        );

        releaseReservationUseCase = new ReleaseReservationUseCaseImpl(
                reservationPersistencePort, inventoryPersistencePort,
                outboxPort, clockPort
        );

        expireReservationsUseCase = new ExpireReservationsUseCaseImpl(
                reservationPersistencePort, inventoryPersistencePort,
                outboxPort, clockPort
        );
    }

    @Test
    void shouldReserveTickets_whenInventoryIsAvailable() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        when(reservationPersistencePort.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(showId, ticketTypeId))
                .thenReturn(Optional.of(inventory));
        when(reservationPersistencePort.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReserveTicketCommand command = new ReserveTicketCommand(ticketTypeId, showId, userId, 2, requestId);
        Reservation result = reserveTicketUseCase.reserveTicket(command);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(inventory.getQuantity().available()).isEqualTo(98);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(2);

        verify(outboxPort).publish(any(InventoryReservedEvent.class));
        verify(distributedLockPort).acquireLock(any(), any(), any(), any());
        verify(distributedLockPort).releaseLock(any(), any());
    }

    @Test
    void shouldReturnExistingReservation_whenRequestIdIsDuplicated() {
        Reservation existing = Reservation.create(ticketTypeId, showId, userId, requestId, 2, now, 15);
        when(reservationPersistencePort.findByRequestId(requestId)).thenReturn(Optional.of(existing));

        ReserveTicketCommand command = new ReserveTicketCommand(ticketTypeId, showId, userId, 2, requestId);
        Reservation result = reserveTicketUseCase.reserveTicket(command);

        assertThat(result).isEqualTo(existing);
        verify(distributedLockPort, never()).acquireLock(any(), any(), any(), any());
        verify(inventoryPersistencePort, never()).save(any());
    }

    @Test
    void shouldRejectReservation_whenInventoryIsInsufficient() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 1);
        when(reservationPersistencePort.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(showId, ticketTypeId))
                .thenReturn(Optional.of(inventory));

        ReserveTicketCommand command = new ReserveTicketCommand(ticketTypeId, showId, userId, 5, requestId);

        assertThatThrownBy(() -> reserveTicketUseCase.reserveTicket(command))
                .isInstanceOf(InsufficientInventoryException.class);

        verify(distributedLockPort).releaseLock(any(), any());
    }

    @Test
    void shouldConfirmReservation_whenStatusIsHeld() {
        Reservation reservation = Reservation.create(ticketTypeId, showId, userId, requestId, 2, now, 15);
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(2);

        when(reservationPersistencePort.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(showId, ticketTypeId))
                .thenReturn(Optional.of(inventory));

        confirmReservationUseCase.confirmReservation(new ConfirmReservationCommand(reservation.getId()));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(inventory.getQuantity().sold()).isEqualTo(2);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(0);
        verify(outboxPort).publish(any(InventoryConfirmedEvent.class));
    }

    @Test
    void shouldRejectConfirmation_whenReservationIsExpired() {
        Reservation reservation = Reservation.create(ticketTypeId, showId, userId, requestId, 2, now, 15);
        when(reservationPersistencePort.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(clockPort.now()).thenReturn(now.plus(16, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> confirmReservationUseCase.confirmReservation(new ConfirmReservationCommand(reservation.getId())))
                .isInstanceOf(ReservationExpiredException.class);
    }

    @Test
    void shouldReleaseReservation_whenStatusIsHeld() {
        Reservation reservation = Reservation.create(ticketTypeId, showId, userId, requestId, 2, now, 15);
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(2);

        when(reservationPersistencePort.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(showId, ticketTypeId))
                .thenReturn(Optional.of(inventory));

        releaseReservationUseCase.releaseReservation(new ReleaseReservationCommand(reservation.getId()));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventory.getQuantity().available()).isEqualTo(100);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(0);
        verify(outboxPort).publish(any(InventoryReleasedEvent.class));
    }

    @Test
    void shouldExpireAllHeldReservations_pastExpiration() {
        Reservation exp1 = Reservation.create(ticketTypeId, showId, userId, requestId, 2, now.minus(20, ChronoUnit.MINUTES), 15);
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(2);

        when(reservationPersistencePort.findExpiredHeldReservations(now)).thenReturn(List.of(exp1));
        when(inventoryPersistencePort.findByShowIdAndTicketTypeIdForUpdate(showId, ticketTypeId))
                .thenReturn(Optional.of(inventory));

        int expiredCount = expireReservationsUseCase.expireReservations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(exp1.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(inventory.getQuantity().available()).isEqualTo(100);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(0);
    }
}
