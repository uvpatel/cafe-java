# Café POS Backend – Architecture Guide

This document provides detailed architecture guidance for the Café POS Backend project.

## Table of Contents

- [Overview](#overview)
- [Architectural Patterns](#architectural-patterns)
- [Modular Monolith Structure](#modular-monolith-structure)
- [Dependency Management](#dependency-management)
- [Module Anatomy](#module-anatomy)
- [Cross-Module Communication](#cross-module-communication)
- [Data Flow](#data-flow)
- [Multi-Tenancy Architecture](#multi-tenancy-architecture)
- [Security Architecture](#security-architecture)
- [Scalability Considerations](#scalability-considerations)
- [Technology Decisions](#technology-decisions)

---

## Overview

The Café POS Backend is designed as a **modular monolith** – a single deployable artifact composed of independent, loosely-coupled domain modules that can eventually be extracted into separate services.

### Why Modular Monolith?

| Benefit | Description |
|---------|-------------|
| **Simplicity** | Single deployment, shared database, no distributed transactions |
| **Development Speed** | Refactor across modules easily; shared infrastructure |
| **Team Efficiency** | Teams can work on different modules in parallel |
| **Optimization** | In-process calls faster than RPC; easier to debug |
| **Extraction Ready** | Clear module boundaries make extraction straightforward |

### When to Extract to Services

Extract a module to a service when:

- Module requires **independent scaling** (e.g., Kitchen needs 10x resources)
- Module has **data ownership** concerns (separate retention policies)
- Module needs **team autonomy** (deployed independently)
- Module has **different SLAs** (payment processing vs. reporting)

---

## Architectural Patterns

### 1. Hexagonal Architecture (Ports & Adapters)

Each module follows hexagonal architecture with clear separation:

```
                    ┌─ Domain ─┐
                    │ (Core)   │
                    └─────────┬┘
                          ↕
        ┌───────────────────┴───────────────────┐
        ↓                                         ↓
    [Port Interface]                        [Port Interface]
    (Repository)                            (External Service)
        ↓                                         ↓
    [Adapter]                               [Adapter]
    (JPA Repository)                        (REST Client)
```

**Benefits:**
- Domain code has no external dependencies
- Easy to test domain logic (no mocks needed)
- Easy to swap implementations (in-memory repo for tests)
- Clear contracts via port interfaces

### 2. Domain-Driven Design (DDD)

Structure modules around business domains, not technical concerns:

✅ **Good modules:**
- `ordering` – Order business logic
- `payment` – Payment processing
- `kitchen` – Kitchen operations

❌ **Bad modules:**
- `persistence` – Technical concern, should be shared
- `api` – Transport layer, not a business domain
- `utils` – Vague, spreads functionality

### 3. Event-Driven Architecture

Modules communicate asynchronously via domain events:

```
Module A
  └─ Publishes OrderCreatedEvent
     └─ @EventListener in Module B consumes it
        └─ Takes action (e.g., create receipt)
```

**Advantages:**
- Loose coupling – Modules don't know about each other
- Scalability – Events can be processed asynchronously
- Auditability – Complete event trail
- Resilience – Failure in one module doesn't crash others

**Implementation:**
- Use Spring's `ApplicationEventPublisher`
- Publish in application service after persistence
- Listen with `@EventListener` in other modules

Example:
```java
// Module A: Ordering
@Service
public class OrderApplicationService {
    public void createOrder(CreateOrderDto dto) {
        var order = new Order(...);
        repository.save(order);
        publisher.publishEvent(new OrderCreatedEvent(order.getId()));
    }
}

// Module B: Kitchen
@Component
public class KitchenEventListener {
    @EventListener(OrderCreatedEvent.class)
    public void onOrderCreated(OrderCreatedEvent event) {
        // Create kitchen ticket
    }
}
```

### 4. Dependency Injection

Use constructor injection exclusively:

```java
// ✅ Good – Constructor injection
@Service
public class OrderApplicationService {
    private final OrderRepository repository;
    
    public OrderApplicationService(OrderRepository repository) {
        this.repository = repository;
    }
}

// ❌ Bad – Field injection (harder to test, hides dependencies)
@Service
public class OrderApplicationService {
    @Autowired
    private OrderRepository repository;
}

// ❌ Bad – Setter injection (mutable dependencies)
@Service
public class OrderApplicationService {
    private OrderRepository repository;
    
    @Autowired
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }
}
```

---

## Modular Monolith Structure

### Module Layout

```
src/main/java/com/cafepos/
├── shared/                  # Kernel (no dependencies on other modules)
│   ├── domain/
│   │   ├── Money.java       # Value objects
│   │   ├── BaseEntity.java  # Base types
│   │   └── DomainEvent.java # Event base class
│   ├── exception/
│   │   └── CafePosException.java
│   ├── util/
│   │   └── DateTimeUtil.java
│   └── Shared.java          # Module marker
│
├── identity/                # Module 1
│   ├── domain/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── UserCreatedEvent.java
│   │   └── exception/
│   │       └── InvalidCredentialsException.java
│   ├── application/
│   │   ├── AuthenticationService.java
│   │   ├── UserApplicationService.java
│   │   └── dto/
│   │       ├── CreateUserDto.java
│   │       └── LoginDto.java
│   ├── ports/
│   │   └── UserRepository.java
│   ├── adapters/
│   │   ├── api/
│   │   │   └── AuthController.java
│   │   ├── persistence/
│   │   │   └── JpaUserRepository.java
│   │   └── events/
│   │       └── UserEventListener.java
│   └── Identity.java        # Module marker
│
├── ordering/                # Module 2 (similar structure)
│   ├── domain/
│   ├── application/
│   ├── ports/
│   ├── adapters/
│   └── Ordering.java
│
└── CafePosApplication.java  # Entry point
```

### Module Marker Classes

Each module has a marker class that declares the module to Spring Modulith:

```java
// identity/Identity.java
@NamedModule(displayName = "Identity & Access Management")
package com.cafepos.identity;

import org.springframework.modulith.NamedModule;
```

---

## Dependency Management

### Allowed Dependencies

```
            ┌─────────────────────┐
            │     Application     │
            │    (Use cases)      │
            └──────────┬──────────┘
                       ↓
            ┌─────────────────────┐
            │ Domain/Business     │
            │   (No Spring)       │
            └──────────┬──────────┘
                       ↓
            ┌─────────────────────┐
            │      Ports          │
            │ (Interfaces only)   │
            └──────────┬──────────┘
                       ↓
            ┌─────────────────────┐
            │   Infrastructure    │
            │   (DB, APIs, etc)   │
            └─────────────────────┘
```

### Module-to-Module Dependencies

**Allowed:**
- ✅ One module uses another module's **public API** (services)
- ✅ Modules publish **domain events** to shared kernel
- ✅ Modules share **kernel** types (Money, BaseEntity)

**Not Allowed:**
- ❌ Direct access to another module's repositories
- ❌ Direct instantiation of another module's domain objects
- ❌ Internal imports (marked with `com.cafepos.module.internal`)

**Example – Correct:**
```java
// ordering/application/OrderApplicationService.java
public class OrderApplicationService {
    // ✅ Use payment module's service interface
    private final PaymentService paymentService;
    
    public void createOrder(CreateOrderDto dto) {
        var order = new Order(...);
        
        // ✅ Valid cross-module call through service
        paymentService.authorizePayment(order.getTotal());
    }
}
```

**Example – Incorrect:**
```java
// ordering/application/OrderApplicationService.java
public class OrderApplicationService {
    // ❌ WRONG: Directly accessing another module's repository
    private final PaymentRepository paymentRepository;
}
```

### Preventing Unauthorized Dependencies

Use Spring Modulith to enforce rules:

```java
// src/test/java/com/cafepos/ApplicationModulithTest.java
@Modulithic
class ApplicationModulithTest {
    
    @Test
    void verifyModuleStructure(Modules modules) {
        modules.verify(
            // Prohibit circular dependencies
            Verify.that()
                .noModuleDepends()
                .onModulesMatching("..internal..")
                .and()
                // Specific rules
                .layer()
                .layeredArch()
                .layer("application").definedBy("..application..")
                .layer("domain").definedBy("..domain..")
                .layer("adapters").definedBy("..adapters..")
                .whereLayer("application").mayNotAccessAnyLayer()
                .and()
                .whereLayer("domain").mayNotAccessAnyLayer()
        );
    }
}
```

---

## Module Anatomy

### Domain Layer (Pure Business Logic)

**Responsibility:** Encode business rules and domain knowledge

**Characteristics:**
- No Spring annotations (except value objects)
- No external dependencies
- Immutable when possible
- Thorough validation
- Rich domain types (not just primitives)

**Example:**
```java
// ordering/domain/Order.java
public class Order {
    private UUID id;
    private OrderStatus status;
    private List<OrderLine> lines;
    private Money total;
    
    // Constructor enforces invariants
    private Order(UUID id, List<OrderLine> lines) {
        if (lines.isEmpty()) {
            throw new OrderMustHaveLinesException();
        }
        this.id = id;
        this.lines = new ArrayList<>(lines); // Defensive copy
        this.status = OrderStatus.DRAFT;
        validateTotal();
    }
    
    // Factory method (preferred over public constructor)
    public static Order create(List<OrderLine> lines) {
        return new Order(UUID.randomUUID(), lines);
    }
    
    // Business methods with clear intent
    public void cancel() {
        if (!canBeCanceled()) {
            throw new OrderCannotBeCanceledException(status);
        }
        status = OrderStatus.CANCELLED;
    }
    
    // Encapsulate complex logic
    private void validateTotal() {
        if (total.isGreaterThan(MAX_ORDER_TOTAL)) {
            throw new OrderTotalExceededException(total);
        }
    }
    
    private boolean canBeCanceled() {
        return status == OrderStatus.DRAFT || status == OrderStatus.CONFIRMED;
    }
    
    // Getters (no setters – immutability)
    public UUID getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public Money getTotal() { return total; }
}
```

### Application Layer (Use Cases & Orchestration)

**Responsibility:** Coordinate domain logic and manage transactions

**Characteristics:**
- Spring services with `@Service` annotation
- One service per major use case
- Transaction management via `@Transactional`
- DTOs for input/output
- Publishes domain events after persistence
- No business logic (delegate to domain)

**Example:**
```java
// ordering/application/OrderApplicationService.java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository repository;
    private final CatalogService catalogService;
    private final PromotionService promotionService;
    private final ApplicationEventPublisher publisher;
    
    public OrderApplicationService(
            OrderRepository repository,
            CatalogService catalogService,
            PromotionService promotionService,
            ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.promotionService = promotionService;
        this.publisher = publisher;
    }
    
    // Use case: Create order
    public OrderResponse createOrder(CreateOrderDto dto) {
        // 1. Validate input (business rules)
        var lines = dto.getLines().stream()
            .map(lineDto -> OrderLine.create(
                lineDto.getProductId(),
                lineDto.getQuantity()))
            .collect(toList());
        
        // 2. Create aggregate
        var order = Order.create(lines);
        
        // 3. Orchestrate with other services
        var catalogData = catalogService.getProductsForOrder(order);
        var promotions = promotionService.getApplicablePromotions(order);
        order.applyPromotions(promotions);
        
        // 4. Persist
        var savedOrder = repository.save(order);
        
        // 5. Publish event (other modules listen)
        publisher.publishEvent(new OrderCreatedEvent(savedOrder.getId()));
        
        // 6. Return DTO
        return OrderResponse.fromOrder(savedOrder);
    }
}
```

### Port Layer (Interfaces)

**Responsibility:** Define contracts for external dependencies

**Characteristics:**
- Interface, never implementation
- Owned by domain module
- No Spring annotations
- Clear, business-focused names

**Example:**
```java
// ordering/ports/OrderRepository.java
public interface OrderRepository {
    OrderId save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCafeId(CafeId cafeId);
    void delete(OrderId id);
}

// ordering/ports/PromotionService.java
public interface PromotionService {
    List<Promotion> getApplicablePromotions(Order order);
}
```

### Adapter Layer (Implementations)

**Responsibility:** Implement technical details

**Sub-layers:**
- **API Adapter** – HTTP controllers, request/response mapping
- **Persistence Adapter** – JPA repositories, database queries
- **Event Adapter** – Event listeners, async processing

**Example – API Adapter:**
```java
// ordering/adapters/api/OrderController.java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderApplicationService service;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        var dto = CreateOrderDto.fromRequest(request);
        var response = service.createOrder(dto);
        return ResponseEntity.status(CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        var response = service.getOrder(new OrderId(id));
        return ResponseEntity.ok(response);
    }
}
```

**Example – Persistence Adapter:**
```java
// ordering/adapters/persistence/JpaOrderRepository.java
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final SpringDataOrderRepository springRepository;
    
    @Override
    public OrderId save(Order order) {
        var entity = OrderEntity.fromDomain(order);
        var saved = springRepository.save(entity);
        return new OrderId(saved.getId());
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepository.findById(id.value())
            .map(OrderEntity::toDomain);
    }
}

// Internal – only used by JpaOrderRepository
@Repository
interface SpringDataOrderRepository extends JpaRepository<OrderEntity, UUID> {
    // Spring Data queries here
}
```

---

## Cross-Module Communication

### 1. Synchronous – Service Injection

Use for immediate, required dependencies:

```java
@Service
public class OrderApplicationService {
    private final PaymentService paymentService;
    
    public void createOrder(CreateOrderDto dto) {
        var order = Order.create(...);
        
        // Synchronous call – payment must succeed
        paymentService.authorizePayment(order.getTotal());
        
        repository.save(order);
    }
}
```

**Pros:** Immediate consistency, simple debugging  
**Cons:** Tight coupling, cascading failures

### 2. Asynchronous – Domain Events

Use for loosely coupled, side-effect operations:

```java
// Module A: Ordering
@Service
public class OrderApplicationService {
    private final ApplicationEventPublisher publisher;
    
    public void createOrder(CreateOrderDto dto) {
        var order = Order.create(...);
        repository.save(order);
        
        // Publish event asynchronously
        publisher.publishEvent(new OrderCreatedEvent(order.getId()));
    }
}

// Module B: Kitchen
@Component
public class KitchenEventListener {
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // Create kitchen ticket
        kitchenService.createTicket(event.getOrderId());
    }
}

// Module C: Receipt
@Component
public class ReceiptEventListener {
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // Queue receipt generation
        receiptService.generateReceipt(event.getOrderId());
    }
}
```

**Pros:** Loose coupling, parallelizable, resilient  
**Cons:** Eventual consistency, harder to debug

### 3. Queries – Read Services

For read-only access to another module's data:

```java
// Ordered module exposes read service
@Service
public class OrderReadService {
    private final OrderRepository repository;
    
    public List<OrderSummary> getOrdersByOutlet(OutletId outletId) {
        return repository.findByOutletId(outletId).stream()
            .map(OrderSummary::from)
            .collect(toList());
    }
}

// Reporting module uses it
@Service
public class ReportingService {
    private final OrderReadService orderService;
    
    public SalesReport generateSalesReport(OutletId outletId) {
        var orders = orderService.getOrdersByOutlet(outletId);
        return SalesReport.from(orders);
    }
}
```

---

## Data Flow

### HTTP Request → Response

```
1. HTTP Request arrives at Controller
   POST /api/v1/orders
   {"items": [...]}

2. Controller validates & maps to DTO
   CreateOrderDto dto = CreateOrderDto.from(request)

3. Application Service coordinates use case
   OrderApplicationService.createOrder(dto)

4. Domain logic creates & validates aggregate
   Order order = Order.create(lines)

5. Repository persists to database
   OrderRepository.save(order)

6. Event published to event bus
   publisher.publishEvent(new OrderCreatedEvent(...))

7. Other modules listen asynchronously
   @EventListener void onOrderCreated(...)

8. Controller returns Response
   ResponseEntity<OrderResponse>

9. HTTP Response sent to client
   201 Created
   {"id": "...", "status": "DRAFT"}
```

### Database Transaction Scope

All database operations in a single `@Transactional` block:

```java
@Service
@Transactional
public class OrderApplicationService {
    
    public OrderResponse createOrder(CreateOrderDto dto) {
        // ✅ Inside transaction
        var order = Order.create(dto.getLines());
        repository.save(order);  // INSERT
        
        // ✅ Still inside transaction, can query
        var existing = repository.findByOrderNumber(dto.getOrderNumber());
        
        // ✅ Event published after method returns (transaction commits first)
        publisher.publishEvent(new OrderCreatedEvent(order.getId()));
        
        return OrderResponse.fromOrder(order);
    }
    // ✅ Transaction commits here
}
```

---

## Multi-Tenancy Architecture

All data is isolated by **organization** and **outlet**:

### Schema: Every Table Has Tenant Keys

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,  -- Tenant isolation
    outlet_id UUID NOT NULL,        -- Outlet isolation
    order_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    
    UNIQUE(organization_id, outlet_id, order_number),
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (outlet_id) REFERENCES outlets(id)
);

CREATE INDEX idx_orders_org_outlet 
    ON orders(organization_id, outlet_id);
```

### Application: Enforce Tenant Context

```java
@Service
@Transactional
public class OrderApplicationService {
    
    public OrderResponse createOrder(CreateOrderDto dto) {
        // 1. Get tenant context from security
        var organizationId = SecurityContext.getOrganizationId();
        var outletId = SecurityContext.getOutletId();
        
        // 2. Create order with tenant keys
        var order = Order.create(
            organizationId,
            outletId,
            dto.getLines());
        
        // 3. Persist with tenant isolation
        repository.save(order);
        
        return OrderResponse.fromOrder(order);
    }
}

// Repository always filters by tenant
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    @Override
    public Optional<Order> findById(OrderId id) {
        var organizationId = SecurityContext.getOrganizationId();
        
        return springRepository.findById(id, organizationId)
            .map(OrderEntity::toDomain);
    }
}
```

### Security: Tenant Context

```java
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) 
            throws ServletException, IOException {
        
        // Extract from JWT token
        var token = extractToken(request);
        var organizationId = tokenService.getOrganizationId(token);
        var outletId = tokenService.getOutletId(token);
        
        // Store in SecurityContext
        SecurityContext.setOrganizationId(organizationId);
        SecurityContext.setOutletId(outletId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }
}
```

---

## Security Architecture

### Authentication Flow

```
1. Client sends credentials
   POST /auth/login
   {"email": "...", "password": "..."}

2. AuthenticationService verifies credentials
   - Hash password with Argon2id
   - Compare with stored hash

3. JWT token generated
   - Header: {"alg": "RS256"}
   - Payload: {"org_id": "...", "outlet_id": "...", "roles": [...]}
   - Signature: HMAC-SHA256

4. Token returned to client
   {"token": "eyJhbGc...", "expiresIn": 3600}

5. Client sends token with each request
   Authorization: Bearer eyJhbGc...

6. ResourceServerConfig validates token
   - Verify signature with public key
   - Check expiration
   - Extract tenant/role information

7. SecurityContext populated
   - Principal: authenticated user
   - Authorities: roles and permissions
   - Tenant context: organization_id, outlet_id
```

### Authorization Rules

```java
@Service
public class OrderApplicationService {
    
    @PreAuthorize("hasRole('CASHIER') or hasRole('MANAGER')")
    public OrderResponse createOrder(CreateOrderDto dto) {
        // Only cashiers and managers can create orders
        var order = Order.create(dto.getLines());
        repository.save(order);
        return OrderResponse.fromOrder(order);
    }
    
    @PreAuthorize("hasRole('MANAGER') and @securityService.isOutletManager(#outletId)")
    public void closeSession(OutletId outletId) {
        // Only outlet managers can close sessions
        sessionService.close(outletId);
    }
}
```

---

## Scalability Considerations

### Current Architecture (Monolith)

```
┌─────────────────────────────────────┐
│ Single Deployed Application         │
│ (All 15+ modules)                   │
└──────────────┬──────────────────────┘
               ↓
         ┌───────────┐
         │PostgreSQL │
         └───────────┘
```

**Suitable for:** Small to medium deployments (< 1000 orders/day)

### Future: Module Extraction

When scaling requirements demand it:

```
        ┌─────────────────┐
        │  Main API App   │
        │ (Ordering, etc) │
        └────────┬────────┘
                 │
      ┌──────────┼──────────┐
      │          │          │
      ↓          ↓          ↓
   [Kitchen]  [Payment]  [Reporting]
   Service    Service    Service
     │          │          │
     └──────────┼──────────┘
                │
         [Message Queue]
        (Kafka/RabbitMQ)
```

**Extraction Criteria:**
1. Module has clear **data ownership**
2. Module needs **independent scaling**
3. Module requires **deployment autonomy**
4. Module traffic justifies **operational overhead**

### Optimization Strategies

**1. Caching**
```java
@Service
@CacheConfig(cacheNames = "products")
public class CatalogService {
    
    @Cacheable(key = "#id")
    public Product getProduct(ProductId id) {
        return repository.findById(id).orElseThrow();
    }
    
    @CacheEvict(allEntries = true)
    public void updateProduct(Product product) {
        repository.save(product);
    }
}
```

**2. Database Indexing**
```sql
-- High-cardinality queries need indexes
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_status ON orders(status) 
    WHERE status IN ('DRAFT', 'CONFIRMED');
```

**3. Async Processing**
```java
@Service
public class ReportingService {
    
    @Async
    public void generateReportAsync(ReportId id) {
        // Long-running operation in thread pool
        var report = generateReport(id);
        reportRepository.save(report);
    }
}
```

**4. Read Replicas**
```properties
# Primary for writes
spring.datasource.primary.url=jdbc:postgresql://primary:5432/cafepos
spring.datasource.primary.hikari.maximum-pool-size=20

# Replica for reads
spring.datasource.replica.url=jdbc:postgresql://replica:5432/cafepos
spring.datasource.replica.hikari.maximum-pool-size=50
spring.datasource.replica.hikari.read-only=true
```

---

## Technology Decisions

### Why Java 25?

- Latest LTS features (records, sealed classes, pattern matching)
- Steady cadence of language improvements
- 5-year support window
- Tooling maturity and IDE support

### Why Spring Boot?

- Industry-standard ecosystem
- Battle-tested in production
- Strong security features
- Comprehensive documentation
- Large community

### Why PostgreSQL?

- ACID compliance with excellent transaction support
- Advanced features (JSON, arrays, full-text search)
- Strong partitioning and scaling capabilities
- Mature, reliable open-source database
- Multi-tenancy patterns well-supported

### Why Event-Driven?

- Loose coupling between modules
- Natural audit trail (event log)
- Supports asynchronous processing
- Scales better than synchronous calls
- Facilitates eventual consistency patterns

### Why Modular Monolith (not Microservices)?

**Benefits:**
- Shared Spring context (faster startup, less memory)
- No distributed transaction complexity
- Single database (referential integrity)
- Easy debugging (stack traces span modules)
- Simpler deployment (one Docker image)

**Tradeoffs:**
- All modules must scale together
- Requires discipline to maintain boundaries
- Harder to use different tech stacks
- Can become unmaintainable if boundaries blur

---

## References

- [Spring Framework Documentation](https://spring.io/projects/spring-framework)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [Domain-Driven Design – Eric Evans](https://domainlanguage.com/)
- [Clean Architecture – Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Building Microservices – Sam Newman](https://samnewman.io/books/building_microservices/)

---

**Last Updated:** August 2026  
**Author:** Engineering Team
