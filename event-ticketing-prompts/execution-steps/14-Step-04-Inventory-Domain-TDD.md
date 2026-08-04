# STEP 04 — INVENTORY SERVICE: DOMAIN MODEL AND TDD

## Objective

Design and implement the pure domain model for Inventory Service using strict TDD. This step produces only domain layer code (plain Java, no framework dependencies) and unit tests. No database, no Redis, no Kafka in this step.

---

## Prerequisites

- Step 03 completed (Catalog Service running, seed data available with Event/Show/TicketType IDs).
- Architecture documents from Step 00 (especially ADR-002: Inventory Consistency Strategy).

---

## Task 1: Inventory Service — Project Setup

### 1.1 Initialize Project

Create `services/inventory-service/` with:
- `build.gradle.kts` — dependencies: Spring Boot Web, Spring Data JPA, Spring Data Redis, Spring Security OAuth2 Resource Server, Spring Kafka, Flyway, Spring Boot Actuator, Testcontainers (PostgreSQL, Redis, Kafka), JUnit 5, AssertJ, Mockito, ArchUnit, Awaitility.
- `application.yml` — placeholder configuration (will be filled in later steps).
- `application-test.yml` — Testcontainers overrides.

### 1.2 Package Structure

```
inventory-service/src/main/java/com/example/inventory/
├── domain/
│   ├── model/
│   │   ├── Inventory.java
│   │   ├── Reservation.java
│   │   ├── InventoryQuantity.java       (Value Object)
│   │   ├── ReservationId.java           (Value Object)
│   │   ├── TicketTypeId.java            (Value Object)
│   │   ├── ShowId.java                  (Value Object)
│   │   ├── UserId.java                  (Value Object)
│   │   ├── RequestId.java               (Value Object)
│   │   ├── ReservationStatus.java       (Enum)
│   │   └── ReservationExpiration.java   (Value Object)
│   ├── event/
│   │   ├── InventoryReservedEvent.java
│   │   ├── InventoryReservationRejectedEvent.java
│   │   ├── InventoryConfirmedEvent.java
│   │   ├── InventoryReleasedEvent.java
│   │   └── InventoryReservationExpiredEvent.java
│   ├── exception/
│   │   ├── InsufficientInventoryException.java
│   │   ├── InvalidQuantityException.java
│   │   ├── ReservationNotFoundException.java
│   │   ├── ReservationAlreadyConfirmedException.java
│   │   ├── ReservationExpiredException.java
│   │   ├── InvalidStateTransitionException.java
│   │   └── DuplicateRequestException.java
│   ├── policy/
│   │   ├── ReservationPolicy.java
│   │   └── ExpirationPolicy.java
│   └── service/
│       └── InventoryDomainService.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── ReserveTicketUseCase.java
│   │   │   ├── ConfirmReservationUseCase.java
│   │   │   ├── ReleaseReservationUseCase.java
│   │   │   ├── ExpireReservationsUseCase.java
│   │   │   └── QueryInventoryUseCase.java
│   │   └── out/
│   │       ├── InventoryPersistencePort.java
│   │       ├── ReservationPersistencePort.java
│   │       ├── InventoryCachePort.java
│   │       ├── OutboxPort.java
│   │       └── ClockPort.java
│   ├── command/
│   │   ├── ReserveTicketCommand.java
│   │   ├── ConfirmReservationCommand.java
│   │   └── ReleaseReservationCommand.java
│   └── usecase/
│       ├── ReserveTicketUseCaseImpl.java
│       ├── ConfirmReservationUseCaseImpl.java
│       ├── ReleaseReservationUseCaseImpl.java
│       └── ExpireReservationsUseCaseImpl.java
├── adapter/        (empty in this step — implemented in Steps 05-07)
└── bootstrap/      (empty in this step)
```

---

## Task 2: Value Objects (TDD)

### 2.1 InventoryQuantity

Encapsulates quantity invariants:
```
availableQuantity >= 0
reservedQuantity >= 0
soldQuantity >= 0
totalQuantity = availableQuantity + reservedQuantity + soldQuantity
```

### 2.2 ReservationExpiration

Encapsulates expiration logic:
- Created with a `Clock` and a duration (e.g., 15 minutes).
- `isExpired(Clock)` — returns true if current time is past expiration.
- Immutable.

### 2.3 Test Cases (RED)

```java
// InventoryQuantity
shouldCreateInventoryQuantity_whenAllValuesAreValid()
shouldRejectInventoryQuantity_whenAvailableIsNegative()
shouldRejectInventoryQuantity_whenReservedIsNegative()
shouldRejectInventoryQuantity_whenSoldIsNegative()
shouldRejectInventoryQuantity_whenTotalDoesNotMatchSum()
shouldDeductAvailable_whenReserving()
shouldRejectDeduction_whenAvailableIsInsufficient()
shouldReturnToAvailable_whenReleasing()
shouldMoveFromReservedToSold_whenConfirming()

// ReservationExpiration
shouldNotBeExpired_whenCreatedJustNow()
shouldBeExpired_whenDurationHasElapsed()
shouldCalculateRemainingTime()

// ReservationId, TicketTypeId, ShowId, UserId, RequestId
shouldCreateValueObject_withValidUuid()
shouldRejectValueObject_withNullUuid()
shouldBeEqual_whenSameUuid()
shouldNotBeEqual_whenDifferentUuid()
```

