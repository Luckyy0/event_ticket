# STEP 00 — INITIAL ARCHITECTURE DESIGN

## Objective

Design the overall system architecture, define service boundaries, establish data ownership, choose repository strategy, design the local development environment, and create the initial test strategy. This step produces documentation only — no source code.

---

## Instructions

You have read all global rules. Now produce the following architecture artifacts. Use Mermaid diagrams where specified. Output each artifact as a separate markdown section with a clear file path header.

---

## Required Output

### 1. System Context Overview

File: `docs/architecture/system-context.md`

Write a single paragraph summarizing the overall architecture strategy:
- Why microservices.
- Why event-driven.
- Why Hexagonal Architecture for core services.
- Why PostgreSQL + Redis + MongoDB.
- Why Keycloak + BFF.
- How Eventual Consistency is achieved.
- How Over-selling is prevented.

Then produce a Mermaid C4 System Context diagram showing:
- External actors (Customer, Event Organizer, Staff, Admin).
- The system boundary.
- External systems (Keycloak, Payment Gateway, Email Provider).

---

### 2. Container Diagram

File: `docs/architecture/container-diagram.md`

Produce a Mermaid C4 Container diagram showing:
- Browser.
- BFF Service.
- API Gateway.
- All 12 microservices.
- PostgreSQL instances (per service).
- MongoDB (catalog).
- Redis Cluster.
- Kafka Cluster.
- Keycloak.
- Arrows with protocol labels (HTTP, Kafka, Redis, JDBC).

---

### 3. Service Boundaries and Responsibilities

File: `docs/architecture/service-boundaries.md`

For each of the 12 services, document:
- Service name.
- Single-sentence responsibility summary.
- Domain events it publishes.
- Domain events it consumes.
- Primary storage.
- APIs it exposes (summary, not full spec).
- Dependencies on other services (synchronous and asynchronous).

Format as a table or structured list per service.

---

### 4. Data Ownership Matrix

File: `docs/architecture/data-ownership.md`

Produce a table:

| Data Entity | Owner Service | Storage | Source of Truth | Read By (other services) |
|---|---|---|---|---|

Include: Event, Show, TicketType, Venue, Inventory, Reservation, Order, Payment, Ticket, UserProfile, AuditLog, OutboxEvent.

---

### 5. Authentication Flow

File: `docs/architecture/authentication-flow.md`

Produce a Mermaid sequence diagram for:

**Login Flow**: Browser → BFF → Keycloak → BFF Callback → Session Cookie → Browser.

Include: state, nonce, PKCE code_verifier/code_challenge, authorization_code exchange, token storage, cookie setting.

**Authenticated API Call Flow**: Browser → BFF → Gateway → Resource Service.

Include: session resolution, token attachment, JWT validation at gateway, JWT validation at resource service, role/scope check, ownership check.

**Logout Flow**: Browser → BFF → Keycloak end_session → cookie cleared.

---

### 6. Reservation and Booking Flow

File: `docs/architecture/reservation-flow.md`

Produce a Mermaid sequence diagram for the full ticket reservation flow:

```
Browser → BFF → Gateway → Inventory Service (Redis Lua + PostgreSQL + Outbox)
→ Kafka → Order Service (create Order)
→ Order Service → Payment Service (create Payment Intent)
→ Payment Gateway → Payment Service (webhook callback)
→ Kafka → Order Service (PAID)
→ Kafka → Inventory Service (CONFIRMED)
→ Kafka → Ticket Service (ISSUED)
→ Kafka → Notification Service (email sent)
```

Annotate each arrow with:
- Protocol (HTTP, Kafka).
- Whether synchronous or asynchronous.
- Key data passed.

---

### 7. Payment Flow

File: `docs/architecture/payment-flow.md`

Produce a Mermaid sequence diagram for:

**Happy Path**: Order AWAITING_PAYMENT → Payment Intent → Provider redirect → Webhook → Payment SUCCEEDED → Order PAID.

**Failure Path**: Webhook → Payment FAILED → Order PAYMENT_FAILED → Inventory RELEASED.

**Refund Path**: Refund Request → Provider Refund API → Refund Webhook → Payment REFUNDED → Order REFUNDED → Ticket CANCELLED → Inventory RELEASED.

