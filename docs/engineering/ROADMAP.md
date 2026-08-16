# Engineering Roadmap (200 Pull Requests)

This document tracks the target 200 pull requests for transforming `uvpatel/cafe-java` into a production-grade multi-tenant Café POS backend.

---

## Track 1: Baseline Build & Repository Hygiene (PRs 1–8)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level | Follow-Up Work |
|---|---|---|---|---|---|---|---|---|---|
| PR-001 | Baseline | Baseline build hygiene, Spotless formatter, CI workflow & engineering roadmap setup | OPEN | `chore/baseline-build-hygiene-and-ci-workflow` | 5 | Spotless, Surefire, Failsafe | None | LOW | Standardize code quality gates |
| PR-002 | Baseline | Enforce PMD & SpotBugs static code analysis quality gates in Maven build | PLANNED | `chore/pmd-spotbugs-quality-gates` | TBD | Static Analysis | PR-001 | LOW | Architecture rule checks |
| PR-003 | Baseline | Spring Modulith ArchUnit architectural boundary validation test suite | PLANNED | `test/modulith-architectural-boundary-tests` | TBD | ArchUnit | PR-001 | MEDIUM | Module refactoring |
| PR-004 | Baseline | Docker Compose local development stack with PostgreSQL and Adminer | PLANNED | `chore/docker-compose-local-dev-stack` | TBD | Compose Healthcheck | PR-001 | LOW | Multi-stage Docker optimization |
| PR-005 | Baseline | Centralized application configuration properties validation and profile segregation | PLANNED | `refactor/centralized-configuration-properties` | TBD | Property Validation | PR-001 | LOW | Security secret handling |
| PR-006 | Baseline | Global RFC 9457 Problem Details error handling and uniform exception hierarchy | PLANNED | `feat/rfc9457-problem-details-exception-handler` | TBD | ControllerAdvice Tests | PR-001 | MEDIUM | API contract alignment |
| PR-007 | Baseline | OpenAPI 3.1 Swagger UI documentation generator and annotations baseline | PLANNED | `docs/openapi-swagger-documentation-baseline` | TBD | OpenAPI Generation | PR-001 | LOW | Client SDK generation |
| PR-008 | Baseline | Seed data runner and Flyway baseline migration verification test suite | PLANNED | `test/flyway-migration-verification-suite` | TBD | Flyway Migration Tests | PR-001 | MEDIUM | Tenant database onboarding |

---

## Track 2: CI/CD & Quality Gates (PRs 9–20)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-009 | CI/CD | CodeQL static analysis and security scanning GitHub Actions workflow | PLANNED | `ci/codeql-security-analysis-workflow` | TBD | CodeQL Scan | PR-001 | LOW |
| PR-010 | CI/CD | Dependabot dependency version updates and security patch automation | PLANNED | `ci/dependabot-configuration` | TBD | Dependency Audit | PR-001 | LOW |
| PR-011 | CI/CD | JaCoCo coverage threshold enforcement (85%+ line coverage gate) | PLANNED | `ci/jacoco-coverage-threshold-gate` | TBD | Coverage Check | PR-001 | LOW |
| PR-012 | CI/CD | Automated mutation testing with PIT for financial and tax calculations | PLANNED | `test/pitest-mutation-testing-financials` | TBD | PIT Mutation | PR-001 | MEDIUM |
| PR-013 | CI/CD | GitHub Actions Docker container image build and GHCR push pipeline | PLANNED | `ci/docker-build-push-ghcr-pipeline` | TBD | Docker Build | PR-004 | LOW |
| PR-014 | CI/CD | Semantic release automated changelog generation and tagging workflow | PLANNED | `ci/semantic-release-tagging-workflow` | TBD | Release Tagging | PR-001 | LOW |
| PR-015 | CI/CD | PR title linting and commit message validation GitHub Action | PLANNED | `ci/conventional-commits-pr-linting` | TBD | Lint Check | PR-001 | LOW |
| PR-016 | CI/CD | Automated API diff contract breaking change detector pipeline | PLANNED | `ci/api-contract-breaking-change-detector` | TBD | OpenAPI Diff | PR-007 | LOW |
| PR-017 | CI/CD | Testcontainers PostgreSQL integration test workflow on GitHub Actions | PLANNED | `ci/testcontainers-postgres-github-actions` | TBD | Integration Tests | PR-001 | MEDIUM |
| PR-018 | CI/CD | Automatic dependency vulnerability audit using OWASP Dependency-Check | PLANNED | `ci/owasp-dependency-check-workflow` | TBD | Vulnerability Scan | PR-001 | LOW |
| PR-019 | CI/CD | Continuous bench-marking and JVM memory consumption diagnostic pipeline | PLANNED | `ci/jvm-performance-benchmark-pipeline` | TBD | Benchmark Test | PR-001 | LOW |
| PR-020 | CI/CD | Gitleaks secret scanning and pre-commit security hook configuration | PLANNED | `ci/gitleaks-secret-scanning-hooks` | TBD | Secret Scan | PR-001 | LOW |

---

