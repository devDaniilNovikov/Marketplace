# Structure

**Analysis Date:** 2026-09-16

## Top-level

- `src/main/java/dn/marketplace/` — монолит
- `src/main/resources/db/changelog/` — Liquibase (`core/`, `domain/`)
- `src/test/java/dn/marketplace/` — зеркало пакетов + `support/AbstractIntegrationTest`
- `keycloak-spi/` — отдельный Gradle-модуль
- `docker/` — compose assets (keycloak realm, prometheus, grafana)
- `.agents/` — MEMORY/TASKS/PLAN/SCENARIOS/CLAUDE
- `.planning/` — GSD

## Domain layout (account — образец)

```
account/
  AccountFacade.java, AccountView.java, AccountOutboxEvents.java
  api/controller, api/dto, api/enums, api/event, api/mapper
  entity/ AccountEntity
  repository/ AccountRepository
  service/ AccountService, AccountServiceImpl, UserUpdatedHandler
  config/
```

## Stubs until later phases

- `product/api/ProductEntity` — только `@Id` (фаза 3 уедет в entity/)
- `order/api/OrderEntity` — то же (фаза 5)
- `02-product.sql`, `03-order.sql` — `id UUID PK`

## Tests

- `*Test` — unit / @WebMvcTest
- `*IT` / `AbstractIntegrationTest` — Testcontainers
- `architecture/ArchitectureTest` — ArchUnit + freeze store

---
*Structure analysis: 2026-09-16*
