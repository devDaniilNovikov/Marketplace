# Technology Stack

**Analysis Date:** 2026-09-16

## Languages

**Primary:**
- Java 25 — монолит `dn.marketplace` (toolchain в `build.gradle.kts`)

**Secondary:**
- Kotlin DSL — Gradle
- SQL — Liquibase formatted SQL
- JSON — realm-export Keycloak

## Runtime

**Environment:**
- JVM 25, виртуальные потоки (`spring.threads.virtual.enabled`)
- Gradle 9.5.1

## Frameworks

**Core:**
- Spring Boot 4.1 / Spring Framework 7 — webmvc, data-jpa, security oauth2 resource server, data-redis, liquibase, actuator

**Testing:**
- JUnit 5 + AssertJ + Spring Boot test
- Testcontainers 1.21.3 (Docker API 1.44: `src/test/resources/docker-java.properties`)
- ArchUnit 1.4.1 FreezingArchRule

**Build/Dev:**
- MapStruct 1.6.3 + Lombok
- Jackson 3 (`tools.jackson.databind.json.JsonMapper`)

## Key Dependencies

**Critical:**
- PostgreSQL JDBC — схема `market_place`
- Spring Data Redis — Pub/Sub USER_UPDATED
- spring-retry
- stripe-java, minio, resilience4j — подключены, домен ещё не использует

**Infrastructure:**
- Keycloak 26 (compose + SPI `keycloak-spi/`, compile Java 17)
- Redis 7, MinIO, Prometheus, Grafana — compose

## Configuration

**Environment:**
- Креды в `application.yml` совпадают с compose (решение №7)
- `KEYCLOAK_URL`, `REDIS_HOST`, `KEYCLOAK_EVENTS_CHANNEL`

**Build:**
- `build.gradle.kts` + `keycloak-spi/build.gradle.kts`

## Platform Requirements

**Development:**
- Docker Desktop Engine 29 (min API 1.40; клиент тестов 1.44)
- macOS (сокет `~/.docker/run/docker.sock`)

---
*Stack analysis: 2026-09-16*
