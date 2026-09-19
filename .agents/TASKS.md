# Глобальный бэклог проекта (TASKS.md)

Полный реестр задач, фаз и текущий статус разработки HighLoad маркетплейса.

**Дорожки** (решение №1, см. `MEMORY.md`):
**[I]** — инфраструктура, делает ИИ-агент. **[D]** — домены и бизнес-логика, делает владелец проекта.

Порядок и зависимости — `.agents/PLAN.md`.

---

## Статус проекта: Фаза B в коде закрыта, дальше C0 (спека Product) и F параллельно

### Фаза 0: Проектирование монолита [ЗАВЕРШЕНО]
- [x] Утверждение архитектуры и стека (Java 25, Spring Boot 4.1)
- [x] Определение правил изоляции модулей (`api`, `entity`, `repository`, `service`)
- [x] Проектирование кросс-доменных сценариев (`SCENARIOS.md`)

### Фаза 1: Инфраструктура и ядро [ЧАСТИЧНО]
- [x] Создание структуры каталогов (`dn.marketplace.*`)
- [x] Проектирование схемы Liquibase (`db.changelog-master.yaml`)
- [x] Создание SQL-миграции ядра (`00-outbox.sql`) — переработана в задаче A4
- [x] `build.gradle.kts` — добито в задаче A2

### Фаза A: Инфраструктура и зелёный старт [ЗАВЕРШЕНО] **[I]**
- [x] **A0** Baseline-коммит + актуализация `MEMORY.md`
- [x] **A2** Зависимости под заявленный стек
- [x] **A3** `application.yml`: Liquibase, UTC, схема `market_place`
- [x] **A4** Liquibase-каркас
- [x] **A1** `docker-compose.yml`
- [x] **A6** `GlobalExceptionHandler` на RFC 7807
- [x] **A5** Security-каркас
- [x] **A7** ArchUnit (FreezingArchRule)
- [x] **A8** Testcontainers-каркас + `LiquibaseMigrationTest`
- [x] **Прогон полного `./gradlew test`** — 127 тестов, Docker Engine 29, API 1.44 (`docker-java.properties`)

### Фаза B: Домен Account [КОД ЗАКРЫТ, РУЧНОЙ ПРОГОН ОТКРЫТ] **[D]**
Контракт: `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`.

- [x] **B1** Переезд `entity`/`repository` в подпакеты, **public** (решение №9). ArchUnit package-private снят.
- [x] **B2** `01-account.sql` V4: `market_place.accounts`, SSOT-поля, частичные UNIQUE, триггер, `--rollback`
- [x] **B3** `AccountEntity`: `Instant`, `@Version`, без `@GeneratedValue`, rich-переходы
- [x] **B4** `AccountRepository`: JIT `INSERT ... ON CONFLICT DO NOTHING`, `findActiveById`
- [x] **B5** `AccountFacade implements AccountProvisioner`; `AccountView`; `provision(UUID, String username)`
- [x] **B6** DTO без `AccountEntity`
- [x] **B7** Контроллер `/api/v1`
- [x] **B-REST** Эндпоинты spec §6; черновой CRUD удалён
- [x] **B-AUTH** `@PreAuthorize` по spec §7
- [x] **B8.1** Redis-консьюмер `USER_UPDATED` (порядок: `concurrencyLimit=1`)
- [x] **B8.2** Keycloak SPI (`keycloak-spi/`), listener в `realm-export.json`

**Критерий закрытия B:**
- [x] freeze-стор без строк про `account`
- [x] `MarketplaceApplicationTests` / `LiquibaseMigrationTest` / `AccountServiceIT` / `JdbcOutboxPublisherTest` зелёные на Docker
- [x] `approveSeller` пишет в outbox (код + IT)
- [ ] **B9** SPI-jar скопирован в `docker/keycloak/providers/` (локально, не в git)
- [ ] **B10** JWT-запрос → строка в `market_place.accounts` (ручная проверка, сценарий 1)
- [ ] **B11** Сценарий 2 на compose: профиль в Keycloak → `*_snapshot`

