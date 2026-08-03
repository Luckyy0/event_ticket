# Event Ticketing & Flash Sale System — Prompt Library

## Purpose

This repository contains a complete set of prompts designed to guide an AI assistant through the full development lifecycle of a high-concurrency Event Ticketing & Flash Sale System using Java 21, Spring Boot 3.x, Microservices, Kafka, PostgreSQL, Redis, and Keycloak.

The prompts are divided into two categories:

1. **Global Rules** — Context and constraints that the AI must follow at every step.
2. **Execution Steps** — Sequential, atomic tasks that the AI executes one at a time.

---

## File Structure

```text
event-ticketing-prompts/
│
├── README.md                           ← You are here
│
├── global-rules/
│   ├── 00-Context.md                   ← System overview, actors, business states, invariants
│   ├── 01-Technology-And-Architecture-Rules.md
│   ├── 02-IAM-And-BFF-Rules.md
│   ├── 03-Testing-And-TDD-Rules.md
│   ├── 04-Hexagonal-And-Code-Quality-Rules.md
│   ├── 05-API-Event-And-Consistency-Rules.md
│   └── 06-Security-Observability-And-Operations-Rules.md
│
└── execution-steps/
    ├── 10-Step-00-Initial-Architecture.md
    ├── 11-Step-01-Keycloak-And-BFF-Authentication.md
    ├── 12-Step-02-API-Gateway-And-Resource-Security.md
    ├── 13-Step-03-Catalog-Event-Show-Foundation.md
    ├── 14-Step-04-Inventory-Domain-TDD.md
    ├── 15-Step-05-Inventory-PostgreSQL-Persistence.md
    ├── 16-Step-06-Inventory-Redis-Reservation.md
    ├── 17-Step-07-Inventory-Outbox-Kafka.md
    ├── 18-Step-08-Inventory-Expiration-And-Reconciliation.md
    ├── 19-Step-09-Order-Domain-And-Idempotent-API.md
    ├── 20-Step-10-Order-Kafka-Inbox-Outbox.md
    ├── 21-Step-11-Payment-Intent-And-Provider-Adapter.md
    ├── 22-Step-12-Payment-Webhook-And-Idempotency.md
    ├── 23-Step-13-Payment-Refund.md
    ├── 24-Step-14-Ticket-Issuance.md
    ├── 25-Step-15-Ticket-Check-In.md
    ├── 26-Step-16-Notification-Delivery.md
    ├── 27-Step-17-Saga-Compensation.md
    ├── 28-Step-18-Reconciliation-And-Repair.md
    ├── 29-Step-19-End-To-End-Testing.md
    ├── 30-Step-20-Load-Test-And-Resilience.md
    ├── 31-Step-21-CI-CD-And-Quality-Gates.md
    └── 32-Step-22-Kubernetes-And-Operations.md
```

---

## How to Use

### First Session

Send to the AI in a single message:

```text
1. README.md
2. global-rules/00-Context.md
3. global-rules/01-Technology-And-Architecture-Rules.md
4. global-rules/02-IAM-And-BFF-Rules.md
5. global-rules/03-Testing-And-TDD-Rules.md
6. global-rules/04-Hexagonal-And-Code-Quality-Rules.md
7. global-rules/05-API-Event-And-Consistency-Rules.md
8. global-rules/06-Security-Observability-And-Operations-Rules.md
9. execution-steps/10-Step-00-Initial-Architecture.md
```

### Subsequent Sessions

Each time you start a new execution step, send:

```text
1. README.md
2. All files in global-rules/
3. The current execution step file
4. Source code generated so far (or a summary of current project state)
5. Key artifacts from previous steps (architecture decisions, schemas, contracts)
6. List of architecture decisions already finalized
```

Example — when running Inventory Domain TDD:

```text
README.md
global-rules/*
execution-steps/14-Step-04-Inventory-Domain-TDD.md
docs/architecture/*
Current source code tree
```

### Context Recovery

If the AI loses context mid-session:

1. Re-send all global rules.
2. Re-send the current execution step.
3. Provide the current source code tree or a `git diff` of recent changes.
4. Summarize which steps have been completed and key decisions made.

---

## Output Structure

The AI will generate the following directory structure for the project:

```text
event-ticketing/
├── docs/
│   ├── architecture/          ← System diagrams, flow descriptions, ADRs
│   └── adr/                   ← Architecture Decision Records
├── infrastructure/
│   ├── docker-compose.yml     ← Local development environment
│   ├── docker-compose.test.yml
│   └── keycloak/              ← Realm config, test users
├── services/
│   ├── bff-service/
│   ├── api-gateway-service/
│   ├── catalog-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── ticket-service/
│   ├── notification-service/
│   ├── check-in-service/
│   ├── user-profile-service/
│   ├── audit-service/
│   └── reconciliation-service/
├── shared/
│   ├── event-contracts/       ← Event envelope definitions, shared DTOs
│   └── test-support/          ← Shared test utilities, base test classes
└── load-tests/
    └── k6/ or gatling/
```

---

## Step Completion Criteria

A step is considered DONE only when the AI has provided ALL of the following:

1. Acceptance criteria.
2. Test plan.
3. Failing tests (RED).
4. Minimal production code to pass tests (GREEN).
5. Refactored code (REFACTOR).
6. All unit tests passing.
7. All integration tests passing (with Testcontainers where applicable).
8. Required configuration files.
9. Instructions to run.
10. Failure scenarios analyzed.
11. Security considerations documented.
12. Performance considerations documented.
13. Trade-offs explained.
14. List of files created or modified.
15. Definition of Done checklist.

### A Step MUST NOT Be Marked Complete If:

- Code does not compile.
- Tests are incomplete or missing.
- Testcontainers are not used for real infrastructure tests.
- Transaction boundaries are undefined.
- Idempotency strategy is undefined.
- Source of truth is unclear.
- Consistency strategy is unexplained.
- Output contains only skeleton or pseudo-code.

---

## Conventions

- **Language**: All prompts and AI output are in English.
- **Timestamps**: UTC (ISO 8601).
- **IDs**: UUID v7 or ULID where applicable.
- **Naming**: `shouldExpectedBehavior_whenCondition()` for test methods.
- **Test structure**: Given-When-Then.
- **Architecture**: Hexagonal (Domain → Application → Adapter).
- **Events**: Transactional Outbox → Kafka.
- **Consistency**: Eventual, with compensation and reconciliation.
