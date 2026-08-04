# STEP 05 — INVENTORY SERVICE: POSTGRESQL PERSISTENCE

## Objective

Implement the PostgreSQL persistence layer for Inventory Service. Design the database schema, write Flyway migrations, build persistence adapters that implement the output ports defined in Step 04, and verify everything with integration tests using PostgreSQL Testcontainer. No Redis, no Kafka in this step.

---

## Prerequisites

- Step 04 completed (domain model, value objects, use cases, unit tests all passing).
- docker-compose.yml includes PostgreSQL.

---

## Task 1: Flyway Migration Scripts

### 1.1 V001 — Inventory Table

File: `src/main/resources/db/migration/V001__create_inventory_table.sql`

```sql
CREATE TABLE inventories (
    id              UUID PRIMARY KEY,
    show_id         UUID          NOT NULL,
    ticket_type_id  UUID          NOT NULL,
    total_quantity   INT           NOT NULL CHECK (total_quantity >= 0),
    available_quantity INT         NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity  INT         NOT NULL CHECK (reserved_quantity >= 0),
    sold_quantity    INT           NOT NULL CHECK (sold_quantity >= 0),
    version          INT           NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_inventory_show_ticket UNIQUE (show_id, ticket_type_id),
    CONSTRAINT ck_inventory_quantity_sum
        CHECK (total_quantity = available_quantity + reserved_quantity + sold_quantity)
);

CREATE INDEX idx_inventory_show_id ON inventories (show_id);
CREATE INDEX idx_inventory_ticket_type ON inventories (ticket_type_id);
```

### 1.2 V002 — Reservation Table

File: `src/main/resources/db/migration/V002__create_reservation_table.sql`

```sql
CREATE TABLE reservations (
    id              UUID PRIMARY KEY,
    ticket_type_id  UUID          NOT NULL,
    show_id         UUID          NOT NULL,
    user_id         UUID          NOT NULL,
    request_id      UUID          NOT NULL,
    quantity         INT           NOT NULL CHECK (quantity > 0),
    status           VARCHAR(20)   NOT NULL DEFAULT 'HELD',
    expires_at       TIMESTAMPTZ   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    confirmed_at     TIMESTAMPTZ,
    released_at      TIMESTAMPTZ,
    version          INT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_reservation_request_id UNIQUE (request_id),
    CONSTRAINT ck_reservation_status
        CHECK (status IN ('PENDING','HELD','CONFIRMED','RELEASED','EXPIRED','CANCELLED'))
);

CREATE INDEX idx_reservation_user_id ON reservations (user_id);
CREATE INDEX idx_reservation_show_ticket ON reservations (show_id, ticket_type_id);
CREATE INDEX idx_reservation_status_expires ON reservations (status, expires_at)
    WHERE status = 'HELD';
CREATE INDEX idx_reservation_request_id ON reservations (request_id);
```

### 1.3 V003 — Processed Requests Table (Idempotency)

File: `src/main/resources/db/migration/V003__create_processed_requests_table.sql`

```sql
CREATE TABLE processed_requests (
    request_id      UUID PRIMARY KEY,
    reservation_id  UUID,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    result_status   VARCHAR(20) NOT NULL
);
```

### 1.4 Test Case

```java
shouldRunAllFlywayMigrations_successfully()
```

---

## Task 2: JPA Entities

### 2.1 InventoryJpaEntity

File: `adapter/out/persistence/InventoryJpaEntity.java`

