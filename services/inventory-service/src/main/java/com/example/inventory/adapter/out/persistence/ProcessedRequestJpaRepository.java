package com.example.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedRequestJpaRepository extends JpaRepository<ProcessedRequestJpaEntity, UUID> {
    boolean existsByRequestId(UUID requestId);
}