## Track 3: Security & Authentication (PRs 21–38)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-021 | Security | Argon2id secure password hashing provider integration | PLANNED | `security/argon2id-password-encoder` | TBD | Password Encoding | PR-001 | HIGH |
| PR-022 | Security | JWT access and refresh token pair generation with RSA key signing | PLANNED | `security/jwt-rsa-keypair-token-provider` | TBD | Token Tests | PR-001 | HIGH |
| PR-023 | Security | Refresh token rotation, sliding expiration, and reuse detection | PLANNED | `security/refresh-token-rotation-and-reuse-detection` | TBD | Auth Tests | PR-022 | HIGH |
| PR-024 | Security | Refresh token revocation blacklist with Redis/in-memory store | PLANNED | `security/token-revocation-blacklist` | TBD | Revocation Tests | PR-023 | HIGH |
| PR-025 | Security | Rate limiting filter for login and authentication endpoints | PLANNED | `security/login-rate-limiting-filter` | TBD | Rate Limit Tests | PR-022 | MEDIUM |
| PR-026 | Security | Account lockout policy after consecutive failed login attempts | PLANNED | `security/account-lockout-policy` | TBD | Lockout Tests | PR-025 | MEDIUM |
| PR-027 | Security | Role-Based Access Control (RBAC) method-level authorization `@PreAuthorize` | PLANNED | `security/rbac-method-security-annotations` | TBD | Security Tests | PR-022 | HIGH |
| PR-028 | Security | Granular permission-based authorization engine (`PermissionEvaluator`) | PLANNED | `security/permission-evaluator-engine` | TBD | Permission Tests | PR-027 | HIGH |
| PR-029 | Security | Security response headers (HSTS, CSP, X-Frame-Options, X-Content-Type) | PLANNED | `security/security-headers-filter` | TBD | Header Tests | PR-001 | LOW |
| PR-030 | Security | Secure CORS configuration policy and allowed origin whitelist | PLANNED | `security/cors-policy-configuration` | TBD | CORS Tests | PR-001 | LOW |
| PR-031 | Security | Request body size limiter filter to prevent Denial of Service | PLANNED | `security/request-size-limiter-filter` | TBD | Size Limit Tests | PR-001 | LOW |
| PR-032 | Security | Output encoding and HTML/XSS sanitization helper library | PLANNED | `security/xss-output-sanitizer` | TBD | Sanitizer Tests | PR-001 | LOW |
| PR-033 | Security | IDOR protection interceptor and resource ownership validator | PLANNED | `security/idor-resource-ownership-validator` | TBD | Security Tests | PR-028 | HIGH |
| PR-034 | Security | Sensitive log masking for passwords, JWT tokens, and PII | PLANNED | `security/log-sanitizer-pattern-layout` | TBD | Log Tests | PR-001 | MEDIUM |
| PR-035 | Security | User password reset workflow with time-bound single-use token | PLANNED | `security/password-reset-token-workflow` | TBD | Auth Tests | PR-021 | HIGH |
| PR-036 | Security | Suspicious login detector (IP/Location anomaly tracking) | PLANNED | `security/suspicious-login-anomaly-detector` | TBD | Anomaly Tests | PR-022 | MEDIUM |
| PR-037 | Security | User invitation token generator and activation workflow | PLANNED | `security/user-invitation-activation-workflow` | TBD | Auth Tests | PR-022 | MEDIUM |
| PR-038 | Security | Security vulnerability regression test suite for identity endpoints | PLANNED | `test/security-vulnerability-regression-suite` | TBD | Security Tests | PR-037 | HIGH |

---

## Track 4: Multi-Tenancy & Authorization (PRs 39–52)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-039 | Multi-Tenancy | Verified JWT claim tenant context extractor and thread context | PLANNED | `security/jwt-tenant-context-extractor` | TBD | Context Tests | PR-022 | CRITICAL |
| PR-040 | Multi-Tenancy | Automatic tenant thread local context cleanup filter | PLANNED | `security/tenant-context-cleanup-filter` | TBD | Filter Tests | PR-039 | CRITICAL |
| PR-041 | Multi-Tenancy | Multi-tenant JPA entity listener and tenant ID assignment | PLANNED | `security/jpa-tenant-entity-listener` | TBD | Entity Tests | PR-039 | CRITICAL |
| PR-042 | Multi-Tenancy | Hibernate filter integration for automatic query tenant scoping | PLANNED | `security/hibernate-tenant-filter-integration` | TBD | Query Tests | PR-041 | CRITICAL |
| PR-043 | Multi-Tenancy | Tenant-aware database unique constraint audit and migration | PLANNED | `security/tenant-aware-database-unique-constraints` | TBD | DB Constraint Tests | PR-042 | CRITICAL |
| PR-044 | Multi-Tenancy | Async task executor tenant context propagator decorator | PLANNED | `security/async-tenant-context-propagator` | TBD | Async Tests | PR-039 | HIGH |
| PR-045 | Multi-Tenancy | Scheduled job multi-tenant context execution wrapper | PLANNED | `security/scheduled-task-tenant-executor` | TBD | Scheduler Tests | PR-044 | HIGH |
| PR-046 | Multi-Tenancy | Multi-tenant cache key prefix decorator to eliminate cross-tenant leakage | PLANNED | `security/multi-tenant-cache-key-decorator` | TBD | Cache Tests | PR-039 | CRITICAL |
| PR-047 | Multi-Tenancy | Outlet switcher service with verified permissions check | PLANNED | `security/outlet-context-switcher-service` | TBD | Context Tests | PR-039 | HIGH |
| PR-048 | Multi-Tenancy | Audit log event tenant context metadata decorator | PLANNED | `security/audit-tenant-context-metadata` | TBD | Audit Tests | PR-039 | MEDIUM |
| PR-049 | Multi-Tenancy | Cross-tenant data isolation integration test suite (Tenant A vs B) | PLANNED | `test/cross-tenant-isolation-integration-suite` | TBD | Multi-Tenant IT | PR-042 | CRITICAL |
| PR-050 | Multi-Tenancy | Organization subscription tier limits enforcement engine | PLANNED | `feat/organization-subscription-limit-enforcer` | TBD | Limit Tests | PR-039 | MEDIUM |
| PR-051 | Multi-Tenancy | Multi-tenant WebSocket channel authorization interceptor | PLANNED | `security/multitenant-websocket-authorization-interceptor` | TBD | WS Tests | PR-039 | HIGH |
| PR-052 | Multi-Tenancy | Multi-tenant dynamic database connection routing provider | PLANNED | `architecture/dynamic-tenant-datasource-router` | TBD | Datasource Tests | PR-042 | HIGH |

