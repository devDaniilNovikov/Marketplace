# Бизнес-правила домена `account` — дизайн

**Дата:** 2026-09-16 · **Статус:** на ревью владельца · **Дорожка:** [D] домен, [I] инфраструктура (`core.outbox`, `core.time`)

## 1. Контекст

Домен `account` до сих пор был «транспортом данных»: CRUD по `username`/`email` без единого правила. При этом Keycloak — SSOT для аутентификации и профиля (решение №4 в `.agents/MEMORY.md`): аккаунт создаётся JIT-фильтром, `email`/имя приходят событием `USER_UPDATED`. Значит, `create`/`update` полей профиля из REST противоречат архитектуре и будут перезаписаны следующим событием.

Что у домена есть *своего* — то, что не знает Keycloak: путь «покупатель → продавец», бан, удаление по GDPR. Этот документ фиксирует эти правила как контракт для задач B2 (миграция), B3 (сущность), B5 (фасад) и последующих.

**Решения, принятые в диалоге 2026-09-16:**

| # | Решение |
|---|---|
| D1 | Роли: покупатели и продавцы; продавцом становятся после проверки админом. `business_status` — стадия этого пути. |
| D2 | Роль `SELLER` в Keycloak назначает/снимает монолит через Admin API — асинхронно, через outbox. |
| D3 | Граф статусов: отказ не финален (можно подать снова), отзыв статуса продавца есть. |
| D4 | `banned` — «всё запрещено», реализация: `enabled=false` в Keycloak через outbox + проверка флага в доменных операциях через `AccountFacade`. `JitProvisioningFilter` не читает БД. |
| D5 | Удаление: soft delete (`deleted_at`) + `DELETE user` в Keycloak через outbox + обнуление `*_snapshot`. Восстановления нет. |
| D6 | Правила живут в сущности (rich entity), сервис только оркестрирует транзакцию и outbox. |
| D7 | Отказ — с обязательной причиной. |
| D8 | Заявок — не более 5 подряд, затем холд (по умолчанию 3 минуты, настраивается). |
| D9 | В этой итерации — только запись в outbox. Воркер и Keycloak Admin-клиент — фаза F. |
| D10 | Авторизация (`@PreAuthorize`) реализуется последним шагом. |

## 2. Модель

### 2.1 `AccountEntity` (`dn.marketplace.account.entity`, задача B3)

| Поле | Тип Java | Колонка | Кто пишет |
|---|---|---|---|
| `id` | `UUID` (без `@GeneratedValue`) | `id UUID PK` | JIT (`sub` из JWT) и консьюмер `USER_UPDATED` (`REGISTER` приходит раньше первого запроса) — оба через один `INSERT … ON CONFLICT DO NOTHING`; REST не создаёт |
| `username` | `String` | `user_name VARCHAR(255) NOT NULL` | JIT (`preferred_username`), консьюмер `USER_UPDATED` |
| `businessStatus` | `BusinessStatus` | `business_status VARCHAR(32) NOT NULL DEFAULT 'BUYER'` | только методы-переходы |
| `banned` | `boolean` | `banned BOOLEAN NOT NULL DEFAULT false` | `ban()`, `unban()` |
| `emailSnapshot` | `String` | `email_snapshot VARCHAR(255) NULL` | консьюмер `USER_UPDATED`; `delete()` обнуляет |
| `firstNameSnapshot` | `String` | `first_name_snapshot VARCHAR(255) NULL` | то же (255 — лимит Keycloak) |
| `lastNameSnapshot` | `String` | `last_name_snapshot VARCHAR(255) NULL` | то же |
| `rejectionReason` | `String` | `rejection_reason TEXT NULL` | `rejectSeller(reason)`; `applyAsSeller`/`approveSeller` обнуляют |
| `sellerApplications` | `int` | `seller_applications SMALLINT NOT NULL DEFAULT 0` | `applyAsSeller` (++), `approveSeller` (=0), истечение холда (=0) |
| `sellerApplicationHoldUntil` | `Instant` | `seller_application_hold_until TIMESTAMPTZ NULL` | `applyAsSeller` |
| `version` | `Long` (`@Version`) | `version BIGINT NOT NULL DEFAULT 0` | Hibernate |
| `createdAt` | `Instant` (`insertable=false, updatable=false`) | `created_at TIMESTAMPTZ NOT NULL DEFAULT now()` | БД |
| `updatedAt` | `Instant` (`insertable=false, updatable=false`) | `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()` | триггер `set_updated_at` |
| `deletedAt` | `Instant` | `deleted_at TIMESTAMPTZ NULL` | `delete()` |