---

## Task 3: Inventory Aggregate (TDD)

### 3.1 Inventory Entity

The `Inventory` entity represents the ticket pool for a specific Show + TicketType combination.

Fields:
- id (UUID)
- showId (ShowId)
- ticketTypeId (TicketTypeId)
- quantity (InventoryQuantity)
- version (int, for optimistic locking)

Methods:
- `reserve(int quantity)` — deducts from available, adds to reserved. Returns updated InventoryQuantity. Throws `InsufficientInventoryException` if not enough.
- `confirmReservation(int quantity)` — moves from reserved to sold.
- `releaseReservation(int quantity)` — moves from reserved back to available.

### 3.2 Test Cases (RED)

```java
shouldReserveTickets_whenEnoughAvailable()
shouldUpdateQuantities_afterSuccessfulReservation()
shouldRejectReservation_whenInsufficientAvailable()
shouldRejectReservation_whenQuantityIsZero()
shouldRejectReservation_whenQuantityIsNegative()
shouldConfirmReservation_whenReservedQuantitySufficient()
shouldRejectConfirmation_whenReservedQuantityInsufficient()
shouldReleaseReservation_whenReservedQuantitySufficient()
shouldRejectRelease_whenReservedQuantityInsufficient()
shouldMaintainInvariant_totalEqualsAvailablePlusReservedPlusSold()
```

---

## Task 4: Reservation Entity (TDD)

### 4.1 Reservation Entity

Fields:
- id (ReservationId)
- ticketTypeId (TicketTypeId)
- showId (ShowId)
- userId (UserId)
- requestId (RequestId — for idempotency)
- quantity (int)
- status (ReservationStatus)
- expiration (ReservationExpiration)
- createdAt (Instant)
- confirmedAt (Instant, nullable)
- releasedAt (Instant, nullable)
- version (int)

### 4.2 State Machine

```
PENDING → HELD        (tickets successfully held in Redis/DB)
HELD → CONFIRMED      (payment succeeded)
HELD → RELEASED       (user cancelled)
HELD → EXPIRED        (TTL elapsed)
HELD → CANCELLED      (system cancellation)
```

Invalid transitions (must throw InvalidStateTransitionException):
- CONFIRMED → any other state
- RELEASED → any other state
- EXPIRED → any other state (except EXPIRED → RELEASED as a reconciliation edge case, document this)
- CANCELLED → any other state
- PENDING → CONFIRMED (cannot confirm without holding first)

### 4.3 Test Cases (RED)

```java
// Creation
shouldCreateReservation_withStatusHeld()
shouldSetExpirationTime_whenCreated()
shouldStoreRequestId_forIdempotency()

// State transitions
shouldTransitionToConfirmed_whenStatusIsHeld()
shouldTransitionToReleased_whenStatusIsHeld()
shouldTransitionToExpired_whenStatusIsHeld()
shouldTransitionToCancelled_whenStatusIsHeld()

// Invalid transitions
shouldRejectTransitionToConfirmed_whenStatusIsReleased()
shouldRejectTransitionToConfirmed_whenStatusIsExpired()
shouldRejectTransitionToConfirmed_whenStatusIsCancelled()
shouldRejectTransitionToReleased_whenStatusIsConfirmed()
shouldRejectTransitionToHeld_whenStatusIsConfirmed()
shouldRejectAnyTransition_whenStatusIsAlreadyTerminal()

// Expiration
shouldBeExpired_whenExpirationTimeHasPassed()
shouldNotBeExpired_whenExpirationTimeHasNotPassed()
shouldRejectConfirmation_whenReservationIsExpired()
```

---

## Task 5: Domain Events (TDD)

### 5.1 Event Definitions

All events are plain Java records (no framework dependency):

```java
public record InventoryReservedEvent(
    ReservationId reservationId,
    TicketTypeId ticketTypeId,
    ShowId showId,
    UserId userId,
    int quantity,
    Instant expiresAt,
    Instant occurredAt
) {}

public record InventoryReservationRejectedEvent(
    TicketTypeId ticketTypeId,
    ShowId showId,
    UserId userId,
    int requestedQuantity,
    int availableQuantity,
    String reason,
    Instant occurredAt
) {}

public record InventoryConfirmedEvent(...)
public record InventoryReleasedEvent(...)
public record InventoryReservationExpiredEvent(...)
```

### 5.2 Test Cases (RED)

