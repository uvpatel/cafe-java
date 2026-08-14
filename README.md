# Odoo Cafe POS Backend

Production-grade, backend-only restaurant Point-of-Sale platform built with Java 25, Spring Boot 3.4/4.x, PostgreSQL, and REST/WebSocket APIs.

## Architecture

Modular monolith following Spring Modulith patterns.

### Domain Modules
- `shared`: Common utilities, money domain types, base exceptions, audit listeners, multi-tenancy.
- `identity`: User authentication, token lifecycle, security context, role permissions.
- `organization`: Cafe organization management, settings, business details.
- `catalog`: Products, categories, taxes, units of measure, pricing history.
- `floor`: Floors, tables, occupancy management.
- `customer`: Customer directory and profile management.
- `promotion`: Coupons, automated promotions, precedence & stacking logic.
- `session`: POS cashier sessions, cash movements, cash drawer reconciliation.
- `ordering`: Orders, lines, taxes, discounts, server-driven pricing engine.
- `kitchen`: Kitchen station routing, tickets, items, outbox dispatch.
- `payment`: Cash, card reference, manual UPI QR and confirmation handling.
- `receipt`: Immutable receipt snapshot generation, print payload, email queue.
- `booking`: Table reservations and occupancy calendar logic.
- `reporting`: Operational reporting, metrics aggregation, asynchronous export jobs.
- `notification`: Outbox event publishing and email dispatching.
- `activitylog`: Audit trail and system activity tracking.

## Getting Started

### Prerequisites
- Java 25 SDK
- Docker and Docker Compose
- Maven (or `./mvnw`)

### Running Local Infrastructure
```bash
docker compose up -d
```

### Build & Run
```bash
./mvnw clean compile
./mvnw test
./mvnw spring-boot:run
```

## Implementation Plan
See [PLAN.md](file:///Users/urvilpatel/freelance-projects/javaproject/PLAN.md) for full engineering specifications, PR breakdown, domain rules, and architecture decision records.