- Enum `BusinessStatus { BUYER, SELLER_PENDING, SELLER, SELLER_REJECTED }` в `dn.marketplace.account.api.enums` (публичный: нужен DTO и фасаду). Прежний `AccountStatus { ACTIVE, BLOCKED, VERIFYING }` **удаляется**.
- Lombok: `@Getter`, `@NoArgsConstructor(access = PROTECTED)`. **Никаких `@Setter`.** Профиль (`username` и три `*Snapshot`) обновляет единственный метод `applyProfile(...)` — для консьюмера `USER_UPDATED` (B8.1).
- Даты — только `java.time.Instant` (правило проекта).
- Поле `email` без суффикса (текущее) — удаляется (задача 3 плана исправлений).

### 2.2 Инварианты в БД (`01-account.sql` V4, задача B2)

```sql
CHECK (business_status IN ('BUYER','SELLER_PENDING','SELLER','SELLER_REJECTED'))
CHECK (seller_applications >= 0)
CHECK (deleted_at IS NULL OR (email_snapshot IS NULL AND first_name_snapshot IS NULL AND last_name_snapshot IS NULL))
CREATE UNIQUE INDEX ux_accounts_user_name_active ON market_place.accounts (user_name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_accounts_email_snapshot_active ON market_place.accounts (email_snapshot) WHERE deleted_at IS NULL AND email_snapshot IS NOT NULL;
CREATE INDEX ix_accounts_business_status ON market_place.accounts (business_status) WHERE deleted_at IS NULL;
```

Триггер `set_updated_at` из `00-functions.sql`. Колонки `role` нет. Каждый changeset — со своим `--rollback`. Прежняя таблица `public.account` заменяется; так как прод-данных нет, допустимо переписать changeset `market:01-account-table` (локальные БД пересоздаются `docker compose down -v`), а не наслаивать `ALTER` — это решение фиксируется в сообщении коммита B2.

## 3. Переходы

Все методы — на `AccountEntity`. Нарушение предусловия → `dn.marketplace.core.exception.BusinessRuleViolationException` с текстом на русском; `GlobalExceptionHandler` уже отдаёт 422 с `type = urn:marketplace:error:business-rule`.

```
                 applyAsSeller()                approveSeller()
   BUYER ───────────────────────► SELLER_PENDING ─────────────► SELLER
     ▲                               │      ▲                      │
     │                 rejectSeller()│      │applyAsSeller()       │ revokeSeller()
     │                               ▼      │                      │
     └───────────────────────── SELLER_REJECTED                    │
     ◄─────────────────────────────────────────────────────────────┘
```

| Метод | Предусловия (все обязательны) | Эффект | Outbox |
|---|---|---|---|
| `applyAsSeller(Instant now)` | не удалён; не забанен; статус ∈ {`BUYER`, `SELLER_REJECTED`}; холд не активен (см. 3.1) | `SELLER_PENDING`; `rejectionReason = null`; `sellerApplications++`; при `== 5` — `holdUntil = now + hold` | — |
| `approveSeller()` | не удалён; статус `SELLER_PENDING` | `SELLER`; `sellerApplications = 0`; `holdUntil = null`; `rejectionReason = null` | `SELLER_ROLE_GRANTED` |
| `rejectSeller(String reason)` | не удалён; статус `SELLER_PENDING`; `reason` не пустой | `SELLER_REJECTED`; `rejectionReason = reason` | — |
| `revokeSeller()` | не удалён; статус `SELLER` | `BUYER` | `SELLER_ROLE_REVOKED` |
| `ban()` | не удалён; `banned == false` | `banned = true`; статус не меняется | `ACCOUNT_DISABLED` |
| `unban()` | не удалён; `banned == true` | `banned = false` | `ACCOUNT_ENABLED` |
| `delete(Instant now)` | не удалён | `deletedAt = now`; все `*Snapshot = null`; статус и `banned` не меняются | `ACCOUNT_DELETED` |

