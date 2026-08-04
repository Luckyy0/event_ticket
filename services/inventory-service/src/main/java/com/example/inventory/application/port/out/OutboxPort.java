package com.example.inventory.application.port.out;

public interface OutboxPort {
    void publish(Object domainEvent);
}
