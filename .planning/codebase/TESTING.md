# Testing

**Analysis Date:** 2026-09-16

## Runner

`./gradlew test` — JUnit 5. IT требуют Docker.

## Layout

| Тип | Пример | Инфра |
|-----|--------|-------|
| Unit entity | `AccountEntityTest` | нет Spring |
| WebMvc | `AccountControllerTest` | SecurityConfig + mock CacheManager |
| IT | `AccountServiceIT`, `AbstractIntegrationTest` | Postgres 17 + Redis 7 |
| Migration | `LiquibaseMigrationTest` | минимальный контекст JDBC+Liquibase |
| Arch | `ArchitectureTest` | freeze `src/test/resources/archunit_store` |

## Docker

Engine 29 отклоняет API 1.32. Клиент: `api.version=1.44` в `docker-java.properties` и `systemProperty` в Gradle.

## Gaps

- Нет Playwright/E2E браузера — REST API
- Сценарии 1–2 на compose не автоматизированы (фаза 2.1)
- Product/Order entity в api/ заморожены ArchUnit

## Commands

- Unit+IT: `./gradlew test`
- Один класс: `./gradlew test --tests 'dn.marketplace.account.entity.AccountEntityTest'`

---
*Testing analysis: 2026-09-16*
