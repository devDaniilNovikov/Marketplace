# Глобальный бэклог проекта (TASKS.md)

Полный реестр задач, фаз и текущий статус разработки HighLoad маркетплейса.

**Дорожки** (решение №1, см. `MEMORY.md`):
**[I]** — инфраструктура, делает ИИ-агент. **[D]** — домены и бизнес-логика, делает владелец проекта.

---

## Статус проекта: Фаза A завершена, Фаза B в работе

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
- [ ] **Прогон полного `./gradlew test`** — нужен Docker-демон

### Фаза B: Домен Account [В ПРОЦЕССЕ] **[D]**
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
- [x] **B8.1** Redis-консьюмер `USER_UPDATED`
- [x] **B8.2** Keycloak SPI (`keycloak-spi/`), jar кладётся в `docker/keycloak/providers/`

**Критерий закрытия B** (ещё не зелёный целиком — нет Docker-демона):
- [x] freeze-стор без строк про `account`
- [ ] `MarketplaceApplicationTests` / `LiquibaseMigrationTest` / `AccountServiceIT` / `JdbcOutboxPublisherTest` — нужен Docker
- [ ] JWT-запрос → строка в `market_place.accounts` (ручная проверка с compose)
- [x] `approveSeller` пишет в outbox (код + IT; IT не прогнан без Docker)

### Фаза C: Product & Inventory [ПЛАН]
После B5 (`AccountView.canSell()`).
- [ ] `02-product.sql`: каталог, история цен, склад `available`/`reserved`/`quarantine` **[D]**
- [ ] `ProductEntity` без полноценной модели — чинится здесь **[D]**
- [ ] `ProductFacade`: цены и атомарный резерв без `SELECT FOR UPDATE` **[D]**
- [ ] MinIO для картинок + presigned URL **[I]**
- [ ] Redis-кэш каталога: `CacheManager` **[I]**

### Фаза D: Order & State Machine [ПЛАН] **[D]**
После C и `OutboxPublisher`.
- [ ] `03-order.sql`: заказы и позиции со снимком цены
- [ ] Автомат статусов: Java + `CHECK` в БД
- [ ] Оркестрация сценария 3 + запись в `outbox_messages`
- [ ] Довести `OrderEntity` / `OrderServiceImpl`

### Фаза E: Payment [ПЛАН] **[D]**
- [ ] `04-payment.sql`, Stripe PaymentIntent `capture_method: manual`
- [ ] Идемпотентность, вебхуки, circuit breaker

### Фаза F: Notification & Outbox worker [ПЛАН]
После того как B пишет события.
- [ ] Поллер `SKIP LOCKED`, порядок по `aggregate_id`+`created_at` **[I]**
- [ ] Keycloak Admin API для `SELLER_ROLE_*` / `ACCOUNT_*` **[I]**
- [ ] Публикация в Redis, email, WebSocket **[D]**

### Фаза G: Качество и Observability [ПЛАН] **[I]**
- [ ] **G1** Расширить ArchUnit, обнулить freeze-стор
- [ ] **G2** Интеграционные тесты на каждый сценарий `SCENARIOS.md`
- [ ] **G3** Grafana; пиннинг виртуальных потоков на Hikari
- [ ] **G4** Сверочный Batch-job проекций через Keycloak Admin REST
- [ ] **G5** Нагрузочное тестирование

### Фаза H: Актуализация .agents [В ПРОЦЕССЕ] **[I]**
- [x] `MEMORY.md` приведён к факту Фазы B, добавлены D1–D10
- [x] `TASKS.md` приведён к факту, B6/B7 отмечены, критерий закрытия B явный
- [x] `SCENARIOS.md`: сценарий 2 — Redis; сценарий 1 — INSERT с `user_name`; сценарий 3 дописан; 4–5 после E/F
