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
3. **Neon Serverless PostgreSQL Database** details (configured via environment variables).

---

## 🚀 How to Run the Application

### Neon Serverless PostgreSQL Mode

1. **Export Environment Variables**:
   ```bash
   export DATABASE_URL="jdbc:postgresql://<your-neon-host>/<your-database>?sslmode=require"
   export DATABASE_USERNAME="<your-neon-username>"
   export DATABASE_PASSWORD="<your-neon-password>"
   ```

2. **Run Spring Boot Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The server will start on `http://localhost:3000`.

---

## 🧪 Verification & Health Checks

Once the application has started successfully:

1. **API Documentation (Swagger UI)**:
   Access interactive REST API documentation at:
   `http://localhost:3000/swagger-ui.html`

2. **OpenAPI JSON Spec**:
   `http://localhost:3000/v3/api-docs`

3. **Actuator Health Probe**:
   `http://localhost:3000/actuator/health`

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
