package com.example.catalog.application.usecase;

import com.example.catalog.application.port.in.QueryEventUseCase;
import com.example.catalog.application.port.out.EventRepositoryPort;
import com.example.catalog.domain.exception.EventNotFoundException;
import com.example.catalog.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class QueryEventUseCaseImpl implements QueryEventUseCase {

    private final EventRepositoryPort eventRepository;

    public QueryEventUseCaseImpl(EventRepositoryPort eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Page<Event> listPublishedEvents(String search, String city, Instant dateFrom, Instant dateTo, Pageable pageable) {
        return eventRepository.findPublishedEvents(search, city, dateFrom, dateTo, pageable);
    }

    @Override
    public Event getEventById(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Override
    public Page<Event> listAllEventsForAdmin(Pageable pageable) {
        return eventRepository.findAllEvents(pageable);
    }

    @Override
    public void deleteEventForAdmin(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        eventRepository.deleteById(eventId);
    }
}
