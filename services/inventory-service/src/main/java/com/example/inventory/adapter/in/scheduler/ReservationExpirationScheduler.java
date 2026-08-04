package com.example.inventory.adapter.in.scheduler;

import com.example.inventory.application.port.in.ExpireReservationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);
    private final ExpireReservationsUseCase expireReservationsUseCase;

    public ReservationExpirationScheduler(ExpireReservationsUseCase expireReservationsUseCase) {
        this.expireReservationsUseCase = expireReservationsUseCase;
    }

    @Scheduled(fixedDelayString = "${inventory.expiration-job.interval-ms:30000}")
    public void runExpirationJob() {
        int expiredCount = expireReservationsUseCase.expireReservations();
        if (expiredCount > 0) {
            log.info("Expired and reconciled {} overdue ticket reservations.", expiredCount);
        }
    }
}
