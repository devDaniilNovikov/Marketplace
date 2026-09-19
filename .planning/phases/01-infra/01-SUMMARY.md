---
phase: 01-infra
plan: 00
subsystem: infra
tags: [liquibase, testcontainers, archunit, rfc7807]
requires: []
provides:
  - docker-compose полный стек
  - Liquibase core + outbox
  - GlobalExceptionHandler
  - Security JWT каркас
  - Testcontainers + ArchUnit freeze
affects: [account, product, order]
requirements-completed: [CORE-01, CORE-02, CORE-03, CORE-04, CORE-05]
coverage:
  - id: D1
    description: Liquibase создаёт схему market_place и outbox
    requirement: CORE-01
    verification:
      - kind: integration
        ref: dn.marketplace.db.LiquibaseMigrationTest
        status: pass
    human_judgment: false
  - id: D2
    description: RFC 7807 catch-all пробрасывает заголовки
    requirement: CORE-02
    verification:
      - kind: unit
        ref: dn.marketplace.core.GlobalExceptionHandlerTest
        status: pass
    human_judgment: false
completed: 2026-09-16
status: complete
---

# Phase 1: Infra Summary

Ядро монолита поднимается: схема, UTC, outbox-таблица, security-каркас, 127 тестов на Docker.

## Files Changed

- `docker-compose.yml`
- `src/main/resources/application.yml`
- `src/main/resources/db/changelog/**`
- `src/main/java/dn/marketplace/core/**`
- `src/test/java/dn/marketplace/db/LiquibaseMigrationTest.java`
- `src/test/java/dn/marketplace/architecture/ArchitectureTest.java`
- `src/test/resources/docker-java.properties`
- `build.gradle.kts`

## Accomplishments

- Compose: Postgres 17, Redis 7, Keycloak 26, MinIO, Prometheus, Grafana
- `00-schema/functions/outbox.sql`
- RFC 7807, Clock UTC, JitProvisioningFilter-каркас
- Testcontainers API 1.44 для Docker 29
