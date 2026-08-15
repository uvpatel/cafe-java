# Contributing to Café POS Backend

Thank you for your interest in contributing to Café POS Backend! This document provides guidelines and workflows for all contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Code Quality Standards](#code-quality-standards)
- [Testing Requirements](#testing-requirements)
- [Documentation Standards](#documentation-standards)
- [Reporting Issues](#reporting-issues)
- [Asking Questions](#asking-questions)

---

## Code of Conduct

We are committed to providing a welcoming and inclusive environment. All contributors are expected to:

- Be respectful and professional in all interactions
- Welcome diverse perspectives and constructive feedback
- Focus on what is best for the community and project
- Show empathy toward other community members

Unacceptable behavior includes harassment, discrimination, or any form of abuse. Such conduct will result in removal from the project.

---

## Getting Started

### Prerequisites

- Java 21 SDK (LTS)
- Docker & Docker Compose
- Maven 3.8+
- Git 2.30+

### Local Setup

1. **Fork and Clone**
   ```bash
   git clone https://github.com/your-username/cafe-pos-backend.git
   cd cafe-pos-backend
   git remote add upstream https://github.com/original/cafe-pos-backend.git
   ```

2. **Start Infrastructure**
   ```bash
   docker compose up -d
   ```

3. **Verify Build**
   ```bash
   ./mvnw clean verify
   ```

4. **Create Feature Branch**
   ```bash
   git checkout -b feat/your-feature-name
   ```

---

## Development Workflow

### Branch Naming Convention

Use clear, descriptive branch names:

```
feat/module-feature-name       # New feature
fix/module-issue-name          # Bug fix
refactor/module-improvement    # Code refactoring
test/module-test-addition      # Test improvements
docs/description               # Documentation
perf/module-optimization       # Performance improvement
chore/description              # Tooling, deps, build
```

**Examples:**
- `feat/ordering-order-cancellation`
- `fix/payment-concurrent-capture-race-condition`
- `refactor/shared-money-value-object`
- `docs/api-contract-documentation`

### Feature Development

1. **Create feature branch** from latest `main`
   ```bash
   git fetch upstream
   git checkout -b feat/module-description upstream/main
   ```

2. **Implement feature**
   - Follow code organization within the module
   - Add comprehensive tests
   - Update related documentation
   - Verify multi-tenancy isolation

3. **Commit incrementally** with logical, atomic commits
   ```bash
   git add <files>
   git commit -m "feat(module): specific change"
   ```

4. **Keep branch up-to-date**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

5. **Push to your fork**
   ```bash
   git push origin feat/module-description
   ```

### Local Testing Before Push

```bash
# Full verification
./mvnw clean verify

# Code formatting
./mvnw spotless:apply

# All tests
./mvnw test

# Integration tests only
./mvnw test -Dgroups=integration

# Specific test
./mvnw test -Dtest=YourTestClass
```

---

## Commit Guidelines

### Commit Message Format

Follow the conventional commits format:

```
<type>(<module>): <subject>

<body>

<footer>
```

### Type

- `feat` – New feature or capability
- `fix` – Bug fix
- `test` – Test additions or improvements
- `refactor` – Code refactoring (no behavior change)
- `docs` – Documentation updates
- `perf` – Performance optimization
- `chore` – Tooling, dependencies, build changes
- `pr` – PR merge commit (automation)
- `style` – Code style fixes (formatting)
- `ci` – CI/CD configuration changes

### Module

Specify the affected module (lowercase):
- `shared`, `identity`, `organization`, `catalog`, `floor`, `customer`, `promotion`, `loyalty`, `session`, `ordering`, `kitchen`, `payment`, `receipt`, `booking`, `reporting`, `inventory`, `activitylog`

Or `core` for cross-cutting changes.

### Subject

- Imperative mood: "add" not "added" or "adds"
- Don't capitalize first letter
- No period at the end
- Limit to 50 characters
- Specific and descriptive

### Body

- Explain **what** and **why**, not how
- Wrap at 72 characters
- Separate from subject with a blank line
- Use bullet points for multiple changes
- Reference issues and PRs

### Footer

- Reference related issues: `Closes #123`, `Fixes #456`
- Reference related PRs: `Related-To #789`
- Break-change notifications for API changes: `BREAKING CHANGE: description`

### Examples

**Feature:**
```
feat(ordering): implement order cancellation workflow

- Add cancelOrder() use case to OrderApplicationService
- Implement state validation to prevent invalid cancellations
- Handle refund processing and kitchen ticket cancellation
- Add comprehensive domain and integration tests

Closes #42
```

**Bug Fix:**
```
fix(payment): prevent race condition in concurrent payment capture

When two capture requests arrive simultaneously, both could succeed
due to missing row-level locking. This caused duplicate charges.

- Add version field to Payment entity for optimistic locking
- Validate payment state before capture
- Add integration test reproducing race condition

Fixes #156
Related-To #155
```

**Refactoring:**
```
refactor(shared): extract Money value object into separate module

The Money value object is used across 8+ modules and has grown
to include currency conversion logic. Moving it to a dedicated
shared module improves clarity and reusability.

- Extract com.cafepos.money package
- Move Money and Currency classes
- Update all imports across modules
- Add comprehensive value object tests

No behavior change; internal refactoring only.
```

**Documentation:**
```
docs: add API contract testing documentation

Provide guidance for contract-driven API development using Pact
and REST Assured for ensuring API compatibility across services.

- Add contract testing best practices
- Document Pact setup and workflow
- Add Spring Cloud Contract examples
- Link to external resources

See also: https://pact.foundation
```

### Commit Best Practices

- ✅ **Atomic commits** – Each commit should compile and pass tests
- ✅ **Logical grouping** – Related changes in one commit, unrelated changes separate
- ✅ **Descriptive messages** – Clear intent and reasoning
- ✅ **No merge conflicts** – Rebase before push, not merge
- ✅ **Clean history** – Squash work-in-progress commits before PR
- ✅ **One feature per branch** – Easier to review and revert if needed

### Commit Anti-Patterns ❌

- Empty commits or commits with only whitespace
- Mixing multiple unrelated features
- Commits that break tests (except in feature branches)
- Vague messages ("fix stuff", "update", "misc changes")
- Huge commits with hundreds of line changes (split into logical units)

---

## Pull Request Process

### Before Creating PR

1. **Verify local build**
   ```bash
   ./mvnw clean verify
   ```

2. **Apply code formatting**
   ```bash
   ./mvnw spotless:apply
   ```

3. **Run mutation testing** (for critical code)
   ```bash
   ./mvnw org.pitest:pitest-maven:mutationCoverage
   ```

4. **Rebase on latest main**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

5. **Push to your fork**
   ```bash
   git push origin feat/your-feature --force-with-lease
   ```

### PR Title

Follow the same format as commits:
```
feat(ordering): add order cancellation workflow
fix(payment): prevent concurrent capture race condition
docs: expand README with troubleshooting section
```

### PR Description Template

```markdown
## Description

[Brief description of the feature/fix]

## Changes

- [Change 1]
- [Change 2]
- [Change 3]

## Related Issues

Closes #123
Fixes #456

## Type of Change

- [ ] New feature
- [ ] Bug fix
- [ ] Breaking change
- [ ] Documentation update

## Testing

- [x] Unit tests added/updated
- [x] Integration tests added/updated
- [x] Manual testing completed

### Test Coverage

- Domain logic: [coverage %]
- Application service: [coverage %]
- Adapter/Controller: [coverage %]

## Checklist

- [x] Code follows style guide (`./mvnw spotless:check`)
- [x] Tests pass locally (`./mvnw verify`)
- [x] New tests added for new functionality
- [x] Documentation updated (README, Swagger, etc.)
- [x] Migrations are forward-only
- [x] Multi-tenancy validated (org_id and outlet_id present)
- [x] Security implications considered
- [x] Performance impact assessed
- [x] Commit history is clean and logical

## Screenshots (if applicable)

[Add screenshots for UI changes]

## Additional Notes

[Any additional context or concerns]
```

### Review Process

1. **Automated Checks**
   - Build must pass on CI
   - Tests must pass
   - Code coverage must not decrease
   - No style violations

2. **Code Review**
   - At least one approval required
   - Maintainers review for architecture compliance
   - Security and multi-tenancy validation
   - Performance considerations

3. **Merge Strategy**
   - Squash commits if history is messy
   - Rebase if history is clean
   - One commit per logical unit

---

## Code Quality Standards

### Code Style

**Java Style Guide:** Google Java Style Guide (enforced by Spotless)

```bash
# Check formatting
./mvnw spotless:check

# Apply formatting
./mvnw spotless:apply
```

### Key Principles

- **Clarity over cleverness** – Code is read more than written
- **Explicitness over implicitness** – Avoid magic
- **Names matter** – Use full, descriptive names
- **Single responsibility** – Classes and methods do one thing
- **DRY principle** – Don't repeat yourself
- **SOLID principles** – Especially in domain code

### Naming Conventions

| Element | Pattern | Example |
|---------|---------|---------|
| **Classes** | `PascalCase` | `OrderApplicationService` |
| **Interfaces** | `PascalCase` | `OrderRepository` |
| **Methods** | `camelCase`, imperative | `createOrder()`, `validatePrice()` |
| **Constants** | `UPPER_SNAKE_CASE` | `DEFAULT_TIMEOUT_MS` |
| **Variables** | `camelCase`, descriptive | `totalAmount`, `isProcessing` |
| **Packages** | `lowercase.dot.separated` | `com.cafepos.ordering.domain` |

### Documentation Comments

Use Javadoc for:
- **Public classes** – Purpose and typical usage
- **Public methods** – What it does, parameters, return value, exceptions
- **Complex algorithms** – Explanation of approach
- **Non-obvious design decisions** – Why, not what

Example:
```java
/**
 * Calculates the final order total including taxes, discounts, and service charge.
 *
 * <p>Pricing calculation follows this sequence:
 * 1. Subtotal from order lines
 * 2. Discount application (coupon and automatic promotions)
 * 3. Tax calculation on discounted subtotal
 * 4. Service charge on pretax total
 * 5. Rounding to nearest paisa (2 decimals)
 *
 * @param order the order to calculate total for (must not be null)
 * @return the final order total as Money (never null)
 * @throws OrderTotalExceededException if total exceeds maximum allowed
 * @throws InvalidCurrencyException if currencies don't match
 */
public Money calculateTotal(Order order) { ... }
```

### Code Smells to Avoid

| Smell | Issue | Solution |
|-------|-------|----------|
| **God Class** | Too many responsibilities | Split into focused classes |
| **Long Method** | >20-30 lines | Extract to smaller methods |
| **Primitive Obsession** | Using primitives for domain concepts | Use value objects (Money, UUID) |
| **Feature Envy** | Method using many other object's methods | Move method closer to data |
| **Data Classes** | Only getters/setters, no behavior | Add domain logic |
| **Comments Smell** | Comments explaining obvious code | Improve code clarity instead |
| **Duplicate Code** | Same code in multiple places | Extract to shared method/class |

---

## Testing Requirements

### Coverage Targets

- **Domain layer** – 90%+ coverage (most critical)
- **Application layer** – 80%+ coverage
- **Adapter layer** – 60%+ coverage

### Test Types Required

1. **Unit Tests** (fastest, most numerous)
   - Domain entities and value objects
   - Business logic validation
   - Exception cases

2. **Integration Tests**
   - Repository behavior with database
   - Transaction boundaries
   - Multi-module interactions

3. **REST Endpoint Tests**
   - Controller contracts
   - Request/response mapping
   - HTTP status codes

4. **Architecture Tests**
   - Module dependency rules
   - No forbidden cross-module access
   - Layer separation enforcement

### Test Structure

```java
@DisplayName("OrderApplicationService")
class OrderApplicationServiceTest {
    
    @Mock OrderRepository repository;
    @Mock PromotionService promotionService;
    @InjectMocks OrderApplicationService service;
    
    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {
        
        @Test
        @DisplayName("should create order with valid input")
        void shouldCreateOrderWithValidInput() {
            // Arrange
            var createDto = new CreateOrderDto(...);
            
            // Act
            var result = service.createOrder(createDto);
            
            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(any(Order.class));
        }
        
        @Test
        @DisplayName("should reject order exceeding maximum total")
        void shouldRejectOrderExceedingMaximum() {
            // Arrange
            var createDto = new CreateOrderDto(...); // Large total
            
            // Act & Assert
            assertThatThrownBy(() -> service.createOrder(createDto))
                .isInstanceOf(OrderTotalExceededException.class)
                .hasMessageContaining("exceeds maximum");
        }
    }
}
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific module
./mvnw test -Dtest=com.cafepos.ordering.**

# With coverage report
./mvnw test jacoco:report
# View: target/site/jacoco/index.html

# Fail fast on first failure
./mvnw test -DfailIfNoTests=false --fail-at-end
```

---

## Documentation Standards

### README Updates

Update README.md if your change:
- Adds a new major feature
- Changes architecture significantly
- Adds/removes a dependency
- Changes build/deployment process
- Introduces new modules

### API Documentation

Use Springdoc OpenAPI annotations:

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    @PostMapping
    @Operation(summary = "Create a new order",
               description = "Creates a new order for the specified outlet")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        // ...
    }
}
```

### Architecture Decisions

Document significant architectural decisions in ADRs (Architecture Decision Records):

**File:** `docs/adr/ADR-NN-brief-title.md`

**Format:**
```markdown
# ADR-NN: Brief Title

## Status
Proposed | Accepted | Deprecated | Superseded

## Context
[Background and problem statement]

## Decision
[The decision we've made]

## Consequences
[Resulting context, positive and negative implications]

## Alternatives Considered
- Alternative 1: [explanation and why rejected]
- Alternative 2: [explanation and why rejected]
```

### Changelog Entry

For significant changes, add entry to CHANGELOG.md (if used):

```markdown
## [0.2.0] - 2024-08-20

### Added
- Order cancellation workflow with refund handling
- Payment method enhancements (UPI support)

### Fixed
- Race condition in concurrent payment capture

### Changed
- API response format for create order endpoint
```

---

## Reporting Issues

### Bug Reports

Include:
- **Title** – Clear, concise description
- **Description** – What happened, what should have happened
- **Steps to reproduce** – Exact steps to trigger the bug
- **Expected behavior** – What should happen
- **Actual behavior** – What actually happens
- **Environment** – Java version, Spring Boot version, OS
- **Screenshots** – If applicable
- **Error logs** – Stack traces or error messages

**Template:**
```markdown
## Bug Report: [Title]

### Description
[What is the issue?]

### Steps to Reproduce
1. [First step]
2. [Second step]
3. [etc.]

### Expected Behavior
[What should happen]

### Actual Behavior
[What actually happens]

### Environment
- Java version: 21
- Spring Boot version: 3.4.2
- Database: PostgreSQL 16
- OS: [your OS]

### Error Log
[Paste stack trace here]

### Additional Context
[Any other relevant information]
```

### Feature Requests

Include:
- **Title** – Brief description of feature
- **Motivation** – Why this feature is needed
- **Proposed solution** – How you envision it working
- **Alternatives** – Other approaches considered
- **Additional context** – Examples, related features

**Template:**
```markdown
## Feature Request: [Title]

### Motivation
[Why is this feature needed?]

### Proposed Solution
[How should this work?]

### Alternatives
- [Alternative approach 1]
- [Alternative approach 2]

### Additional Context
[Any other relevant information]

### Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3
```

---

## Asking Questions

### Before Asking

1. Search existing issues and discussions
2. Check README.md and documentation
3. Search Stack Overflow
4. Review related code

### Getting Help

- **Usage questions** – GitHub Discussions
- **Bug reports** – GitHub Issues
- **Security concerns** – See README.md for security contact
- **Architecture questions** – Create an issue tagged `discussion`

### Question Format

Provide:
- Clear, specific question
- Context and background
- What you've already tried
- Relevant code snippets or error logs

---

## Review Guidelines for Maintainers

When reviewing PRs, consider:

- **Correctness** – Does it work? Are edge cases handled?
- **Architecture** – Does it follow patterns and design principles?
- **Testability** – Is there adequate test coverage?
- **Performance** – Any potential performance issues?
- **Security** – Multi-tenancy isolation? Input validation? Authorization?
- **Maintainability** – Is it understandable? Well-documented?
- **Style** – Code formatting, naming, structure
- **Compatibility** – Breaking changes? Backward compatible?

---

## Resources

- **Java Style Guide** – https://google.github.io/styleguide/javaguide.html
- **Spring Framework Docs** – https://spring.io/projects/spring-framework
- **Spring Boot Docs** – https://spring.io/projects/spring-boot
- **Spring Modulith Docs** – https://spring.io/projects/spring-modulith
- **Domain-Driven Design** – https://domainlanguage.com/
- **Conventional Commits** – https://www.conventionalcommits.org/
- **Keep a Changelog** – https://keepachangelog.com/

---

## Questions?

- Check existing issues and PRs
- Create a new issue with the `question` label
- Join discussions

Thank you for contributing to Café POS Backend! 🎉
