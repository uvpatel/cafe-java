# Engineering Backlog — Prioritized Workstream Tasks

This document tracks technical debt, bug fixes, features, and operational improvements categorized strictly by priority.

---

## P0 — Security, Data Integrity, and Build Failures (Urgent)

- [x] **[P0-01] Fix Spring Modulith Package Encapsulation Violations**
  - **Description**: Resolve cross-module encapsulation failures where `promotion` and `ordering` had cyclic dependencies and unexposed internal packages.
  - **Status**: Completed in PR #001 (`fix/architecture-modulith-boundaries-and-hygiene`).

- [x] **[P0-02] Untrack Build Artifacts in Git**
  - **Description**: Remove tracked `target/` surefire report XMLs and class files from git repository tracking.
  - **Status**: Completed in PR #001 (`fix/architecture-modulith-boundaries-and-hygiene`).

- [ ] **[P0-03] Strict Tenant Isolation Guard in Repository Queries**
  - **Description**: Ensure all JPA queries strictly enforce `cafeId` filtering and prevent IDOR/cross-tenant access vulnerabilities.

---

## P1 — Critical Product Workflows

- [ ] **[P1-01] Order State Transition Guard & Concurrency Control**
  - **Description**: Enforce invalid state transition rejection (e.g. `COMPLETED` -> `DRAFT`) and handle concurrent order modifications using `@Version` optimistic locking.

- [ ] **[P1-02] Payment Processing Idempotency & Partial Refunds**
  - **Description**: Ensure payment transactions require `Idempotency-Key` headers and prevent double charging.

- [ ] **[P1-03] Automatic Recipe Inventory Stock Deduction**
  - **Description**: Automatically deduct ingredient stock levels upon order completion.

---

## P2 — Reliability, Performance, and Operational Readiness

- [ ] **[P2-01] Distributed Audit Logging with Tenant Information**
  - **Description**: Capture structured audit events for sensitive employee, role, pricing, and refund operations.

- [ ] **[P2-02] ArchUnit Boundary Enforcement Suite**
  - **Description**: Prevent field injection and enforce domain purity across all present and future modules.

- [ ] **[P2-03] Neon PostgreSQL Integration IT Pipeline**
  - **Description**: Continuous integration verification against Testcontainers PostgreSQL / Neon DB environment.

---

## P3 — Developer Experience and Enhancements

- [ ] **[P3-01] Spotless Code Formatting Maven Plugin Integration**
  - **Description**: Integrate spotless formatting rules into Maven build cycle to maintain uniform code structure across all 1,000+ planned commits.

- [ ] **[P3-02] OpenAPI 3.0 Documentation Polish**
  - **Description**: Annotate all REST controllers with clear RFC 9457 problem detail error responses and request/response examples.
