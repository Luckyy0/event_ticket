package com.example.inventory.domain;

import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.exception.InvalidQuantityException;
import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryAggregateTest {

    private final ShowId showId = new ShowId(UUID.randomUUID());
    private final TicketTypeId ticketTypeId = new TicketTypeId(UUID.randomUUID());

    @Test
    void shouldReserveTickets_whenEnoughAvailable() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(10);

        assertThat(inventory.getQuantity().available()).isEqualTo(90);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(10);
        assertThat(inventory.getQuantity().sold()).isEqualTo(0);
        assertThat(inventory.getQuantity().total()).isEqualTo(100);
    }

    @Test
    void shouldRejectReservation_whenInsufficientAvailable() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 10);

        assertThatThrownBy(() -> inventory.reserve(11))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient available inventory");
    }

    @Test
    void shouldRejectReservation_whenQuantityIsZeroOrNegative() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);

        assertThatThrownBy(() -> inventory.reserve(0))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        assertThatThrownBy(() -> inventory.reserve(-5))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    void shouldConfirmReservation_whenReservedQuantitySufficient() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(20);
        inventory.confirmReservation(15);

        assertThat(inventory.getQuantity().available()).isEqualTo(80);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(5);
        assertThat(inventory.getQuantity().sold()).isEqualTo(15);
    }

    @Test
    void shouldRejectConfirmation_whenReservedQuantityInsufficient() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(5);

        assertThatThrownBy(() -> inventory.confirmReservation(10))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Cannot confirm more than reserved quantity");
    }

    @Test
    void shouldReleaseReservation_whenReservedQuantitySufficient() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(20);
        inventory.releaseReservation(15);

        assertThat(inventory.getQuantity().available()).isEqualTo(95);
        assertThat(inventory.getQuantity().reserved()).isEqualTo(5);
        assertThat(inventory.getQuantity().sold()).isEqualTo(0);
    }

    @Test
    void shouldRejectRelease_whenReservedQuantityInsufficient() {
        Inventory inventory = Inventory.create(showId, ticketTypeId, 100);
        inventory.reserve(5);

        assertThatThrownBy(() -> inventory.releaseReservation(10))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Cannot release more than reserved quantity");
    }
}
