package com.example.catalog.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataMongoEventRepository extends MongoRepository<EventDocument, String> {
}
