package com.example.inventory.adapter.in.web;

import com.example.inventory.adapter.in.web.dto.InventoryResponse;
import com.example.inventory.adapter.in.web.dto.ReservationResponse;
import com.example.inventory.adapter.in.web.dto.ReserveTicketRequest;
import com.example.inventory.application.command.ConfirmReservationCommand;
import com.example.inventory.application.command.ReleaseReservationCommand;
import com.example.inventory.application.command.ReserveTicketCommand;
import com.example.inventory.application.port.in.ConfirmReservationUseCase;
import com.example.inventory.application.port.in.QueryInventoryUseCase;
import com.example.inventory.application.port.in.ReleaseReservationUseCase;
import com.example.inventory.application.port.in.ReserveTicketUseCase;
import com.example.inventory.bootstrap.config.SecurityUtils;
import com.example.inventory.domain.model.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {

    private final ReserveTicketUseCase reserveTicketUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final QueryInventoryUseCase queryInventoryUseCase;

    public InventoryController(
            ReserveTicketUseCase reserveTicketUseCase,
            ConfirmReservationUseCase confirmReservationUseCase,
            ReleaseReservationUseCase releaseReservationUseCase,
            QueryInventoryUseCase queryInventoryUseCase
    ) {
        this.reserveTicketUseCase = reserveTicketUseCase;
        this.confirmReservationUseCase = confirmReservationUseCase;
        this.releaseReservationUseCase = releaseReservationUseCase;
        this.queryInventoryUseCase = queryInventoryUseCase;
    }

    @PostMapping("/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> reserveTicket(@Valid @RequestBody ReserveTicketRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ReserveTicketCommand command = new ReserveTicketCommand(
                new TicketTypeId(request.ticketTypeId()),
                new ShowId(request.showId()),
                new UserId(currentUserId),
                request.quantity(),
                new RequestId(request.requestId())
        );

        Reservation reservation = reserveTicketUseCase.reserveTicket(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.fromDomain(reservation));
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirmReservation(@PathVariable UUID reservationId) {
        confirmReservationUseCase.confirmReservation(new ConfirmReservationCommand(new ReservationId(reservationId)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reservations/{reservationId}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseReservation(@PathVariable UUID reservationId) {
        releaseReservationUseCase.releaseReservation(new ReleaseReservationCommand(new ReservationId(reservationId)));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shows/{showId}/ticket-types/{ticketTypeId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable UUID showId,
            @PathVariable UUID ticketTypeId
    ) {
        Inventory inventory = queryInventoryUseCase.getInventory(new ShowId(showId), new TicketTypeId(ticketTypeId));
        return ResponseEntity.ok(InventoryResponse.fromDomain(inventory));
    }

    @GetMapping("/shows/{showId}")
    public ResponseEntity<List<InventoryResponse>> getInventoriesByShow(@PathVariable UUID showId) {
        List<Inventory> inventories = queryInventoryUseCase.getInventoriesByShow(new ShowId(showId));
        return ResponseEntity.ok(inventories.stream().map(InventoryResponse::fromDomain).toList());
    }
}
