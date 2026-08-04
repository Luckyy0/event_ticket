# STEP 03 — CATALOG SERVICE: EVENT, SHOW, AND TICKET TYPE FOUNDATION

## Objective

Build the Catalog Service to manage Events, Shows, Ticket Types, and Venues. This service provides the read-heavy foundation that all other services reference. It uses MongoDB as primary storage, Mongock for migrations, and seeds the data that Inventory Service will use in subsequent steps.

---

## Prerequisites

- Step 02 completed (API Gateway running, JWT validation working, Resource Server pattern established).
- docker-compose.yml includes MongoDB.

---

## Task 1: Catalog Service — Project Setup

### 1.1 Initialize Project

Create `services/catalog-service/` with:
- `build.gradle.kts` — dependencies: Spring Boot Web, Spring Data MongoDB, Spring Security OAuth2 Resource Server, Mongock, Spring Boot Actuator, Testcontainers (MongoDB), JUnit 5, AssertJ.
- `application.yml` — MongoDB connection, Keycloak JWT issuer-uri.
- `application-test.yml` — Testcontainers dynamic port overrides.

### 1.2 Package Structure

```
catalog-service/src/main/java/com/example/catalog/
├── domain/
│   ├── model/
│   │   ├── Event.java
│   │   ├── Show.java
│   │   ├── TicketType.java
│   │   ├── Venue.java
│   │   ├── SaleWindow.java
│   │   ├── EventStatus.java
│   │   └── ShowStatus.java
│   ├── exception/
│   │   ├── EventNotFoundException.java
│   │   └── ShowNotFoundException.java
│   └── service/
│       └── CatalogDomainService.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── CreateEventUseCase.java
│   │   │   ├── QueryEventUseCase.java
│   │   │   └── ManageShowUseCase.java
│   │   └── out/
│   │       ├── EventRepository.java
│   │       └── ShowRepository.java
│   ├── command/
│   │   ├── CreateEventCommand.java
│   │   ├── CreateShowCommand.java
│   │   └── CreateTicketTypeCommand.java
│   └── usecase/
│       ├── CreateEventUseCaseImpl.java
│       └── QueryEventUseCaseImpl.java
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── EventController.java
│   │       ├── ShowController.java
│   │       └── AdminCatalogController.java
│   └── out/
│       └── persistence/
│           ├── MongoEventRepository.java
│           ├── EventDocument.java
│           ├── ShowDocument.java
│           └── DocumentMapper.java
└── bootstrap/
    ├── config/
    │   ├── SecurityConfig.java
    │   └── MongockConfig.java
    ├── migration/
    │   └── V001_SeedInitialData.java
    └── CatalogApplication.java
```

---

## Task 2: Domain Model (TDD)

### 2.1 Domain Entities

**Event**:
- id (UUID)
- name (String, required, max 200 chars)
- description (String, max 5000 chars)
- imageUrl (String, optional)
- organizerId (UUID, required)
- venue (Venue, required)
- status (EventStatus: DRAFT, PUBLISHED, CANCELLED, COMPLETED)
- shows (List of Show)
- createdAt, updatedAt

**Show**:
- id (UUID)
- eventId (UUID)
- startTime (Instant, required, must be in the future when creating)
- endTime (Instant, required, must be after startTime)
- status (ShowStatus: SCHEDULED, ON_SALE, SOLD_OUT, IN_PROGRESS, COMPLETED, CANCELLED)
- saleWindow (SaleWindow)
- ticketTypes (List of TicketType)

**TicketType**:
- id (UUID)
- name (String, required — e.g., "VIP", "Standard", "Early Bird")
- description (String)
- price (BigDecimal, required, > 0)
- currency (String, default "VND")
- totalQuantity (int, required, > 0)
- sortOrder (int)

**Venue**:
- name (String, required)
- address (String)
- city (String)
- capacity (int)

**SaleWindow**:
- opensAt (Instant, required)
- closesAt (Instant, required, must be after opensAt and before show startTime)

### 2.2 Test Cases (RED)