Правила, общие для всех переходов:
- **Удалённый аккаунт неизменяем.** Любой метод на `deletedAt != null` → 422 «Аккаунт удалён». Сервис дополнительно не находит удалённых через `findActiveById` → 404, так что до 422 доходит только гонка.
- **Бан ортогонален статусу.** Забаненный `SELLER` остаётся `SELLER`; торговлю блокирует `AccountView.canTrade()` в других доменах. После `unban()` продавец не проходит проверку заново.
- **Забаненному нельзя подать заявку** — единственный переход, зависящий от `banned`. Админские переходы (`approve`, `reject`, `revoke`) на забаненном разрешены: модерация не должна упираться в бан.
- **Время передаётся параметром** (`Instant now`) — из бина `Clock` в сервисе. Сущность не зовёт `Instant.now()` сама, чтобы тестироваться чистым JUnit.

### 3.1 Лимит заявок (D8)

`applyAsSeller(now)`:
1. `holdUntil != null && now.isBefore(holdUntil)` → 422 «Повторная заявка возможна после {holdUntil}» (значение — в `detail`, клиент показывает таймер).
2. `holdUntil != null && !now.isBefore(holdUntil)` → холд истёк: `sellerApplications = 0`, `holdUntil = null`.
3. Остальные предусловия из таблицы.
4. `sellerApplications++`; если стало `== 5` → `holdUntil = now + hold`.

Пятая заявка проходит; шестая — только после холда. Длительность холда — свойство `account.seller-application.hold` (`Duration`, по умолчанию `PT3M`) в `application.yml`, читается сервисом (`@ConfigurationProperties`) и передаётся в метод параметром `Duration hold`. Число 5 — константа сущности `MAX_SELLER_APPLICATIONS`; выносить в конфиг не нужно.

## 4. Outbox (D2, D9)

Таблица `market_place.outbox_messages` уже существует (`00-outbox.sql`). Java-кода для неё нет — появляется в этой итерации, дорожка [I]:

```java
package dn.marketplace.core.outbox;
public interface OutboxPublisher {
    void publish(String aggregateType, UUID aggregateId, String eventType, Object payload);
}
```
Реализация `JdbcOutboxPublisher` — `INSERT` через `JdbcClient` в текущей транзакции (`@Transactional(propagation = MANDATORY)`: вызов вне транзакции — ошибка, а не тихая потеря атомарности). `payload` сериализуется общим `JsonMapper` приложения в `JSONB`. `id` — `UUID.randomUUID()` в Java (в таблице нет default).

**События домена `account`** (`aggregate_type = "account"`, `aggregate_id = account.id`, `payload = {"accountId": "<uuid>"}`):

| `event_type` | Что сделает воркер фазы F в Keycloak Admin API |
|---|---|
| `SELLER_ROLE_GRANTED` | `POST /admin/realms/{realm}/users/{id}/role-mappings/realm` с ролью `SELLER` |
| `SELLER_ROLE_REVOKED` | `DELETE` той же роли |
| `ACCOUNT_DISABLED` | `PUT /users/{id}` `{"enabled": false}` |
| `ACCOUNT_ENABLED` | `{"enabled": true}` |
| `ACCOUNT_DELETED` | `DELETE /users/{id}` |

Все операции идемпотентны на стороне Keycloak → at-least-once доставки достаточно, дедупликация не нужна.

**Ограничение для фазы F (фиксируется здесь, реализуется там):** события одного `aggregate_id` обрабатываются в порядке `created_at`. `ACCOUNT_DISABLED` → `ACCOUNT_ENABLED` в обратном порядке оставит пользователя выключенным. При одном воркере это даёт `ORDER BY created_at` + `SKIP LOCKED`; при нескольких — партиционирование по `aggregate_id`.

**Сервис** пишет событие *после* вызова метода-перехода, в той же `@Transactional`. Если `publish` бросит — откатывается и статус. Это единственная причина, по которой переход не может быть чистым методом сущности «до конца»: сущность не знает про outbox.

## 5. Фасад (задача B5)

`dn.marketplace.account.AccountFacade` — единственная публичная точка входа для других доменов. Реализует `dn.marketplace.core.security.AccountProvisioner`.

