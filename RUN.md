# Café POS Backend Execution Guide (`RUN.md`)

This guide explains how to run, test, and verify the **Café POS Backend** application locally.

---

## 📋 Prerequisites

Before running the application, ensure you have:

1. **Java 21 SDK** (LTS) installed:
   ```bash
   java -version
   ```
2. **Maven 3.8+** (bundled via `./mvnw` wrapper).
3. **PostgreSQL 16+** or Docker (for production database mode).

---

## 🚀 How to Run the Application

### Option A: Standard PostgreSQL Mode (Production & Full Feature Integration)

1. **Start PostgreSQL Infrastructure**:
   Ensure PostgreSQL is running locally on port `5432` with database `cafepos_db`, user `cafepos_user`, and password `cafepos_password` (or start via Docker Compose if Docker daemon is active):
   ```bash
   docker compose up -d postgres
   ```

2. **Run Spring Boot Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The server will start on `http://localhost:8080`.

---

### Option B: Local In-Memory Development Mode (Quick Local Start)

If local PostgreSQL or Docker is not currently running, launch the application using in-memory mode:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.datasource.url=jdbc:h2:mem:cafepos_dev;DB_CLOSE_DELAY=-1;MODE=PostgreSQL -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.datasource.username=sa -Dspring.datasource.password= -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
```

---

## 🧪 Verification & Health Checks

Once the application has started successfully:

1. **API Documentation (Swagger UI)**:
   Access interactive REST API documentation at:
   `http://localhost:8080/swagger-ui.html`

2. **OpenAPI JSON Spec**:
   `http://localhost:8080/v3/api-docs`

3. **Actuator Health Probe**:
   `http://localhost:8080/actuator/health`

---

## ⚡ Running Automated Verification & Tests

To execute the unit, integration, and architecture test suite (458+ test cases):

```bash
./mvnw test
```

To perform a complete clean build and verification package:

```bash
./mvnw clean verify
```