### Фаза C: Product & Inventory [СЛЕДУЮЩАЯ, после C0] 
После B5 (`AccountView.canSell()`). Заглушки: `02-product.sql` (`id` only), `ProductEntity` в `api/`, пустые `ProductFacade` / `ProductService`.

- [ ] **C0** Спека product (кто продаёт, склад, цена, картинки, кэш, резерв) **[D]** — до SQL
- [ ] **C1** `02-product.sql` по спеке: каталог, история цен, `available`/`reserved`/`quarantine` **[D]**
- [ ] **C2** `ProductEntity` в `entity/`, инварианты, TDD **[D]**
- [ ] **C3** Атомарный резерв без `SELECT FOR UPDATE` (один `UPDATE … RETURNING`) **[D]**
- [ ] **C4** `ProductFacade`: цены и резерв для order, без entity наружу **[D]**
- [ ] **C5** MinIO: бакет + presigned URL **[I]**
- [ ] **C6** Redis `CacheManager` + `@EnableCaching` на витрине (не на складе) **[I]**
- [ ] **C7** REST витрина / карточка продавца; `@PreAuthorize` последним **[D]**
- [ ] **C8** ArchUnit: product не в `api/`; `./gradlew test` зелёный **[I]**

### Фаза D: Order & State Machine [ПЛАН] **[D]**
После C4 и порта `OutboxPublisher` (уже есть).
- [ ] `03-order.sql`: заказы и позиции со снимком цены
- [ ] Автомат статусов: Java + `CHECK` в БД
- [ ] Оркестрация сценария 3 + запись в `outbox_messages`
- [ ] Довести `OrderEntity` / `OrderServiceImpl`; entity уходит из `api/`
- [ ] До E — заглушка `PaymentFacade`, не Stripe

### Фаза E: Payment [ПЛАН] **[D]**
После заказа в `CREATED`.
- [ ] `04-payment.sql`, Stripe PaymentIntent `capture_method: manual`
- [ ] Идемпотентность, вебхуки, circuit breaker
- [ ] Сценарий 4 в `SCENARIOS.md` — вместе с кодом

### Фаза F: Notification & Outbox worker [ПЛАН, параллельно с C]
B уже пишет события — воркер можно делать, не дожидаясь Product.
- [ ] Поллер `SKIP LOCKED`, порядок по `aggregate_id`+`created_at` **[I]**
- [ ] Keycloak Admin API для `SELLER_ROLE_*` / `ACCOUNT_*` **[I]**
- [ ] Публикация в Redis, email, WebSocket **[D]**
- [ ] Сценарий 5 в `SCENARIOS.md` — вместе с кодом

### Фаза G: Качество и Observability [ПЛАН] **[I]**
После сквозного контура C+F (витрина + реальная роль SELLER в JWT).
- [ ] **G1** Расширить ArchUnit, обнулить freeze-стор (остались Product/Order в `api/`)
- [ ] **G2** Интеграционные тесты на каждый сценарий `SCENARIOS.md`
- [ ] **G3** Grafana; пиннинг виртуальных потоков на Hikari
- [ ] **G4** Сверочный Batch-job проекций через Keycloak Admin REST
- [ ] **G5** Нагрузочное тестирование

### Фаза H: Актуализация .agents [В ПРОЦЕССЕ] **[I]**
- [x] `MEMORY.md` приведён к факту Фазы B, добавлены D1–D10
- [x] `TASKS.md` приведён к факту, B6/B7 отмечены, критерий закрытия B явный
- [x] `SCENARIOS.md`: сценарий 2 — Redis; сценарий 1 — INSERT с `user_name`; сценарий 3 дописан; 4–5 после E/F
- [x] `PLAN.md` — порядок C–G и параллельная дорожка F
- [ ] После C0 — вписать спеку product в `MEMORY.md` как D11+
