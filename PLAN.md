# Odoo Café POS — Production-Grade Java Engineering Plan

## 1. Mission

Build a production-ready, multi-tenant Restaurant Point-of-Sale platform in Java that supports administration, POS sessions, floor and table operations, kitchen workflows, payments, promotions, receipts, reporting, and operational monitoring.

The project is intentionally sized as a long-term engineering portfolio:

- **1,000+ meaningful commits**
- **500+ focused pull requests**
- Every commit must compile or form a clearly marked intermediate step in a short-lived branch.
- Every PR must deliver a reviewable outcome with tests, documentation, or operational evidence.
- Empty commits, generated noise, duplicated changes, artificial file splitting, and fake PRs do not count.

This is a **Java backend-first** project. A web, mobile, or desktop client may consume the APIs later, but frontend work is outside the core target.

## 2. Product Outcomes

The completed backend must allow a restaurant organization to:

1. Create and manage outlets, employees, roles, products, categories, taxes, floors, tables, payment methods, and kitchen stations.
2. Open and close cashier sessions with cash-control reconciliation.
3. Create dine-in, takeaway, and delivery orders.
4. Prevent concurrent table and order conflicts.
5. Calculate prices, taxes, coupons, promotions, service charges, rounding, refunds, and totals deterministically.
6. Send only eligible order items to the correct Kitchen Display station in real time.
7. Process cash, UPI, card, split, partial, and mixed payments through idempotent workflows.
8. Generate receipts, invoices, exports, and business reports.
9. Preserve a complete, tamper-evident audit trail for sensitive operations.
10. Remain secure, observable, recoverable, scalable, and maintainable under production load.

## 3. Architecture Decision

Start as a **modular monolith** using domain-driven boundaries and hexagonal architecture. This provides strong isolation without the operational cost of premature microservices. Modules communicate through application interfaces and domain events, never by reaching directly into another module’s persistence layer.

Extract a module into a service only when measured scaling, deployment isolation, data ownership, or team autonomy justifies it.

### Request path

`HTTP/WebSocket -> Adapter -> Application Use Case -> Domain -> Port -> Infrastructure Adapter`

### Core rules

- Domain code has no dependency on Spring MVC, JPA, messaging, or external SDKs.
- Controllers perform transport mapping only.
- Application services coordinate use cases and transactions.
- Entities protect invariants; setters are not a business model.
- Repository interfaces are owned by the domain/application boundary.
- Cross-module integration uses published contracts and versioned events.
- All money uses `BigDecimal` plus an explicit ISO-4217 currency.
- All timestamps use `Instant`; business schedules use an outlet time zone.
- Every tenant-owned record includes `organization_id`; outlet data also includes `outlet_id`.

## 4. Recommended Technology Baseline

Pin exact versions in the repository and update through automated dependency PRs.

| Concern | Choice |
| --- | --- |
| Language | Java 21+ LTS |
| Framework | Spring Boot 3.x |
| Build | Maven Wrapper with a multi-module reactor |
| API | Spring MVC, Bean Validation, Problem Details (RFC 9457) |
| Security | Spring Security, OAuth 2.1/OIDC resource server, JWT, Argon2id |
| Persistence | PostgreSQL, Spring Data JPA, jOOQ for reporting queries |
| Migrations | Flyway; forward-only migrations |
| Cache/coordination | Redis with explicit TTL and cache ownership |
| Messaging | Kafka or RabbitMQ behind an application port |
| Real time | WebSocket/STOMP or Server-Sent Events |
| Object storage | S3-compatible storage for exports and receipts |
| API contract | OpenAPI 3.1 generated and contract-tested |
| Testing | JUnit 5, AssertJ, Mockito, Testcontainers, REST Assured, ArchUnit, WireMock |
| Quality | Spotless, Checkstyle, SpotBugs, PMD, JaCoCo, PIT on critical modules |
| Observability | Actuator, Micrometer, OpenTelemetry, Prometheus, Grafana, structured logs |
| Delivery | Docker, Docker Compose, GitHub Actions, Helm/Kubernetes after MVP |
| Security scanning | CodeQL, OWASP Dependency-Check, Trivy, Gitleaks, SBOM |

