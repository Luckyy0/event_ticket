# TESTING AND TDD RULES

These rules govern Test-Driven Development practices, test pyramid structure, Testcontainers usage, and testing conventions across the entire system. Every execution step must comply with these rules.

---

## 1. Test-Driven Development — Mandatory Cycle

Every feature MUST be implemented following the TDD cycle:

```
RED    → Write a test describing the desired behavior.
       → Run the test and confirm it fails for the correct reason.

GREEN  → Write the minimum amount of code to make the test pass.
       → Do not add abstractions that are not yet needed.

REFACTOR → Clean up the code.
         → Keep all tests passing.
         → Remove duplication.
         → Improve naming and architecture.
```

**Strict rule**: Do NOT write the entire production implementation first and then add tests afterward.

---

## 2. Feature Implementation Order

When implementing any feature, you MUST follow this exact sequence:

```
1.  Describe the behavior.
2.  Define acceptance criteria.
3.  List all test cases.
4.  Write the failing test (RED).
5.  Explain why the test is currently failing.
6.  Write the minimum production code to pass the test (GREEN).
7.  Run the test and confirm it passes.
8.  Refactor the code while keeping tests green.
9.  Add integration tests (with Testcontainers where applicable).
10. Add edge-case and failure tests.
11. Explain trade-offs and design decisions.
```

---

## 3. Test Pyramid

Every service MUST have the following test layers:

### Unit Tests

Test targets:
- Domain Entity behavior.
- Value Object validation and equality.
- Domain Service logic.
- Application Use Case orchestration.
- State transitions (State Machine).
- Validation rules.
- Idempotency rules.
- Critical mapping logic.

Technology:
- JUnit 5.
- AssertJ.
- Mockito — only when genuinely needed to mock external dependencies.

**Rule**: Do NOT mock domain objects. Do NOT mock things you own unless there is a real external boundary.

### Integration Tests

Test targets:
- Repository implementations against real databases.
- PostgreSQL queries (constraints, locking, atomic updates).
- MongoDB repository operations.
- Redis Lua Scripts on real Redis.
- Kafka producer and consumer behavior.
- Outbox publisher relay.
- Flyway migration execution.
- Mongock migration execution.
- Security filter chain (when needed).

Technology:
- Spring Boot Test.
- Testcontainers (mandatory for infrastructure tests).
- Awaitility (for async assertions).
- WireMock or MockWebServer (for external HTTP APIs).

### Component Tests

Test a service nearly end-to-end:

```
HTTP Request → Controller → Application → Domain → Database → Kafka/Outbox
```

Only mock external providers that are truly outside the system (payment gateway, email provider).

### Contract Tests

Test targets:
- REST API contracts (request/response schemas).
- Kafka event contracts (payload structure).
- Backward compatibility of API and event changes.
- Required vs optional fields.
- Event version compatibility.

Technology: Spring Cloud Contract (when appropriate).

### End-to-End Tests

Test critical flows spanning multiple services:
- Login via BFF and Keycloak.
- Reserve tickets.
- Create Order.
- Process Payment.
- Issue Ticket.
- Reservation expiration.
- Duplicate payment callback.
- Kafka message retry.
- Order cancellation.
- Refund flow.

**Rule**: E2E tests do NOT replace Unit Tests and Integration Tests. They supplement them.

---

## 4. Test Naming Convention

Use the following structure:

```java
shouldExpectedBehavior_whenCondition()
```

Examples:

```java
shouldReserveTickets_whenInventoryIsAvailable()
shouldRejectReservation_whenRequestedQuantityExceedsAvailableStock()
shouldReturnExistingOrder_whenIdempotencyKeyWasAlreadyProcessed()
shouldIgnoreDuplicatePaymentWebhook_whenTransactionWasProcessed()
shouldReleaseInventory_whenReservationExpires()
shouldRejectCheckIn_whenTicketAlreadyCheckedIn()
shouldCreatePaymentIntent_whenOrderIsAwaitingPayment()
```

**Rule**: Test names MUST describe business behavior, not implementation method names.

---

## 5. Test Structure — Given-When-Then

Every test MUST clearly separate three sections:

```java
@Test
void shouldReserveTickets_whenInventoryIsAvailable() {
    // Given
    var inventory = createInventoryWithAvailable(100);
    var command = new ReserveTicketsCommand(inventory.ticketTypeId(), 2, requestId);

    // When
    var result = useCase.reserve(command);

    // Then
    assertThat(result.status()).isEqualTo(ReservationStatus.HELD);
    assertThat(result.quantity()).isEqualTo(2);
}
```

Use descriptive helper methods to keep Given sections readable.

---

## 6. Mock Usage Rules

### DO NOT Mock

- PostgreSQL repository in integration tests.
- Redis in Lua Script tests.
- Kafka in event integration tests.
- Keycloak in full authentication integration tests.
- Database behavior that is critical to business correctness.
- Domain objects.

### Acceptable to Mock

- External payment provider (WireMock / MockWebServer).
- External email provider.
- External SMS provider.
- `Clock` in unit tests (for time-dependent logic).
- `UUID` generator (for deterministic tests).
- External HTTP services not owned by the system.

---

## 7. Concurrency Tests

