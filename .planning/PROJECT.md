# Marketplace (dn.marketplace)

## What This Is

HighLoad-маркетплейс: покупатели оформляют заказы, продавцы выставляют товары после проверки админом. Модульный монолит на Spring Boot; Keycloak — SSOT аутентификации и ролей.

## Core Value

Заказ создаётся только если покупатель может торговать (`AccountView.canTrade()`), товар атомарно резервируется и цена фиксируется в позиции — без распределённых транзакций.

## Requirements

### Validated

- ✓ Схема `market_place`, Liquibase, UTC/`Instant` — Phase 1
- ✓ RFC 7807, ArchUnit freeze, Testcontainers Postgres/Redis — Phase 1
- ✓ JIT-провижининг `INSERT … ON CONFLICT DO NOTHING` — Phase 2
- ✓ Статусы продавца, бан, soft delete, outbox-запись — Phase 2
- ✓ Redis `USER_UPDATED` по порядку, SPI после коммита Keycloak — Phase 2

### Active

- [ ] Каталог и склад с атомарным резервом без `SELECT FOR UPDATE`
- [ ] Заказ со снимком цены и автоматом статусов
- [ ] Stripe escrow (`capture_method: manual`)
- [ ] Outbox-воркер `SKIP LOCKED` + Keycloak Admin API
- [ ] Сверка проекций с Keycloak (G4)

### Out of Scope

- Kafka — выбран Redis Pub/Sub (решение №5)
- Колонка `role` в `accounts` — роли только в JWT/Keycloak (решение №4)
- 2PC между доменами — только Facade + outbox
- Восстановление soft-deleted аккаунта — D5, удаления нет

## Context

Brownfield. Фазы A/B закрыты в коде (127 тестов). Источник решений: `.agents/MEMORY.md`, бэклог `.agents/TASKS.md`, порядок `.agents/PLAN.md`. Спека account: `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`.

Дорожки: **[I]** инфраструктура — агент; **[D]** домен — владелец, агент ревьюит, код домена пишет только по команде.

## Constraints

- **Stack**: Java 25, Spring Boot 4.1, PostgreSQL 17, Redis 7, Keycloak 26, MinIO — зафиксировано в compose
- **Time**: только `java.time.Instant` + `TIMESTAMPTZ` UTC
- **Schema**: `ddl-auto: validate`, Liquibase 1 changeset = 1 `--rollback`
- **Isolation**: общение доменов только через `*Facade`; ArchUnit
- **Process**: 1 задача = 1 коммит; комментарии/коммиты на русском

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Модульный монолит + ArchUnit | Изоляция без микросервисов | ✓ Good |
| Keycloak SSOT, нет `role` в БД | Роли в JWT | ✓ Good |
| Redis Pub/Sub не Kafka | Проще стек; риск fire-and-forget → G4 | ✓ Good |
| Rich entity, переходы методами | Инварианты в одном месте | ✓ Good |
| Outbox write-only в фазе B | Доставка — фаза F | — Pending |
| Подпакеты entity/repo public | Package-private не работает с mapper | ✓ Good |
| `@EnableCaching` отложен | Нет CacheManager до C6 | ✓ Good |

---
*Last updated: 2026-09-16 after GSD brownfield init*
