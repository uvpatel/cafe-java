# Local Development Guide

This guide provides detailed instructions for setting up and working with the Café POS Backend locally.

## Table of Contents

- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Building and Running](#building-and-running)
- [Debugging](#debugging)
- [Database Management](#database-management)
- [Common Development Tasks](#common-development-tasks)
- [Troubleshooting](#troubleshooting)

---

## Development Environment Setup

### Prerequisites

- **Java 21 SDK** (https://adoptium.net/)
- **Docker & Docker Compose** (https://www.docker.com/)
- **Git** (https://git-scm.com/)
- **Maven 3.8+** (included as `./mvnw`)
- **IDE**: IntelliJ IDEA (recommended) or VS Code
- **5 GB disk space** for dependencies and Docker images

### Step 1: Install Java 21

```bash
# macOS (using Homebrew)
brew install openjdk@21

# Verify installation
java -version
# Should output: openjdk 21...
```

### Step 2: Clone Repository

```bash
git clone https://github.com/your-org/cafe-pos-backend.git
cd cafe-pos-backend
```

### Step 3: Verify Maven Setup

```bash
./mvnw --version
# Should output: Maven 3.8+ with Java 21
```

### Step 4: Start Docker Infrastructure

```bash
# Start PostgreSQL, Redis, and other services
docker compose up -d

# Verify containers are running
docker compose ps
# Should show: postgres (up), redis (up)
```

### Step 5: Run Initial Build

```bash
# Clean and compile
./mvnw clean compile

# Build and run tests
./mvnw verify

# If successful, you're ready to develop!
```

### IDE Setup

#### IntelliJ IDEA

1. **Open Project**
   - File → Open → Select project root
   - Trust project when prompted

2. **Configure JDK**
   - File → Project Structure → Project
   - Set Project SDK to Java 21
   - Click Apply

3. **Enable Annotation Processing**
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Enable "Obtain processors from project classpath"
   - Enable "Store generated sources relative to module content root"

4. **Install Plugins** (optional but recommended)
   - Lombok
   - Checkstyle-IDEA
   - Save Actions

#### VS Code

1. **Install Extensions**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - REST Client

2. **Configure Workspace**
   - Create `.vscode/settings.json`:
   ```json
   {
     "java.home": "/path/to/java-21",
     "java.configuration.updateBuildConfiguration": "automatic",
     "editor.formatOnSave": true,
     "editor.defaultFormatter": "esbenp.prettier-vscode"
   }
   ```

---

## Project Structure

### Directory Organization

```
cafe-pos-backend/
├── pom.xml                          # Root Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/cafepos/
│   │   │   ├── shared/              # Common utilities and kernel
│   │   │   ├── identity/            # Auth and user management
│   │   │   ├── organization/        # Multi-tenant setup
│   │   │   ├── catalog/             # Products and categories
│   │   │   ├── ordering/            # Order management
│   │   │   ├── payment/             # Payment processing
│   │   │   ├── kitchen/             # Kitchen operations
│   │   │   ├── reporting/           # Analytics and exports
│   │   │   └── ... (other modules)
│   │   └── resources/
│   │       ├── application.yml      # Main configuration
│   │       ├── application-dev.yml  # Development config
│   │       ├── application-test.yml # Test config
│   │       └── db/migration/        # Flyway migrations
│   │
│   └── test/
│       ├── java/com/cafepos/        # Unit and integration tests
│       └── resources/               # Test fixtures and data
│
├── docs/
│   ├── adr/                         # Architecture Decision Records
│   ├── api/                         # API specifications
│   └── diagrams/                    # Architecture diagrams
│
├── docker-compose.yml               # Local infrastructure
├── README.md                        # Project overview
├── CONTRIBUTING.md                  # Contribution guidelines
├── ARCHITECTURE.md                  # Architecture guide
└── PLAN.md                          # Implementation plan
```

### Module Structure

Each module follows this pattern:

```
module/
├── domain/
│   ├── {Entity}.java                # Domain entities
│   ├── {ValueObject}.java           # Value objects
│   ├── {Service}.java               # Domain services
│   ├── {Event}.java                 # Domain events
│   └── exception/
│       ├── {Exception1}.java
│       └── {Exception2}.java
├── application/
│   ├── {ApplicationService}.java    # Use cases
│   └── dto/
│       ├── Create{Entity}Dto.java
│       ├── Update{Entity}Dto.java
│       └── {Entity}Response.java
├── ports/
│   ├── {Repository}.java            # Repository interface
│   └── {Service}.java               # Service interface
├── adapters/
│   ├── api/
│   │   └── {Controller}.java        # REST controller
│   ├── persistence/
│   │   ├── {Entity}Entity.java      # JPA entity
│   │   └── Jpa{Entity}Repository.java
│   └── events/
│       └── {EventListener}.java     # Event listeners
└── {ModuleName}.java                # Module marker
```

---

## Building and Running

### Development Build (Fast)

```bash
# Compile without tests (faster for iteration)
./mvnw clean compile

# Run application
./mvnw spring-boot:run

# Or run directly from IDE
# Main class: com.cafepos.CafePosApplication
```

### Full Build (Verification)

```bash
# Clean build with all checks
./mvnw clean verify

# This runs:
# - Clean
# - Compile
# - Test (all tests)
# - Code formatting checks (spotless)
# - Code quality checks (checkstyle, spotbugs)
```

### Build with Specific Profiles

```bash
# Development profile (logging, features)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Test profile
./mvnw test -P test

# Production build
./mvnw clean package -DskipTests -P prod
```

### Docker Build & Run

```bash
# Build Docker image
docker build -t cafe-pos-backend:dev .

# Run in container with Docker Compose
docker compose up

# Run specific service
docker compose up postgres  # Just database
docker compose up redis     # Just cache

# View logs
docker compose logs -f app

# Stop all services
docker compose down
```

### Running on Different Port

If port 8080 is in use:

```bash
# Via environment variable
PORT=8081 ./mvnw spring-boot:run

# Via application.yml override
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Via IDE (create Run Configuration with VM options)
# Set: -Dserver.port=8081
```

---

## Debugging

### IDE Debugging

#### IntelliJ IDEA

1. **Set Breakpoints**
   - Click left margin in editor to set breakpoint
   - Right-click for conditional breakpoints

2. **Start Debugger**
   - Click Run → Debug...
   - Or press Shift + F9

3. **Debug Controls**
   - Step Over (F8)
   - Step Into (F7)
   - Step Out (Shift + F8)
   - Continue (F9)
   - Evaluate Expression (Alt + F9)

#### VS Code

1. **Debug Configuration** (`.vscode/launch.json`)
   ```json
   {
     "configurations": [
       {
         "type": "java",
         "name": "Launch Application",
         "request": "launch",
         "mainClass": "com.cafepos.CafePosApplication",
         "preLaunchTask": "build",
         "console": "integratedTerminal",
         "internalConsoleOptions": "openOnFirstSessionStart"
       }
     ]
   }
   ```

2. **Start Debugging**
   - Press F5 or Run → Start Debugging

### Remote Debugging

```bash
# Start application with debug port open
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Connect IDE debugger to localhost:5005
# (IDE: Run → Attach to Process or Debug Configuration)
```

### Logging

**Configuration** (`application-dev.yml`):
```yaml
logging:
  level:
    com.cafepos: DEBUG
    org.springframework: INFO
    org.springframework.security: DEBUG
    org.hibernate: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

**In Code:**
```java
private static final Logger log = LoggerFactory.getLogger(MyService.class);

log.debug("Detailed debug message");
log.info("Normal information");
log.warn("Warning: something unexpected");
log.error("Error: operation failed", exception);
```

### HTTP Debugging

**Using REST Client Plugin (VS Code):**

Create `.http` files to test endpoints:

```http
### Create Order
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
Authorization: Bearer eyJhbGc...

{
  "items": [
    {
      "productId": "123e4567-e89b-12d3-a456-426614174000",
      "quantity": 2
    }
  ]
}

### Get Order
GET http://localhost:8080/api/v1/orders/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer eyJhbGc...
```

**Using cURL:**

```bash
# Create order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"items": [{"productId": "...", "quantity": 1}]}'

# Get order
curl http://localhost:8080/api/v1/orders/123 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Database Management

### Accessing PostgreSQL

```bash
# Via Docker Compose
docker compose exec postgres psql -U cafepos -d cafepos

# Via local psql (if installed)
psql -h localhost -U cafepos -d cafepos

# Connection string
postgresql://cafepos:secure-password@localhost:5432/cafepos
```

### Common SQL Queries

```sql
-- List all tables
\dt

-- Show table schema
\d orders

-- Count records in table
SELECT COUNT(*) FROM orders;

-- View recent orders (last 10)
SELECT id, status, created_at FROM orders ORDER BY created_at DESC LIMIT 10;

-- Check organization isolation
SELECT DISTINCT organization_id FROM orders;

-- View pending orders
SELECT id, order_number, status FROM orders WHERE status = 'DRAFT';
```

### Database Migrations

```bash
# Check migration status
./mvnw flyway:info

# Apply pending migrations
./mvnw flyway:migrate

# Validate migrations
./mvnw flyway:validate

# Create new migration
# 1. Create file: src/main/resources/db/migration/V{number}__{description}.sql
# 2. Add migration SQL
# 3. Restart application (migrations run automatically)
```

### Database Reset (Development Only)

```bash
# Delete and recreate database
docker compose down -v  # Remove volumes
docker compose up -d    # Recreate fresh database

# Or manually in psql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

# Flyway will re-run all migrations on next startup
```

---

## Common Development Tasks

### Adding a New Feature

**Example: Add "discount reason" field to Order**

1. **Update Domain Model**
   ```java
   // ordering/domain/Order.java
   public class Order {
       private String discountReason;  // Add field
       
       public void applyDiscount(Money amount, String reason) {
           if (amount.isNegative()) {
               throw new InvalidDiscountException();
           }
           this.total = total.subtract(amount);
           this.discountReason = reason;  // Set reason
       }
   }
   ```

2. **Create Migration**
   ```sql
   -- src/main/resources/db/migration/V30__add_discount_reason_to_orders.sql
   ALTER TABLE orders ADD COLUMN discount_reason VARCHAR(255);
   ```

3. **Update JPA Entity**
   ```java
   // ordering/adapters/persistence/OrderEntity.java
   @Entity
   @Table(name = "orders")
   public class OrderEntity {
       @Column(name = "discount_reason")
       private String discountReason;
       // ... getters/setters
   }
   ```

4. **Update DTOs**
   ```java
   // ordering/application/dto/UpdateOrderDto.java
   public class UpdateOrderDto {
       private String discountReason;
   }
   ```

5. **Update Controller**
   ```java
   // ordering/adapters/api/OrderController.java
   @PatchMapping("/{id}/discount")
   public ResponseEntity<OrderResponse> applyDiscount(
           @PathVariable UUID id,
           @RequestBody ApplyDiscountRequest request) {
       service.applyDiscount(id, request.getAmount(), request.getReason());
       // ...
   }
   ```

6. **Add Tests**
   ```java
   class OrderApplicationServiceTest {
       @Test
       void shouldApplyDiscountWithReason() {
           var order = createTestOrder();
           service.applyDiscount(order.getId(), Money.of(100), "VIP");
           
           var updated = repository.findById(order.getId()).orElseThrow();
           assertThat(updated.getDiscountReason()).isEqualTo("VIP");
       }
   }
   ```

7. **Verify Build**
   ```bash
   ./mvnw clean verify
   ```

8. **Commit**
   ```bash
   git add .
   git commit -m "feat(ordering): add discount reason field to orders

   - Add discountReason field to Order aggregate
   - Add Flyway V30 migration for database schema
   - Update OrderEntity JPA mapping
   - Add applyDiscount use case with reason
   - Include comprehensive test coverage"
   ```

### Updating Dependencies

```bash
# Check for updates
./mvnw versions:display-dependency-updates

# Update specific dependency
./mvnw versions:use-latest-versions

# Update Spring Boot parent
# Edit pom.xml: <version>3.x.x</version> in parent section

# Verify build still works
./mvnw clean verify
```

### Code Formatting

```bash
# Check formatting (without changes)
./mvnw spotless:check

# Apply formatting
./mvnw spotless:apply

# Format specific module
./mvnw -pl com.cafepos:ordering spotless:apply
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=OrderApplicationServiceTest

# Specific test method
./mvnw test -Dtest=OrderApplicationServiceTest#testCreateOrder

# Tests in module
./mvnw test -pl com.cafepos:ordering

# With coverage
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

---

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
PORT=8081 ./mvnw spring-boot:run
```

### Database Connection Failed

```bash
# Check if PostgreSQL is running
docker compose ps

# View logs
docker compose logs postgres

# Restart database
docker compose restart postgres

# Verify connection
docker compose exec postgres psql -U cafepos -d cafepos -c "SELECT 1"
```

### Maven Build Slow

```bash
# Use offline mode (requires initial download)
./mvnw -o clean compile

# Skip tests for faster build
./mvnw clean compile -DskipTests

# Increase memory
export MAVEN_OPTS="-Xmx2048m -XX:+UseG1GC"

# Build in parallel (modules)
./mvnw -T 1C clean verify
```

### IDE Not Recognizing Code

```bash
# IntelliJ: Refresh Maven
Right-click pom.xml → Maven → Reimport

# VS Code: Reload window
Cmd + Shift + P → Developer: Reload Window

# Clear IDE cache
./mvnw clean
# Then close and reopen IDE
```

### Test Failures on CI but Pass Locally

```bash
# Run tests with CI environment
./mvnw clean verify -Dci=true

# Check timezone issues
TZ=UTC ./mvnw test

# Run tests sequentially (not parallel)
./mvnw test -DthreadCount=1
```

### Out of Memory Errors

```bash
# Increase JVM heap
export MAVEN_OPTS="-Xmx4096m"

# In IDE: increase heap
# IntelliJ: Help → Change Memory Settings
# VS Code: Settings → Java: Memory → Set higher limit
```

### Git Conflicts After Merging

```bash
# Resolve conflicts in files

# Then stage and commit
git add .
git commit -m "Merge branch with resolved conflicts"

# Verify build works
./mvnw clean verify
```

---

## Next Steps

- Review [CONTRIBUTING.md](CONTRIBUTING.md) for workflow and commit standards
- Check [ARCHITECTURE.md](ARCHITECTURE.md) for design patterns
- Read [README.md](README.md) for API and deployment info
- Join team communication channels for questions

Happy coding! 🚀