## 5. Repository Structure

```
cafe-pos/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── apps/
│   ├── api-server/
│   ├── worker/
│   └── migration-runner/
├── modules/
│   ├── identity-access/
│   ├── organization/
│   ├── catalog/
│   ├── pricing/
│   ├── floor-management/
│   ├── pos-session/
│   ├── ordering/
│   ├── kitchen/
│   ├── payment/
│   ├── customer/
│   ├── booking/
│   ├── receipt/
│   ├── reporting/
│   ├── notification/
│   └── audit/
├── platform/
│   ├── common-kernel/
│   ├── persistence/
│   ├── messaging/
│   ├── observability/
│   ├── security/
│   └── test-support/
├── contracts/
│   ├── openapi/
│   ├── asyncapi/
│   └── event-schemas/
├── deploy/
│   ├── docker/
│   ├── compose/
│   ├── helm/
│   └── observability/
├── docs/
│   ├── adr/
│   ├── api/
│   ├── runbooks/
│   ├── threat-models/
│   └── diagrams/
└── scripts/
```

Each business module should contain `domain`, `application`, `ports`, and `adapters` packages. Architecture tests must enforce these dependency directions.

## 6. Bounded Contexts and Responsibilities

### Identity and Access

- User invitation, activation, login, refresh, logout, password reset, MFA readiness
- Roles: platform admin, organization owner, outlet manager, cashier, waiter, kitchen staff, accountant, auditor
- Fine-grained permissions and outlet-level assignments
- Session revocation, failed-login throttling, security events

Public signup creates an organization owner only through a verified onboarding workflow. Employees cannot self-promote.

### Organization

- Organization, outlet, legal identity, locale, currency, business date, time zone
- Outlet configuration and operating hours
- Tenant isolation and subscription/feature flags

### Catalog

- Categories, products, variants, modifiers, combos, units of measure
- Kitchen routing, availability windows, dietary/allergen metadata
- Price history and soft archive

### Pricing and Tax

- Tax-inclusive and tax-exclusive pricing
- Coupon validation and redemption limits
- Product-, category-, customer-, and order-level promotions
- Priority, stacking, exclusivity, validity window, outlet eligibility
- Service charge, rounding strategy, immutable calculation snapshot

The pricing engine must be a pure deterministic component with property-based and mutation tests.

### Floor Management and Booking

- Floors, tables, capacities, statuses, table merge/split/transfer
- Reservations, waitlist, seating, no-show, cancellation
- Conflict protection through database constraints and locking

### POS Session

- Opening float, cash drawer, session ownership, shift handover
- Cash in/out with reason and approval
- Closing count by denomination, expected-versus-actual variance
- Manager approval for excessive variance

### Ordering

- Dine-in, takeaway, delivery
- Draft order, item customization, notes, course, hold/fire
- Optimistic concurrency using version fields
- Item void, order cancellation, manager approval and reason codes
- Order transfer, table transfer, split/merge bills
- Immutable paid-order snapshot

### Kitchen

- Kitchen stations and product routing
- Kitchen ticket and item states: queued, accepted, preparing, ready, served, cancelled
- Re-fire, priority, preparation timer, SLA breach
- Real-time delivery with reconnect and replay
- Outbox-backed delivery to prevent lost kitchen tickets

### Payment

- Cash, card, UPI, split, partial, mixed tender, tips
- Provider abstraction; never store raw card data
- Idempotency key for every payment/refund command
- Payment intent, attempt, transaction, reconciliation, refund
- Webhook signature verification, replay protection, audit evidence
- Manual UPI confirmation clearly separated from provider-confirmed payment

### Customer

- Search, contact details, preferences, tax identity, consent
- Duplicate detection, merge, retention, anonymization
- Loyalty ledger as an optional later phase

