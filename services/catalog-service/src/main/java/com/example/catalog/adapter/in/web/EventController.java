package com.example.catalog.adapter.in.web;

import com.example.catalog.adapter.in.web.dto.EventResponse;
import com.example.catalog.adapter.in.web.dto.ShowResponse;
import com.example.catalog.application.port.in.QueryEventUseCase;
import com.example.catalog.domain.exception.ShowNotFoundException;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final QueryEventUseCase queryEventUseCase;

    public EventController(QueryEventUseCase queryEventUseCase) {
        this.queryEventUseCase = queryEventUseCase;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> listPublishedEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<Event> events = queryEventUseCase.listPublishedEvents(search, city, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(events.map(EventResponse::fromDomain));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID eventId) {
        Event event = queryEventUseCase.getEventById(eventId);
        return ResponseEntity.ok(EventResponse.fromDomain(event));
    }

    @GetMapping("/{eventId}/shows")
    public ResponseEntity<List<ShowResponse>> listShowsForEvent(@PathVariable UUID eventId) {
        Event event = queryEventUseCase.getEventById(eventId);
        List<ShowResponse> shows = event.getShows().stream()
                .map(ShowResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/{eventId}/shows/{showId}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable UUID eventId, @PathVariable UUID showId) {
        Event event = queryEventUseCase.getEventById(eventId);
        Show show = event.getShows().stream()
                .filter(s -> s.getId().equals(showId))
                .findFirst()
                .orElseThrow(() -> new ShowNotFoundException(showId));
        return ResponseEntity.ok(ShowResponse.fromDomain(show));
    }
}