---

## Track 5: Organization & Outlet Management (PRs 53–62)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-053 | Organization | Café organization self-service onboarding API | PLANNED | `feat/organization-self-service-onboarding` | TBD | Onboarding Tests | PR-039 | MEDIUM |
| PR-054 | Organization | Multi-outlet CRUD application service and REST endpoints | PLANNED | `feat/multi-outlet-crud-service` | TBD | Outlet Tests | PR-053 | MEDIUM |
| PR-055 | Organization | Outlet tax configuration aggregate (Inclusive vs Exclusive tax) | PLANNED | `feat/outlet-tax-configuration-aggregate` | TBD | Tax Config Tests | PR-054 | HIGH |
| PR-056 | Organization | Currency and outlet time zone setting manager | PLANNED | `feat/outlet-currency-timezone-manager` | TBD | Timezone Tests | PR-054 | HIGH |
| PR-057 | Organization | Operating hours schedule and weekly business day boundary service | PLANNED | `feat/operating-hours-business-day-boundary-service` | TBD | Schedule Tests | PR-056 | HIGH |
| PR-058 | Organization | Outlet receipt preferences, header/footer text, and logo branding | PLANNED | `feat/outlet-receipt-branding-preferences` | TBD | Branding Tests | PR-054 | LOW |
| PR-059 | Organization | Outlet activation, soft suspension, and emergency maintenance toggle | PLANNED | `feat/outlet-activation-suspension-workflow` | TBD | Outlet Tests | PR-054 | MEDIUM |
| PR-060 | Organization | Feature flags engine per outlet and organization level | PLANNED | `feat/outlet-feature-flags-engine` | TBD | Feature Flag Tests | PR-054 | MEDIUM |
| PR-061 | Organization | Bulk outlet settings import and synchronization API | PLANNED | `feat/bulk-outlet-settings-sync-api` | TBD | Sync Tests | PR-054 | LOW |
| PR-062 | Organization | Organization and outlet operational metrics dashboard endpoint | PLANNED | `feat/organization-outlet-metrics-dashboard` | TBD | Dashboard Tests | PR-054 | LOW |

---

## Track 6: Employee & Shift Management (PRs 63–72)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-063 | Employee | Employee profile management application service and REST API | PLANNED | `feat/employee-profile-management-api` | TBD | Profile Tests | PR-027 | MEDIUM |
| PR-064 | Employee | Employee shift scheduling domain aggregate and planner API | PLANNED | `feat/employee-shift-scheduling-domain` | TBD | Shift Tests | PR-063 | MEDIUM |
| PR-065 | Employee | Shift assignment conflict detector (Overlapping shifts check) | PLANNED | `feat/shift-assignment-conflict-detector` | TBD | Conflict Tests | PR-064 | MEDIUM |
| PR-066 | Employee | Clock-in and clock-out time tracking domain service | PLANNED | `feat/employee-clockin-clockout-service` | TBD | Attendance Tests | PR-064 | HIGH |
| PR-067 | Employee | Shift break duration and meal tracking domain model | PLANNED | `feat/employee-shift-break-tracking` | TBD | Break Tests | PR-066 | LOW |
| PR-068 | Employee | Cashier POS float authorization and shift handover verification | PLANNED | `feat/cashier-shift-float-handover-service` | TBD | Float Tests | PR-066 | HIGH |
| PR-069 | Employee | Employee attendance summary and timesheet report generator | PLANNED | `feat/employee-attendance-timesheet-report` | TBD | Report Tests | PR-066 | LOW |
| PR-070 | Employee | Employee login history and session tracking ledger | PLANNED | `feat/employee-login-history-ledger` | TBD | History Tests | PR-022 | LOW |
| PR-071 | Employee | Suspicious activity and pin override audit log tracking for staff | PLANNED | `feat/employee-pin-override-audit-tracker` | TBD | Audit Tests | PR-070 | MEDIUM |
| PR-072 | Employee | Staff performance summary (Orders processed, tips earned, prep time) | PLANNED | `feat/employee-performance-summary-api` | TBD | Summary Tests | PR-066 | LOW |

---

