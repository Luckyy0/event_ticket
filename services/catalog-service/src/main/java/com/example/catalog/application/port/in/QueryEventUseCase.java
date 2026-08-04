package com.example.catalog.application.port.in;

import com.example.catalog.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface QueryEventUseCase {
    Page<Event> listPublishedEvents(String search, String city, Instant dateFrom, Instant dateTo, Pageable pageable);
    Event getEventById(UUID eventId);
    Page<Event> listAllEventsForAdmin(Pageable pageable);
    void deleteEventForAdmin(UUID eventId);
}