### Receipt and Notification

- Number series per outlet/fiscal year
- HTML/PDF receipt generation and printer-friendly output
- Email/SMS adapter, templates, retry, dead-letter handling
- Receipt reissue without altering the original transaction

### Reporting

- Sales, taxes, discounts, tenders, sessions, products, employees, categories
- Time-zone-aware business-day aggregation
- CSV/XLSX/PDF export as asynchronous jobs
- Read models/materialized views for heavy analytics
- Data freshness and report provenance displayed in output metadata

### Audit and Compliance

- Append-only audit events with actor, tenant, request, IP, before/after summary, and reason
- PII classification, retention schedule, export, anonymization
- Sensitive actions require permission plus step-up/manager approval where applicable

## 7. Core Domain Model

Primary aggregates:

- `Organization`, `Outlet`, `EmployeeAssignment`, `Role`, `Permission`
- `Category`, `Product`, `ProductVariant`, `ModifierGroup`, `TaxRule`
- `Promotion`, `Coupon`, `CouponRedemption`, `PriceSnapshot`
- `Floor`, `DiningTable`, `Reservation`
- `PosSession`, `CashDrawerMovement`, `SessionClosing`
- `Order`, `OrderLine`, `OrderDiscount`, `OrderTax`, `OrderStatusHistory`
- `KitchenTicket`, `KitchenTicketItem`, `KitchenStation`
- `PaymentIntent`, `PaymentAttempt`, `PaymentTransaction`, `Refund`
- `Customer`, `ConsentRecord`
- `Receipt`, `ExportJob`, `Notification`
- `AuditEvent`, `OutboxEvent`, `InboxMessage`

### Required invariants

- One table cannot have two active dine-in orders unless explicitly configured.
- One cashier cannot open conflicting sessions for the same outlet/device.
- A paid order cannot be edited; corrections use void/refund flows.
- A refund cannot exceed the settled refundable amount.
- Currency must match across order, payment, refund, and outlet.
- Promotion use must satisfy validity, eligibility, usage, and stacking rules.
- Kitchen dispatch is idempotent and cannot silently lose an item.
- Repeated payment or webhook requests cannot create duplicate financial records.
- Deactivated products remain visible in historical orders.
- Tenant and outlet authorization is checked inside the use case, not only at the URL layer.

## 8. API Standards

- Base path: `/api/v1`
- Resource-oriented endpoints; commands for meaningful state transitions
- UUID/ULID identifiers; never expose sequential database IDs
- Cursor pagination for high-volume feeds; offset pagination only for small administration lists
- `Idempotency-Key` required for order submission, payment, refund, kitchen send, session close, and export creation
- `ETag`/`If-Match` or version field for concurrent edits
- Standard Problem Details response with stable error code, correlation ID, field errors, and safe detail
- UTC timestamps in API responses plus outlet time-zone metadata where relevant
- OpenAPI examples, error cases, auth scopes, and pagination documented
- Deprecation policy and backward-compatible versioning

Representative endpoints:

```
POST   /api/v1/auth/login
POST   /api/v1/organizations/{organizationId}/employees/invitations
GET    /api/v1/outlets/{outletId}/products
POST   /api/v1/outlets/{outletId}/pos-sessions
POST   /api/v1/pos-sessions/{sessionId}/close
POST   /api/v1/outlets/{outletId}/orders
POST   /api/v1/orders/{orderId}/lines
POST   /api/v1/orders/{orderId}/send-to-kitchen
POST   /api/v1/orders/{orderId}/payments
POST   /api/v1/payments/{paymentId}/refunds
GET    /api/v1/kitchen/stations/{stationId}/tickets
PATCH  /api/v1/kitchen/tickets/{ticketId}/status
POST   /api/v1/reports/sales/exports
```

## 9. Data and Transaction Strategy