## Track 7: Catalog & Menu Management (PRs 73–88)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-073 | Catalog | Category hierarchy aggregate (Parent/Child category relationships) | PLANNED | `feat/category-hierarchy-nested-tree` | TBD | Category Tests | PR-001 | MEDIUM |
| PR-074 | Catalog | Category display order reordering API with drag-and-drop support | PLANNED | `feat/category-display-reordering-api` | TBD | Reorder Tests | PR-073 | LOW |
| PR-075 | Catalog | Product aggregate root enhancements with tax category linking | PLANNED | `feat/product-aggregate-tax-category-link` | TBD | Product Tests | PR-073 | MEDIUM |
| PR-076 | Catalog | Product variants aggregate (Size, Flavor, Roast level) | PLANNED | `feat/product-variants-aggregate` | TBD | Variant Tests | PR-075 | HIGH |
| PR-077 | Catalog | Modifier group aggregate root (Single selection vs Multi selection) | PLANNED | `feat/modifier-group-aggregate-root` | TBD | Modifier Tests | PR-075 | HIGH |
| PR-078 | Catalog | Modifier group min/max selection bounds validator | PLANNED | `feat/modifier-group-bounds-validator` | TBD | Validation Tests | PR-077 | HIGH |
| PR-079 | Catalog | Product to modifier group linking application service and REST endpoints | PLANNED | `feat/product-modifier-group-linking` | TBD | Linking Tests | PR-077 | MEDIUM |
| PR-080 | Catalog | Combo meal aggregate (Bundle items with choice sets and discounts) | PLANNED | `feat/combo-meal-bundle-aggregate` | TBD | Combo Tests | PR-076 | HIGH |
| PR-081 | Catalog | Outlet-specific product pricing override domain service | PLANNED | `feat/outlet-product-pricing-override` | TBD | Pricing Tests | PR-075 | HIGH |
| PR-082 | Catalog | Time-based menu availability schedule (Breakfast, Lunch, Late-night) | PLANNED | `feat/time-based-menu-availability-schedule` | TBD | Schedule Tests | PR-075 | HIGH |
| PR-083 | Catalog | Dietary information tags (Vegan, Gluten-free, Keto, Nut-free) | PLANNED | `feat/product-dietary-information-tags` | TBD | Tag Tests | PR-075 | LOW |
| PR-084 | Catalog | Allergen warnings matrix for products and modifier options | PLANNED | `feat/product-allergen-matrix` | TBD | Allergen Tests | PR-075 | MEDIUM |
| PR-085 | Catalog | Product image storage URL metadata and thumbnail generator | PLANNED | `feat/product-image-metadata-service` | TBD | Image Tests | PR-075 | LOW |
| PR-086 | Catalog | Bulk catalog import and export runner (CSV/JSON schema) | PLANNED | `feat/bulk-catalog-import-export-runner` | TBD | Import Tests | PR-075 | MEDIUM |
| PR-087 | Catalog | Menu versioning and snapshot publishing workflow | PLANNED | `feat/menu-versioning-snapshot-publishing` | TBD | Versioning Tests | PR-075 | HIGH |
| PR-088 | Catalog | Product archiving and safe soft-deletion integrity guard | PLANNED | `feat/product-archiving-integrity-guard` | TBD | Archiving Tests | PR-075 | MEDIUM |

---

