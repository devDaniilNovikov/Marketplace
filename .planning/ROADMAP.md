# Roadmap: Marketplace

## Overview

Инфра и account уже в коде. Дальше: закрыть SPI на compose, каталог со складом, параллельно воркер outbox, затем заказ, оплата, качество.

**Phase Numbering:** целые — плановая работа; 2.1 — ручной хвост фазы B.

## Phases

- [x] **Phase 1: Infra** - Ядро, Liquibase, security-каркас, Testcontainers
- [x] **Phase 2: Account** - SSOT account, JIT, seller graph, USER_UPDATED
- [ ] **Phase 2.1: Account compose** - SPI-jar и ручные сценарии 1–2 (INSERTED)
- [ ] **Phase 3: Product** - Каталог, склад, MinIO, кэш витрины
- [ ] **Phase 4: Outbox** - Поллер SKIP LOCKED и Keycloak Admin (параллельно с 3)
- [ ] **Phase 5: Order** - Оркестрация заказа, снимок цены
- [ ] **Phase 6: Payment** - Stripe escrow
- [ ] **Phase 7: Quality** - ArchUnit, сценарии, сверка G4

## Phase Details

### Phase 1: Infra
**Goal**: Монолит поднимается на compose, миграции применяются, тесты на настоящем Postgres
**Depends on**: Nothing
**Requirements**: CORE-01, CORE-02, CORE-03, CORE-04, CORE-05
**Success Criteria**:
  1. `./gradlew test` зелёный при запущенном Docker
  2. Схема `market_place` создаётся Liquibase
  3. JWT resource server стартует без Keycloak на IT (jwk-set-uri заглушка)
**Plans**: закрыты в Фазе A
**Agent**: `gsd-phase-01-infra`

Plans:
- [x] 01-01: docker-compose, application.yml, UTC
- [x] 01-02: Liquibase-каркас + outbox table
- [x] 01-03: GlobalExceptionHandler RFC 7807
- [x] 01-04: Security-каркас + ArchUnit + Testcontainers

### Phase 2: Account
**Goal**: Проекция аккаунта, статусы продавца, JIT, outbox-запись, Redis USER_UPDATED
**Depends on**: Phase 1
**Requirements**: ACCT-01, ACCT-02, ACCT-03, ACCT-04, ACCT-05, ACCT-06
**Success Criteria**:
  1. JWT создаёт строку в `accounts`
  2. Админ одобряет продавца — статус SELLER и запись в outbox
  3. USER_UPDATED не переставляет события местами
**Plans**: закрыты в Фазе B (код)
**Agent**: `gsd-phase-02-account`

Canonical refs:
- `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`
- `.agents/SCENARIOS.md` §1–2

Plans:
- [x] 02-01: 01-account.sql V4 + AccountEntity
- [x] 02-02: JIT + Facade + REST §6/§7
- [x] 02-03: USER_UPDATED consumer + Keycloak SPI

### Phase 2.1: Account compose (INSERTED)
**Goal**: Сценарии 1–2 работают на docker compose, не только в Testcontainers
**Depends on**: Phase 2
**Requirements**: ACCT-07
**Success Criteria**:
  1. Jar SPI лежит в `docker/keycloak/providers/`
  2. Запрос с JWT создаёт строку в `market_place.accounts`
  3. Смена профиля в Keycloak обновляет снапшот
**Plans**: 1 plan
**Agent**: `gsd-phase-02-account-compose`

Plans:
- [ ] 02.1-01: Собрать SPI-jar, положить в providers, ручной прогон сценариев 1–2

### Phase 3: Product
**Goal**: Каталог, склад, резерв без SELECT FOR UPDATE, картинки, кэш витрины
**Depends on**: Phase 2 (`AccountView.canSell()`)
**Requirements**: PROD-01, PROD-02, PROD-03, PROD-04, PROD-05, PROD-06, PROD-07
**Success Criteria**:
  1. Продавец создаёт карточку
  2. Два параллельных резерва не уходят в минус
  3. ProductFacade отдаёт цену/резерв без entity
**Plans**: TBD — сначала C0 спека
**Agent**: `gsd-phase-03-product`

Canonical refs:
- `.agents/PLAN.md` § Фаза C
- `.agents/TASKS.md` C0–C8

Plans:
- [ ] 03-01: C0 спека product (до SQL)
- [ ] 03-02: 02-product.sql + entity/repo + атомарный резерв
- [ ] 03-03: ProductFacade + REST; MinIO + CacheManager [I]

### Phase 4: Outbox
**Goal**: События из outbox доезжают до Keycloak и каналов уведомлений
**Depends on**: Phase 2 (писать события уже умеем)
**Requirements**: OUTB-01, OUTB-02, OUTB-03
**Success Criteria**:
  1. approveSeller в итоге даёт роль SELLER в JWT
  2. Падение доставки не откатывает доменную транзакцию
  3. Порядок по aggregate_id сохраняется
**Plans**: TBD
**Agent**: `gsd-phase-04-outbox`

Plans:
- [ ] 04-01: Поллер SKIP LOCKED
- [ ] 04-02: Keycloak Admin API
- [ ] 04-03: Email / WebSocket потребители

### Phase 5: Order
**Goal**: Оформление заказа по сценарию 3
**Depends on**: Phase 3 (ProductFacade.reserve)
**Requirements**: ORDR-01, ORDR-02, ORDR-03, ORDR-04
**Success Criteria**:
  1. Забаненный не оформляет заказ
  2. Позиция хранит цену на момент заказа
  3. ORDER_CREATED в outbox в той же транзакции
**Plans**: TBD
**Agent**: `gsd-phase-05-order`

Canonical refs:
- `.agents/SCENARIOS.md` §3

Plans:
- [ ] 05-01: 03-order.sql + автомат статусов
- [ ] 05-02: Оркестрация + outbox; заглушка PaymentFacade до фазы 6

### Phase 6: Payment
**Goal**: Холд денег до подтверждения заказа
**Depends on**: Phase 5
**Requirements**: PAY-01, PAY-02
**Success Criteria**:
  1. PaymentIntent manual capture создаётся при заказе
  2. Повтор вебхука идемпотентен
**Plans**: TBD
**Agent**: `gsd-phase-06-payment`

Plans:
- [ ] 06-01: 04-payment.sql + Stripe + вебхуки

### Phase 7: Quality
**Goal**: Сквозные гарантии после C+F
**Depends on**: Phase 3 и Phase 4
**Requirements**: QUAL-01, QUAL-02, QUAL-03
**Success Criteria**:
  1. Freeze-стор ArchUnit пуст
  2. Каждый сценарий SCENARIOS.md покрыт IT
  3. Сверка Keycloak ↔ accounts не расходится молча
**Plans**: TBD
**Agent**: `gsd-phase-07-quality`

Plans:
- [ ] 07-01: G1 ArchUnit
- [ ] 07-02: G2 сценарии
- [ ] 07-03: G4 сверка

## Progress

**Execution Order:**
1 → 2 → 2.1 → 3 → 4 → 5 → 6 → 7

Phase 4 (Outbox) зависит только от 2: её можно вести параллельно с 3 через workstream.

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Infra | 4/4 | Complete | 2026-09-16 |
| 2. Account | 3/3 | Complete | 2026-09-16 |
| 2.1 Account compose | 0/1 | Not started | - |
| 3. Product | 0/3 | Not started | - |
| 4. Outbox | 0/3 | Not started | - |
| 5. Order | 0/2 | Not started | - |
| 6. Payment | 0/1 | Not started | - |
| 7. Quality | 0/3 | Not started | - |