- PostgreSQL is the source of truth.
- One schema initially; every tenant table has mandatory tenant keys and composite indexes.
- Apply tenant filtering in repository specifications and verify with isolation tests.
- Use database unique/check/exclusion constraints for correctness, not application checks alone.
- Use optimistic locking for frequently edited aggregates and pessimistic/advisory locking only for narrowly defined contention.
- Use the transactional outbox for messages and an inbox/deduplication table for consumers.
- Never make an external payment/email call inside a database transaction.
- Keep migrations backward-compatible during rolling deployment: expand, migrate, contract.
- Seed data is deterministic and environment-specific secrets are forbidden.
- Define backup, point-in-time recovery, restore testing, archival, and deletion policies.

## 10. Security Baseline

- Deny by default; authorize every command at use-case level.
- Short-lived access tokens, rotating refresh tokens, revocation, secure cookies where applicable.
- Argon2id password hashing, breached-password checks, lockout/rate limiting, optional MFA.
- Validate issuer, audience, expiry, algorithm, and key rotation for JWTs.
- Strict CORS allowlist; CSRF protection for cookie authentication.
- Validation at transport and domain boundaries; safe output encoding.
- Encrypt data in transit and at rest; secrets only through a secret manager.
- Mask tokens, emails, phone numbers, payment references, and PII in logs.
- Webhook signatures, timestamps, nonce/replay protection.
- OWASP ASVS-inspired verification checklist and documented threat models.
- Dependency, container, IaC, source, and secret scanning in CI.
- Produce CycloneDX SBOM and sign release images.

## 11. Reliability and Observability

Define initial service-level objectives and refine using production evidence:

- API availability: 99.9%
- Read endpoint p95: under 300 ms
- Order command p95: under 600 ms
- Kitchen event delivery p95: under 2 seconds
- Payment command correctness: no duplicate capture from identical idempotency key
- Recovery point objective: 5 minutes; recovery time objective: 30 minutes

Implementation requirements:

- JSON structured logging with trace ID, span ID, tenant ID, outlet ID, user ID, and safe request metadata
- Metrics for HTTP, JVM, database pool, cache, messages, payments, orders, kitchen lag, exports, and notification failures
- Distributed tracing across HTTP, jobs, messages, and provider adapters
- Health groups for liveness, readiness, and dependency diagnostics
- Timeouts, bounded retries with jitter, circuit breakers, bulkheads, and rate limits
- Dead-letter queues with replay runbooks
- Dashboards and actionable alerts tied to SLO symptoms
- Graceful shutdown and zero-downtime migration/deployment rules

## 12. Testing Strategy

| Test layer | Purpose | Required tools/evidence |
| --- | --- | --- |
| Unit | Domain rules and pure calculations | JUnit, AssertJ; fast and deterministic |
| Property | Pricing, tax, money invariants | jqwik or QuickTheories |
| Mutation | Strength of critical tests | PIT for pricing, orders, payments |
| Slice | Web, security, persistence mappings | Spring test slices |
| Integration | PostgreSQL, Redis, broker, object storage | Testcontainers; no H2 substitute |
| Contract | API and provider compatibility | OpenAPI validation, WireMock, consumer contracts |
| Architecture | Module and dependency boundaries | ArchUnit |
| End-to-end | Critical business journeys | REST Assured against production-like stack |
| Performance | Throughput, latency, contention | k6 or Gatling with thresholds |
| Resilience | Timeout, duplicate, broker, provider failures | fault injection scenarios |
| Security | Authorization, tenant isolation, abuse cases | automated negative test suite |

Quality gates:

- New/changed business logic: at least 90% line and 85% branch coverage.
- Repository-wide coverage is a signal, not a target to game.
- Zero critical/high security findings without a time-bound approved exception.
- No flaky test may be silently retried or ignored.
- Critical mutation score target: 75% initially, raised over time.

## 13. Delivery Pipeline

### Pull request pipeline