## Track 8: Ordering & Pricing (PRs 89–110)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-089 | Ordering | Order entity state machine (DRAFT, SUBMITTED, IN_KITCHEN, READY, SERVED, PAID, CANCELLED) | PLANNED | `feat/order-state-machine` | TBD | State Machine Tests | PR-001 | CRITICAL |
| PR-090 | Ordering | Dine-in, Takeaway, and Delivery order type handling strategy | PLANNED | `feat/order-type-handling-strategy` | TBD | Strategy Tests | PR-089 | HIGH |
| PR-091 | Ordering | Order line item calculation with selected modifiers and extra pricing | PLANNED | `feat/order-line-item-modifier-pricing` | TBD | Line Item Tests | PR-089 | CRITICAL |
| PR-092 | Ordering | Order item special kitchen notes and preparation instructions | PLANNED | `feat/order-item-special-instructions` | TBD | Instruction Tests | PR-091 | LOW |
| PR-093 | Ordering | Table assignment and guest count tracking for dine-in orders | PLANNED | `feat/order-table-assignment-guest-count` | TBD | Table Order Tests | PR-089 | HIGH |
| PR-094 | Ordering | Hold and resume order workflow for guest cart pausing | PLANNED | `feat/hold-resume-order-workflow` | TBD | Hold Tests | PR-089 | MEDIUM |
| PR-095 | Ordering | Split order by line items or guest seat count service | PLANNED | `feat/split-order-line-items-service` | TBD | Split Tests | PR-091 | CRITICAL |
| PR-096 | Ordering | Merge orders application service with price re-calculation | PLANNED | `feat/merge-orders-application-service` | TBD | Merge Tests | PR-095 | CRITICAL |
| PR-097 | Ordering | Transfer active order to another dining table workflow | PLANNED | `feat/transfer-order-table-workflow` | TBD | Transfer Tests | PR-093 | HIGH |
| PR-098 | Ordering | Order cancellation workflow with cancellation reason audit | PLANNED | `feat/order-cancellation-audit-workflow` | TBD | Cancellation Tests | PR-089 | HIGH |
| PR-099 | Ordering | Item void workflow requiring manager PIN approval | PLANNED | `feat/item-void-manager-approval` | TBD | Void Tests | PR-098 | HIGH |
| PR-100 | Ordering | Tax calculation engine (Itemized inclusive/exclusive breakdown) | PLANNED | `feat/tax-calculation-engine` | TBD | Tax Engine Tests | PR-091 | CRITICAL |
| PR-101 | Ordering | Line-item and order-level discount calculator | PLANNED | `feat/order-discount-calculator` | TBD | Discount Tests | PR-091 | CRITICAL |
| PR-102 | Ordering | Service charge calculation strategy per order type | PLANNED | `feat/service-charge-calculation-strategy` | TBD | Charge Tests | PR-091 | HIGH |
| PR-103 | Ordering | Total rounding calculator (Half-up, Cash rounding rules) | PLANNED | `feat/total-cash-rounding-calculator` | TBD | Rounding Tests | PR-091 | HIGH |
| PR-104 | Ordering | Tip allocation engine per order and staff member | PLANNED | `feat/tip-allocation-engine` | TBD | Tip Tests | PR-091 | MEDIUM |
| PR-105 | Ordering | Order history query API with filtering, sorting, and search | PLANNED | `feat/order-history-query-api` | TBD | History Tests | PR-089 | LOW |
| PR-106 | Ordering | Optimistic locking `@Version` conflict prevention on order aggregate | PLANNED | `feat/optimistic-locking-order-aggregate` | TBD | Concurrency Tests | PR-089 | CRITICAL |
| PR-107 | Ordering | Idempotent order submission via client idempotency headers | PLANNED | `feat/idempotent-order-submission-interceptor` | TBD | Idempotency Tests | PR-089 | CRITICAL |
| PR-108 | Ordering | Order price simulation endpoint for cart preview | PLANNED | `feat/order-price-simulation-endpoint` | TBD | Simulation Tests | PR-101 | MEDIUM |
| PR-109 | Ordering | Dine-in course sequencing (Appetizers, Mains, Desserts) | PLANNED | `feat/dine-in-course-sequencing` | TBD | Course Tests | PR-089 | MEDIUM |
| PR-110 | Ordering | Concurrency stress test suite for simultaneous order updates | PLANNED | `test/order-concurrency-stress-suite` | TBD | Concurrency Tests | PR-106 | CRITICAL |

---

## Track 9: Kitchen Operations (PRs 111–122)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-111 | Kitchen | Kitchen station aggregate root (Hot Kitchen, Bar, Cold Station) | PLANNED | `feat/kitchen-station-aggregate` | TBD | Station Tests | PR-001 | MEDIUM |
| PR-112 | Kitchen | Kitchen routing rules engine based on item categories | PLANNED | `feat/kitchen-routing-rules-engine` | TBD | Routing Tests | PR-111 | HIGH |
| PR-113 | Kitchen | Kitchen ticket aggregate root and ticket item statuses | PLANNED | `feat/kitchen-ticket-aggregate-root` | TBD | Ticket Tests | PR-112 | CRITICAL |
| PR-114 | Kitchen | Kitchen ticket preparation priority queue ordering algorithm | PLANNED | `feat/kitchen-ticket-priority-queue` | TBD | Queue Tests | PR-113 | HIGH |
| PR-115 | Kitchen | Estimated preparation time (ETA) calculation service | PLANNED | `feat/estimated-prep-time-calculation` | TBD | ETA Tests | PR-113 | MEDIUM |
| PR-116 | Kitchen | Delayed order SLA breach alert generator | PLANNED | `feat/delayed-order-sla-breach-alert` | TBD | SLA Tests | PR-115 | MEDIUM |
| PR-117 | Kitchen | Ticket item re-fire workflow for kitchen mistakes | PLANNED | `feat/ticket-item-refire-workflow` | TBD | Re-fire Tests | PR-113 | HIGH |
| PR-118 | Kitchen | Cancelled item kitchen alert and printing notification | PLANNED | `feat/cancelled-item-kitchen-notification` | TBD | Cancel Tests | PR-113 | MEDIUM |
| PR-119 | Kitchen | Kitchen performance metrics service (Avg prep time, throughput) | PLANNED | `feat/kitchen-performance-metrics-service` | TBD | Metrics Tests | PR-113 | LOW |
| PR-120 | Kitchen | STOMP WebSocket real-time kitchen ticket updates topic | PLANNED | `feat/stomp-websocket-kitchen-ticket-topic` | TBD | WebSocket Tests | PR-113 | CRITICAL |
| PR-121 | Kitchen | Transactional outbox event publisher for reliable kitchen delivery | PLANNED | `feat/transactional-outbox-kitchen-publisher` | TBD | Outbox Tests | PR-120 | CRITICAL |
| PR-122 | Kitchen | Kitchen display system (KDS) integration test suite | PLANNED | `test/kitchen-display-system-integration-suite` | TBD | KDS IT | PR-121 | HIGH |

---