```java
public record AccountView(UUID id, String username, BusinessStatus status, boolean banned, boolean deleted) {
    public boolean canTrade() { return !banned && !deleted; }
    public boolean canSell()  { return canTrade() && status == BusinessStatus.SELLER; }
}

public Optional<AccountView> find(UUID accountId);       // и удалённые тоже — вызывающий решает по deleted
public void provision(UUID accountId, String username);  // JIT: INSERT ... ON CONFLICT (id) DO NOTHING
```

- Порт `AccountProvisioner.provision(UUID)` **расширяется** до `provision(UUID accountId, String username)`: `user_name NOT NULL`, а `preferred_username` в JWT Keycloak есть всегда. `JitProvisioningFilter` передаёт `jwt.getClaimAsString("preferred_username")`.
- `find` возвращает и удалённых: `order` должен уметь показать историю заказа с «удалённый пользователь». Торговать ему не даст `canTrade()`.
- Наружу — только `AccountView` (record), никаких `AccountEntity`.

## 6. REST API

Контроллер `AccountController`, префикс `/api/v1/accounts`. Переходы — `POST`/`DELETE` на подресурс, ответ `200` с обновлённым `AccountResponse` (клиенту нужны новый статус, `rejectionReason`, `sellerApplicationHoldUntil`); `delete` → `204`.

| Метод | Путь | Тело | Переход |
|---|---|---|---|
| `POST` | `/me/seller-application` | — | `applyAsSeller` |
| `POST` | `/{accountId}/seller-application/approve` | — | `approveSeller` |
| `POST` | `/{accountId}/seller-application/reject` | `{"reason": "…"}` (`@NotBlank`, ≤ 1000) | `rejectSeller` |
| `DELETE` | `/{accountId}/seller-status` | — | `revokeSeller` |
| `POST` | `/{accountId}/ban` | — | `ban` |
| `DELETE` | `/{accountId}/ban` | — | `unban` |
| `DELETE` | `/me` | — | `delete` (владелец) |
| `DELETE` | `/{accountId}` | — | `delete` (админ) |
| `GET` | `/me` | — | свой профиль, со снапшотами |
| `GET` | `/{accountId}` | — | как сейчас, без снапшотов |
| `GET` | `/by-username/{username}` | — | как сейчас |
| `GET` | `/by-status?status=&pageNumber=&pageSize=` | — | как сейчас, `status: BusinessStatus` |

`accountId` в `/me` берётся из `authentication.name` (= `sub`).

**DTO:**
- `AccountResponse(UUID id, String username, BusinessStatus businessStatus, boolean banned, String rejectionReason, Instant sellerApplicationHoldUntil, boolean deleted)`.
- `AccountProfileResponse` = `AccountResponse` + `emailSnapshot`, `firstNameSnapshot`, `lastNameSnapshot` — только для `GET /me`.
- `RejectSellerRequest(@NotBlank @Size(max = 1000) String reason)`.
- `AccountListResponse` — восстановить полную пагинацию (`pageNumber`, `pageSize`, `totalElements: long`, `totalPages`, `hasNext`) из `page`.

**Удаляется из текущего черновика** (коммит `f026a2c`): `POST /accounts`, `PATCH /{id}`, `GET /by-email/{email}`, `AccountRequest`, `AccountMapper.toEntity`/`toEntityList`, `AccountService.findAccountByStatus(status, List)`, `AccountRepository.findByEmail`. Основание: SSOT Keycloak (раздел 1) и «оракул существования» для поиска по email.

**Сервис** — на каждый переход метод-оркестратор:
```
@Transactional
AccountResponse approveSeller(UUID id) {
    var account = findActiveById(id);          // 404, если нет или удалён
    account.approveSeller();                   // 422 при нарушении
    outbox.publish("account", id, "SELLER_ROLE_GRANTED", Map.of("accountId", id));
    return mapper.toResponse(account);         // dirty checking сделает UPDATE, save() не нужен
}
```
Класс — `@Transactional(readOnly = true)`, write-методы переопределяют. `Clock` и `AccountProperties` инжектятся.

## 7. Авторизация (D10 — последний шаг реализации)

Механизм — существующий `@PreAuthorize` с ролями из JWT (`KeycloakRoleConverter`) и `authentication.name == sub`.