1. Validate commit/PR metadata and changed-file policy.
2. Format, compile, static analysis, architecture tests.
3. Unit, property, and slice tests.
4. Integration and contract tests with Testcontainers.
5. Build OCI image and generate SBOM.
6. Scan source, dependencies, secrets, image, and manifests.
7. Start ephemeral stack and run smoke/E2E tests.
8. Publish test, coverage, mutation, and migration reports.

### Main/release pipeline

- Protected main branch, required review, required checks, signed commits/tags where practical
- Conventional Commits and automated semantic release notes
- Immutable versioned images; never deploy `latest`
- Staging deployment plus migration compatibility check
- Manual production approval initially
- Canary or blue/green release with automated rollback signals
- Post-deployment smoke tests and release evidence

## 14. Definition of Ready

A work item is ready only when it contains:

- User/business outcome
- Acceptance criteria and failure cases
- Authorization and tenant scope
- API/event/data changes
- Concurrency and idempotency needs
- Observability and audit needs
- Test strategy
- Migration/rollback impact
- Dependencies and out-of-scope statement

## 15. Definition of Done

A PR is complete only when applicable items are satisfied:

- Acceptance criteria implemented without unrelated scope
- Tests cover happy paths, boundaries, failures, permissions, and tenant isolation
- API/event schemas and examples updated
- Database migration is safe and reviewed
- Logs, metrics, traces, audit events, alerts, and runbook updated
- Security/privacy implications reviewed
- Documentation/ADR updated
- CI is green and no unresolved high-risk findings remain
- Reviewer approval obtained; author has self-reviewed the diff
- Squash title/changelog is meaningful and linked to an issue

## 16. Git and PR Operating Model

### Branches

- `main` is always releasable.
- Branch names: `feat/123-order-splitting`, `fix/456-payment-idempotency`, `docs/789-refund-runbook`.
- Branch lifetime target: under three working days.
- Prefer PRs of roughly 50–400 changed hand-written lines; larger changes must explain why they cannot be divided safely.

### Commit convention

```
feat(ordering): add immutable paid-order snapshot
fix(payment): reject duplicate provider webhook
test(pricing): cover exclusive coupon precedence
docs(adr): choose transactional outbox
perf(reporting): add indexed daily sales projection
security(auth): rotate refresh token on use
```

Each commit should represent one coherent idea. Do not use messages such as `update`, `changes`, `work`, `fix stuff`, or numbered filler.

### PR categories

- Feature vertical slice
- Bug fix with regression test
- Refactor preserving behavior
- Test-strength improvement
- Security hardening
- Performance/scale improvement with benchmark
- Observability or runbook improvement
- Dependency/platform update
- Documentation/ADR/API contract improvement

## 17. 1,000-Commit / 500-PR Portfolio Plan

The following budget is a planning ceiling/floor, not permission to manufacture activity. Each phase targets **50 meaningful commits and 25 merged PRs**, producing **1,000 commits and 500 PRs** across 20 phases. A PR will commonly contain two coherent commits: implementation and verification/documentation.

| Phase | Engineering outcome | Commits | PRs |
| --- | --- | --- | --- |
| 1 | Repository foundation, standards, ADRs, local environment | 50 | 25 |
| 2 | Common kernel, error model, IDs, money, time, test support | 50 | 25 |
| 3 | Identity, authentication, invitations, authorization | 50 | 25 |
| 4 | Organization, outlet, tenant isolation, configuration | 50 | 25 |
| 5 | Catalog, categories, variants, modifiers, availability | 50 | 25 |
| 6 | Taxes, pricing engine, coupons, promotions | 50 | 25 |
| 7 | Floors, tables, reservations, waitlist | 50 | 25 |
| 8 | POS sessions, cash drawer, opening and closing | 50 | 25 |
| 9 | Order aggregate, cart, totals, concurrency | 50 | 25 |
| 10 | Advanced ordering: transfer, split, merge, void | 50 | 25 |
| 11 | Kitchen stations, tickets, real-time workflow | 50 | 25 |
| 12 | Payment abstraction, cash, manual UPI, card adapter | 50 | 25 |
| 13 | Split payments, refunds, webhooks, reconciliation | 50 | 25 |
| 14 | Customers, consent, deduplication, retention | 50 | 25 |
| 15 | Receipts, numbering, email, printing, notifications | 50 | 25 |
| 16 | Reporting, exports, projections, scheduled jobs | 50 | 25 |
| 17 | Audit, compliance, security hardening, threat models | 50 | 25 |
| 18 | Observability, resilience, performance, capacity | 50 | 25 |
| 19 | CI/CD, containers, staging, deployment, recovery | 50 | 25 |
| 20 | Release hardening, chaos drills, documentation, v1.0 | 50 | 25 |
| **Total** | **Production-grade portfolio** | **1,000** | **500** |

