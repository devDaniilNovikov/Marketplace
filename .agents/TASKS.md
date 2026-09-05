# Глобальный бэклог проекта (TASKS.md)

Полный реестр задач, фаз и текущий статус разработки HighLoad маркетплейса.

**Дорожки** (решение №1, см. `MEMORY.md`):
**[I]** — инфраструктура, делает ИИ-агент. **[D]** — домены и бизнес-логика, делает владелец проекта.

---

## 🚀 Статус проекта: Фаза A завершена, Фаза B в работе

### Фаза 0: Проектирование монолита [ЗАВЕРШЕНО ✅]
- [x] Утверждение архитектуры и стека (Java 25, Spring Boot 4.1)
- [x] Определение правил изоляции модулей (`api`, `entity`, `repository`, `service`)
- [x] Проектирование кросс-доменных сценариев (`SCENARIOS.md`)

### Фаза 1: Инфраструктура и ядро [ЧАСТИЧНО ⚠️]
Раньше фаза была помечена завершённой, хотя половина пунктов не работала.
Фактический статус:
- [x] Создание структуры каталогов (`dn.marketplace.*`)
- [x] Проектирование схемы Liquibase (`db.changelog-master.yaml`)
- [x] Создание SQL-миграции ядра (`00-outbox.sql`) — переработана в задаче A4
- [x] ~~Настройка `build.gradle.kts`~~ — была неполной: отсутствовали security,
      actuator, validation, spring-retry, Testcontainers. Добито в задаче A2.

### Фаза A: Инфраструктура и зелёный старт [ЗАВЕРШЕНО ✅] **[I]**
- [x] **A0** Baseline-коммит + актуализация `MEMORY.md`
- [x] **A2** Зависимости под заявленный стек; удалён дубль конструктора,
      из-за которого проект не компилировался
- [x] **A3** `application.yml`: Liquibase включён, `hibernate.jdbc.time_zone: UTC`, схема `market_place`
- [x] **A4** Liquibase-каркас: `00-schema.sql`, `00-functions.sql`, переработанный
      `00-outbox.sql`, `--rollback` в каждом changeset
- [x] **A1** `docker-compose.yml`: Postgres, Redis, Keycloak (realm-export), MinIO, Prometheus, Grafana
- [x] **A6** `GlobalExceptionHandler` на RFC 7807 вместо HTTP 200 на любую ошибку
- [x] **A5** Security-каркас: resource server, `KeycloakRoleConverter`, `JitProvisioningFilter`
- [x] **A7** ArchUnit: правила `CLAUDE.md` стали исполняемыми (FreezingArchRule)
- [x] **A8** Testcontainers-каркас + `LiquibaseMigrationTest`
- [ ] ⏳ **Прогон тестов** — блокируется незапущенным Docker-демоном

### Фаза B: Домен Account [В ПРОЦЕССЕ 🔄] **[D]**
- [ ] **B1** Переезд `entity` и `repository` из публичного `api/` в свои пакеты, package-private
- [ ] **B2** `01-account.sql` до V4 по строгому SSOT: `*_snapshot` (nullable), `business_status`,
      `banned`, `version`, `deleted_at`, частичные UNIQUE-индексы, триггер `set_updated_at`,
      `--rollback` в каждом changeset. Колонки `role` нет (решение №4).
- [ ] **B3** `AccountEntity`: package-private, `Instant`, `@Version`, без `@GeneratedValue`
      (id приходит из Keycloak), без тотального `@Setter`
- [ ] **B4** `AccountRepository`: package-private + нативный `INSERT ... ON CONFLICT DO NOTHING`
- [ ] **B5** `AccountFacade` реализует `dn.marketplace.core.security.AccountProvisioner`;
      наружу отдаёт record-проекции, не Entity
- [ ] **B6** DTO: убрать `AccountEntity` из `AccountListResponse` и `AccountMapResponse`
- [ ] **B7** Контроллер: явные `@RequestParam`, `@Max(100)` на pageSize, `@PreAuthorize`, `/api/v1`
- [ ] **B8.1** Redis-консьюмер `USER_UPDATED` -> обновление проекций с `@Version` **[D]**
- [ ] **B8.2** Keycloak SPI EventListener, публикующий в Redis **[I]**

### Фаза C: Product & Inventory [ПЛАН 📋]
- [ ] `02-product.sql`: каталог, история цен, склад с `available`/`reserved`/`quarantine` **[D]**
- [ ] `ProductEntity` без `@Id` — чинится здесь **[D]**
- [ ] `ProductFacade`: цены и атомарный резерв без `SELECT FOR UPDATE` **[D]**
- [ ] MinIO для картинок + presigned URL **[I]**
- [ ] Redis-кэш каталога: `@EnableCaching` стоит, но `CacheManager` не настроен **[I]**

### Фаза D: Order & State Machine [ПЛАН 📋] **[D]**
- [ ] `03-order.sql`: заказы и позиции со снимком цены на момент заказа
- [ ] Автомат статусов: валидация переходов в Java и `CHECK` в БД
- [ ] Оркестрация сценария 3 + запись в `outbox_messages` в той же транзакции
- [ ] `OrderEntity` без `@Id` и `OrderServiceImpl`, не реализующий `OrderService` — чинятся здесь

### Фаза E: Payment [ПЛАН 📋] **[D]**
- [ ] `04-payment.sql`, Stripe PaymentIntent с `capture_method: manual` (эскроу)
- [ ] Идемпотентность, вебхуки с проверкой подписи, circuit breaker resilience4j

### Фаза F: Notification & Outbox worker [ПЛАН 📋]
- [ ] Поллер `SKIP LOCKED` на виртуальных потоках с retry по `next_retry_at` **[I]**
- [ ] Публикация в Redis, email, WebSocket-уведомления **[D]**

### Фаза G: Качество и Observability [ПЛАН 📋] **[I]**
- [ ] **G1** Расширить ArchUnit на все домены, обнулить freeze-стор
- [ ] **G2** Интеграционные тесты на каждый сценарий `SCENARIOS.md`
- [ ] **G3** Grafana-дашборды; проверить пиннинг виртуальных потоков на Hikari
- [ ] **G4** Сверочный Batch-job проекций через Keycloak Admin REST — митигация риска решения №5
- [ ] **G5** Нагрузочное тестирование каталога и оформления заказа

### Фаза H: Актуализация .agents [В ПРОЦЕССЕ 🔄] **[I]**
- [x] `MEMORY.md` переписан по факту, добавлена таблица решений
- [x] `TASKS.md` приведён к фактическому статусу, размечены дорожки
- [ ] `SCENARIOS.md` оборван на середине JSON-блока: дописать сценарий 3,
      добавить сценарии 4 (Payment/эскроу) и 5 (Notification/Outbox)
