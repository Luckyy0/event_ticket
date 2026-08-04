package com.example.inventory.domain.model;

import com.example.inventory.domain.exception.InsufficientInventoryException;
import com.example.inventory.domain.exception.InvalidQuantityException;

public record InventoryQuantity(
        int total,
        int available,
        int reserved,
        int sold
) {
    public InventoryQuantity {
        if (available < 0) {
            throw new InvalidQuantityException("Available quantity cannot be negative: " + available);
        }
        if (reserved < 0) {
            throw new InvalidQuantityException("Reserved quantity cannot be negative: " + reserved);
        }
        if (sold < 0) {
            throw new InvalidQuantityException("Sold quantity cannot be negative: " + sold);
        }
        if (total != (available + reserved + sold)) {
            throw new InvalidQuantityException("Total quantity must equal available + reserved + sold. Total: "
                    + total + ", Sum: " + (available + reserved + sold));
        }
    }

    public static InventoryQuantity ofInitial(int total) {
        if (total < 0) {
            throw new InvalidQuantityException("Total quantity cannot be negative: " + total);
        }
        return new InventoryQuantity(total, total, 0, 0);
    }

    public InventoryQuantity reserve(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero: " + quantity);
        }
        if (available < quantity) {
            throw new InsufficientInventoryException("Insufficient available inventory. Requested: "
                    + quantity + ", Available: " + available);
        }
        return new InventoryQuantity(total, available - quantity, reserved + quantity, sold);
    }

    public InventoryQuantity confirm(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero: " + quantity);
        }
        if (reserved < quantity) {
            throw new InvalidQuantityException("Cannot confirm more than reserved quantity. Requested: "
                    + quantity + ", Reserved: " + reserved);
        }
        return new InventoryQuantity(total, available, reserved - quantity, sold + quantity);
    }

    public InventoryQuantity release(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero: " + quantity);
        }
        if (reserved < quantity) {
            throw new InvalidQuantityException("Cannot release more than reserved quantity. Requested: "
                    + quantity + ", Reserved: " + reserved);
        }
        return new InventoryQuantity(total, available + quantity, reserved - quantity, sold);
    }
}
