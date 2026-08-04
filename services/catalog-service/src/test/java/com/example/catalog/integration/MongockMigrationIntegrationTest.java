package com.example.catalog.integration;

import com.example.catalog.adapter.out.persistence.EventDocument;
import com.example.catalog.bootstrap.migration.V001_SeedInitialData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class MongockMigrationIntegrationTest {

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
    private MongoTemplate mongoTemplate;

    @Test
    void shouldRunMigration_andSeedInitialData() {
        V001_SeedInitialData migration = new V001_SeedInitialData();
        migration.execute(mongoTemplate);

        long count = mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), EventDocument.class);
        assertThat(count).isEqualTo(3);

        EventDocument flashSaleEvent = mongoTemplate.findById(V001_SeedInitialData.FLASH_SALE_EVENT_ID, EventDocument.class);
        assertThat(flashSaleEvent).isNotNull();
        assertThat(flashSaleEvent.getName()).isEqualTo("Flash Sale Concert");
        assertThat(flashSaleEvent.getShows()).hasSize(1);
        assertThat(flashSaleEvent.getShows().get(0).getTicketTypes()).hasSize(1);
        assertThat(flashSaleEvent.getShows().get(0).getTicketTypes().get(0).getTotalQuantity()).isEqualTo(100);
    }
}
