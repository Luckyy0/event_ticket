package com.example.catalog.adapter.in.web;

import com.example.catalog.adapter.in.web.dto.EventResponse;
import com.example.catalog.application.port.in.QueryEventUseCase;
import com.example.catalog.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final QueryEventUseCase queryEventUseCase;

    public AdminCatalogController(QueryEventUseCase queryEventUseCase) {
        this.queryEventUseCase = queryEventUseCase;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> listAllEventsForAdmin(@PageableDefault(size = 20) Pageable pageable) {
        Page<Event> events = queryEventUseCase.listAllEventsForAdmin(pageable);
        return ResponseEntity.ok(events.map(EventResponse::fromDomain));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        queryEventUseCase.deleteEventForAdmin(eventId);
        return ResponseEntity.noContent().build();
    }
}
