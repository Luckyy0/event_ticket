package com.example.inventory.domain;

import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.exception.InvalidQuantityException;
import com.example.inventory.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void shouldCreateInventoryQuantity_whenAllValuesAreValid() {
        InventoryQuantity qty = new InventoryQuantity(100, 70, 20, 10);

        assertThat(qty.total()).isEqualTo(100);
        assertThat(qty.available()).isEqualTo(70);
        assertThat(qty.reserved()).isEqualTo(20);
        assertThat(qty.sold()).isEqualTo(10);
    }

    @Test
    void shouldCreateInitialInventoryQuantity_fromTotal() {
        InventoryQuantity qty = InventoryQuantity.ofInitial(100);

        assertThat(qty.total()).isEqualTo(100);
        assertThat(qty.available()).isEqualTo(100);
        assertThat(qty.reserved()).isEqualTo(0);
        assertThat(qty.sold()).isEqualTo(0);
    }

    @Test
    void shouldRejectInventoryQuantity_whenAvailableIsNegative() {
        assertThatThrownBy(() -> new InventoryQuantity(100, -1, 50, 51))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Available quantity cannot be negative");
    }

    @Test
    void shouldRejectInventoryQuantity_whenReservedIsNegative() {
        assertThatThrownBy(() -> new InventoryQuantity(100, 50, -1, 51))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Reserved quantity cannot be negative");
    }

    @Test
    void shouldRejectInventoryQuantity_whenSoldIsNegative() {
        assertThatThrownBy(() -> new InventoryQuantity(100, 50, 51, -1))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Sold quantity cannot be negative");
    }

    @Test
    void shouldRejectInventoryQuantity_whenTotalDoesNotMatchSum() {
        assertThatThrownBy(() -> new InventoryQuantity(100, 50, 20, 20))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("Total quantity must equal available + reserved + sold");
    }

    @Test
    void shouldDeductAvailable_whenReserving() {
        InventoryQuantity initial = InventoryQuantity.ofInitial(100);
        InventoryQuantity updated = initial.reserve(10);

        assertThat(updated.available()).isEqualTo(90);
        assertThat(updated.reserved()).isEqualTo(10);
        assertThat(updated.sold()).isEqualTo(0);
        assertThat(updated.total()).isEqualTo(100);
    }

    @Test
    void shouldRejectDeduction_whenAvailableIsInsufficient() {
        InventoryQuantity initial = InventoryQuantity.ofInitial(10);

        assertThatThrownBy(() -> initial.reserve(15))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient available inventory");
    }

    @Test
    void shouldReturnToAvailable_whenReleasing() {
        InventoryQuantity initial = new InventoryQuantity(100, 80, 20, 0);
        InventoryQuantity updated = initial.release(15);

        assertThat(updated.available()).isEqualTo(95);
        assertThat(updated.reserved()).isEqualTo(5);
        assertThat(updated.sold()).isEqualTo(0);
        assertThat(updated.total()).isEqualTo(100);
    }

    @Test
    void shouldMoveFromReservedToSold_whenConfirming() {
        InventoryQuantity initial = new InventoryQuantity(100, 80, 20, 0);
        InventoryQuantity updated = initial.confirm(15);

        assertThat(updated.available()).isEqualTo(80);
        assertThat(updated.reserved()).isEqualTo(5);
        assertThat(updated.sold()).isEqualTo(15);
        assertThat(updated.total()).isEqualTo(100);
    }

    @Test
    void shouldNotBeExpired_whenCreatedJustNow() {
        Instant now = Instant.parse("2026-08-04T12:00:00Z");
        ReservationExpiration exp = ReservationExpiration.ofMinutesFrom(now, 15);

        assertThat(exp.isExpired(now)).isFalse();
        assertThat(exp.isExpired(now.plus(Duration.ofMinutes(14)))).isFalse();
        assertThat(exp.remainingDuration(now)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void shouldBeExpired_whenDurationHasElapsed() {
        Instant now = Instant.parse("2026-08-04T12:00:00Z");
        ReservationExpiration exp = ReservationExpiration.ofMinutesFrom(now, 15);

        Instant later = now.plus(Duration.ofMinutes(16));
        assertThat(exp.isExpired(later)).isTrue();
    }

    @Test
    void shouldCreateValueObject_withValidUuid() {
        UUID uuid = UUID.randomUUID();
        ReservationId id1 = new ReservationId(uuid);
        ReservationId id2 = new ReservationId(uuid);

        assertThat(id1.value()).isEqualTo(uuid);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void shouldRejectValueObject_withNullUuid() {
        assertThatThrownBy(() -> new ReservationId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID cannot be null");
    }
}
