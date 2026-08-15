# Café POS Backend

<div align="center">

**Production-grade, multi-tenant restaurant Point-of-Sale platform built in Java.**

[![Java 25](https://img.shields.io/badge/Java-25-blue?logo=openjdk)](https://openjdk.java.net)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-green?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue?logo=postgresql)](https://www.postgresql.org)
[![License](https://img.shields.io/badge/License-Proprietary-red)](LICENSE)

</div>

## 📋 Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Domain Modules](#domain-modules)
- [Technology Stack](#technology-stack)
- [Development Guide](#development-guide)
- [Testing](#testing)
- [Database Migrations](#database-migrations)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Deployment](#deployment)
- [Contributing](#contributing)

---

## Overview

**Café POS** is a production-ready, backend-only restaurant Point-of-Sale system designed to support multi-tenant restaurant chains with complex operational requirements:

- **Multi-tenant architecture** with complete data isolation per organization/outlet
- **Modular monolith** following domain-driven design and Spring Modulith patterns
- **Complex business logic** for pricing, taxes, promotions, payments, reconciliation
- **Real-time operations** via REST APIs and WebSocket connections
- **Enterprise-grade observability** with structured logging and metrics
- **Audit trail** for all sensitive operations with immutable activity logs
- **Scalable design** ready for horizontal scaling and microservice extraction

### Key Capabilities

✅ **Organization & Outlet Management** – Multi-outlet support with independent configurations  
✅ **Catalog Management** – Products, categories, modifiers, taxes, and pricing  
✅ **POS Sessions** – Cashier sessions with opening/closing balances and reconciliation  
✅ **Order Management** – Dine-in, takeaway, delivery with real-time kitchen integration  
✅ **Table Management** – Floor layouts, table occupancy, reservations, and bookings  
✅ **Payment Processing** – Multiple payment methods (cash, card, UPI) with split payments  
✅ **Promotional Engine** – Coupons, automatic promotions, stacking rules  
✅ **Receipt Management** – Immutable snapshots, print formatting, email delivery  
✅ **Operational Reporting** – Sales summaries, product analytics, payment breakdowns, export jobs  
✅ **Loyalty Programs** – Customer loyalty points and rewards tracking  
✅ **Inventory Tracking** – Stock management and low-stock alerts  
✅ **Activity Logging** – Comprehensive audit trail for compliance  

---

## Getting Started

### Prerequisites

- **Java 25 SDK** (or latest LTS)
- **Docker & Docker Compose** (for PostgreSQL, Redis)
- **Maven 3.8+** (bundled as `./mvnw`)
- **Git**

### Quick Start

1. **Clone and Setup**
   ```bash
   git clone <repo-url>
   cd cafe-pos-backend
   ```

2. **Start Infrastructure**
   ```bash
   docker compose up -d
   ```
   This starts PostgreSQL (port 5432) and Redis (port 6379).

3. **Build the Project**
   ```bash
   ./mvnw clean compile
   ```

4. **Run Tests**
   ```bash
   ./mvnw test
   ```

5. **Start the Application**
   ```bash
   ./mvnw spring-boot:run
   ```
   Application runs on `http://localhost:8080`

6. **Access API Documentation**
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## Project Structure

```
café-pos-backend/
├── src/
│   ├── main/java/com/cafepos/
│   │   ├── shared/              # Cross-cutting concerns & common utilities
│   │   ├── identity/            # Auth, users, roles, permissions
│   │   ├── organization/        # Outlets, business settings, tenancy
│   │   ├── catalog/             # Products, categories, modifiers, taxes
│   │   ├── floor/               # Floors, tables, occupancy
│   │   ├── customer/            # Customer profiles & directory
│   │   ├── promotion/           # Coupons, promotions, stacking rules
│   │   ├── loyalty/             # Loyalty programs, points, rewards
│   │   ├── session/             # POS sessions, cash drawer reconciliation
│   │   ├── ordering/            # Orders, order lines, pricing engine
│   │   ├── kitchen/             # Kitchen stations, tickets, routing
│   │   ├── payment/             # Payment processing & methods
│   │   ├── receipt/             # Receipt generation & storage
│   │   ├── booking/             # Table reservations & calendar
│   │   ├── inventory/           # Stock tracking & management
│   │   ├── reporting/           # Analytics, exports, reports
│   │   └── activitylog/         # Audit trail & activity tracking
│   │
│   ├── main/resources/
│   │   ├── application.yml      # Main configuration
│   │   ├── db/migration/        # Flyway migrations
│   │   └── templates/           # Email templates
│   │
│   └── test/java/com/cafepos/   # Comprehensive test suites per module
│
├── pom.xml                      # Maven configuration
├── docker-compose.yml           # Local dev infrastructure
├── PLAN.md                      # Detailed engineering plan
└── README.md                    # This file
```

---

## Architecture

### Design Principles

**Modular Monolith** – Organized into independent domain modules that communicate through:
- Published **domain events** (asynchronous, loosely coupled)
- **Application service interfaces** (synchronous, explicit contracts)
- Shared **kernel components** (shared types, utilities, exceptions)

**Hexagonal Architecture** – Each module contains:
- **Domain layer** – Business logic, entities, value objects (no Spring)
- **Application layer** – Use cases, transaction management, orchestration
- **Port layer** – Interfaces defining contracts (repositories, external services)
- **Adapter layer** – REST controllers, JPA repositories, event subscribers

### Request Flow

```
HTTP Request
    ↓
[Controller] — Transport mapping & validation
    ↓
[Application Service] — Use case orchestration & transactions
    ↓
[Domain] — Business logic, invariant protection
    ↓
[Port Interface] — Abstract boundaries
    ↓
[Infrastructure Adapter] — Database, messaging, external APIs
```

### Core Rules

| Rule | Rationale |
|------|-----------|
| Domain code is **Spring-free** | Testability, portability, clarity of intent |
| Controllers only **map transport** | Single responsibility, thin API layer |
| Repositories are **domain-owned** | Explicit data access contracts |
| **No direct module cross-references** into persistence | Loose coupling, clear integration seams |
| All money uses **BigDecimal + Currency** | Precision, explicit intent |
| All timestamps use **Instant** | Timezone-independent, immutable |
| **Every record includes org_id + outlet_id** | Multi-tenancy built in, not bolted on |

### Module Dependencies

```
                    ┌─────────────────┐
                    │     Shared      │ (Money, exceptions, base types)
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
      Identity         Organization      Platform Modules
          │                  │              (see below)
          └──────────────────┼──────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
    Ordering            Kitchen         Payment & Receipt
    Promotion           Reporting             Session
```

---

## Domain Modules

### 🔐 Identity
**Authentication, Authorization, User Management**

- User signup, activation, login, token lifecycle
- Roles: Platform Admin, Organization Owner, Outlet Manager, Cashier, Waiter, Kitchen, Accountant, Auditor
- Fine-grained permission checks
- Security event tracking
- Failed login throttling, MFA readiness

**Files**: `src/main/java/com/cafepos/identity/`

### 🏢 Organization
**Multi-tenant Organization & Outlet Management**

- Organization creation and configuration
- Multiple outlets per organization with independent settings
- Business profile, locale, currency, time zone
- Operating hours and business day boundaries
- Feature flags and subscription tier enforcement

**Files**: `src/main/java/com/cafepos/organization/`

### 📦 Catalog
**Product, Category, and Pricing Management**

- Product hierarchies with categories and subcategories
- Modifier groups and individual modifiers (e.g., toppings with price deltas)
- Tax categories and rates per outlet
- Units of measure and conversions
- Historical pricing and archival
- SKU management and barcodes

**Files**: `src/main/java/com/cafepos/catalog/`

### 🛏️ Floor
**Table and Floor Management**

- Floor layouts with multiple floors per outlet
- Table definitions with sections and capacity
- Table occupancy tracking
- Reserved/blocked table states
- Floor layout visualization support

**Files**: `src/main/java/com/cafepos/floor/`

### 👥 Customer
**Customer Directory and Profiles**

- Customer contact management
- Phone and email verification
- Customer preferences and history
- Dining preferences and allergies
- Integration with loyalty programs

**Files**: `src/main/java/com/cafepos/customer/`

### 🎁 Promotion
**Promotional Engine & Coupon Management**

- Percentage-off and fixed-amount coupons
- Automatic rule-based promotions
- Promotion precedence and stacking rules
- Validity windows and usage limits
- Category and product-level applicability
- Minimum purchase thresholds

**Files**: `src/main/java/com/cafepos/promotion/`

### ⭐ Loyalty
**Loyalty Programs & Points**

- Points-earning rules based on order amount
- Tier-based rewards (Bronze, Silver, Gold)
- Point redemption workflows
- Expiration and carryover policies
- Integration with order pricing

**Files**: `src/main/java/com/cafepos/loyalty/`

### 💼 Session
**POS Cashier Sessions & Cash Reconciliation**

- Session lifecycle: Open → Active → Close → Reconcile
- Opening/closing balances and cash declarations
- Float management and transfers between sessions
- Cash variance calculation and tracking
- Session-level reporting and metrics
- Multi-cashier sessions with audit trail

**Files**: `src/main/java/com/cafepos/session/`

### 🛒 Ordering
**Order Management & Pricing Engine**

- Order types: Dine-in, Takeaway, Delivery
- Table and order linking with conflict prevention
- Order lines with modifiers and special instructions
- Deterministic tax calculation (inclusive/exclusive)
- Coupon and promotion application
- Service charge and rounding rules
- Order status workflow with state transitions
- Concurrent order safety via optimistic locking

**Files**: `src/main/java/com/cafepos/ordering/`

### 👨‍🍳 Kitchen
**Kitchen Display System (KDS) Integration**

- Kitchen stations (grill, fryer, assembly) with routing rules
- Ticket generation from orders with filtering by station
- Item-level status tracking (pending → ready → served)
- Real-time WebSocket updates to KDS displays
- Outbox pattern for guaranteed delivery
- Integration with receipt & payment modules

**Files**: `src/main/java/com/cafepos/kitchen/`

### 💳 Payment
**Payment Processing & Methods**

- Multiple payment method support: Cash, Card Reference, UPI
- Split payments and partial payments
- Idempotent payment processing
- Payment status workflow (Pending → Authorized → Captured → Settled)
- Refund handling with manual confirmation
- Payment reconciliation with bank feeds (future)
- PCI compliance considerations (tokenized card references)

**Files**: `src/main/java/com/cafepos/payment/`

### 🧾 Receipt
**Receipt Generation & Storage**

- Immutable receipt snapshots (JSON schema)
- Print-friendly formatting and layout
- Email delivery with retry logic
- Receipt search and reprint capabilities
- Digital archival and compliance storage
- Customizable headers/footers per outlet

**Files**: `src/main/java/com/cafepos/receipt/`

### 📅 Booking
**Table Reservations & Calendar**

- Reservation creation with time slots
- Table availability checking
- Guest list management
- Reservation status workflow
- Walk-in and confirmation workflows
- Reservation calendar views
- Integration with floor management

**Files**: `src/main/java/com/cafepos/booking/`

### 📊 Reporting
**Operational Reporting & Data Export**

- Sales summaries (gross, discounts, tax, net revenue)
- Top products by quantity and revenue
- Shift reconciliation reports
- Payment method breakdowns
- Asynchronous export jobs (CSV, XLSX)
- Metrics aggregation and caching
- Historical trend analysis

**Files**: `src/main/java/com/cafepos/reporting/`

### 📦 Inventory
**Stock Management & Tracking**

- Stock level tracking per outlet
- Low-stock alerts and thresholds
- Stock adjustments and reconciliation
- Stock consumption from orders (future)
- Stock transfer between outlets (future)
- Historical audit trail

**Files**: `src/main/java/com/cafepos/inventory/`

### 📝 Activity Log
**Audit Trail & Compliance**

- Immutable activity records for all sensitive operations
- User attribution and action details
- Before/after data snapshots
- Timestamp and outlet context
- Query-friendly indexing for audit searches
- Data retention and archival policies
- Integration with compliance reporting

**Files**: `src/main/java/com/cafepos/activitylog/`

### 🔧 Shared
**Common Kernel & Utilities**

- **Money** – `BigDecimal`-based value object with currency
- **Domain exceptions** – Base exception hierarchy
- **Base entities** – ID generation, audit fields, soft deletes
- **Multi-tenancy** – Organization/outlet context helpers
- **Pagination & sorting** – Standard query builders
- **Event publishing** – Application event interfaces
- **Date/time utilities** – Instant and ZonedDateTime helpers

**Files**: `src/main/java/com/cafepos/shared/`

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Java | 25 |
| **Framework** | Spring Boot | 3.4.2 |
| **Modular Framework** | Spring Modulith | 1.3.1 |
| **API Framework** | Spring MVC | 6.x |
| **Security** | Spring Security | 6.x |
| **Database** | PostgreSQL | 16+ |
| **Persistence** | Spring Data JPA | 3.x |
| **Validation** | Jakarta Bean Validation | 3.x |
| **Real-time** | Spring WebSocket | 6.x |
| **Async Tasks** | Spring @Async | 6.x |
| **Caching** | Redis | 7.x |
| **Data Querying** | jOOQ | 3.18+ |
| **Migrations** | Flyway | 10.x |
| **API Documentation** | Springdoc OpenAPI | 2.8.4 |
| **Mapping** | MapStruct | 1.6.3 |
| **QR Codes** | ZXing | 3.5.3 |
| **Testing** | JUnit 5 | 5.x |
| **Testing** | AssertJ | 3.x |
| **Mocking** | Mockito | 5.x |
| **Test Containers** | Testcontainers | 1.x |
| **Build** | Maven | 3.8+ |

---

## Development Guide

### Building Locally

```bash
# Full clean build
./mvnw clean verify

# Build without tests (faster)
./mvnw clean package -DskipTests

# Compile only
./mvnw clean compile

# Build and run
./mvnw spring-boot:run

# Build Docker image
docker build -t cafe-pos-backend:latest .
```

### Code Organization

Each module follows this structure:

```
module/
├── domain/              # Entities, value objects, aggregates, domain services
│   ├── {Entity}.java
│   ├── {Service}.java
│   └── {Exception}.java
├── application/         # Use cases, transaction management, DTOs
│   ├── {Service}.java
│   └── dto/
│       ├── Create{Entity}Dto.java
│       └── Update{Entity}Dto.java
├── ports/              # Interfaces abstracting external dependencies
│   ├── {Repository}.java
│   └── {Service}.java
├── adapters/           # HTTP controllers, JPA repositories, event subscribers
│   ├── api/
│   │   └── {Controller}.java
│   ├── persistence/
│   │   └── {JpaRepository}.java
│   └── events/
│       └── {EventListener}.java
└── {ModuleClass}.java  # Module marker for Spring Modulith
```

### Code Style

- **Language**: Java 25 features encouraged (records, sealed classes, pattern matching)
- **Formatting**: Spotless with Google Java Style
- **Naming**: Clear, explicit, no abbreviations (ProductName not ProdName)
- **Comments**: Domain logic only; self-documenting code preferred
- **Exceptions**: Checked exceptions only in ports; unchecked in domain

Apply formatting:
```bash
./mvnw spotless:apply
```

### Running Spring Modulith Analysis

```bash
./mvnw spring-modulith:visualize
```
This generates an Asciidoc diagram showing module dependencies.

---

## Testing

### Test Strategy

| Layer | Type | Framework | Example |
|-------|------|-----------|---------|
| **Domain** | Unit | JUnit 5 | Entity invariants, value object calculations |
| **Application** | Unit | JUnit 5 + Mockito | Use case workflows with mocked repositories |
| **Integration** | Integration | JUnit 5 + Testcontainers | Repository + database verification |
| **API** | REST | REST Assured | Controller endpoint contracts |
| **Architecture** | ArchUnit | ArchUnit | Module dependency rules enforcement |

### Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=OrderApplicationServiceTest

# Single test method
./mvnw test -Dtest=OrderApplicationServiceTest#testCreateOrder

# With coverage
./mvnw test jacoco:report
# View: target/site/jacoco/index.html

# Mutation testing (critical paths)
./mvnw test org.pitest:pitest-maven:mutationCoverage
```

### Writing Tests

**Domain Test Example:**
```java
@DisplayName("Order creation and validation")
class OrderTest {
    @Test
    void shouldValidateTotalBelowMaximum() {
        var order = new Order(...);
        assertThatThrownBy(() -> order.addLine(...))
            .isInstanceOf(OrderTotalExceededException.class);
    }
}
```

**Application Service Test Example:**
```java
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {
    @Mock OrderRepository repository;
    @InjectMocks OrderApplicationService service;
    
    @Test
    void shouldCreateOrderAndPublishEvent() {
        var result = service.createOrder(new CreateOrderDto(...));
        verify(repository).save(any(Order.class));
        assertThat(result).isNotNull();
    }
}
```

---

## Database Migrations

We use **Flyway** for forward-only migrations. Migrations are versioned and immutable.

### Migration Workflow

1. **Create migration file** in `src/main/resources/db/migration/`
   ```bash
   # Naming: V{number}__{description}.sql
   # Example: V25__add_product_modifier_groups.sql
   ```

2. **Write migration** (idempotent when possible):
   ```sql
   -- V25__add_product_modifier_groups.sql
   CREATE TABLE product_modifier_groups (
       id UUID PRIMARY KEY,
       product_id UUID NOT NULL REFERENCES products(id),
       modifier_group_id UUID NOT NULL REFERENCES modifier_groups(id),
       created_at TIMESTAMP NOT NULL
   );
   
   CREATE INDEX idx_product_modifier_groups_product_id 
       ON product_modifier_groups(product_id);
   ```

3. **Run migration** (automatic on startup, or manual):
   ```bash
   ./mvnw flyway:migrate
   ```

### Key Rules

- ✅ Migrations are **immutable** once committed
- ✅ Use **explicit column types** (no SERIAL, use UUID + DEFAULT)
- ✅ Always include **indexes** for foreign keys and frequently queried columns
- ✅ Add **constraints** (NOT NULL, UNIQUE, CHECK) upfront
- ✅ Use **explicit timestamps** with DEFAULT CURRENT_TIMESTAMP
- ✅ Include **comments** for non-obvious schemas

### Viewing Migrations

```bash
# List applied migrations
./mvnw flyway:info

# Validate pending migrations (dry-run)
./mvnw flyway:validate
```

Current migrations are in `src/main/resources/db/migration/`.

---

## API Documentation

### OpenAPI/Swagger

The API is fully documented with OpenAPI 3.1 specification via Springdoc OpenAPI.

**Access:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

### API Versioning

- Current version: **v1**
- Versioning strategy: URL-based (`/api/v1/...`)
- Deprecation: Use `@Deprecated` + `@DeprecationNotice` on endpoints

### Common Response Formats

**Success Response (200):**
```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2024-08-15T10:30:00Z",
    "version": "1.0"
  }
}
```

**Error Response (4xx/5xx):**
```json
{
  "type": "https://api.example.com/errors/validation-failed",
  "title": "Validation Failed",
  "status": 422,
  "detail": "Invalid order total",
  "instance": "/orders"
}
```
(Follows RFC 9457 Problem Details)

---

## Security

### Authentication & Authorization

- **JWT tokens** with RS256 signature
- **Spring Security** resource server configuration
- **Fine-grained permissions** checked at service layer
- **Role-based access control (RBAC)** with outlet-level assignments
- **Audit trail** for all sensitive operations

### Data Protection

- **Multi-tenancy enforcement** at repository layer
- **Row-level security** via `org_id` and `outlet_id` filters
- **Password hashing** with Argon2id
- **Sensitive field masking** in logs (credit card PAN, tokens)
- **Immutable audit records** for compliance

### Best Practices

| Practice | Implementation |
|----------|-----------------|
| Input validation | `@Valid` with Jakarta Bean Validation |
| SQL injection | Parameterized queries via Spring Data JPA |
| CSRF | CSRF token per session (for form-based APIs) |
| Rate limiting | [To be implemented] |
| API key rotation | [To be implemented] |
| Secrets management | Environment variables, Vault integration |

### Security Headers

The application sets:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

### Reporting Security Issues

⚠️ **Do not open public issues for security vulnerabilities.**

Contact: `security@cafepos.example.com`

---

## Deployment

### Docker

**Build:**
```bash
docker build -t cafe-pos-backend:0.1.0 .
```

**Run (with Docker Compose):**
```bash
docker compose up
```

**Environment Variables:**
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cafepos
SPRING_DATASOURCE_USERNAME=cafepos
SPRING_DATASOURCE_PASSWORD=secure-password
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
LOGGING_LEVEL_COM_CAFEPOS=INFO
```

### Kubernetes (Helm)

For production Kubernetes deployments, use Helm charts (to be added).

### Health Checks

```bash
# Liveness probe
GET /actuator/health/liveness

# Readiness probe (includes DB connectivity)
GET /actuator/health/readiness

# Full health with detailed info
GET /actuator/health
```

### Metrics & Monitoring

Metrics are exposed via Micrometer on `/actuator/prometheus`:

```bash
curl http://localhost:8080/actuator/prometheus
```

Key metrics:
- `cafepos_orders_created_total` – Total orders created
- `cafepos_payment_processed_total` – Total payments processed
- `cafepos_order_duration_seconds` – Order fulfillment time
- JVM & process metrics (memory, threads, GC)

---

## Contributing

### Workflow

1. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feat/short-description
   git checkout -b fix/short-description
   ```

2. **Write code** following module structure and code style
3. **Add tests** – Unit + integration for every feature
4. **Run full verification**:
   ```bash
   ./mvnw clean verify
   ```
5. **Format code**:
   ```bash
   ./mvnw spotless:apply
   ```
6. **Commit with clear message**:
   ```bash
   git commit -m "feat(module): short description
   
   - Detailed explanation of what and why
   - Reference any related issues or PRs
   - Include test coverage notes"
   ```
7. **Push and create Pull Request** with description

### Commit Message Format

```
<type>(<module>): <subject>

<body>

<footer>
```

**Types:**
- `feat` – New feature
- `fix` – Bug fix
- `test` – Test additions/changes
- `refactor` – Code refactoring without behavior change
- `docs` – Documentation
- `perf` – Performance improvement
- `chore` – Tooling, dependencies, build changes
- `pr` – PR merge commit (contains sub-commits)

**Modules:** `identity`, `organization`, `catalog`, `ordering`, `payment`, `kitchen`, `reporting`, etc.

**Example:**
```
feat(ordering): add order cancellation workflow

- Implement cancel order use case with refund handling
- Add order status transition rules to prevent invalid cancellations
- Include comprehensive unit and integration tests
- Covers ticket cancellation in kitchen module

Closes #42
```

### PR Review Checklist

- [ ] Feature/fix is scoped and well-defined
- [ ] Tests pass locally (`./mvnw verify`)
- [ ] Code follows style guide (`./mvnw spotless:check`)
- [ ] Migrations are forward-only and idempotent
- [ ] API documentation is updated (Swagger comments)
- [ ] Performance impact is considered
- [ ] Security implications reviewed (multi-tenancy, auth, data)
- [ ] Commit history is clean and logical
- [ ] PLAN.md updated if architecture changed

### Long-Term Goals

This project targets:
- **High-Quality Domain Coverage** – Every capability backed by hexagonal domain modules and comprehensive unit/integration tests
- **Robust PR & Verification Pipeline** – Automated verification (`./mvnw clean verify`), archunit compliance, and static analysis
- **Production readiness** – Security, performance, observability, fault tolerance, and multi-tenant isolation

See [PLAN.md](PLAN.md) for detailed engineering specifications, [ARCHITECTURE.md](ARCHITECTURE.md) for domain design patterns, and [DOCS.md](DOCS.md) for the complete documentation index.

---

## Troubleshooting

### Database Connection Issues

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Restart infrastructure
docker compose down && docker compose up -d

# Check logs
docker compose logs postgres
```

### Build Failures

```bash
# Clean build
./mvnw clean compile

# Check Java version
java -version  # Should be 25+

# Update Maven
./mvnw wrapper:wrapper@maven-wrapper
```

### Test Failures

```bash
# Run single failing test with verbose output
./mvnw test -Dtest=FailingTest -X

# Check if Docker containers are running (for integration tests)
docker ps
```

### Port Conflicts

If port 8080 is in use:
```bash
# Change in application.yml
server:
  port: 8081
```

---

## Quick Links

- **Full Plan**: [PLAN.md](PLAN.md)
- **API Docs**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Spring Modulith Docs**: https://spring.io/projects/spring-modulith

---

## License

Proprietary – See LICENSE file

---

**Last Updated**: August 2026  
**Status**: Active Development  
**Contributors**: Engineering Team