```java
// Event validation
shouldCreateEvent_whenAllFieldsAreValid()
shouldRejectEvent_whenNameIsEmpty()
shouldRejectEvent_whenNameExceedsMaxLength()
shouldRejectEvent_whenOrganizerIdIsNull()
shouldRejectEvent_whenVenueIsNull()

// Show validation
shouldCreateShow_whenAllFieldsAreValid()
shouldRejectShow_whenStartTimeIsInThePast()
shouldRejectShow_whenEndTimeIsBeforeStartTime()
shouldRejectShow_whenSaleWindowOpensAfterShowStarts()
shouldRejectShow_whenNoTicketTypesDefined()

// TicketType validation
shouldCreateTicketType_whenPriceIsPositive()
shouldRejectTicketType_whenPriceIsZeroOrNegative()
shouldRejectTicketType_whenQuantityIsZeroOrNegative()
shouldRejectTicketType_whenNameIsEmpty()

// Event status transitions
shouldPublishEvent_whenEventIsDraft()
shouldRejectPublish_whenEventIsAlreadyCancelled()
shouldCancelEvent_whenEventIsPublished()

// Show status transitions
shouldTransitionToOnSale_whenSaleWindowOpens()
shouldTransitionToSoldOut_whenAllTicketTypesExhausted()
```

### 2.3 Production Code (GREEN)

Implement domain models with validation logic and state transitions.

---

## Task 3: REST API (TDD)

### 3.1 Public Endpoints (No Authentication)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/events` | List published events (paginated, filterable) |
| GET | `/api/v1/events/{eventId}` | Get event detail with shows and ticket types |
| GET | `/api/v1/events/{eventId}/shows` | List shows for an event |
| GET | `/api/v1/events/{eventId}/shows/{showId}` | Get show detail with ticket types |

Query parameters for listing:
- `search` — full-text search on name and description.
- `city` — filter by venue city.
- `dateFrom`, `dateTo` — filter by show date range.
- `status` — filter by event status (default: PUBLISHED only for public).
- `page`, `size` — pagination (offset acceptable here, catalog is bounded).
- `sort` — `startTime,asc` or `name,asc`.

### 3.2 Organizer Endpoints (EVENT_ORGANIZER role)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/events` | Create new event |
| PUT | `/api/v1/events/{eventId}` | Update event details |
| POST | `/api/v1/events/{eventId}/publish` | Publish event (DRAFT → PUBLISHED) |
| POST | `/api/v1/events/{eventId}/cancel` | Cancel event |
| POST | `/api/v1/events/{eventId}/shows` | Add show to event |
| PUT | `/api/v1/events/{eventId}/shows/{showId}` | Update show |
| POST | `/api/v1/events/{eventId}/shows/{showId}/ticket-types` | Add ticket type |

### 3.3 Admin Endpoints (ADMIN role)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/admin/events` | List all events (including DRAFT) |
| DELETE | `/api/v1/admin/events/{eventId}` | Hard delete (if no orders exist) |

### 3.4 Controller Test Cases (RED)

```java
// Public endpoints
shouldReturnPublishedEvents_whenListingEvents()
shouldNotReturnDraftEvents_inPublicListing()
shouldReturnEventDetail_whenEventExists()
shouldReturn404_whenEventDoesNotExist()
shouldFilterEventsByCity()
shouldFilterEventsByDateRange()
shouldSearchEventsByName()
shouldPaginateResults()

// Organizer endpoints
shouldCreateEvent_whenUserIsOrganizer()
shouldReturn403_whenCustomerTriesToCreateEvent()
shouldReturn403_whenOrganizerTriesToUpdateAnotherOrganizersEvent()
shouldPublishEvent_whenOrganizerOwnsEvent()
shouldAddShow_whenOrganizerOwnsEvent()
shouldAddTicketType_toShow()

// Admin endpoints
shouldListAllEvents_whenUserIsAdmin()
shouldReturn403_whenNonAdminAccessesAdminEndpoint()
```

---

## Task 4: MongoDB Persistence (TDD)

### 4.1 Document Structure

