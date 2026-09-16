# Requirements: Marketplace

**Defined:** 2026-09-16
**Core Value:** Заказ только при canTrade + атомарный резерв + снимок цены, без 2PC

## v1 Requirements

### Core / Infra

- [x] **CORE-01**: Приложение стартует с Liquibase на схеме `market_place`, Hibernate `validate`
- [x] **CORE-02**: Ошибки API — RFC 7807 ProblemDetail
- [x] **CORE-03**: Дата/время только Instant/UTC
- [x] **CORE-04**: ArchUnit фиксирует изоляцию доменов (freeze для незакрытого техдолга)
- [x] **CORE-05**: IT на Postgres 17 и Redis 7 через Testcontainers

### Account

- [x] **ACCT-01**: JWT с валидным `sub` создаёт строку в `accounts` (JIT, без предварительного SELECT)
- [x] **ACCT-02**: Покупатель подаёт заявку на продавца; админ одобряет/отклоняет с причиной
- [x] **ACCT-03**: Не больше 5 заявок подряд, затем холд
- [x] **ACCT-04**: Бан не даёт торговать (`canTrade() == false`); Keycloak disable — через outbox
- [x] **ACCT-05**: Soft delete + обнуление снапшотов; username/email можно переиспользовать
- [x] **ACCT-06**: Смена профиля в Keycloak обновляет `*_snapshot` (USER_UPDATED)
- [ ] **ACCT-07**: SPI-jar в Keycloak providers, сценарии 1–2 проверены на compose

### Product

- [ ] **PROD-01**: Продавец (`canSell`) создаёт карточку товара
- [ ] **PROD-02**: Склад: available / reserved / quarantine
- [ ] **PROD-03**: Атомарный резерв одним UPDATE без SELECT FOR UPDATE
- [ ] **PROD-04**: Текущая цена + история
- [ ] **PROD-05**: Картинки в MinIO, в БД только ключ
- [ ] **PROD-06**: Кэш витрины в Redis; склад не кэшируется
- [ ] **PROD-07**: ProductFacade отдаёт цену/резерв без entity наружу

### Order

- [ ] **ORDR-01**: Оформление проверяет `canTrade()`
- [ ] **ORDR-02**: Позиция хранит снимок цены
- [ ] **ORDR-03**: Статусы заказа — методы сущности + CHECK в БД
- [ ] **ORDR-04**: В той же транзакции пишется `ORDER_CREATED` в outbox

### Payment

- [ ] **PAY-01**: Холд Stripe PaymentIntent `capture_method: manual`
- [ ] **PAY-02**: Идемпотентность вебхуков

### Outbox / Notification

- [ ] **OUTB-01**: Поллер `SKIP LOCKED`, порядок aggregate_id + created_at
- [ ] **OUTB-02**: Доставка SELLER_ROLE_* и ACCOUNT_* в Keycloak Admin API
- [ ] **OUTB-03**: Уведомления (email / WebSocket) по событиям заказа

### Quality

- [ ] **QUAL-01**: Freeze-стор ArchUnit обнулён (entity не в api/)
- [ ] **QUAL-02**: IT на каждый сценарий SCENARIOS.md
- [ ] **QUAL-03**: Сверка проекций с Keycloak Admin REST (G4)

## v2 Requirements

- Нагрузка (G5)
- Grafana-дашборды под виртуальные потоки (G3)
- Чат покупатель–продавец

## Out of Scope

| Feature | Reason |
|---------|--------|
| Kafka | Решение №5: Redis Pub/Sub |
| Роль в таблице accounts | SSOT Keycloak |
| Восстановление удалённого аккаунта | D5 |
| H2 в тестах | Частичные индексы / SKIP LOCKED только в Postgres |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CORE-01..05 | Phase 1 | Complete |
| ACCT-01..06 | Phase 2 | Complete |
| ACCT-07 | Phase 2.1 | Pending |
| PROD-01..07 | Phase 3 | Pending |
| OUTB-01..03 | Phase 4 | Pending |
| ORDR-01..04 | Phase 5 | Pending |
| PAY-01..02 | Phase 6 | Pending |
| QUAL-01..03 | Phase 7 | Pending |

**Coverage:**
- v1 requirements: 28 total
- Mapped to phases: 28
- Unmapped: 0

---
*Requirements defined: 2026-09-16*
*Last updated: 2026-09-16 after GSD init*