Inventory Service MUST have concurrency tests that prove anti-over-selling guarantees.

Concurrency test specification:

```
Given: available quantity = 100
When:  1,000 concurrent requests each requesting 1 ticket
Then:  exactly 100 requests succeed
And:   exactly 900 requests fail
And:   available quantity is never negative
And:   no more than 100 valid reservations exist
And:   database state is fully consistent after all requests complete
```

Must verify:
- Race conditions.
- Lost updates.
- Duplicate reservations.
- Deadlocks.
- Lock timeouts.
- Redis atomicity.
- Database consistency after the test completes.

**Rule**: Do NOT write only sequential unit tests and then claim the system prevents over-selling. You MUST have actual concurrent integration tests.

---

## 8. Idempotency Tests

Must have tests for:
- Sending the same `Idempotency-Key` multiple times → same result returned.
- Duplicate payment webhook → processed only once.
- Duplicate Kafka event → consumed only once (business effect applied once).
- Consumer restart mid-processing → event reprocessed safely.
- Outbox publisher re-publishes after crash → consumer handles duplicate.
- Client timeout then retry → no duplicate resource created.
- Events arriving out of order → handled gracefully.

---

## 9. Testcontainers — Mandatory Rules

All integration tests involving infrastructure MUST use Testcontainers. Do NOT substitute H2 for PostgreSQL.

Reasons H2 is unacceptable for PostgreSQL tests:
- Different SQL dialect.
- Different locking behavior.
- Different transaction semantics.
- Different index behavior.
- No JSONB support.
- No partitioning.
- Different constraint behavior.

### Required Containers (per service)

| Container | Used By |
|---|---|
| `PostgreSQLContainer` | inventory, order, payment, ticket, check-in, audit |
| `MongoDBContainer` | catalog |
| `KafkaContainer` | all services that produce or consume events |
| `GenericContainer` (Redis) | inventory, bff (session store), rate limiting |
| Keycloak container | bff, gateway, security tests |
| WireMock container | payment (external provider mock) |

### Base Integration Test Pattern

```java
@Testcontainers
@SpringBootTest
abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

### Singleton Container Pattern

Reusing containers across a test suite is acceptable for speed, but MUST guarantee:
- Test isolation (no shared mutable state).
- Data cleanup between tests.
- No dependency on test execution order.
- No state leakage from one test to another.

---

## 10. PostgreSQL Integration Test Requirements

Must verify:
- Flyway migration runs successfully.
- UNIQUE constraints reject duplicates.
- Foreign key constraints are enforced.
- CHECK constraints work correctly.
- Optimistic locking detects concurrent modifications.
- Pessimistic locking prevents concurrent access.
- Atomic updates (e.g., `UPDATE ... WHERE available >= :qty`) work under concurrency.
- Critical query plans (when performance is a concern).
- Partition routing (when partitioning is used).

---

## 11. Redis Integration Test Requirements

Lua Scripts MUST be tested on real Redis via Testcontainers. Do NOT mock `RedisTemplate` to prove Lua Script correctness.

Must verify:
- Atomic decrement works correctly.
- TTL is set and expires as expected.
- Reservation key is created correctly.
- Insufficient inventory is rejected.
- Duplicate reservation request is detected.
- Key expiration triggers expected behavior.
- Concurrent requests produce correct results.
- Redis failure behavior (connection lost, timeout).

---

## 12. Kafka Integration Test Requirements

Use Kafka Testcontainer to verify:
- Producer publishes events successfully.
- Consumer receives and processes events.
- Retry mechanism works for transient failures.
- Duplicate events are handled idempotently.
- Poison messages go to Dead Letter Topic.
- Consumer group assignment works correctly.
- Event serialization/deserialization is correct.
- Event version compatibility.
- Outbox relay publishes pending events.

**Rule**: Do NOT use fixed `Thread.sleep()` for waiting. Use Awaitility:

```java
await()
    .atMost(Duration.ofSeconds(10))
    .untilAsserted(() -> {
        // assertion here
    });
```

---

## 13. Keycloak Integration Test Requirements

BFF and security integration tests MUST run against a real Keycloak Testcontainer.

Must verify:
- Authorization Code Flow completes successfully.
- Token endpoint returns valid tokens.
- Valid token grants access.
- Expired token is rejected.
- Invalid issuer is rejected.
- Invalid audience is rejected.
- Missing required role is rejected.
- Correct role grants access.
- Refresh Token works.
- Logout invalidates the session.
- Key rotation is handled.
- BFF session cookie is set correctly.
- CSRF protection works.

Test realm (`realm-test.json`) must contain:
- Test client for BFF.
- Test client for API.
- Test users with known passwords.
- Test roles.
- Redirect URIs for test environment.

---

## 14. Full Environment Test

Provide `docker-compose.test.yml` or equivalent for system-level tests containing:
- PostgreSQL, MongoDB, Redis, Kafka, Keycloak, API Gateway, BFF, core services.

**Rule**: Do NOT require starting ALL microservices for every integration test. Clearly distinguish:
- Service integration test (single service + its infrastructure).
- Component test (single service end-to-end).
- System test (multiple services).
- E2E test (full flow from browser to all services).
