# Prioritized Engineering Backlog

This backlog maintains a live, prioritized queue of technical improvements, features, bug fixes, and operational tasks for the `uvpatel/cafe-java` platform.

---

## P0: Security, Data Integrity & Broken Builds

- **[P0-01] Spotless & CI Code Quality Gate**: Enforce Spotless Java formatting check and compile checks on every pull request via GitHub Actions CI pipeline. (*Assigned: PR-001*)
- **[P0-02] Cross-Tenant Data Isolation Guard**: Ensure all database queries and cached records enforce strict tenant context checks derived from verified JWT claims. Prevent IDOR and cross-tenant access. (*Assigned: Track 4*)
- **[P0-03] Financial & Tax Calculation Determinism**: Prevent floating-point inaccuracy in pricing, discounts, service charges, and taxes by strictly enforcing `BigDecimal` with rounding modes. (*Assigned: Track 8*)
- **[P0-04] Idempotent Payment & Order Submission**: Eliminate duplicate order creation and double-charging risks using unique client idempotency keys and transactional outbox. (*Assigned: Tracks 8 & 12*)
- **[P0-05] Database Concurrency Control**: Add optimistic locking `@Version` fields on critical aggregate roots (Order, Stock, Session) to prevent lost updates under high concurrency. (*Assigned: Tracks 8 & 13*)

---

## P1: Critical Product Workflows

- **[P1-01] Multi-Outlet Management & Settings**: Support multi-tenant organization onboarding with outlet-specific tax rules, operating hours, and time zones. (*Assigned: Track 5*)
- **[P1-02] Catalog & Modifier Selection Rules**: Enforce minimum and maximum modifier selection rules and outlet pricing overrides. (*Assigned: Track 7*)
- **[P1-03] Kitchen Display System (KDS) & Real-Time Delivery**: Route order line items to specific kitchen stations with real-time STOMP WebSocket notifications and SLA tracking. (*Assigned: Track 9*)
- **[P1-04] Split Billing & Multi-Tender Payments**: Allow customers to split bills by item/seat and pay using cash, UPI QR codes, or card references. (*Assigned: Track 12*)
- **[P1-05] Recipe-Based Automated Stock Deduction**: Deduct ingredient stock automatically upon order completion and maintain an immutable stock ledger. (*Assigned: Track 13*)

---

## P2: Reliability, Performance & Operations

- **[P2-01] Actuator Health & Neon PostgreSQL Monitoring**: Configure custom Spring Actuator health indicators and database connection pool diagnostics. (*Assigned: Track 17*)
- **[P2-02] Structured JSON Logging & Masking**: Output structured logs containing request correlation IDs and tenant IDs while redacting sensitive tokens and credentials. (*Assigned: Track 17*)
- **[P2-03] Multi-Stage Production Docker Build**: Provide optimized container image configurations adhering to non-root execution and healthcheck standards. (*Assigned: Track 18*)
- **[P2-04] Asynchronous CSV & PDF Reporting**: Stream large sales summaries and inventory valuation reports asynchronously without blocking UI threads. (*Assigned: Track 16*)

---

## P3: Enhancements & Developer Experience

- **[P3-01] OpenAPI Swagger Documentation Baseline**: Annotate all public REST endpoints with descriptive OpenAPI documentation and DTO schemas. (*Assigned: Track 1*)
- **[P3-02] Developer Seed Data & Mock Providers**: Furnish realistic seed scripts and mock payment gateway adapters for frictionless local testing. (*Assigned: Tracks 1 & 12*)
- **[P3-03] ArchUnit Module Boundary Tests**: Verify Spring Modulith architectural boundaries automatically during Maven verify builds. (*Assigned: Track 1*)
