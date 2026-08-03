# API, EVENT, AND CONSISTENCY RULES

These rules govern REST API conventions, Kafka event contracts, idempotency mechanisms, the Transactional Outbox pattern, Saga design, and consistency strategies across the entire system.

---

## 1. REST API Conventions

### Versioning
- URL path versioning: `/api/v1/...`
- Major version only in path. Minor/patch changes must be backward compatible.

### Resource Naming
- Plural nouns: `/api/v1/events`, `/api/v1/orders`, `/api/v1/tickets`.
- Nested resources for strong ownership: `/api/v1/orders/{orderId}/payments`.
- Use kebab-case for multi-word paths: `/api/v1/ticket-types`.

### HTTP Methods
| Method | Usage |
|---|---|
| GET | Read resource (idempotent, safe) |
| POST | Create resource or trigger action |
| PUT | Full resource replacement (idempotent) |
| PATCH | Partial resource update |
| DELETE | Remove resource (idempotent) |

### Standard Headers
| Header | Purpose |
|---|---|
| `Authorization` | Bearer token |
| `X-Request-Id` | Client-generated unique request identifier |
| `X-Idempotency-Key` | Client-generated key for write idempotency |
| `X-Correlation-Id` | Injected by Gateway, propagated across services |
| `Content-Type` | `application/json` |

### OpenAPI 3
- Every service MUST provide an OpenAPI 3 specification.
- Specification must be kept in sync with implementation.
- Include request/response schemas, error responses, and security requirements.

---

## 2. Pagination

### Keyset Pagination (Preferred for Large Datasets)

```
GET /api/v1/orders?after={lastSeenId}&limit=20
```

Response includes a cursor for the next page:
```json
{
  "data": [...],
  "pagination": {
    "nextCursor": "01J5ABC...",
    "hasMore": true
  }
}
```

### Offset Pagination (Acceptable for Small Datasets Only)

```
GET /api/v1/events?page=0&size=20
```

**Rule**: Do NOT use OFFSET pagination on tables expected to exceed 100,000 rows. Use Keyset Pagination instead.

---

## 3. Idempotency

### Idempotency-Key for Write APIs

All POST endpoints that create resources MUST support `X-Idempotency-Key` header.

Behavior:
1. Client sends POST with `X-Idempotency-Key: <unique-key>`.
2. Server checks if the key has been processed before.
3. If YES and payload matches → return the previously created resource (HTTP 200 or 201).
4. If YES but payload differs → return HTTP 409 Conflict.
5. If NO → process the request, store the key with the result, return HTTP 201.

Storage: `processed_requests` table or Redis cache with TTL.

### Request ID

Every request SHOULD carry `X-Request-Id` for tracing and deduplication. If the client does not provide one, the Gateway or BFF generates it.

---

## 4. Correlation and Causation IDs

```
X-Correlation-Id: Ties all actions in a single user flow together.
                   Generated at the BFF or Gateway. Propagated to all services and Kafka events.

Causation-Id:      The ID of the event or command that directly caused this event.
                   Enables event chain tracing.
```

Example chain:
```
User clicks "Reserve" → Correlation-Id: C1
  → InventoryReserved event (eventId: E1, correlationId: C1, causationId: <requestId>)
    → OrderCreated event (eventId: E2, correlationId: C1, causationId: E1)
      → PaymentCreated event (eventId: E3, correlationId: C1, causationId: E2)
```

---

## 5. Kafka Event Conventions

### Topic Naming

```
<domain>.<aggregate>.<event-type>
```

Examples:
```
inventory.reservation.held
inventory.reservation.confirmed
inventory.reservation.expired
order.order.created
order.order.paid
payment.payment.succeeded
payment.payment.failed
ticket.ticket.issued
```

### Event Envelope — Standard Structure

Every Kafka event MUST use this envelope:

```json
{
  "eventId": "01J5ABC-DEF-GHI",
  "eventType": "inventory.reservation.held",
  "eventVersion": 1,
  "aggregateType": "reservation",
  "aggregateId": "01J5ABC-RESERVATION-ID",
  "occurredAt": "2026-08-03T13:00:00Z",
  "correlationId": "01J5ABC-CORRELATION",
  "causationId": "01J5ABC-CAUSATION",
  "producer": "inventory-service",
  "payload": {
    "reservationId": "...",
    "ticketTypeId": "...",
    "quantity": 2,
    "expiresAt": "2026-08-03T13:15:00Z"
  }
}
```

### Event Versioning

- Every event contract MUST have a version number from day one.
- Version is part of the event type: `inventory.reservation.held.v1`.
- Adding optional fields is a compatible change (no version bump required).
- Removing fields, renaming fields, or changing field types requires a new version.
- Consumers must handle unknown fields gracefully (forward compatibility).

### Schema Evolution Strategy

- Phase 1: JSON with documented schema.
- Phase 2 (advanced): Avro or Protobuf with Schema Registry for production.

---

## 6. Transactional Outbox Pattern

All services that publish domain events related to a business transaction MUST use the Transactional Outbox.

### Write Flow (Same Database Transaction)

```
BEGIN TRANSACTION
  1. Update aggregate (e.g., create Reservation, update Inventory).
  2. Insert OutboxEvent record.
COMMIT
```

### Publish Flow (Outbox Relay)

