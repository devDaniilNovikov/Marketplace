# AI Agent Context Memory (HighLoad Marketplace)

## 🧠 Текущий статус (Current State)
- **Базовый пакет Java:** `dn.marketplace`
- **Текущая фаза:** Фаза A (инфраструктура и зелёный старт), параллельно Фаза B (домен `account`).
- **Последнее успешное действие:** Baseline-коммит репозитория. До него в git было 0 коммитов.

### ⚠️ Фактическое состояние против прежних записей
Прежняя версия этого файла утверждала, что `01-account.sql` — это «Production-Ready V4 с проекциями,
soft delete и триггерами». **Это не соответствовало коду.** Фактически на момент baseline-коммита:

- `01-account.sql` — наивный V1: нет `deleted_at`, `version`, `*_snapshot`, триггеров;
  обычные `UNIQUE` вместо частичных; ни одного `--rollback` ни в одном changeset.
- Приложение **не стартует**: `AccountEntity` мапится на `market_place.accounts`
  (колонки `account_status`, `user_name`), а Liquibase создаёт `public.account`
  (колонки `email`, `role`, `first_name`). Схема `market_place` не создаётся нигде.
  При этом `liquibase.enabled: false` и `ddl-auto: validate`.
- `OrderEntity` и `ProductEntity` — `@Entity` без `@Id`.
- `@EnableRetry` без зависимости `spring-retry`; `management...prometheus` без `actuator`;
  Keycloak в `application.yml` без `spring-boot-starter-security` и `oauth2-resource-server`.
- Правила изоляции нарушены: `entity` и `repository` лежат внутри публичного `api/`,
  `AccountEntity` — `public`, DTO отдают JPA-сущности наружу, все `*Facade` — пустые классы.

**Вывод-урок:** этот файл ведём только по факту проверенного кода. Запись «сделано» ставится
после того, как соответствующая проверка из раздела «Верификация» плана прошла зелёной.

## 🏗 Архитектурные решения (Guardrails)
- **Модульный монолит (Package-by-Feature):** Домены изолированы (`api`, `entity`, `repository`, `service`).
  Инкапсуляция через `package-private`. Общение через `*Facade`.
- **Даты и Время:** Исключительно `java.time.Instant` в Java и `TIMESTAMP WITH TIME ZONE` в БД
  (хранение строго в UTC). Использование `OffsetDateTime` и `LocalDateTime` запрещено.
  Обязателен `hibernate.jdbc.time_zone: UTC`.
- **Интеграция с Keycloak (SSOT):**
    - Keycloak владеет авторизацией (`roles`, `email`), монолит владеет бизнес-статусом
      (`business_status`, `banned`).
    - **Provisioning:** Сверхбыстрый JIT (`INSERT ... ON CONFLICT DO NOTHING`) в Security-фильтре.
    - **Синхронизация:** Асинхронное обновление проекций профиля через события Keycloak.
- **Liquibase:** Жёсткое правило «1 логический блок = 1 changeset со своим `--rollback`».
  Для триггеров/функций обязателен атрибут `splitStatements:false`.
- **Удаление (GDPR):** Soft Delete (`deleted_at`). Уникальные индексы частичные (`WHERE deleted_at IS NULL`).

## 📌 Решения, зафиксированные в сессии от 2026-09-05
| # | Решение |
|---|---|
| 1 | **Гибридный режим работы.** Инфраструктура (Liquibase, docker-compose, конфиги, зависимости, Security-каркас, тесты, ArchUnit) — на ИИ-агенте **[I]**. Домены и бизнес-логика — на владельце проекта **[D]**; агент делает ревью и челленджит решения. Это уточняет правило 7 из `CLAUDE.md`: в дорожке **[I]** агент пишет код без отдельной команды. |
| 2 | **Чиним по ходу.** Отдельной фазы стабилизации нет — расхождения правятся внутри доменных задач. Контроль от повторного дрейфа — ArchUnit, введённый рано (задача A7), а не в Фазе 3. |
| 3 | **Полный стек сразу.** docker-compose поднимает Postgres, Redis, Keycloak, MinIO, Prometheus, Grafana. |
| 4 | **Строгий SSOT для Account.** Колонки `role` в БД **нет** — роли живут только в JWT. В `accounts`: `business_status`, `banned`, read-only проекции `*_snapshot`, `version`, `deleted_at`. Прежний enum `AccountStatus{ACTIVE,BLOCKED,VERIFYING}` заменяется на `business_status` + отдельный флаг `banned`. |
| 5 | **Redis Pub/Sub** — транспорт событий Keycloak → `account`. Kafka в стек **не берём**: консьюмера для неё нет, поднимать брокер без потребителя незачем. Это разрешает противоречие между `CLAUDE.md` (Redis) и `SCENARIOS.md` п.2 (Kafka/Webhook) в пользу Redis. |
| 6 | **Локальные коммиты.** Baseline-коммит + 1 задача = 1 коммит в `main`. Remote и PR пока нет — ревью по диффу в чате. Это ослабляет правило 8 из `CLAUDE.md` («1 задача = 1 коммит = 1 PR») до «1 задача = 1 коммит». |
| 7 | **Конфиги как есть.** Креды в `application.yml` не выносим в env; docker-compose поднимает Postgres с теми же кредами (`daniilnovikov`/`demo2228`, БД `postgres`). Вернуться к этому перед публикацией репозитория. |
| 8 | **Схема БД — `market_place`**, таблицы во множественном числе (`accounts`, `orders`, `products`) — под уже написанные `@Table`. Liquibase создаёт схему сам отдельным changeset. |

### ⚠️ Принятый риск (следствие решения №5)
Redis Pub/Sub — fire-and-forget: если монолит лежал в момент `USER_UPDATED`, событие теряется
навсегда и проекции разъезжаются молча. Митигация — сверочный Spring Batch job через
Keycloak Admin REST (задача G4). До её реализации риск открыт и принят сознательно.

## 🎯 Активная задача (Next Action)
**Дорожка [I]:** A2 (зависимости `build.gradle.kts`) → A3 (`application.yml`) → A4 (Liquibase-каркас)
→ A1 (docker-compose) → A8 (Testcontainers) → A6 (`GlobalExceptionHandler`) → A7 (ArchUnit) → A5 (Security).

**Дорожка [D]:** B1 (переезд `entity`/`repository` из `api/`) → B2 (`01-account.sql` до V4 по SSOT)
→ B3 (`AccountEntity`) → B4 (`AccountRepository` + JIT) → B5 (`AccountFacade`) → B6 (DTO) → B7 (контроллер).

Полный план: `/Users/daniilnovikov/.claude/plans/drifting-tinkering-wind.md`
