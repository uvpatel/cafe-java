# Café POS & Restaurant Management Platform — 200-PR Engineering Roadmap

This roadmap outlines the 200 focused Pull Requests designed to transform `uvpatel/cafe-java` into an enterprise-ready, multi-tenant café and restaurant management platform.

## Workstream Breakdown (200 PR Target)

1. **Baseline Build & Repository Hygiene** — 8 PRs (PRs #001–#008)
2. **CI/CD & Quality Gates** — 12 PRs (PRs #009–#020)
3. **Security & Authentication** — 18 PRs (PRs #021–#038)
4. **Multi-Tenancy & Authorization** — 14 PRs (PRs #039–#052)
5. **Organization & Outlet Management** — 10 PRs (PRs #053–#062)
6. **Employee & Shift Management** — 10 PRs (PRs #063–#072)
7. **Catalog & Menu Management** — 16 PRs (PRs #073–#088)
8. **Ordering & Pricing** — 22 PRs (PRs #089–#110)
9. **Kitchen Operations** — 12 PRs (PRs #111–#122)
10. **Floor & Table Management** — 10 PRs (PRs #123–#132)
11. **Reservations & Waitlist** — 10 PRs (PRs #133–#142)
12. **Payments, Refunds & Reconciliation** — 18 PRs (PRs #143–#160)
13. **Inventory, Recipes & Suppliers** — 18 PRs (PRs #161–#178)
14. **Customers, Loyalty & Promotions** — 14 PRs (PRs #179–#192)
15. **Receipts & Invoicing** — 8 PRs (PRs #193–#200)

---

## Detailed PR Roadmap (Track 1 — Baseline & Initial Tracks)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level | Follow-up Work |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **PR-001** | Baseline | Fix Modulith boundary violations, untrack build artifacts, and establish ArchUnit rules | **In Progress** | `fix/architecture-modulith-boundaries-and-hygiene` | 5 | ApplicationModulithTest, ArchitectureBoundariesTest, full suite (468 tests) | None | Low | Setup Spotless automated formatting in CI |
| **PR-002** | CI/CD | Enhance GitHub Actions workflow with Spotless linting, JaCoCo coverage, and caching | Planned | `ci/enhance-pipeline-quality-gates` | 4 | Maven spotless:check, verify | PR-001 | Low | Add Dependency Check scanning |
| **PR-003** | Security | Enforce strict JWT token expiration, refresh rotation, and type validation | Planned | `security/jwt-token-rotation-and-validation` | 5 | JwtAuthenticationFilterTest, SecurityConfigTest | PR-001 | Medium | Implement rate limiting on login |
| **PR-004** | Multi-Tenancy | Enforce tenant-isolated repository queries and TenantContext cleanup filters | Planned | `security/tenant-context-isolation-and-filters` | 5 | TenantContextHolderTest, TenantIsolationIT | PR-003 | High | Add cross-tenant data access security tests |
| **PR-005** | Organization | Add multi-outlet configuration, tax settings, and business-day boundary rules | Planned | `feat/organization-multi-outlet-settings` | 5 | CafeServiceTest, OutletControllerTest | PR-004 | Low | Outlet activation/suspension workflow |
| **PR-006** | Employee | Implement shift clock-in/out attendance tracking and break management APIs | Planned | `feat/employee-attendance-shift-tracking` | 5 | ShiftServiceTest, EmployeeShiftControllerTest | PR-004 | Low | Employee performance summary report |
| **PR-007** | Catalog | Add product availability scheduling and modifier group options validation | Planned | `feat/catalog-product-availability-scheduling` | 5 | CatalogServiceTest, ProductPricingValidatorTest | PR-005 | Low | Bulk menu import/export endpoints |
| **PR-008** | Ordering | Implement optimistic locking and idempotent order state transition enforcement | Planned | `feat/ordering-optimistic-locking-state-transitions` | 6 | OrderStateTransitionTest, OrderConcurrencyIT | PR-007 | High | Table transfer and split order workflows |
