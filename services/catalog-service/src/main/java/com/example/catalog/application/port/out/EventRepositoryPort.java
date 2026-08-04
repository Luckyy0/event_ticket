package com.example.catalog.application.port.out;

import com.example.catalog.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EventRepositoryPort {
    Event save(Event event);
    Optional<Event> findById(UUID id);
    Page<Event> findPublishedEvents(String search, String city, Instant dateFrom, Instant dateTo, Pageable pageable);
    Page<Event> findAllEvents(Pageable pageable);
    void deleteById(UUID id);
}