```
1. Poll outbox_events table for records with status = PENDING.
2. Use SELECT ... FOR UPDATE SKIP LOCKED to prevent duplicate relay by multiple instances.
3. Publish event to Kafka.
4. On Kafka ack, update outbox record status to PUBLISHED and set published_at.
5. On Kafka failure, increment retry_count and set next_retry_at with exponential backoff.
```

### Outbox Table Schema

```sql
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100)  NOT NULL,
    aggregate_id    UUID          NOT NULL,
    event_type      VARCHAR(200)  NOT NULL,
    event_version   INT           NOT NULL DEFAULT 1,
    payload         JSONB         NOT NULL,
    occurred_at     TIMESTAMPTZ   NOT NULL,
    published_at    TIMESTAMPTZ,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count     INT           NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    trace_id        VARCHAR(64),
    correlation_id  UUID,
    causation_id    UUID
);

CREATE INDEX idx_outbox_pending ON outbox_events (status, next_retry_at)
    WHERE status = 'PENDING';
```

### Crash Recovery

```
Scenario: Kafka publish succeeds → Service crashes → outbox record not marked PUBLISHED.
Result:   Event is published again when relay resumes.
Consequence: Consumer MUST be idempotent.
```

### Relay Strategy

- Phase 1: Polling Publisher (simple, easy to learn and deploy).
- Phase 2 (advanced): CDC with Debezium for lower latency.

---

## 7. Idempotent Consumer / Inbox Pattern

Every Kafka consumer MUST handle duplicate events safely. Use the Inbox pattern:

### Inbox Table Schema

```sql
CREATE TABLE processed_events (
    event_id        UUID PRIMARY KEY,
    event_type      VARCHAR(200) NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    aggregate_id    UUID,
    consumer_group  VARCHAR(100)
);
```

### Consumer Processing Flow

```
BEGIN TRANSACTION
  1. Check if event_id exists in processed_events.
  2. If YES → skip processing, log as duplicate, commit.
  3. If NO → process the business logic.
  4. Insert event_id into processed_events.
COMMIT
```

**Critical**: The check AND the business processing AND the inbox insert MUST be in the SAME database transaction. Otherwise, duplicates can slip through.

---

## 8. Dead Letter Topic and Retry

### Retry Topic

For transient failures (database temporarily unavailable, timeout):
- Retry with exponential backoff.
- Maximum retry count (e.g., 3-5 retries).
- After max retries exhausted, send to Dead Letter Topic.

### Dead Letter Topic (DLT)

For poison messages (deserialization failure, unrecoverable business error):
- Route to `<topic>.DLT`.
- Log full event payload and error details.
- Alert operations team.
- Provide manual reprocessing mechanism.

---

## 9. Saga Pattern

### Strategy Selection

| Criteria | Choreography | Orchestration |
|---|---|---|
| Number of steps | 2-3 | 4+ |
| Compensation complexity | Simple | Complex |
| Observability need | Low | High |
| Cross-team coordination | Low | High |

**Default**: Start with choreography for simple flows. Introduce orchestration only when compensation logic becomes difficult to trace.

**Rule**: Do NOT create a single giant orchestrator containing all business logic from every service.

### Reservation-to-Ticket Saga Flow

```
Happy Path:
  Inventory HELD → Order AWAITING_PAYMENT → Payment SUCCEEDED → Order PAID → Inventory CONFIRMED → Ticket ISSUED

Compensation Scenarios:
  Payment FAILED    → Order PAYMENT_FAILED → Inventory RELEASED
  Order EXPIRED     → Inventory RELEASED
  Reservation EXPIRED → Order CANCELLED (if exists)
  Refund SUCCEEDED  → Ticket CANCELLED → Inventory RELEASED
```

---

## 10. Redis–PostgreSQL Consistency Strategy

When Redis is used for fast reservation alongside PostgreSQL as source of truth:

### Write Flow

```
1. Redis Lua Script: atomic check-and-reserve (deduct available, set reservation key with TTL).
2. If Redis succeeds: persist Reservation to PostgreSQL + write Outbox event (same transaction).
3. If PostgreSQL fails: compensate Redis (release the reservation key).
```

### Consistency Guarantees

- Redis is the fast-path for concurrency control during Flash Sale.
- PostgreSQL is the durable source of truth.
- If Redis and PostgreSQL diverge, PostgreSQL wins.
- A reconciliation job periodically compares Redis inventory counts with PostgreSQL counts.
- Redis can be rebuilt from PostgreSQL at any time.

### Failure Scenarios

| Failure | Impact | Resolution |
|---|---|---|
| Redis restart | Reservation keys lost | Reconciliation rebuilds from PostgreSQL |
| PostgreSQL down after Redis success | Redis has reservation but no durable record | TTL expires, Redis auto-releases; or reconciliation detects orphan |
| Kafka down | Outbox events accumulate | Outbox relay retries when Kafka recovers |
| Network partition | Partial state updates | Reconciliation detects and repairs |

---

## 11. Out-of-Order Event Handling

Events may arrive out of order. Every consumer MUST handle this:

- Check the current state of the aggregate before applying the event.
- If the event is no longer applicable (e.g., `PaymentSucceeded` arrives but Order is already `CANCELLED`), log a warning and skip.
- Use `occurredAt` timestamp or sequence numbers to detect stale events.
- Never blindly overwrite state based on the last-received event.
