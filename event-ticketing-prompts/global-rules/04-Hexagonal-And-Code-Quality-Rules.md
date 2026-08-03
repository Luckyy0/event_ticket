# HEXAGONAL ARCHITECTURE AND CODE QUALITY RULES

These rules govern the internal architecture of every core service, package structure, dependency direction, coding standards, and code quality enforcement.

---

## 1. Hexagonal Architecture — Layer Structure

All core services (inventory-service, order-service, payment-service, ticket-service) MUST be organized into the following layers:

```
domain
application
adapter
bootstrap
```

### Reference Package Structure

```
inventory-service
└── src/main/java/com/example/inventory
    ├── domain
    │   ├── model          ← Aggregates, Entities, Value Objects
    │   ├── event          ← Domain Events (plain Java, no framework dependency)
    │   ├── exception      ← Domain-specific exceptions
    │   ├── policy         ← Domain policies and business rules
    │   └── service        ← Domain Services (stateless logic operating on domain objects)
    ├── application
    │   ├── port
    │   │   ├── in         ← Input Ports (Use Case interfaces)
    │   │   └── out        ← Output Ports (Repository, Messaging, External service interfaces)
    │   ├── command        ← Command objects (input DTOs for write operations)
    │   ├── query          ← Query objects (input DTOs for read operations)
    │   └── usecase        ← Use Case implementations (orchestrate domain + ports)
    ├── adapter
    │   ├── in
    │   │   ├── web        ← REST Controllers (inbound HTTP adapter)
    │   │   └── messaging  ← Kafka Consumers (inbound messaging adapter)
    │   └── out
    │       ├── persistence ← JPA Repositories, JPA Entities, Mappers (outbound persistence adapter)
    │       ├── redis       ← Redis operations, Lua Script execution (outbound cache adapter)
    │       └── messaging   ← Kafka Producers, Outbox Publisher (outbound messaging adapter)
    └── bootstrap
        ├── config         ← Spring configuration classes, Bean definitions
        └── InventoryApplication.java
```

---

## 2. Dependency Rules

```
Adapter → Application → Domain
```

### Domain Layer

The domain layer is the innermost layer. It MUST NOT depend on:
- Spring Framework (no @Component, @Service, @Transactional, @Autowired).
- JPA (no @Entity, @Table, @Column, @Id from jakarta.persistence).
- Kafka.
- Redis.
- HTTP.
- Keycloak.
- Any framework annotation.

The domain layer contains only:
- Pure Java classes.
- Business logic.
- Business validation.
- State transitions.
- Domain events (plain Java records/classes).
- Domain exceptions.

### Application Layer

- Defines Input Ports (Use Case interfaces that the adapter calls).
- Defines Output Ports (Repository/Messaging interfaces that the adapter implements).
- Contains Use Case implementations that orchestrate domain logic and output ports.
- May use `@Transactional` from Spring (this is the one acceptable framework dependency in this layer).
- MUST NOT contain HTTP-specific logic (no HttpServletRequest, no @RequestBody).
- MUST NOT contain Kafka-specific logic (no @KafkaListener, no ConsumerRecord).

### Adapter Layer

- Implements Output Ports (persistence adapters, messaging adapters, Redis adapters).
- Calls Input Ports (controllers call use cases, Kafka consumers call use cases).
- Contains all framework-specific code (JPA entities, Kafka listeners, REST controllers).
- Maps between adapter-specific models and domain models.

### Bootstrap Layer

- Spring Boot application entry point.
- Spring configuration classes.
- Bean wiring.
- Profile configuration.

---

## 3. Domain-Driven Design — Tactical Patterns

Apply the following patterns where appropriate:

| Pattern | Usage |
|---|---|
| Aggregate | Consistency boundary. One Aggregate per transaction. |
| Entity | Has identity, mutable state, lifecycle. |
| Value Object | No identity, immutable, compared by value. Use Java `record`. |
| Domain Event | Records something that happened in the domain. Plain Java. |
| Domain Service | Stateless logic that does not naturally belong to a single Entity or Value Object. |
| Domain Policy | Encapsulates a business rule that can vary or be composed. |

**Rule**: Do NOT over-engineer DDD. Apply tactical patterns only when they provide clarity. A simple CRUD service (e.g., catalog-service) does not need full DDD treatment.

---

## 4. Coding Standards

### Mandatory

- Java 21 features (records, sealed classes, pattern matching where beneficial).
- Constructor Injection exclusively (no field injection, no setter injection).
- Immutable objects where appropriate (especially Value Objects and DTOs).
- Java `record` for DTOs, Commands, Queries, Value Objects, and Events.
- Explicit `@Transactional` boundaries with clear scope documentation.
- Validation in domain layer (invariant checks) AND adapter layer (input validation).
- Structured exception handling with specific exception types.
- Structured logging (SLF4J + Logback) with contextual fields.

