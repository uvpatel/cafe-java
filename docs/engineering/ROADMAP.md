# Engineering Roadmap (200 Pull Requests)

This document tracks the target 200 pull requests for transforming `uvpatel/cafe-java` into a production-grade multi-tenant Café POS backend.

---

## Track 1: Baseline Build & Repository Hygiene (PRs 1–8)

| PR # | Workstream | Title | Status | Branch | Commits | Tests | Dependencies | Risk Level | Follow-Up Work |
|---|---|---|---|---|---|---|---|---|---|
| PR-001 | Baseline | Baseline build hygiene, Spotless formatter, CI workflow & engineering roadmap setup | OPEN | `chore/baseline-build-hygiene-and-ci-workflow` | 5 | Spotless, Surefire, Failsafe | None | LOW | Standardize code quality gates |
| PR-002 | Baseline | Enforce PMD & SpotBugs static code analysis quality gates in Maven build | OPEN | `chore/pmd-spotbugs-quality-gates` | 5 | PMD, SpotBugs, JUnit | PR-001 | LOW | Architecture rule checks |
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
