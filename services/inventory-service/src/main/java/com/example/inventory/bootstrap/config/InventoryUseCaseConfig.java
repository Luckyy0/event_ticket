package com.example.inventory.bootstrap.config;

import com.example.inventory.application.port.in.*;
import com.example.inventory.application.port.out.*;
import com.example.inventory.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryUseCaseConfig {

    @Bean
    public ReserveTicketUseCase reserveTicketUseCase(
            InventoryPersistencePort inventoryPersistencePort,
            ReservationPersistencePort reservationPersistencePort,
            DistributedLockPort distributedLockPort,
            OutboxPort outboxPort,
            ClockPort clockPort
    ) {
        return new ReserveTicketUseCaseImpl(
                inventoryPersistencePort,
                reservationPersistencePort,
                distributedLockPort,
                outboxPort,
                clockPort
        );
    }

    @Bean
    public ConfirmReservationUseCase confirmReservationUseCase(
            ReservationPersistencePort reservationPersistencePort,
            InventoryPersistencePort inventoryPersistencePort,
            OutboxPort outboxPort,
            ClockPort clockPort
    ) {
        return new ConfirmReservationUseCaseImpl(
                reservationPersistencePort,
                inventoryPersistencePort,
                outboxPort,
                clockPort
        );
    }

    @Bean
    public ReleaseReservationUseCase releaseReservationUseCase(
            ReservationPersistencePort reservationPersistencePort,
            InventoryPersistencePort inventoryPersistencePort,
            OutboxPort outboxPort,
            ClockPort clockPort
    ) {
        return new ReleaseReservationUseCaseImpl(
                reservationPersistencePort,
                inventoryPersistencePort,
                outboxPort,
                clockPort
        );
    }

    @Bean
    public ExpireReservationsUseCase expireReservationsUseCase(
            ReservationPersistencePort reservationPersistencePort,
            InventoryPersistencePort inventoryPersistencePort,
            OutboxPort outboxPort,
            ClockPort clockPort
    ) {
        return new ExpireReservationsUseCaseImpl(
                reservationPersistencePort,
                inventoryPersistencePort,
                outboxPort,
                clockPort
        );
    }

    @Bean
    public QueryInventoryUseCase queryInventoryUseCase(InventoryPersistencePort inventoryPersistencePort) {
        return new QueryInventoryUseCaseImpl(inventoryPersistencePort);
    }
}
