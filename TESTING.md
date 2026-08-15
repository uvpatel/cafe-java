# Testing Guide

This document provides comprehensive guidance on testing practices and strategies for Café POS Backend.

## Table of Contents

- [Testing Strategy](#testing-strategy)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [API Testing](#api-testing)
- [Architecture Testing](#architecture-testing)
- [Test Data & Fixtures](#test-data--fixtures)
- [Mocking & Test Doubles](#mocking--test-doubles)
- [Running Tests](#running-tests)
- [Coverage Goals](#coverage-goals)
- [Common Patterns](#common-patterns)

---

## Testing Strategy

### Test Pyramid

```
        /\
       /  \    End-to-End Tests (5%)
      /────\   (Slow, integration focused)
     /      \
    /────────\  Integration Tests (15%)
   /          \ (Medium speed, subsystem focused)
  /____________\

 ╔════════════════╗
 ║                ║  Unit Tests (80%)
 ║ (Fast, focused)║  (Milliseconds per test)
 ╚════════════════╝
```

### Test Types and Responsibilities

| Type | Scope | Speed | Tools | Coverage Target |
|------|-------|-------|-------|-----------------|
| **Unit** | Single class/method | <100ms | JUnit, AssertJ, Mockito | 90%+ (domain) |
| **Integration** | Multiple components | 100ms-5s | JUnit, Testcontainers | 80%+ (application) |
| **API** | HTTP endpoints | 200ms-2s | REST Assured | 70%+ (adapters) |
| **Architecture** | Module structure | <500ms | ArchUnit | 100% |

---

## Unit Testing

### Purpose

Test business logic in isolation with no external dependencies.

### Structure

Use nested test classes with `@DisplayName` for clear organization:

```java
@DisplayName("Order")
class OrderTest {
    
    @Nested
    @DisplayName("creation and validation")
    class CreationAndValidationTests {
        
        @Test
        @DisplayName("should create order with valid lines")
        void shouldCreateOrderWithValidLines() {
            // Arrange
            var lines = List.of(
                new OrderLine(productId1, quantity2)
            );
            
            // Act
            var order = Order.create(lines);
            
            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
            assertThat(order.getLines()).hasSize(1);
        }
        
        @Test
        @DisplayName("should reject order with no lines")
        void shouldRejectOrderWithNoLines() {
            // Arrange
            var emptyLines = Collections.emptyList();
            
            // Act & Assert
            assertThatThrownBy(() -> Order.create(emptyLines))
                .isInstanceOf(OrderMustHaveLinesException.class);
        }
    }
    
    @Nested
    @DisplayName("status transitions")
    class StatusTransitionTests {
        
        @Test
        @DisplayName("should allow transition from DRAFT to CONFIRMED")
        void shouldAllowDraftToConfirmedTransition() {
            // Arrange
            var order = Order.create(validLines);
            
            // Act
            order.confirm();
            
            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
        
        @Test
        @DisplayName("should reject invalid status transition")
        void shouldRejectInvalidTransition() {
            // Arrange
            var order = Order.create(validLines);
            order.confirm();
            
            // Act & Assert
            assertThatThrownBy(order::draft)
                .isInstanceOf(InvalidStateTransitionException.class);
        }
    }
}
```

### Assertions

Use AssertJ for fluent, readable assertions:

```java
// ✅ Good – Clear, expressive assertions
assertThat(order.getTotal())
    .isNotNull()
    .isGreaterThan(Money.of(0))
    .isLessThanOrEqualTo(MAX_ORDER_TOTAL);

assertThat(order.getStatus())
    .isEqualTo(OrderStatus.CONFIRMED)
    .as("Order should be confirmed after confirmation");

// ✅ Good – Exception assertions
assertThatThrownBy(() -> order.cancel())
    .isInstanceOf(OrderCannotBeCanceledException.class)
    .hasMessageContaining("CONFIRMED");

// ✅ Good – Collections
assertThat(order.getLines())
    .isNotEmpty()
    .hasSize(2)
    .extracting(OrderLine::getProductId)
    .contains(productId1, productId2);

// ❌ Bad – Unclear assertEquals
assertEquals(order.getStatus(), OrderStatus.CONFIRMED);

// ❌ Bad – Multiple assertions in one assert
assertTrue(order.getStatus() == CONFIRMED && order.getLines().size() == 2);
```

### Testing Domain Rules

Focus on business logic and invariant protection:

```java
@DisplayName("Money value object")
class MoneyTest {
    
    @Test
    @DisplayName("should calculate total with tax")
    void shouldCalculateTotalWithTax() {
        var amount = Money.of(100, INR);
        var taxRate = BigDecimal.valueOf(0.18); // 18% GST
        
        var taxAmount = amount.multiply(taxRate);
        var total = amount.add(taxAmount);
        
        assertThat(total).isEqualTo(Money.of(118, INR));
    }
    
    @Test
    @DisplayName("should reject negative amounts")
    void shouldRejectNegativeAmounts() {
        assertThatThrownBy(() -> Money.of(-100, INR))
            .isInstanceOf(InvalidMoneyException.class)
            .hasMessageContaining("negative");
    }
    
    @Test
    @DisplayName("should reject operations with different currencies")
    void shouldRejectDifferentCurrencies() {
        var inr = Money.of(100, INR);
        var usd = Money.of(100, USD);
        
        assertThatThrownBy(() -> inr.add(usd))
            .isInstanceOf(IncompatibleCurrencyException.class);
    }
}
```

---

## Integration Testing

### Purpose

Test subsystems including real databases, repositories, and persistence.

### Setup with Testcontainers

```java
@SpringBootTest
@Testcontainers
@DisplayName("Order Repository Integration Tests")
class OrderRepositoryIntegrationTest {
    
    // ✅ Use @Container for automatic lifecycle management
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("cafepos_test")
            .withUsername("test")
            .withPassword("test");
    
    // Inject repository
    @Autowired
    private OrderRepository repository;
    
    // Setup test data
    @BeforeEach
    void setUp() {
        // Clear data before each test
        repository.deleteAll();
    }
    
    @Test
    @DisplayName("should save and retrieve order")
    void shouldSaveAndRetrieveOrder() {
        // Arrange
        var order = Order.create(validLines);
        
        // Act
        repository.save(order);
        var retrieved = repository.findById(order.getId());
        
        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLines()).hasSize(1);
    }
    
    @Test
    @DisplayName("should enforce tenant isolation")
    void shouldEnforceTenantIsolation() {
        // Arrange
        var org1 = OrganizationId.of(UUID.randomUUID());
        var org2 = OrganizationId.of(UUID.randomUUID());
        
        var order1 = Order.create(org1, outlet, lines);
        var order2 = Order.create(org2, outlet, lines);
        
        repository.save(order1);
        repository.save(order2);
        
        // Act - Query as org1
        SecurityContext.setOrganization(org1);
        var results = repository.findAll();
        
        // Assert - Should only see org1's order
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOrganizationId()).isEqualTo(org1);
    }
}
```

### Application Service Integration Tests

```java
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Application Service Integration Tests")
class OrderApplicationServiceIntegrationTest {
    
    @Autowired
    private OrderApplicationService service;
    
    @Autowired
    private OrderRepository repository;
    
    @MockBean
    private PaymentService paymentService;  // Mock external service
    
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    
    @Test
    @DisplayName("should create order and publish event")
    void shouldCreateOrderAndPublishEvent() {
        // Arrange
        var dto = new CreateOrderDto(...);
        var eventCaptor = captor(DomainEvent.class);
        
        // Act
        var response = service.createOrder(dto);
        
        // Assert
        assertThat(response).isNotNull();
        
        // Verify order persisted
        var saved = repository.findById(response.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.DRAFT);
        
        // Verify external service called
        verify(paymentService).authorizePayment(any(Money.class));
    }
}
```

---

## API Testing

### REST Endpoint Testing

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Order Controller API Tests")
class OrderControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @MockBean
    private OrderApplicationService service;
    
    private static final String API_URL = "/api/v1/orders";
    
    @Test
    @DisplayName("POST /api/v1/orders should create order")
    void shouldCreateOrder() {
        // Arrange
        var request = new CreateOrderRequest();
        request.setItems(List.of(...));
        
        var expectedResponse = new OrderResponse();
        expectedResponse.setId(UUID.randomUUID());
        expectedResponse.setStatus("DRAFT");
        
        when(service.createOrder(any())).thenReturn(expectedResponse);
        
        // Act
        var response = restTemplate.postForEntity(
            API_URL,
            request,
            OrderResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("DRAFT");
    }
    
    @Test
    @DisplayName("GET /api/v1/orders/{id} should return order")
    void shouldGetOrder() {
        // Arrange
        var orderId = UUID.randomUUID();
        var expectedResponse = new OrderResponse();
        expectedResponse.setId(orderId);
        
        when(service.getOrder(any())).thenReturn(expectedResponse);
        
        // Act
        var response = restTemplate.getForEntity(
            API_URL + "/" + orderId,
            OrderResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().getId()).isEqualTo(orderId);
    }
}
```

### Using REST Assured

For more fluent API testing:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrderControllerRestAssuredTest {
    
    @LocalServerPort
    int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }
    
    @Test
    void shouldCreateOrder() {
        given()
            .contentType(APPLICATION_JSON)
            .body(new CreateOrderRequest())
        .when()
            .post("/orders")
        .then()
            .statusCode(CREATED.value())
            .body("id", notNullValue())
            .body("status", equalTo("DRAFT"));
    }
    
    @Test
    void shouldValidateOrderAmount() {
        given()
            .contentType(APPLICATION_JSON)
            .body(new CreateOrderRequest()
                .withTotalAmount(Money.of(999999)))  // Too large
        .when()
            .post("/orders")
        .then()
            .statusCode(UNPROCESSABLE_ENTITY.value())
            .body("title", equalTo("Validation Failed"))
            .body("detail", containsString("exceeds maximum"));
    }
}
```

---

## Architecture Testing

### Module Structure Validation

Enforce architectural boundaries:

```java
@Modulithic(systemPackages = "com.cafepos")
class ApplicationModulithTest {
    
    @Test
    void verifiesModuleStructure(Modules modules) {
        modules.verify(
            // No circular dependencies
            Verify.that()
                .noModuleDepends()
                .onModulesMatching(".*"),
            
            // Domain and ports cannot depend on adapters
            Verify.that()
                .layer()
                .layeredArch()
                .layer("domain").definedBy("..domain..")
                .layer("ports").definedBy("..ports..")
                .layer("adapters").definedBy("..adapters..")
                .layer("application").definedBy("..application..")
                .whereLayer("domain").mayNotAccessAnyLayer()
                .and()
                .whereLayer("ports").mayNotAccessAnyLayer()
                .and()
                .whereLayer("adapters").mayNotAccessAnyLayer(),
            
            // No cross-module persistence access
            Verify.that()
                .noseDependencyOnModuleMatching(".*persistence.*")
                .matches(
                    classes()
                        .that()
                        .resideInAPackage("com.cafepos.ordering.*")
                        .and()
                        .doNotResideInAPackage("com.cafepos.ordering.adapters.persistence.*")
                )
        );
    }
}
```

---

## Test Data & Fixtures

### Using Builders for Test Data

```java
@DisplayName("Order Test Fixtures")
class OrderTestFixtures {
    
    // ✅ Builder pattern for flexible test data
    public static class OrderBuilder {
        private List<OrderLine> lines = defaultLines();
        private Money total = Money.of(1000, INR);
        private OrderStatus status = OrderStatus.DRAFT;
        
        public OrderBuilder withLines(OrderLine... lines) {
            this.lines = Arrays.asList(lines);
            return this;
        }
        
        public OrderBuilder withTotal(Money total) {
            this.total = total;
            return this;
        }
        
        public OrderBuilder withStatus(OrderStatus status) {
            this.status = status;
            return this;
        }
        
        public Order build() {
            return new Order(UUID.randomUUID(), lines, total, status);
        }
    }
    
    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }
    
    // Usage in tests
    @Test
    void shouldHandleConfirmedOrders() {
        var order = anOrder()
            .withStatus(OrderStatus.CONFIRMED)
            .withTotal(Money.of(5000, INR))
            .build();
        
        // Use order in test
    }
}
```

### Using Factory Methods

```java
class TestDataFactory {
    
    public static Order createValidOrder() {
        return Order.create(List.of(
            new OrderLine(productId(), 1),
            new OrderLine(productId(), 2)
        ));
    }
    
    public static Money validOrderTotal() {
        return Money.of(2500, INR);
    }
    
    public static UUID productId() {
        return UUID.randomUUID();
    }
}

// Usage
@Test
void shouldProcessValidOrder() {
    var order = TestDataFactory.createValidOrder();
    var result = service.process(order);
    assertThat(result).isNotNull();
}
```

---

## Mocking & Test Doubles

### Mock External Dependencies

```java
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {
    
    @Mock
    private OrderRepository repository;
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private ApplicationEventPublisher publisher;
    
    @InjectMocks
    private OrderApplicationService service;
    
    @Test
    void shouldCreateOrderWithMockedDependencies() {
        // Arrange
        var order = Order.create(validLines);
        when(repository.save(any())).thenReturn(order);
        
        // Act
        service.createOrder(dto);
        
        // Assert
        verify(repository).save(any(Order.class));
        verify(publisher).publishEvent(any(OrderCreatedEvent.class));
        verifyNoMoreInteractions(publisher);
    }
}
```

### Spy on Real Objects

```java
@Test
void shouldCallRealMethodButTrackCalls() {
    // Create real object but track method calls
    var spyService = spy(new OrderApplicationService(...));
    
    // Call real method
    spyService.createOrder(dto);
    
    // Verify real method was called
    verify(spyService).createOrder(any());
}
```

---

## Running Tests

### Basic Commands

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=OrderApplicationServiceTest

# Run specific test method
./mvnw test -Dtest=OrderApplicationServiceTest#testCreateOrder

# Run with pattern
./mvnw test -Dtest=*ApplicationService*Test

# Run tests in module
./mvnw test -pl com.cafepos:ordering
```

### Advanced Options

```bash
# Run with coverage
./mvnw test jacoco:report
# View: target/site/jacoco/index.html

# Run with detailed output
./mvnw test -X

# Fail at end (run all, report failures at end)
./mvnw test --fail-at-end

# Run tests in parallel (per class)
./mvnw test -T 4

# Run single-threaded (useful for race conditions)
./mvnw test -DthreadCount=1

# Skip slow tests
./mvnw test -DexcludedGroups=slow
```

---

## Coverage Goals

### Target Coverage by Layer

| Layer | Target | Priority | Tools |
|-------|--------|----------|-------|
| **Domain** | 90%+ | Critical | JUnit + AssertJ |
| **Application** | 80%+ | High | JUnit + Mockito |
| **Adapters/API** | 70%+ | Medium | REST Assured |
| **Integration** | 80%+ | High | Testcontainers |

### Generating Coverage Reports

```bash
# Generate JaCoCo report
./mvnw clean test jacoco:report

# Open HTML report
open target/site/jacoco/index.html

# View coverage in CI
# GitHub Actions: codecov.io integration

# Check coverage threshold
./mvnw test jacoco:check \
  -Djacoco.check.rules.element=PACKAGE \
  -Djacoco.check.rules.includes=com.cafepos.* \
  -Djacoco.check.rules.limits=COVEREDRATIO:0.80
```

---

## Common Patterns

### Test Parametrization

Test multiple inputs with one method:

```java
@ParameterizedTest
@ValueSource(ints = { 0, 1, 100, 999 })
void shouldAcceptValidQuantities(int quantity) {
    var line = new OrderLine(productId, quantity);
    assertThat(line.getQuantity()).isEqualTo(quantity);
}

@ParameterizedTest
@MethodSource("validOrderInputs")
void shouldCreateOrderWithDifferentInputs(CreateOrderDto dto) {
    var result = service.createOrder(dto);
    assertThat(result).isNotNull();
}

private static Stream<CreateOrderDto> validOrderInputs() {
    return Stream.of(
        new CreateOrderDto().withItems(1),
        new CreateOrderDto().withItems(5),
        new CreateOrderDto().withItems(100)
    );
}
```

### Testing Exceptions

```java
@Test
void shouldThrowExceptionWithCorrectMessage() {
    assertThatThrownBy(() -> service.cancelOrder(invalidId))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessageContaining("No order found");
}

@Test
void shouldThrowSpecificException() {
    assertThatExceptionOfType(InvalidStateException.class)
        .isThrownBy(() -> order.confirm())
        .withMessageContaining("CANCELLED orders cannot be confirmed");
}
```

### Testing Async Operations

```java
@Test
void shouldCompleteAsyncOperation() {
    // Service processes asynchronously
    var future = service.processOrderAsync(orderId);
    
    // Wait for completion
    var result = future.get(5, TimeUnit.SECONDS);
    
    // Assert
    assertThat(result.getStatus()).isEqualTo(PROCESSED);
}
```

### Testing Date/Time

```java
@Test
void shouldSetCreatedAtToNow() {
    var before = Instant.now();
    
    var order = Order.create(lines);
    
    var after = Instant.now();
    
    assertThat(order.getCreatedAt())
        .isGreaterThanOrEqualTo(before)
        .isLessThanOrEqualTo(after);
}
```

---

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/assertj-core-features-highlight.html)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers Guide](https://www.testcontainers.org/)
- [REST Assured Guide](https://rest-assured.io/)
- [ArchUnit Documentation](https://www.archunit.org/)

---

**Last Updated:** August 2026
