package com.example.catalog.domain.model;

import com.example.catalog.domain.exception.InvalidEventStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Event {
    private final UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private final UUID organizerId;
    private Venue venue;
    private EventStatus status;
    private final List<Show> shows;
    private final Instant createdAt;
    private Instant updatedAt;

    public Event(UUID id, String name, String description, String imageUrl, UUID organizerId, Venue venue, EventStatus status, List<Show> shows, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (name.trim().length() > 200) {
            throw new IllegalArgumentException("Event name cannot exceed 200 characters");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Event description cannot exceed 5000 characters");
        }
        if (organizerId == null) {
            throw new IllegalArgumentException("Organizer ID is required");
        }
        if (venue == null) {
            throw new IllegalArgumentException("Venue is required");
        }

        this.id = id;
        this.name = name.trim();
        this.description = description;
        this.imageUrl = imageUrl;
        this.organizerId = organizerId;
        this.venue = venue;
        this.status = status != null ? status : EventStatus.DRAFT;
        this.shows = shows != null ? new ArrayList<>(shows) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Event create(String name, String description, String imageUrl, UUID organizerId, Venue venue) {
        Instant now = Instant.now();
        return new Event(UUID.randomUUID(), name, description, imageUrl, organizerId, venue, EventStatus.DRAFT, new ArrayList<>(), now, now);
    }

    public void updateDetails(String name, String description, String imageUrl, Venue venue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (name.trim().length() > 200) {
            throw new IllegalArgumentException("Event name cannot exceed 200 characters");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Event description cannot exceed 5000 characters");
        }
        if (venue == null) {
            throw new IllegalArgumentException("Venue is required");
        }

        this.name = name.trim();
        this.description = description;
        this.imageUrl = imageUrl;
        this.venue = venue;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        if (this.status == EventStatus.CANCELLED) {
            throw new InvalidEventStateException("Cannot publish a cancelled event");
        }
        this.status = EventStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = EventStatus.CANCELLED;
        this.updatedAt = Instant.now();
        for (Show show : this.shows) {
            show.cancel();
        }
    }

    public void complete() {
        this.status = EventStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void addShow(Show show) {
        if (show == null) {
            throw new IllegalArgumentException("Show cannot be null");
        }
        this.shows.add(show);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public Venue getVenue() {
        return venue;
    }

    public EventStatus getStatus() {
        return status;
    }

    public List<Show> getShows() {
        return Collections.unmodifiableList(shows);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