## Track 10: Floor & Table Management (PRs 123–132)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-123 | Floor | Floor plan aggregate root and multi-floor support | PLANNED | `feat/floor-plan-aggregate-root` | TBD | Floor Tests | PR-001 | MEDIUM |
| PR-124 | Floor | Dining table layout metadata (X/Y coordinates, shape) | PLANNED | `feat/dining-table-layout-metadata` | TBD | Table Tests | PR-123 | LOW |
| PR-125 | Floor | Dining table seating capacity and min/max guest count bounds | PLANNED | `feat/table-seating-capacity-bounds` | TBD | Capacity Tests | PR-124 | LOW |
| PR-126 | Floor | Table status state machine (AVAILABLE, OCCUPIED, RESERVED, CLEANING, BLOCKED) | PLANNED | `feat/table-status-state-machine` | TBD | Status Tests | PR-124 | HIGH |
| PR-127 | Floor | Real-time table occupancy tracker and seating timer | PLANNED | `feat/table-occupancy-seating-timer` | TBD | Occupancy Tests | PR-126 | MEDIUM |
| PR-128 | Floor | Combining adjacent tables domain service | PLANNED | `feat/combine-adjacent-tables-service` | TBD | Combine Tests | PR-126 | HIGH |
| PR-129 | Floor | Separating combined tables application service | PLANNED | `feat/separate-combined-tables-service` | TBD | Separate Tests | PR-128 | HIGH |
| PR-130 | Floor | Table cleaning workflow and busboy notification | PLANNED | `feat/table-cleaning-workflow` | TBD | Cleaning Tests | PR-126 | LOW |
| PR-131 | Floor | Table double-booking and seating conflict prevention guard | PLANNED | `feat/table-seating-conflict-guard` | TBD | Conflict Tests | PR-126 | CRITICAL |
| PR-132 | Floor | Floor utilization and turnover rate reporting API | PLANNED | `feat/floor-utilization-turnover-report` | TBD | Report Tests | PR-127 | LOW |

---

## Track 11: Reservations & Waitlist (PRs 133–142)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-133 | Reservations | Reservation aggregate root and lifecycle management | PLANNED | `feat/reservation-aggregate-root` | TBD | Reservation Tests | PR-126 | HIGH |
| PR-134 | Reservations | Real-time reservation availability search engine | PLANNED | `feat/reservation-availability-search-engine` | TBD | Search Tests | PR-133 | HIGH |
| PR-135 | Reservations | Waitlist queue management aggregate for walk-in guests | PLANNED | `feat/waitlist-queue-management` | TBD | Waitlist Tests | PR-133 | MEDIUM |
| PR-136 | Reservations | Reservation confirmation and SMS/Email notification trigger | PLANNED | `feat/reservation-confirmation-notification` | TBD | Notification Tests | PR-133 | LOW |
| PR-137 | Reservations | Reservation cancellation and automated slot release | PLANNED | `feat/reservation-cancellation-slot-release` | TBD | Cancellation Tests | PR-133 | MEDIUM |
| PR-138 | Reservations | Automated no-show status detection scheduled task | PLANNED | `feat/reservation-noshow-detection-task` | TBD | Task Tests | PR-133 | MEDIUM |
| PR-139 | Reservations | Guest preference notes and VIP customer tagging | PLANNED | `feat/reservation-guest-preferences` | TBD | Preference Tests | PR-133 | LOW |
| PR-140 | Reservations | Reservation deposit requirement and payment link generation | PLANNED | `feat/reservation-deposit-payment-link` | TBD | Deposit Tests | PR-133 | HIGH |
| PR-141 | Reservations | Maximum capacity rules and time-slot throttling engine | PLANNED | `feat/reservation-capacity-throttling-engine` | TBD | Throttling Tests | PR-134 | HIGH |
| PR-142 | Reservations | Seamless reservation-to-order seating conversion API | PLANNED | `feat/reservation-to-order-conversion-api` | TBD | Seating Tests | PR-133 | HIGH |

---