| Операция | Правило |
|---|---|
| `POST /me/seller-application`, `DELETE /me`, `GET /me` | `isAuthenticated()` — id только из токена |
| `approve`, `reject`, `DELETE seller-status` | `hasRole('ADMIN')` |
| `POST`/`DELETE /{id}/ban` | `hasRole('ADMIN') and #accountId.toString() != authentication.name` — себя забанить нельзя |
| `DELETE /{id}` | `hasRole('ADMIN')` |
| `GET /{id}` | `hasRole('ADMIN') or #accountId.toString() == authentication.name` (как сейчас) |
| `GET /by-username`, `/by-status` | `hasRole('ADMIN')` (как сейчас) |

Граничные случаи, которые закрывает домен, а не `@PreAuthorize`:
- Забаненный с живым токеном → `applyAsSeller` → 422 (предусловие). Фильтр не трогаем (D4).
- Удалённый с живым токеном → JIT `DO NOTHING` пропускает → `findActiveById` → 404. Keycloak-пользователь удалён воркером, новый токен не выдаст.

## 8. Тестирование

**Юнит (чистый JUnit, без Spring/БД) — `AccountEntityTest`:**
- Параметризованная таблица `(from, banned, method) → expected | exception` по всем 7 методам × 4 статуса × banned — полнота графа видна из таблицы.
- Лимит заявок: 5 проходят; 6-я → 422 с `holdUntil` в сообщении; после `now + hold` — проходит, счётчик = 1; `approveSeller` обнуляет.
- `delete()` обнуляет снапшоты; все методы на удалённом → 422; `ban()` на `SELLER` не меняет статус.

**Интеграция (Testcontainers, `AbstractIntegrationTest`) — `AccountServiceIT`:**
- Каждый переход с событием → ровно одна строка в `outbox_messages` (`event_type`, `aggregate_id`, `status = PENDING`); `rejectSeller` — ноль строк.
- Атомарность: `OutboxPublisher`-мок бросает → статус в БД не изменился.
- `@Version`: два параллельных `approveSeller` → один `OptimisticLockingFailureException` → 409.
- Частичный индекс: удалённый аккаунт + JIT с тем же `username` под новым `id` → успех.
- `JdbcOutboxPublisher` вне транзакции → `IllegalTransactionStateException`.
- `LiquibaseMigrationTest` — rollback каждого changeset; `MarketplaceApplicationTests` — снять `@Disabled`.

**Контроллер (`@WebMvcTest` + `spring-security-test`, сервис замокан) — `AccountControllerTest`:**
- Маршруты, коды 200/204/400/404/422, `reason` пустой → 400 с `errors.reason`.
- Авторизация: на каждую строку таблицы раздела 7 — позитив и 403.

**Не тестируется в этой итерации:** доставка в Keycloak (нет воркера), сценарий 2 (`USER_UPDATED`, задача B8.1).

## 9. Вне scope

- Воркер outbox и Keycloak Admin-клиент — фаза F [I].
- Консьюмер `USER_UPDATED` — B8.1; SPI Keycloak — B8.2 [I].
- Переезд `entity`/`repository` из `api/` — B1 (делается до B3, решение по package-private — отдельно).
- Кулдаун/лимиты на `ban`/`unban`, аудит-лог действий админа (кто и когда) — не сейчас; если понадобится — отдельная таблица `account_audit`, не поля в `accounts`.
- Уведомление продавца об одобрении/отказе — фаза F.

## 10. Порядок реализации (для плана)

1. [I] `core.time.ClockConfig` (`Clock.systemUTC()`), `core.outbox.OutboxPublisher` + `JdbcOutboxPublisher` + тест.
2. [D] `BusinessStatus`, `AccountEntity` с методами — TDD от `AccountEntityTest`.
3. [D] `01-account.sql` V4 + `LiquibaseMigrationTest`; снять `@Disabled` с `MarketplaceApplicationTests`.
4. [D] `AccountRepository.findActiveById`, JIT `INSERT … ON CONFLICT`; расширение `AccountProvisioner`.
5. [D] `AccountFacade` + `AccountView`; `JitProvisioningFilter` передаёт `username`.
6. [D] `AccountServiceImpl` — оркестраторы + outbox; `AccountServiceIT`.
7. [D] Контроллер, DTO, удаление черновых эндпоинтов; `AccountControllerTest` без авторизации.
8. [D] `@PreAuthorize` + тесты авторизации.
9. `.agents/TASKS.md`, `MEMORY.md`: зафиксировать D1–D10 и новый статус B-задач.