```java
shouldCreateInventoryReservedEvent_withCorrectFields()
shouldCreateRejectedEvent_withReason()
shouldBeImmutable()
```

---

## Task 6: Use Cases (TDD)

### 6.1 ReserveTicketUseCase

Input: `ReserveTicketCommand(ticketTypeId, showId, userId, quantity, requestId)`

Logic:
1. Check if requestId was already processed (idempotency via output port).
2. If duplicate → return existing reservation.
3. Load Inventory for the given showId + ticketTypeId.
4. Call `inventory.reserve(quantity)`.
5. Create Reservation with status HELD and expiration.
6. Save Reservation via output port.
7. Save updated Inventory via output port.
8. Write InventoryReservedEvent to Outbox via output port.
9. Return Reservation.

### 6.2 ConfirmReservationUseCase

Input: `ConfirmReservationCommand(reservationId)`

Logic:
1. Load Reservation.
2. Validate not expired (using Clock).
3. Transition to CONFIRMED.
4. Load Inventory, call `inventory.confirmReservation(quantity)`.
5. Save both.
6. Write InventoryConfirmedEvent to Outbox.

### 6.3 ReleaseReservationUseCase

Input: `ReleaseReservationCommand(reservationId)`

Logic:
1. Load Reservation.
2. Transition to RELEASED.
3. Load Inventory, call `inventory.releaseReservation(quantity)`.
4. Save both.
5. Write InventoryReleasedEvent to Outbox.

### 6.4 ExpireReservationsUseCase

Logic (scheduled job):
1. Find all HELD reservations past expiration (via output port).
2. For each: transition to EXPIRED, release inventory, write event.

### 6.5 Test Cases (RED) — with Mocked Output Ports

```java
// ReserveTicketUseCase
shouldReserveTickets_whenInventoryIsAvailable()
shouldReturnExistingReservation_whenRequestIdIsDuplicated()
shouldRejectReservation_whenInventoryIsInsufficient()
shouldRejectReservation_whenQuantityIsInvalid()
shouldWriteOutboxEvent_whenReservationSucceeds()
shouldNotWriteOutboxEvent_whenReservationFails()
shouldUseClockForExpiration_notSystemClock()

// ConfirmReservationUseCase
shouldConfirmReservation_whenStatusIsHeld()
shouldRejectConfirmation_whenReservationIsExpired()
shouldRejectConfirmation_whenReservationIsAlreadyConfirmed()
shouldUpdateInventory_whenConfirmationSucceeds()
shouldWriteConfirmedEvent_whenConfirmationSucceeds()

// ReleaseReservationUseCase
shouldReleaseReservation_whenStatusIsHeld()
shouldReturnInventoryToAvailable_whenReleased()
shouldWriteReleasedEvent_whenReleaseSucceeds()

// ExpireReservationsUseCase
shouldExpireAllHeldReservations_pastExpiration()
shouldNotExpireReservations_notYetExpired()
shouldReleaseInventory_forEachExpiredReservation()
shouldWriteExpiredEvent_forEachExpiredReservation()
shouldHandlePartialFailure_whenOneExpirationFails()
```

---

## Task 7: ArchUnit Tests

```java
@AnalyzeClasses(packages = "com.example.inventory")
class InventoryArchitectureTest {

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
    static final ArchRule domain_should_not_depend_on_kafka =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.apache.kafka..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_redis =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework.data.redis..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..");
}
```

---

## Task 8: Clock Abstraction

### 8.1 ClockPort

```java
public interface ClockPort {
    Instant now();
}
```

All time-dependent logic MUST use `ClockPort`, never `Instant.now()` or `System.currentTimeMillis()` directly. This enables deterministic testing by injecting a fixed clock in tests.

### 8.2 Test Cases

```java
shouldUseInjectedClock_forReservationExpiration()
shouldExpireReservation_whenClockAdvancesPastExpiration()
shouldNotExpireReservation_whenClockIsBeforeExpiration()
```

---

## Completion Checklist

- [ ] Project initialized with correct dependencies and package structure.
- [ ] All Value Objects implemented with validation (InventoryQuantity, ReservationExpiration, IDs).
- [ ] Inventory aggregate with reserve/confirm/release methods.
- [ ] Reservation entity with state machine and all valid/invalid transitions.
- [ ] Domain events as plain Java records (no framework dependency).
- [ ] All 4 Use Cases implemented with mocked output ports.
- [ ] Idempotency via RequestId handled in ReserveTicketUseCase.
- [ ] Clock abstraction used everywhere (no direct Instant.now()).
- [ ] ArchUnit tests pass — domain has zero framework dependencies.
- [ ] All unit tests written FIRST (RED) then passed (GREEN).
- [ ] Code refactored for clarity.
- [ ] Total quantity invariant maintained across all operations.
- [ ] No database, no Redis, no Kafka code in this step.
- [ ] Trade-offs documented (e.g., optimistic vs pessimistic locking decision deferred to Step 05).
