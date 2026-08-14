# Odoo Cafe POS — Java Backend Engineering Plan

> Backend-only restaurant Point-of-Sale platform built with Java 25, Spring Boot 4.1, PostgreSQL, and production-grade REST/WebSocket APIs.

## 1. Purpose

This document is the master implementation plan for the Odoo Cafe POS backend. It converts the supplied full-stack product specification into a Java-only backend roadmap covering architecture, domain modeling, REST APIs, real-time kitchen events, security, database integrity, automated tests, deployment, and a meaningful 1,000+ commit / 200+ pull-request workflow.

The backend must remain usable by any future web, mobile, kiosk, or kitchen-display client. No frontend implementation is included.

## 2. Product Vision

Build a reliable restaurant operating system that allows a cafe to:

- Configure products, categories, taxes, floors, tables, payment methods, employees, coupons, and promotions.
- Open and close cashier sessions with traceable totals.
- Create table-based or takeaway orders.
- Calculate quantities, taxes, discounts, promotions, and totals deterministically.
- Send eligible items to a Kitchen Display in real time.
- Track kitchen tickets and individual item preparation states.
- Accept cash, card/digital, and manually confirmed UPI payments.
- Generate printable and emailable receipts.
- Manage customers, bookings, and order history.
- Produce operational sales reports and exports.
- Record who performed every important action.

## 3. Backend Scope

### Included

- Authentication, authorization, sessions, and password management
- Multi-tenant cafe/organization isolation
- Employee administration and role assignment
- Product, category, tax, and unit-of-measure configuration
- Payment-method configuration
- Floor and table configuration
- Coupons and automated promotions
- POS shift/session lifecycle
- Cart and order lifecycle
- Table occupancy and active-order protection
- Customer management
- Kitchen routing, tickets, items, and real-time updates
- Cash, card-reference, and manual UPI confirmation records
- Receipt creation, storage, printing payload, and email delivery
- Bookings/reservations
- Reporting, PDF/XLSX export jobs
- Activity logs, audit trail, metrics, and operational health

### Excluded from Version 1

- Web, mobile, kiosk, or KDS user interfaces
- Inventory/stock depletion
- Purchasing and suppliers
- Payroll and accounting ledger
- Real payment-gateway settlement
- Offline-first terminal synchronization
- Food-delivery marketplace integrations
- Multi-currency orders
- Loyalty points
- Microservices

## 4. Success Criteria

- Every workflow is accessible through versioned and documented APIs.
- Users cannot self-assign Admin privileges.
- Data from one cafe is never visible to another cafe.
- Only one open POS session exists per configured terminal.
- A restaurant table cannot accidentally hold conflicting active orders.
- Paid orders are immutable except through explicit refund/void workflows.
- Prices, taxes, and discounts use decimal-safe arithmetic and stored snapshots.
- Coupon redemption and automated promotion rules are deterministic and tested.
- Sending the same order to the kitchen twice does not duplicate ticket items.
- KDS clients receive authorized real-time events and can recover missed state.
- Cash-session closing totals reconcile against recorded payments.
- Critical operations are protected by database constraints and concurrency tests.
- CI prevents broken, insecure, or untested changes from merging.

## 5. Roles and Capabilities

### Owner/Admin

- Manages cafe configuration, employees, products, categories, taxes, tables, payment methods, coupons, and promotions.
- Views all sessions, orders, reports, exports, and audit logs.
- Assigns roles; public signup cannot assign a privileged role.

### Cashier/Employee

- Opens or operates an authorized POS session.
- Creates and edits draft orders.
- Assigns customers and tables.
- Applies coupons and sends items to the kitchen.
- Records payments and closes permitted sessions.

### Kitchen Staff

- Reads kitchen tickets for their cafe/station.
- Advances ticket and item stages.
- Cannot view financial reports, employee administration, or customer details beyond the minimum required.

### Auditor/Manager Capability

- Reads sessions, cash reconciliation, orders, payments, and activity logs.
- Cannot alter completed financial records.

## 6. Technology Stack

### Core

- Java 25 LTS
- Spring Boot 4.1.x
- Spring Framework 7.0.x
- Maven Wrapper
- Spring Web MVC
- Spring Data JPA and Hibernate
- Spring Security
- Spring Validation
- Spring WebSocket with STOMP for KDS live updates
- Spring Modulith for module boundaries and application events
- PostgreSQL
- Flyway
- Jackson
- MapStruct
- springdoc-openapi

### Supporting Libraries

- Nimbus JOSE + JWT or Spring Authorization Server-compatible JWT support
- ZXing for UPI QR payload image generation
- Thymeleaf as a server-side receipt template engine only
- OpenHTMLtoPDF or PDFBox for receipt/report PDFs
- Apache POI for XLSX exports
- ShedLock for scheduled-job coordination
- Bucket4j for rate limiting where required
- Resilience4j for email/object-storage integration resilience

### Testing and Quality

- JUnit 5
- AssertJ
- Mockito
- Testcontainers PostgreSQL
- REST Assured
- Spring Security Test
- ArchUnit
- Awaitility
- WireMock
- PIT mutation testing for pricing and promotion rules
- JaCoCo
- Spotless
- Checkstyle
- SpotBugs

### Infrastructure

- Docker and Docker Compose
- GitHub Actions
- MinIO locally and S3-compatible storage in production
- Mailpit locally and a transactional email provider in production
- Spring Boot Actuator
- Micrometer with Prometheus
- OpenTelemetry-compatible tracing
- Grafana dashboards

## 7. Architecture

Start as a modular monolith. A restaurant order frequently changes order lines, table state, kitchen dispatch state, payment records, session totals, receipts, and audit entries within closely related transactions. A modular monolith keeps those operations consistent while retaining clean boundaries.

### Modules

```text
com.cafepos
├── shared
│   ├── config
│   ├── error
│   ├── money
│   ├── security
│   ├── tenancy
│   ├── persistence
│   └── events
├── identity
├── organization
├── catalog
├── floor
├── customer
├── promotion
├── session
├── ordering
├── kitchen
├── payment
├── receipt
├── booking
├── reporting
├── notification
└── activitylog
```

The `session` module owns POS terminals, shift opening, cash movements, reconciliation, and closing. It must remain separate from authentication sessions in the `identity` module.

## 8. Repository Structure

```text
cafe-pos-backend/
├── src/main/java/com/cafepos/
├── src/main/resources/
│   ├── db/migration/
│   ├── templates/receipts/
│   └── application.yml
├── src/test/java/com/cafepos/
├── src/test/resources/
├── docs/
│   ├── adr/
│   ├── api/
│   ├── diagrams/
│   └── runbooks/
├── infrastructure/
├── scripts/
├── .github/workflows/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── docker-compose.yml
├── README.md
├── CONTRIBUTING.md
├── SECURITY.md
└── PLAN.md
```
