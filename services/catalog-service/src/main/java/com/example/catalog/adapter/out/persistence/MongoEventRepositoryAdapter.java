package com.example.catalog.adapter.out.persistence;

import com.example.catalog.application.port.out.EventRepositoryPort;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MongoEventRepositoryAdapter implements EventRepositoryPort {

    private final SpringDataMongoEventRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    public MongoEventRepositoryAdapter(SpringDataMongoEventRepository mongoRepository, MongoTemplate mongoTemplate) {
        this.mongoRepository = mongoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Event save(Event event) {
        EventDocument document = DocumentMapper.toDocument(event);
        EventDocument savedDoc = mongoRepository.save(document);
        return DocumentMapper.toDomain(savedDoc);
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return mongoRepository.findById(id.toString())
                .map(DocumentMapper::toDomain);
    }

    @Override
    public Page<Event> findPublishedEvents(String search, String city, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Must be PUBLISHED for public searches
        criteriaList.add(Criteria.where("status").is(EventStatus.PUBLISHED.name()));

        if (city != null && !city.trim().isEmpty()) {
            criteriaList.add(Criteria.where("venue.city").regex("^" + city.trim() + "$", "i"));
        }

        if (dateFrom != null && dateTo != null) {
            criteriaList.add(Criteria.where("shows.startTime").gte(dateFrom).lte(dateTo));
        } else if (dateFrom != null) {
            criteriaList.add(Criteria.where("shows.startTime").gte(dateFrom));
        } else if (dateTo != null) {
            criteriaList.add(Criteria.where("shows.startTime").lte(dateTo));
        }

        if (search != null && !search.trim().isEmpty()) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matching(search.trim()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, EventDocument.class);

        query.with(pageable);
        List<EventDocument> documents = mongoTemplate.find(query, EventDocument.class);

        List<Event> events = documents.stream()
                .map(DocumentMapper::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(events, pageable, total);
    }

    @Override
    public Page<Event> findAllEvents(Pageable pageable) {
        Query query = new Query().with(pageable);
        long total = mongoTemplate.count(query, EventDocument.class);
        List<EventDocument> documents = mongoTemplate.find(query, EventDocument.class);

        List<Event> events = documents.stream()
                .map(DocumentMapper::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(events, pageable, total);
    }

    @Override
    public void deleteById(UUID id) {
        mongoRepository.deleteById(id.toString());
    }
}