## Track 12: Payments, Refunds & Reconciliation (PRs 143–160)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-143 | Payments | Cash payment tender amount, change calculation, and currency validation | PLANNED | `feat/cash-payment-tender-change-validation` | TBD | Cash Tests | PR-089 | CRITICAL |
| PR-144 | Payments | External payment gateway abstraction port (`PaymentGatewayPort`) | PLANNED | `feat/payment-gateway-abstraction-port` | TBD | Port Tests | PR-143 | CRITICAL |
| PR-145 | Payments | UPI QR code generation provider using ZXing library | PLANNED | `feat/upi-qr-code-generation-provider` | TBD | QR Tests | PR-144 | MEDIUM |
| PR-146 | Payments | Card transaction reference and terminal auth code recording | PLANNED | `feat/card-transaction-reference-recorder` | TBD | Card Tests | PR-144 | HIGH |
| PR-147 | Payments | Split payment engine (Mixed payment methods for single order) | PLANNED | `feat/split-payment-mixed-methods-engine` | TBD | Split Payment Tests | PR-143 | CRITICAL |
| PR-148 | Payments | Partial payment workflow and remaining balance ledger | PLANNED | `feat/partial-payment-balance-ledger` | TBD | Partial Tests | PR-147 | CRITICAL |
| PR-149 | Payments | Order refund aggregate root and refund reason recording | PLANNED | `feat/order-refund-aggregate-root` | TBD | Refund Tests | PR-143 | CRITICAL |
| PR-150 | Payments | Partial refund workflow with item-level allocation | PLANNED | `feat/partial-refund-item-allocation` | TBD | Partial Refund Tests | PR-149 | CRITICAL |
| PR-151 | Payments | Idempotent payment processing using database unique keys | PLANNED | `feat/idempotent-payment-processing-guard` | TBD | Idempotency Tests | PR-143 | CRITICAL |
| PR-152 | Payments | Payment webhook endpoint with signature verification | PLANNED | `feat/payment-webhook-signature-verifier` | TBD | Webhook Tests | PR-144 | CRITICAL |
| PR-153 | Payments | Retry-safe payment webhook consumer with outbox processing | PLANNED | `feat/retry-safe-payment-webhook-consumer` | TBD | Consumer Tests | PR-152 | CRITICAL |
| PR-154 | Payments | Payment reconciliation ledger and mismatch audit finder | PLANNED | `feat/payment-reconciliation-mismatch-audit` | TBD | Audit Tests | PR-143 | HIGH |
| PR-155 | Payments | End-of-day cashier payment settlement report generator | PLANNED | `feat/cashier-end-of-day-settlement-report` | TBD | Settlement Tests | PR-154 | HIGH |
| PR-156 | Payments | Blind cash drop and float variance audit ledger for POS sessions | PLANNED | `feat/blind-cash-drop-variance-ledger` | TBD | Audit Tests | PR-143 | HIGH |
| PR-157 | Payments | Payment refund limit validator against original payment amounts | PLANNED | `feat/refund-limit-validation-guard` | TBD | Limit Tests | PR-149 | CRITICAL |
| PR-158 | Payments | Mock Payment Gateway provider for unit and sandbox testing | PLANNED | `test/mock-payment-gateway-sandbox-provider` | TBD | Mock Gateway Tests | PR-144 | LOW |
| PR-159 | Payments | Wallet payment provider adapter (Apple Pay / Google Pay token) | PLANNED | `feat/wallet-payment-provider-adapter` | TBD | Wallet Tests | PR-144 | HIGH |
| PR-160 | Payments | Payment concurrency stress and race condition integration tests | PLANNED | `test/payment-concurrency-stress-suite` | TBD | Concurrency Tests | PR-151 | CRITICAL |

---

## Track 13: Inventory, Recipes & Suppliers (PRs 161–178)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-161 | Inventory | Ingredient aggregate root and Unit of Measurement (UOM) definition | PLANNED | `feat/ingredient-aggregate-uom-definition` | TBD | Ingredient Tests | PR-001 | MEDIUM |
| PR-162 | Inventory | Unit of measurement conversion engine (Grams to Kg, ml to Liter) | PLANNED | `feat/uom-conversion-engine` | TBD | Conversion Tests | PR-161 | HIGH |
| PR-163 | Inventory | Recipe aggregate root (Product to ingredient mapping with quantities) | PLANNED | `feat/recipe-aggregate-ingredient-mapping` | TBD | Recipe Tests | PR-161 | CRITICAL |
| PR-164 | Inventory | Stock level tracker per outlet with real-time stock balance | PLANNED | `feat/outlet-stock-level-tracker` | TBD | Stock Tests | PR-161 | HIGH |
| PR-165 | Inventory | Stock movement immutable audit ledger (RECEIPT, DEDUCTION, WASTAGE, ADJUSTMENT) | PLANNED | `feat/stock-movement-immutable-ledger` | TBD | Ledger Tests | PR-164 | CRITICAL |
| PR-166 | Inventory | Goods Receipt Note (GRN) workflow for receiving inventory shipments | PLANNED | `feat/goods-receipt-note-grn-workflow` | TBD | GRN Tests | PR-165 | HIGH |
| PR-167 | Inventory | Inventory wastage and spoilage log entry service | PLANNED | `feat/inventory-wastage-spoilage-logging` | TBD | Wastage Tests | PR-165 | MEDIUM |
| PR-168 | Inventory | Manual stock count adjustment workflow with approval PIN | PLANNED | `feat/manual-stock-adjustment-workflow` | TBD | Adjustment Tests | PR-165 | HIGH |
| PR-169 | Inventory | Supplier management CRUD application service | PLANNED | `feat/supplier-management-crud-service` | TBD | Supplier Tests | PR-161 | LOW |
| PR-170 | Inventory | Purchase order lifecycle management (DRAFT, ISSUED, RECEIVED, CANCELLED) | PLANNED | `feat/purchase-order-lifecycle-management` | TBD | PO Tests | PR-169 | MEDIUM |
| PR-171 | Inventory | Reorder threshold monitor and automatic low-stock alert trigger | PLANNED | `feat/reorder-threshold-low-stock-alert` | TBD | Alert Tests | PR-164 | MEDIUM |
| PR-172 | Inventory | Inter-outlet stock transfer request and dispatch workflow | PLANNED | `feat/inter-outlet-stock-transfer-workflow` | TBD | Transfer Tests | PR-165 | HIGH |
| PR-173 | Inventory | Batch and expiry date tracking domain model | PLANNED | `feat/batch-expiry-date-tracking` | TBD | Expiry Tests | PR-161 | HIGH |
| PR-174 | Inventory | FIFO (First In First Out) inventory cost calculation engine | PLANNED | `feat/fifo-inventory-cost-engine` | TBD | FIFO Tests | PR-173 | CRITICAL |
| PR-175 | Inventory | Automatic stock deduction event listener on completed orders | PLANNED | `feat/auto-stock-deduction-order-event-listener` | TBD | Auto Deduction Tests | PR-165 | CRITICAL |
| PR-176 | Inventory | Physical stock reconciliation audit workflow | PLANNED | `feat/physical-stock-reconciliation-workflow` | TBD | Reconciliation Tests | PR-165 | HIGH |
| PR-177 | Inventory | Recipe cost analysis and dish gross margin calculator | PLANNED | `feat/recipe-cost-dish-margin-calculator` | TBD | Margin Tests | PR-163 | LOW |
| PR-178 | Inventory | Inventory stock depletion integration test suite | PLANNED | `test/inventory-stock-depletion-integration-suite` | TBD | Inventory IT | PR-175 | CRITICAL |