**EventDocument** in MongoDB:
```json
{
  "_id": "uuid",
  "name": "Concert XYZ",
  "description": "...",
  "imageUrl": "...",
  "organizerId": "uuid",
  "venue": {
    "name": "...",
    "address": "...",
    "city": "Ho Chi Minh",
    "capacity": 5000
  },
  "status": "PUBLISHED",
  "shows": [
    {
      "id": "uuid",
      "startTime": "2026-09-15T19:00:00Z",
      "endTime": "2026-09-15T22:00:00Z",
      "status": "ON_SALE",
      "saleWindow": {
        "opensAt": "2026-08-01T00:00:00Z",
        "closesAt": "2026-09-15T18:00:00Z"
      },
      "ticketTypes": [
        {
          "id": "uuid",
          "name": "VIP",
          "price": 2000000,
          "currency": "VND",
          "totalQuantity": 100,
          "sortOrder": 1
        },
        {
          "id": "uuid",
          "name": "Standard",
          "price": 500000,
          "currency": "VND",
          "totalQuantity": 1000,
          "sortOrder": 2
        }
      ]
    }
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

### 4.2 MongoDB Indexes

```javascript
db.events.createIndex({ "status": 1, "shows.startTime": 1 })
db.events.createIndex({ "venue.city": 1 })
db.events.createIndex({ "organizerId": 1 })
db.events.createIndex({ "name": "text", "description": "text" })
```

### 4.3 Integration Test Cases (RED)

```java
// MongoDB Testcontainer integration tests
shouldSaveAndRetrieveEvent()
shouldFindPublishedEventsByCity()
shouldFindEventsByDateRange()
shouldFullTextSearchByName()
shouldUpdateEventStatus()
shouldAddShowToEvent()
shouldAddTicketTypeToShow()
shouldEnforceUniqueEventId()
shouldReturnEmptyList_whenNoEventsMatch()
shouldPaginate_whenManyEventsExist()
```

### 4.4 Test Infrastructure

```java
@Testcontainers
@DataMongoTest
class MongoEventRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }
}
```

---

## Task 5: Mongock Migration — Seed Data

### 5.1 Seed Data for Development and Testing

Create Mongock changelog that seeds:

| Event | Shows | Ticket Types per Show |
|---|---|---|
| "Summer Music Festival" | 2 shows (different dates) | VIP (100), Standard (500), Early Bird (200) |
| "Tech Conference 2026" | 1 show | Premium (50), Regular (300) |
| "Flash Sale Concert" | 1 show | General Admission (100) — this is for Flash Sale testing |

This seed data will be used by Inventory Service in subsequent steps.

### 5.2 Test Case

```java
shouldRunMongockMigration_andSeedInitialData()
shouldContainFlashSaleConcert_afterMigration()
```

---

## Task 6: OpenAPI Specification

Generate or maintain an OpenAPI 3 specification for all Catalog Service endpoints.

Include:
- Request/response schemas.
- Error response schemas.
- Authentication requirements.
- Pagination parameters.
- Example values.

---

## Task 7: Configuration Files

Produce complete and runnable:
- `build.gradle.kts`
- `application.yml`
- `application-test.yml`
- `Dockerfile`
- Updated `docker-compose.yml` entry for catalog-service and MongoDB (if needed).

---

## Completion Checklist

- [ ] Catalog project initialized with correct dependencies.
- [ ] Domain models have validation and state transition logic.
- [ ] All domain unit tests written first (RED) and passed (GREEN).
- [ ] Public REST endpoints return published events without authentication.
- [ ] Organizer endpoints enforce EVENT_ORGANIZER role and ownership.
- [ ] Admin endpoints enforce ADMIN role.
- [ ] MongoDB persistence works with Testcontainers.
- [ ] Mongock migration seeds initial data.
- [ ] MongoDB indexes created for query patterns.
- [ ] Full-text search works on event name and description.
- [ ] Pagination and filtering work correctly.
- [ ] Seed data includes a Flash Sale event (100 tickets) for Inventory testing.
- [ ] Event IDs, Show IDs, and TicketType IDs are documented for use in subsequent steps.
- [ ] OpenAPI specification generated.
- [ ] Configuration files complete and runnable.
- [ ] No tokens or secrets in logs.
- [ ] Failure scenarios documented.
- [ ] Trade-offs explained.