```java
@Entity
@Table(name = "inventories")
public class InventoryJpaEntity {
    @Id
    private UUID id;
    private UUID showId;
    private UUID ticketTypeId;
    private int totalQuantity;
    private int availableQuantity;
    private int reservedQuantity;
    private int soldQuantity;
    @Version
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### 2.2 ReservationJpaEntity

File: `adapter/out/persistence/ReservationJpaEntity.java`

```java
@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {
    @Id
    private UUID id;
    private UUID ticketTypeId;
    private UUID showId;
    private UUID userId;
    private UUID requestId;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant releasedAt;
    @Version
    private int version;
}
```

### 2.3 ProcessedRequestJpaEntity

File: `adapter/out/persistence/ProcessedRequestJpaEntity.java`

---

## Task 3: Mappers

### 3.1 InventoryMapper

Converts between `Inventory` (domain) ↔ `InventoryJpaEntity` (JPA).

### 3.2 ReservationMapper

Converts between `Reservation` (domain) ↔ `ReservationJpaEntity` (JPA).

### 3.3 Test Cases (RED)

```java
shouldMapInventoryToDomain_correctly()
shouldMapInventoryToEntity_correctly()
shouldMapReservationToDomain_correctly()
shouldMapReservationToEntity_correctly()
shouldPreserveAllFields_duringRoundTrip()
```

---

## Task 4: Spring Data JPA Repositories

### 4.1 InventoryJpaRepository

```java
public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, UUID> {

    Optional<InventoryJpaEntity> findByShowIdAndTicketTypeId(UUID showId, UUID ticketTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryJpaEntity i WHERE i.showId = :showId AND i.ticketTypeId = :ticketTypeId")
    Optional<InventoryJpaEntity> findByShowIdAndTicketTypeIdForUpdate(
        @Param("showId") UUID showId,
        @Param("ticketTypeId") UUID ticketTypeId
    );

    List<InventoryJpaEntity> findAllByShowId(UUID showId);
}
```

### 4.2 ReservationJpaRepository

```java
public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {

    Optional<ReservationJpaEntity> findByRequestId(UUID requestId);

    List<ReservationJpaEntity> findAllByStatusAndExpiresAtBefore(String status, Instant cutoff);

    List<ReservationJpaEntity> findAllByUserIdAndShowId(UUID userId, UUID showId);
}
```

### 4.3 ProcessedRequestJpaRepository

```java
public interface ProcessedRequestJpaRepository extends JpaRepository<ProcessedRequestJpaEntity, UUID> {
    boolean existsByRequestId(UUID requestId);
}
```

---

## Task 5: Persistence Adapters (TDD)

### 5.1 InventoryPersistenceAdapter

Implements `InventoryPersistencePort` (defined in Step 04).

Methods:
- `findByShowIdAndTicketTypeId(ShowId, TicketTypeId)` → loads and maps to domain `Inventory`.
- `findByShowIdAndTicketTypeIdForUpdate(ShowId, TicketTypeId)` → pessimistic lock version.
- `save(Inventory)` → maps to JPA entity and saves.
- `findAllByShowId(ShowId)` → returns list of domain Inventory.

### 5.2 ReservationPersistenceAdapter

Implements `ReservationPersistencePort` (defined in Step 04).

Methods:
- `save(Reservation)` → maps and saves.
- `findById(ReservationId)` → loads and maps to domain.
- `findByRequestId(RequestId)` → idempotency lookup.
- `findExpiredHeldReservations(Instant cutoff)` → for expiration job.

### 5.3 Test Cases (RED) — Integration Tests

```java
// Flyway
shouldRunAllMigrations_successfully()

// Inventory persistence
shouldSaveAndLoadInventory()
shouldFindInventoryByShowAndTicketType()
shouldReturnEmpty_whenInventoryDoesNotExist()
shouldUpdateAvailableQuantity_afterReservation()

// Optimistic locking
shouldThrowOptimisticLockException_whenConcurrentUpdate()

// Pessimistic locking
shouldBlockConcurrentRead_whenPessimisticLockHeld()

// Atomic update
shouldDecrementAvailable_atomically()
shouldRejectDecrementAvailable_whenInsufficient()

// Inventory constraints
shouldRejectNegativeAvailableQuantity_atDatabaseLevel()
shouldRejectQuantitySumMismatch_atDatabaseLevel()
shouldEnforceUniqueShowTicketType()

// Reservation persistence
shouldSaveAndLoadReservation()
shouldFindReservationByRequestId()
shouldEnforceUniqueRequestId()
shouldFindExpiredHeldReservations()
shouldNotReturnConfirmedReservations_inExpiredQuery()

// Processed requests
shouldSaveProcessedRequest()
shouldDetectDuplicateRequestId()

// Transaction behavior
shouldRollbackBothInventoryAndReservation_whenTransactionFails()
```

---

## Task 6: Atomic SQL Update for Inventory

### 6.1 Native Query for Atomic Deduction

In addition to JPA-based updates, provide a native SQL approach for maximum safety:

```java
@Modifying
@Query(value = """
    UPDATE inventories
    SET available_quantity = available_quantity - :qty,
        reserved_quantity = reserved_quantity + :qty,
        version = version + 1,
        updated_at = NOW()
    WHERE show_id = :showId
      AND ticket_type_id = :ticketTypeId
      AND available_quantity >= :qty
    """, nativeQuery = true)
int atomicReserve(@Param("showId") UUID showId,
                  @Param("ticketTypeId") UUID ticketTypeId,
                  @Param("qty") int qty);
```

Return value: number of rows updated (1 = success, 0 = insufficient inventory).

### 6.2 Test Cases (RED)

```java
shouldReturn1_whenAtomicReserveSucceeds()
shouldReturn0_whenAtomicReserveInsufficientInventory()
shouldReturn0_whenAtomicReserveExactlyAvailable_thenAnotherTriesToReserve()
shouldMaintainConstraints_afterAtomicReserve()
```

---

## Task 7: Concurrent Database Test

### 7.1 Test Specification

Even without Redis (Redis is Step 06), test that PostgreSQL alone handles concurrency correctly using pessimistic locking or atomic SQL updates.

```
Given: Inventory with 100 available tickets
When:  50 concurrent threads each try to reserve 1 ticket using atomicReserve()
Then:  All 50 succeed (100 >= 50)
And:   available_quantity = 50, reserved_quantity = 50

Given: Inventory with 10 available tickets
When:  50 concurrent threads each try to reserve 1 ticket using atomicReserve()
Then:  Exactly 10 succeed
And:   Exactly 40 fail
And:   available_quantity = 0, reserved_quantity = 10
And:   No negative quantities exist
```

### 7.2 Test Cases (RED)

```java
shouldHandleConcurrentReservations_withAtomicUpdate()
shouldNotOversell_whenConcurrentRequestsExceedAvailable()
shouldMaintainQuantityInvariant_afterConcurrentReservations()
shouldNotDeadlock_underConcurrentLoad()
```

---

## Task 8: Seed Inventory Data

### 8.1 Flyway Migration for Seed Data

File: `src/main/resources/db/migration/V004__seed_inventory_data.sql`

Seed inventory records matching the Catalog Service seed data from Step 03:

| Show | Ticket Type | Total | Available | Reserved | Sold |
|---|---|---|---|---|---|
| Summer Music Festival Show 1 | VIP | 100 | 100 | 0 | 0 |
| Summer Music Festival Show 1 | Standard | 500 | 500 | 0 | 0 |
| Summer Music Festival Show 1 | Early Bird | 200 | 200 | 0 | 0 |
| Flash Sale Concert | General Admission | 100 | 100 | 0 | 0 |

Use the same UUIDs generated in Catalog Service seed data.

---

## Task 9: Test Infrastructure

### 9.1 Base Integration Test Class

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("inventory_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM processed_requests");
        jdbcTemplate.execute("DELETE FROM reservations");
        jdbcTemplate.execute("DELETE FROM inventories");
    }
}
```

---

## Task 10: Configuration Files

Produce complete and runnable:
- Updated `build.gradle.kts` with JPA, Flyway, PostgreSQL driver dependencies.
- `application.yml` — datasource, JPA, Flyway configuration.
- `application-test.yml` — test profile overrides.

---

## Completion Checklist

- [ ] All Flyway migrations run successfully on PostgreSQL Testcontainer.
- [ ] CHECK constraints enforce non-negative quantities and sum invariant at DB level.
- [ ] UNIQUE constraint on (show_id, ticket_type_id) prevents duplicate inventory.
- [ ] UNIQUE constraint on request_id prevents duplicate reservations.
- [ ] JPA entities correctly map to domain models via mappers.
- [ ] Persistence adapters implement all output ports from Step 04.
- [ ] Optimistic locking throws exception on concurrent modification.
- [ ] Pessimistic locking blocks concurrent access correctly.
- [ ] Atomic SQL update deducts inventory safely.
- [ ] Concurrent database test passes with no over-selling.
- [ ] Quantity invariant holds after all concurrent operations.
- [ ] Seed data matches Catalog Service event/show/ticket-type IDs.
- [ ] All integration tests written FIRST (RED) then passed (GREEN).
- [ ] Test isolation maintained (cleanup between tests).
- [ ] Transaction rollback verified.
- [ ] Configuration files complete and runnable.
- [ ] Trade-offs documented (atomic SQL vs pessimistic lock vs optimistic lock).