---

## Track 14: Customers, Loyalty & Promotions (PRs 179–192)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-179 | Customers | Customer profile management aggregate root | PLANNED | `feat/customer-profile-management-aggregate` | TBD | Customer Tests | PR-001 | MEDIUM |
| PR-180 | Customers | Customer dietary preferences, allergies, and anniversary tracking | PLANNED | `feat/customer-preferences-allergies-tracking` | TBD | Preference Tests | PR-179 | LOW |
| PR-181 | Customers | GDPR consent record tracking and anonymization runner | PLANNED | `feat/gdpr-consent-anonymization-runner` | TBD | GDPR Tests | PR-179 | HIGH |
| PR-182 | Customers | Customer order history query API and frequent item summary | PLANNED | `feat/customer-order-history-summary-api` | TBD | History Tests | PR-179 | LOW |
| PR-183 | Customers | Loyalty points account aggregate and point calculation rule | PLANNED | `feat/loyalty-points-account-aggregate` | TBD | Loyalty Tests | PR-179 | HIGH |
| PR-184 | Customers | Loyalty points redemption application service and discount creation | PLANNED | `feat/loyalty-points-redemption-service` | TBD | Redemption Tests | PR-183 | HIGH |
| PR-185 | Customers | Loyalty tier progression rules engine (Bronze, Silver, Gold, Platinum) | PLANNED | `feat/loyalty-tier-progression-rules-engine` | TBD | Tier Tests | PR-183 | MEDIUM |
| PR-186 | Customers | Loyalty points auto-expiration scheduled runner | PLANNED | `feat/loyalty-points-auto-expiry-runner` | TBD | Expiry Tests | PR-183 | MEDIUM |
| PR-187 | Customers | Referral coupon generation and referral reward tracking | PLANNED | `feat/referral-coupon-reward-tracking` | TBD | Referral Tests | PR-179 | LOW |
| PR-188 | Customers | Digital gift card aggregate root, issuance, and balance redemption | PLANNED | `feat/digital-gift-card-aggregate` | TBD | Gift Card Tests | PR-179 | HIGH |
| PR-189 | Customers | Customer segmentation engine (Recency, Frequency, Monetary - RFM) | PLANNED | `feat/customer-rfm-segmentation-engine` | TBD | RFM Tests | PR-179 | LOW |
| PR-190 | Customers | Duplicate customer detection and profile merge tool | PLANNED | `feat/duplicate-customer-merge-tool` | TBD | Merge Tests | PR-179 | MEDIUM |
| PR-191 | Promotions | Promotion coupon code validator engine and applicability check | PLANNED | `feat/promotion-coupon-code-validator` | TBD | Coupon Tests | PR-101 | CRITICAL |
| PR-192 | Promotions | BOGO (Buy One Get One) promotion stacking and priority policy | PLANNED | `feat/bogo-promotion-stacking-policy` | TBD | BOGO Tests | PR-191 | CRITICAL |

---

## Track 15: Receipts & Invoicing (PRs 193–200)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level |
|---|---|---|---|---|---|---|---|---|
| PR-193 | Receipts | Immutable receipt snapshot generator upon order payment | PLANNED | `feat/immutable-receipt-snapshot-generator` | TBD | Receipt Tests | PR-143 | CRITICAL |
| PR-194 | Receipts | Outlet sequential receipt numbering series generator | PLANNED | `feat/outlet-sequential-receipt-numbering` | TBD | Numbering Tests | PR-193 | CRITICAL |
| PR-195 | Receipts | Compliant tax invoice HTML/PDF renderer using templates | PLANNED | `feat/tax-invoice-html-pdf-renderer` | TBD | Invoice Tests | PR-193 | HIGH |
| PR-196 | Receipts | ESC/POS thermal printer raw byte output generator | PLANNED | `feat/escpos-thermal-printer-output-generator` | TBD | Printer Tests | PR-193 | MEDIUM |
| PR-197 | Receipts | Email receipt dispatch service with attachment support | PLANNED | `feat/email-receipt-dispatch-service` | TBD | Email Tests | PR-193 | LOW |
| PR-198 | Receipts | Receipt reprint audit log tracker and reprint counter | PLANNED | `feat/receipt-reprint-audit-tracker` | TBD | Audit Tests | PR-193 | MEDIUM |
| PR-199 | Receipts | Credit note and refund receipt generator for returned orders | PLANNED | `feat/credit-note-refund-receipt-generator` | TBD | Credit Note Tests | PR-149 | HIGH |
| PR-200 | Receipts | Digital receipt QR code link generator for customer mobile view | PLANNED | `feat/digital-receipt-qr-code-link` | TBD | QR Link Tests | PR-193 | LOW |
