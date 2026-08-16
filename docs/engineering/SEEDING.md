# Database Seeding Architecture & Operational Guide

## Overview

`uvpatel/cafe-java` provides a secure, idempotent database seeding mechanism to populate realistic initial data (organizations, employees, categories, products, floor layouts, tables, terminals, coupons) for local development, demo environments, and automated testing.

---

## 1. Activation & Configuration

Seeding is **disabled by default** in production to prevent unintended data modification.

### Configuration Properties (`application.yml`)

```yaml
app:
  seed:
    enabled: ${SEED_DATA:false}
    admin-password: ${SEED_ADMIN_PASSWORD:password123}
```

* When `app.seed.enabled=false` (default/production):
  * Neither `DatabaseSeeder` nor `ApiSeedController` beans are registered in the Spring ApplicationContext.
  * No seeding overhead or endpoints exist in production.
* When `app.seed.enabled=true` (development / demo):
  * `DatabaseSeeder` runs automatically on application startup via `CommandLineRunner`.
  * `ApiSeedController` is registered and accessible at `POST /api/v1/seed/execute`.

---

## 2. API Endpoint & Security

| Endpoint | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/seed/execute` | `POST` | `OWNER`, `MANAGER`, `ADMIN` | Idempotently triggers database seeding on-demand. |

### Security Safeguards

1. **Role-Based Authorization**: Protected by `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`.
2. **Conditional Exposure**: Endpoint does not exist in the context unless `app.seed.enabled=true`.
3. **Password Hashing**: Initial passwords are encrypted using `BCryptPasswordEncoder`. Plaintext passwords are never logged or stored.

---

## 3. Idempotency Guarantees

All seeding operations check for existing records using natural business keys before insertion:
* **Cafe**: Checked via `findBySlug("central-perk")`
* **Employees**: Checked via `findByCafeIdAndEmail(cafeId, email)`
* **Product Categories**: Checked via `existsByCafeIdAndNameIgnoreCase(cafeId, name)`
* **Products**: Checked via `existsByCafeIdAndSku(cafeId, sku)`
* **Floor & Dining Tables**: Checked via table number per floor
* **Terminals**: Checked via terminal code
* **Coupons**: Checked via coupon code per cafe

Executing seeding multiple times produces no duplicate records or state corruption.
