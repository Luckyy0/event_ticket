package com.example.catalog.domain;

import com.example.catalog.domain.exception.InvalidEventStateException;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.EventStatus;
import com.example.catalog.domain.model.Venue;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventDomainTest {

    private final UUID organizerId = UUID.randomUUID();
    private final Venue venue = new Venue("National Stadium", "123 Stadium Way", "Ho Chi Minh", 50000);

    @Test
    void shouldCreateEvent_whenAllFieldsAreValid() {
        Event event = Event.create(
                "Summer Music Festival",
                "Annual summer music extravaganza",
                "https://images.example.com/banner.jpg",
                organizerId,
                venue
        );

        assertThat(event.getId()).isNotNull();
        assertThat(event.getName()).isEqualTo("Summer Music Festival");
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getOrganizerId()).isEqualTo(organizerId);
        assertThat(event.getVenue().getCity()).isEqualTo("Ho Chi Minh");
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectEvent_whenNameIsEmpty() {
        assertThatThrownBy(() -> Event.create("", "description", null, organizerId, venue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event name is required");
    }

    @Test
    void shouldRejectEvent_whenNameExceedsMaxLength() {
        String longName = "A".repeat(201);
        assertThatThrownBy(() -> Event.create(longName, "description", null, organizerId, venue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event name cannot exceed 200 characters");
    }

    @Test
    void shouldRejectEvent_whenOrganizerIdIsNull() {
        assertThatThrownBy(() -> Event.create("Valid Name", "description", null, null, venue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organizer ID is required");
    }

    @Test
    void shouldRejectEvent_whenVenueIsNull() {
        assertThatThrownBy(() -> Event.create("Valid Name", "description", null, organizerId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Venue is required");
    }

    @Test
    void shouldPublishEvent_whenEventIsDraft() {
        Event event = Event.create("Concert", "desc", null, organizerId, venue);
        event.publish();

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void shouldRejectPublish_whenEventIsAlreadyCancelled() {
        Event event = Event.create("Concert", "desc", null, organizerId, venue);
        event.cancel();

        assertThatThrownBy(event::publish)
                .isInstanceOf(InvalidEventStateException.class)
                .hasMessageContaining("Cannot publish a cancelled event");
    }

    @Test
    void shouldCancelEvent_whenEventIsPublished() {
        Event event = Event.create("Concert", "desc", null, organizerId, venue);
        event.publish();
        event.cancel();

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
    }
}
