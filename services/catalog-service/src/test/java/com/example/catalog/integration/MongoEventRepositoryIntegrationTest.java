package com.example.catalog.integration;

import com.example.catalog.adapter.out.persistence.MongoEventRepositoryAdapter;
import com.example.catalog.adapter.out.persistence.SpringDataMongoEventRepository;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.SaleWindow;
import com.example.catalog.domain.model.Show;
import com.example.catalog.domain.model.TicketType;
import com.example.catalog.domain.model.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(MongoEventRepositoryAdapter.class)
@ActiveProfiles("test")
class MongoEventRepositoryIntegrationTest {

    static final MongoDBContainer mongo;

    static {
        mongo = new MongoDBContainer("mongo:7.0");
        mongo.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private MongoEventRepositoryAdapter repositoryAdapter;

    @Autowired
    private SpringDataMongoEventRepository springDataMongoEventRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDb() {
        springDataMongoEventRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveEvent() {
        UUID organizerId = UUID.randomUUID();
        Venue venue = new Venue("My Dinh Stadium", "Le Duc Tho", "Hanoi", 40000);
        Event event = Event.create("Rock Concert 2026", "Big rock concert", "https://image.com/rock.jpg", organizerId, venue);
        event.publish();

        Instant showStart = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant showEnd = showStart.plus(3, ChronoUnit.HOURS);
        SaleWindow saleWindow = new SaleWindow(Instant.now().plus(1, ChronoUnit.DAYS), showStart.minus(2, ChronoUnit.HOURS));
        TicketType vip = new TicketType(UUID.randomUUID(), "VIP", "VIP Zone", new BigDecimal("1500000"), "VND", 200, 1);

        Show show = Show.create(event.getId(), showStart, showEnd, saleWindow, List.of(vip));
        event.addShow(show);

        repositoryAdapter.save(event);

        Optional<Event> retrieved = repositoryAdapter.findById(event.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Rock Concert 2026");
        assertThat(retrieved.get().getShows()).hasSize(1);
        assertThat(retrieved.get().getShows().get(0).getTicketTypes()).hasSize(1);
        assertThat(retrieved.get().getShows().get(0).getTicketTypes().get(0).getName()).isEqualTo("VIP");
    }

    @Test
    void shouldFindPublishedEventsByCity() {
        UUID organizerId = UUID.randomUUID();
        Venue hanoiVenue = new Venue("Hanoi Opera", "Trang Tien", "Hanoi", 1000);
        Venue hcmVenue = new Venue("Saigon Opera", "Lam Son", "Ho Chi Minh", 1500);

        Event eventHanoi = Event.create("Classical Night Hanoi", "desc", null, organizerId, hanoiVenue);
        eventHanoi.publish();
        repositoryAdapter.save(eventHanoi);

        Event eventHcm = Event.create("Jazz Night Saigon", "desc", null, organizerId, hcmVenue);
        eventHcm.publish();
        repositoryAdapter.save(eventHcm);

        Page<Event> hanoiEvents = repositoryAdapter.findPublishedEvents(null, "Hanoi", null, null, PageRequest.of(0, 10));
        assertThat(hanoiEvents.getTotalElements()).isEqualTo(1);
        assertThat(hanoiEvents.getContent().get(0).getName()).isEqualTo("Classical Night Hanoi");
    }

    @Test
    void shouldNotReturnDraftEvents_inPublishedQuery() {
        UUID organizerId = UUID.randomUUID();
        Venue venue = new Venue("Hall A", "Addr", "Danang", 500);
        Event draftEvent = Event.create("Draft Conference", "desc", null, organizerId, venue);
        repositoryAdapter.save(draftEvent);

        Page<Event> results = repositoryAdapter.findPublishedEvents(null, null, null, null, PageRequest.of(0, 10));
        assertThat(results.getTotalElements()).isEqualTo(0);
    }
}