### Internal breakdown for every 25-PR phase

- 10 feature/domain PRs
- 4 persistence and migration PRs
- 3 API/event-contract PRs
- 3 automated-test and quality PRs
- 2 security/reliability PRs
- 2 observability/performance PRs
- 1 documentation/runbook/ADR PR

Adjust the mix by phase, but never lower the quality gate merely to hit the count.

## 18. Example First 25 Pull Requests

1. Bootstrap Maven reactor and Java toolchain.
2. Add formatting and compiler enforcement.
3. Add static analysis baseline.
4. Add unit-test and coverage reporting.
5. Add architecture-test module.
6. Add local PostgreSQL and Redis Compose stack.
7. Add application configuration profiles with validation.
8. Add structured error response contract.
9. Add correlation ID filter and safe JSON logging.
10. Add health, readiness, and metrics endpoints.
11. Create identifier value-object conventions.
12. Create money and currency value objects.
13. Create time and business-date abstractions.
14. Add current-user and tenant context ports.
15. Add audit metadata interface.
16. Add pagination and sorting contract.
17. Add Flyway migration standards and validation.
18. Add PostgreSQL Testcontainers test support.
19. Add Redis Testcontainers test support.
20. Add OpenAPI generation and validation.
21. Add Docker multi-stage production image.
22. Add dependency, secret, and image scanning.
23. Add pull-request CI workflow and branch policy docs.
24. Add ADRs for modular monolith, PostgreSQL, and outbox.
25. Add developer guide and verified one-command startup.

## 19. Release Milestones

### M0 — Foundation (PR 1–50)

Build, local environment, platform conventions, common kernel, migrations, CI, security skeleton, observability skeleton.

Exit gate: a new developer can clone, run, test, and inspect the service using documented commands.

### M1 — Administration MVP (PR 51–150)

Identity, RBAC, tenant/outlet isolation, employees, categories, products, taxes, payment configuration, floors, tables.

Exit gate: an organization owner can configure an outlet without direct database access.

### M2 — POS MVP (PR 151–275)

Session opening, table selection, order creation, price calculation, coupons, cash/manual UPI/card-reference flows, receipt generation.

Exit gate: a cashier can complete a sale and close a balanced session in an isolated tenant.

### M3 — Kitchen and Advanced Operations (PR 276–350)

Reliable kitchen tickets, item progression, table/order transfer, split bills, reservations, refunds, manager approvals.

Exit gate: duplicate/reordered messages and concurrent cashiers do not corrupt orders or kitchen state.

### M4 — Reporting and Operations (PR 351–425)

Dashboards, exports, reconciliation, audit, notifications, retention, SLOs, alerts, runbooks.

Exit gate: operators can detect, diagnose, recover, reconcile, and explain critical events.

### M5 — Production v1 (PR 426–500+)

Performance, resilience, security assessment, backup/restore drills, zero-downtime deployment, user acceptance, release documentation.

Exit gate: all release criteria, threat mitigations, load thresholds, recovery drills, and operational checklists pass.

## 20. Production Scenarios That Must Be Proven

