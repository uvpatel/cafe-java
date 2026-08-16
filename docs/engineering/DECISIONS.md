# Architectural Decision Records (ADR)

This file documents the key technical and architectural decisions for `uvpatel/cafe-java`.

---

## ADR 001: Modular Monolith Architecture & Spring Modulith Encapsulation Rules

### Status
Accepted

### Context
`uvpatel/cafe-java` is designed as a production-grade modular monolith. To maintain clean boundaries without microservice overhead, each module (`catalog`, `ordering`, `promotion`, `payment`, `identity`, `organization`, `floor`, `booking`, `kitchen`, `session`, `inventory`, `loyalty`, `receipt`, `reporting`, `activitylog`) must encapsulate its internal logic and expose explicit ports.

### Decision
1. **Module Internal Structure**:
   - Each module contains `api`, `application`, `domain`, `infrastructure`, and `package-info.java`.
   - Subpackages (`domain`, `application`, `api`) are internal to the module by default unless exposed via `@org.springframework.modulith.NamedInterface("api")` or `"domain"`.
2. **Cross-Module Communication**:
   - No direct access to internal implementation or domain entities of another module.
   - Controllers for module-specific use cases must reside within that module (e.g. `OrderCouponController` resides in `promotion.api`, not `ordering.api`).
   - `ApplicationModulithTest` verifies module boundaries on every test run.

### Consequences
- Eliminates cyclic package dependencies.
- Prevents cross-module domain coupling.
- Enables eventual microservice extraction if scale demands it in the future.

---

## ADR 002: Tenant Context Propagation and Isolation Strategy

### Status
Accepted

### Context
Café POS is a multi-tenant platform. Tenant separation is non-negotiable.

### Decision
1. **Tenant ID Source**: Tenant ID (`cafeId`) is extracted from verified JWT tokens via `TenantContextHolder` and `JwtAuthenticationFilter`. Client-supplied tenant query parameters or headers are not trusted.
2. **Repository Guard**: All tenant-owned entities include `cafeId`, indexed for performance, and queried using tenant-aware methods (e.g., `findByIdAndCafeId`).
3. **Context Cleanup**: `TenantContextHolder` is cleared in a `finally` block in servlet filters after every request.

---

## ADR 003: Architecture Testing with ArchUnit

### Status
Accepted

### Context
To enforce clean code practices automatically across 1,000+ planned commits, structural invariants must be asserted in automated tests.

### Decision
1. **No Field Injection**: All Spring dependencies must use constructor injection (`@Autowired` on fields is banned).
2. **Domain Isolation**: Domain packages (`..domain..`) cannot import classes from transport (`..api..`) or infrastructure (`..infrastructure..`).
3. **Automated Enforcement**: Enforced via `ArchitectureBoundariesTest` using ArchUnit JUnit 5 integration.
