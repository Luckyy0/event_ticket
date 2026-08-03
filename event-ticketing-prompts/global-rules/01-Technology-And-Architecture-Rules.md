# TECHNOLOGY AND ARCHITECTURE RULES

These rules govern all technology choices, architectural patterns, and infrastructure decisions across the entire system. Every execution step must comply with these rules without exception.

---

## 1. Language and Platform

- Java 21.
- Spring Boot 3.x.
- Jakarta EE namespace (not javax).
- Gradle Kotlin DSL (preferred) or Maven.
- Docker for containerization.
- Docker Compose for local development environment.
- Kubernetes at advanced deployment stage.

---

## 2. Virtual Threads

Virtual Threads (Project Loom) may be used for blocking I/O workloads where appropriate.

Do NOT enable Virtual Threads blindly. When proposing Virtual Threads, you MUST analyze:
- Whether the workload is blocking I/O or CPU-bound.
- Database connection pool sizing (HikariCP max pool size becomes the real concurrency limit).
- Kafka consumer concurrency model (KafkaListener concurrency vs Virtual Threads).
- ThreadLocal usage and SecurityContext propagation.
- Pinning risks (synchronized blocks, native calls).
- Downstream resource limits (Redis connection pool, HTTP client connection pool).

---

## 3. Prohibited Technologies

Do NOT use any of the following:
- Netflix Zuul.
- Hystrix.
- Eureka (unless there is a specific architectural justification).
- Ribbon.
- Spring Cloud Sleuth (legacy — use Micrometer Tracing / OpenTelemetry instead).
- Any library that is deprecated or no longer maintained.
- H2 database for integration tests that test PostgreSQL-specific behavior.
- `latest` tag for any Docker image.

---

## 4. Architecture Patterns

- **Microservices Architecture** — each service is independently deployable.
- **Hexagonal Architecture** — for all core services (Inventory, Order, Payment, Ticket).
- **Domain-Driven Design** — tactical patterns (Aggregate, Entity, Value Object, Domain Event, Domain Service) applied where appropriate.
- **Database per Service** — each service owns its own database or schema.
- **API Gateway Pattern** — Spring Cloud Gateway for routing, rate limiting, JWT validation.
- **Backend for Frontend (BFF) Pattern** — dedicated BFF service for browser clients.
- **Event-Driven Architecture** — Kafka for asynchronous inter-service communication.
- **Saga Pattern** — for distributed transactions across services (choreography or orchestration).
- **Transactional Outbox Pattern** — for reliable event publishing.
- **Idempotent Consumer** — every Kafka consumer must handle duplicate messages safely.
- **CQRS** — local CQRS only when there is a genuine need, not by default.

**Rule**: Do NOT apply a pattern solely to increase system complexity. Every pattern used must be justified with:
- The problem it solves.
- The benefit it provides.
- The cost it introduces.
- The trade-offs.
- When it should NOT be used.

---

## 5. Synchronous Communication

- REST API with OpenAPI 3 specification.
- Spring Cloud Gateway for routing.
- **Timeout** is mandatory on every outbound HTTP call.
- **Retry** must be controlled — never retry write requests blindly.
- **Circuit Breaker** via Resilience4j.
- **Bulkhead** when appropriate to isolate failure domains.

**Retry rules for write requests** — retry is only permitted when ALL of the following exist:
- Idempotency Key.
- Request identifier.
- Server-side deduplication mechanism.

---

## 6. Asynchronous Communication

- Apache Kafka as the message broker.
- Transactional Outbox Pattern for publishing domain events.
- At-least-once delivery semantics.
- Idempotent Consumer on every consumer.
- Dead Letter Topic (DLT) for poison messages.
- Retry Topic for transient failures.
- Event versioning from day one.
- Schema evolution strategy.

Serialization:
- JSON in early stages.
- Avro or Protobuf with Schema Registry at advanced production stage.

**Critical rule**: Do NOT claim Kafka guarantees exactly-once for the entire business transaction. You MUST distinguish between:
- Kafka delivery semantics (producer/consumer level).
- Kafka transactions (producer transactional writes).
- Database transactions (local ACID).
- Business-level exactly-once effect (achieved via idempotency, not Kafka alone).

---

## 7. PostgreSQL

Used for all transactional data: Inventory, Reservation, Order, Payment, Ticket, Outbox, Inbox/processed messages, Audit.

Requirements:
- Flyway for schema migrations.
- Explicit indexes on all query patterns.
- Optimistic Locking or Pessimistic Locking — chosen per use case with justification.
- Atomic SQL updates (e.g., `UPDATE ... SET available = available - :qty WHERE available >= :qty`).
- Table partitioning for large tables when justified by data volume analysis.
- Keyset Pagination for large datasets.
- No abusing OFFSET pagination on large tables.
- Data archival strategy for tables expected to grow to millions of rows.

---

## 8. MongoDB

Used for read-heavy, search-oriented data: Event Catalog, Event Detail, Venue metadata, denormalized projections.

Requirements:
- Mongock for schema migrations.
- Explicit indexes.
- No unbounded document growth.
- Analyze consistency requirements before choosing MongoDB — do NOT default to MongoDB without justification.

---

## 9. Redis Cluster

Used for: short-lived ticket holds/reservations (Lua Script), rate limiting, idempotency cache, temporary session metadata, distributed cache.

**Critical rule**: Redis is NOT the final source of truth for Order or Payment data. PostgreSQL is always the durable source of truth.

When using Redis for ticket reservation, you MUST explain:
- How the Lua Script guarantees atomicity.
- How TTL works for reservation expiration.
- The synchronization strategy between Redis and PostgreSQL.
- What happens when Redis restarts (data loss scenario).
- What happens when Kafka or PostgreSQL is temporarily unavailable.
- The reconciliation mechanism between Redis and PostgreSQL.
- How expired reservations are handled.

---

## 10. Strict Prohibitions

```
Do NOT share databases between services.
Do NOT access another service's tables directly.
Do NOT share JPA entities across services.
Do NOT hold a database transaction open while calling a remote API.
Do NOT retry write requests without idempotency.
Do NOT treat Redis as source of truth for Orders or Payments.
Do NOT use distributed locks as a substitute for database constraints when constraints can solve the problem.
Do NOT default to Redis locks for every concurrency problem.
Do NOT create a common-library containing all domain models.
Do NOT depend on Kafka message ordering across the entire system.
Do NOT assume events are delivered exactly once.
```

---

## 11. Infrastructure Conventions

- All timestamps in UTC (ISO 8601).
- ID strategy: UUID v7 or ULID (sortable, time-ordered).
- Transaction boundaries must be explicitly defined and documented.
- Connection pool sizing must be justified (HikariCP, Lettuce, Kafka).
- Graceful shutdown must be implemented for every service.
- All configuration via environment variables or Spring profiles — no hardcoded secrets.