### Prohibited

```
Do NOT place business logic in Controllers.
Do NOT expose JPA Entities directly via REST API.
Do NOT use field injection (@Autowired on fields).
Do NOT use @Data on JPA Entities.
Do NOT use Optional as a JPA Entity field or request body field.
Do NOT catch generic Exception without a specific reason.
Do NOT swallow exceptions (catch and do nothing).
Do NOT call remote APIs inside long-running database transactions.
Do NOT create excessively large transactions.
Do NOT use distributed locks as a substitute for database constraints.
Do NOT default to Redis locks for every concurrency problem.
Do NOT use Lombok if it obscures the domain model — justify usage if applied.
Do NOT use bidirectional JPA relationships unless genuinely necessary.
Do NOT use uncontrolled cascade operations.
Do NOT use EAGER fetching by default.
Do NOT depend on global Kafka message ordering.
Do NOT assume events are delivered exactly once.
Do NOT create abstractions before there is a concrete need.
```

---

## 5. JPA Entity vs Domain Model Separation

Every core service MUST separate:

| Concept | Location | Purpose |
|---|---|---|
| Domain Model | `domain/model/` | Pure business logic, no JPA annotations |
| JPA Entity | `adapter/out/persistence/` | Database mapping only |
| Mapper | `adapter/out/persistence/` | Converts between Domain Model ↔ JPA Entity |

Example:

```java
// domain/model/Reservation.java — pure domain
public class Reservation {
    private final ReservationId id;
    private final TicketTypeId ticketTypeId;
    private final int quantity;
    private ReservationStatus status;
    private final Instant expiresAt;
    // business methods, state transitions, validations
}

// adapter/out/persistence/ReservationJpaEntity.java — JPA mapping
@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {
    @Id private UUID id;
    private UUID ticketTypeId;
    private int quantity;
    @Enumerated(EnumType.STRING) private String status;
    private Instant expiresAt;
    @Version private int version;
}

// adapter/out/persistence/ReservationMapper.java — conversion
public class ReservationMapper {
    public Reservation toDomain(ReservationJpaEntity entity) { ... }
    public ReservationJpaEntity toEntity(Reservation domain) { ... }
}
```

---

## 6. Error Handling

Every service MUST have a Global Exception Handler (`@RestControllerAdvice`).

### Standard Error Response Format

```json
{
  "code": "INVENTORY_NOT_AVAILABLE",
  "message": "Requested ticket quantity is not available",
  "traceId": "abc-123-def",
  "timestamp": "2026-01-01T10:00:00Z",
  "details": []
}
```

### Error Categories

| Category | HTTP Status | Example |
|---|---|---|
| Validation error | 400 | Invalid quantity, missing required field |
| Authentication error | 401 | Missing or invalid token |
| Authorization error | 403 | Insufficient role or scope, not resource owner |
| Business rule violation | 409 or 422 | Over-selling, invalid state transition |
| Resource not found | 404 | Order not found |
| Conflict | 409 | Duplicate idempotency key with different payload |
| Rate limit exceeded | 429 | Too many requests |
| Infrastructure failure | 503 | Database unavailable, Redis down |
| Temporary downstream failure | 502 | Payment provider timeout |

**Rules**:
- NEVER return stack traces to the client.
- NEVER transform all errors into HTTP 500.
- Always include `traceId` for correlation.

---

## 7. ArchUnit Tests

Every core service SHOULD include ArchUnit tests to enforce architectural rules:

```java
@AnalyzeClasses(packages = "com.example.inventory")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jpa =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories_directly =
        noClasses().that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out.persistence..");
}
```

---

## 8. Response Format for Technical Implementation

When answering an implementation request, use this structure:

```
1.  Objective
2.  Scope
3.  Business rules
4.  Architecture and processing flow
5.  Test plan
6.  RED — Write tests first
7.  GREEN — Minimum production code to pass tests
8.  REFACTOR — Improve design
9.  Integration tests with Testcontainers
10. Configuration files
11. How to run
12. Failure scenarios
13. Security considerations
14. Performance considerations
15. Trade-offs
16. Next steps
```

If code is too long, split into multiple parts, but:
- Each part MUST complete a runnable functional slice.
- Do NOT stop in the middle of a class or transaction flow.
- Do NOT skip tests.
- The first part MUST deliver real value, not just an empty skeleton.
- Do NOT ask for information that has already been provided.
- When minor details are missing, make a reasonable assumption and document it explicitly.