- Two cashiers update the same order simultaneously.
- A kitchen client disconnects, reconnects, and catches up without duplicate tickets.
- A payment request times out after the provider succeeded.
- The same provider webhook arrives multiple times and out of order.
- A promotion expires while an order remains open.
- An outlet changes tax configuration after an order was created.
- A paid order needs correction, partial refund, and receipt reissue.
- A user from Tenant A guesses an identifier belonging to Tenant B.
- Redis, message broker, email provider, or object storage is unavailable.
- Database migration runs during a rolling deployment.
- Business day crosses midnight in the outlet’s time zone.
- Cashier closes with a variance requiring manager approval.
- Backup restoration is performed and reconciliation confirms integrity.

## 21. Backlog Expansion for a Long-Term Product

After the original scope is stable, add only through business-backed epics:

- Inventory ingredients, recipes, stock movements, wastage, and low-stock alerts
- Multi-kitchen routing and preparation capacity
- Loyalty points and gift cards using immutable ledgers
- Online ordering and delivery-provider integration
- Offline-first terminal synchronization and conflict resolution
- Device and printer management
- Dynamic menu schedules and outlet-specific pricing
- Fiscal invoice/GST-ready extension points by jurisdiction
- Fraud/anomaly detection and approval policies
- Forecasting, product affinity, and staffing insights
- Feature flags, subscription plans, and usage metering
- Data warehouse/CDC integration

## 22. AI-Assisted Development Guardrails

Gemini CLI, Codex, or another coding assistant may accelerate implementation, but the developer remains accountable.

For every AI-generated change:

1. Start from a tracked issue and acceptance criteria.
2. Ask for the smallest vertical slice.
3. Inspect every changed line and dependency.
4. Run focused tests, then the complete relevant module suite.
5. Validate authorization, tenant isolation, money, time, concurrency, and idempotency explicitly.
6. Never paste secrets, production data, or private customer information.
7. Reject invented APIs, disabled tests, swallowed exceptions, and insecure shortcuts.
8. Record architectural decisions in ADRs rather than relying on chat history.

### Reusable implementation prompt

```
You are a principal Java engineer working on a Java 21, Spring Boot modular-monolith Restaurant POS.

Implement only issue <ISSUE-ID>: <TITLE>.

Read CONTRIBUTING.md, relevant ADRs, module boundaries, OpenAPI/event contracts, and existing tests before changing code. Preserve hexagonal dependency direction and tenant isolation. Do not introduce cross-module repository access.

Acceptance criteria:
<PASTE CRITERIA>

Required engineering checks:
- Model business invariants in the domain, not controllers.
- Use BigDecimal and explicit currency for money; Instant plus outlet time zone for time.
- Define authorization, audit, idempotency, concurrency, and failure behavior.
- Add/update Flyway migrations with indexes and constraints when persistence changes.
- Add unit, integration/Testcontainers, authorization, tenant-isolation, and regression tests as applicable.
- Update OpenAPI/events, ADRs, metrics, logs, and runbooks when affected.
- Keep the diff focused and backward compatible.
- Do not disable quality gates, expose secrets/PII, or fabricate test results.

Before editing, return a concise implementation plan and list the files expected to change. After editing, run the relevant Maven checks and report actual results, remaining risks, and a Conventional Commit message. Do not commit or open a PR unless explicitly instructed.
```

## 23. Success Metrics

The project is successful when:

- The backend supports the full administration, POS, kitchen, payment, customer, receipt, and reporting lifecycle.
- Domain invariants hold under concurrency, retries, duplicate requests, and partial dependency failures.
- Tenant isolation and permission tests cover every sensitive use case.
- A fresh environment can be provisioned and verified automatically.
- Backup restore, rollback, reconciliation, and incident runbooks are tested.
- The 1,000 commits and 500 PRs form an understandable engineering history, with each change linked to real value.

The numerical Git targets are evidence of sustained engineering only when the repository itself remains coherent, reviewable, secure, tested, and deployable.
