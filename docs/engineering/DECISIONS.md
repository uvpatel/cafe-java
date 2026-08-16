# Architecture Decision Records (ADR)

This file logs key architectural decisions made throughout the evolution of the `uvpatel/cafe-java` repository.

---

## ADR 001: Standardized Build Quality Gates & Automated Code Formatting

- **Status**: ACCEPTED
- **Date**: 2026-08-16
- **Context**: The repository requires strict code formatting, repeatable build checks, and automated validation across local environments and CI pipelines to support a 1,000+ commit and 200-PR engineering lifecycle without code rot.
- **Decision**:
  1. Integrate `spotless-maven-plugin` into `pom.xml` with standard Java code formatting rules (`removeUnusedImports`, `trimTrailingWhitespace`, `endWithNewline`, and 4-space indentation).
  2. Enforce `./mvnw spotless:check` in the local validation pipeline and GitHub Actions CI workflow (`.github/workflows/ci.yml`).
  3. Establish a 200-PR roadmap (`docs/engineering/ROADMAP.md`) and prioritized backlog (`docs/engineering/BACKLOG.md`).
- **Consequences**:
  - All pull requests must pass formatting checks before merging.
  - Code style remain uniform across all modules.

---

## ADR 002: Modular Monolith Architecture with Domain-Driven Scoping

- **Status**: ACCEPTED
- **Date**: 2026-08-15
- **Context**: Premature microservices introduce high operational complexity. A clean modular monolith provides strict boundaries without network latency or deployment overhead.
- **Decision**:
  - Maintain Java packages structured as hexagonal modules (`api`, `application`, `domain`, `infrastructure`).
  - Use Spring Modulith to verify inter-module dependencies and prohibit direct persistence access across modules.
- **Consequences**:
  - Highly maintainable codebase with option to extract microservices cleanly if scale requires.