---

### 8. Saga Compensation Flow

File: `docs/architecture/saga-flow.md`

Produce a Mermaid diagram showing the Saga with compensation paths:

| Step | Action | Compensation |
|---|---|---|
| 1 | Inventory Reserve (HELD) | Inventory Release |
| 2 | Order Create (AWAITING_PAYMENT) | Order Cancel |
| 3 | Payment Create | — (no compensation needed if not yet processed) |
| 4 | Payment Succeed → Order PAID | Refund Payment |
| 5 | Inventory Confirm | Inventory Release |
| 6 | Ticket Issue | Ticket Cancel |

Show what happens when each step fails and which compensations trigger.

---

### 9. API Inventory

File: `docs/architecture/api-inventory.md`

List all REST APIs across all services (summary level):

| Service | Method | Path | Auth | Description |
|---|---|---|---|---|

Include at minimum:
- BFF auth endpoints (login, callback, logout, refresh).
- Catalog CRUD endpoints.
- Inventory reservation endpoints.
- Order CRUD endpoints.
- Payment endpoints.
- Ticket endpoints.
- Check-in endpoint.
- Admin endpoints.

---

### 10. Event Inventory

File: `docs/architecture/event-inventory.md`

List all Kafka events:

| Event Type | Version | Producer | Consumer(s) | Kafka Topic | Key | Description |
|---|---|---|---|---|---|---|

Include all events from: inventory, order, payment, ticket, notification.

---

### 11. Database Inventory

File: `docs/architecture/database-inventory.md`

List all database tables across all services:

| Service | Table | Storage | Key Columns | Key Indexes | Key Constraints | Purpose |
|---|---|---|---|---|---|---|

Include: inventory tables, reservation, outbox_events, processed_events, orders, payments, tickets, check_in_records, user_profiles.

---

### 12. ADR-001: Repository Strategy

File: `docs/adr/ADR-001-repository-strategy.md`

Decide: Monorepo or Multi-repo.

Format:
```
# ADR-001: Repository Strategy
## Status: Accepted
## Context: [why this decision is needed]
## Decision: [monorepo or multi-repo, with justification]
## Consequences: [positive and negative]
## Trade-offs: [what we gain, what we lose]
```

---

### 13. ADR-002: Inventory Consistency Strategy

File: `docs/adr/ADR-002-inventory-consistency-strategy.md`

Decide: How Redis and PostgreSQL work together for inventory reservation.

Cover:
- Why Redis for fast-path.
- Why PostgreSQL as source of truth.
- Write flow (Redis first, then PostgreSQL).
- Failure scenarios and recovery.
- Reconciliation approach.

---

### 14. ADR-003: Saga Strategy

File: `docs/adr/ADR-003-saga-strategy.md`

Decide: Choreography or Orchestration for the reservation-to-ticket flow.

Cover:
- Why the chosen approach.
- When to switch to the alternative.
- Compensation design.
- Observability of the saga.
- Timeout handling.

---

### 15. Local Development Environment

File: `infrastructure/docker-compose.yml`

Produce a Docker Compose file for local development containing:
- PostgreSQL (with multiple databases or schemas per service).
- MongoDB.
- Redis.
- Kafka + Zookeeper (or KRaft).
- Keycloak (with realm import).
- Kafka UI (optional, for debugging).
- RedisInsight (optional, for debugging).

Include health checks, volume mounts, port mappings, and environment variables.

---

### 16. Project Directory Structure

File: `docs/architecture/project-structure.md`

Produce the full directory tree for the project, showing where each service, shared module, documentation, infrastructure config, and load test lives.

---

## Completion Checklist

Before marking this step complete, verify:

- [ ] All 14 documentation files produced.
- [ ] docker-compose.yml produced and valid.
- [ ] All Mermaid diagrams render correctly.
- [ ] Service boundaries are clearly defined with no overlap.
- [ ] Data ownership is unambiguous — every data entity has exactly one owner.
- [ ] All ADRs have Status, Context, Decision, Consequences, Trade-offs.
- [ ] Authentication flow covers login, API call, and logout.
- [ ] Saga flow covers all compensation scenarios.
- [ ] Event inventory covers all inter-service events.
- [ ] No source code has been written in this step.
