# AI Agent Context Memory (HighLoad Marketplace)

## Текущий статус
- **Базовый пакет Java:** `dn.marketplace`
- **Текущая фаза:** Фаза B (домен `account`). Фаза A (A0–A8) закрыта в коде.
- **Последнее проверенное действие:** дизайн бизнес-правил D1–D10 (`docs/superpowers/specs/2026-09-16-account-business-rules-design.md`); PR `#2` ветки `feature/phase-b-account`.

### Урок
Этот файл ведём только по факту проверенного кода. Запись «сделано» ставится после зелёной проверки, не по намерению.

На момент закрытия Фазы A (и старта B):
- `01-account.sql` был V1 (`public.account`, колонка `role`) — не совпадал с `AccountEntity`.
- `JitProvisioningFilter` и порт `AccountProvisioner` уже в `core`; `AccountFacade` был пустым.
- `OrderEntity`/`ProductEntity` — заглушки; у `ProductEntity` не было `@Id` (контекст не поднимался).

## Guardrails
- **Модульный монолит:** общение между доменами только через `*Facade`.
- **Инкапсуляция account (решение №9):** подпакеты `entity/` и `repository/` остаются **public**; изоляцию держит ArchUnit «вне домена нельзя зависеть от entity/repository/service». Package-private + подпакеты в Java несовместимы с `service` и `api.mapper`.
- **Время:** только `java.time.Instant` и `TIMESTAMP WITH TIME ZONE` UTC. `hibernate.jdbc.time_zone: UTC`.
- **Keycloak SSOT:** роли и профиль — в JWT/Keycloak; в `accounts` — `business_status`, `banned`, `*_snapshot`, `version`, `deleted_at`. Колонки `role` нет.
- **Provisioning:** `INSERT ... ON CONFLICT DO NOTHING` в Security-фильтре.
- **Liquibase:** 1 логический блок = 1 changeset со своим `--rollback`. Триггеры/функции: `splitStatements:false`.
- **GDPR:** soft delete (`deleted_at`), частичные UNIQUE `WHERE deleted_at IS NULL`.

## Решения 2026-09-05
| # | Решение |
|---|---|
| 1 | Гибрид: инфраструктура **[I]** пишет агент; домены **[D]** — владелец, агент ревьюит. |
| 2 | Чиним по ходу. ArchUnit введён рано (A7). |
| 3 | Полный стек в docker-compose. |
| 4 | Строгий SSOT Account: нет `role` в БД. |
| 5 | Redis Pub/Sub, не Kafka. |
| 6 | 1 задача = 1 коммит. Remote/PR появились позже (PR `#2`). |
| 7 | Креды в `application.yml` как есть до публикации репозитория. |
| 8 | Схема `market_place`, таблицы во множественном числе. |

## Решения 2026-09-16 (домен account, D1–D10)
| # | Решение |
|---|---|
| 9 | B1 = вариант (б): подпакеты, entity/repo public, изоляция ArchUnit. |
| D1 | Покупатели и продавцы; продавец — после проверки админом. |
| D2 | Роль `SELLER` в Keycloak — через outbox + Admin API (доставка — фаза F). |
| D3 | Отказ не финален, отзыв продавца есть. |
| D4 | `banned`: `enabled=false` в Keycloak (outbox) + `AccountView.canTrade()`. Фильтр БД не читает. |
| D5 | Soft delete + `DELETE user` в Keycloak + обнуление снапшотов. Восстановления нет. |
| D6 | Rich entity: переходы — методы сущности. |
| D7 | Отказ с обязательной причиной. |
| D8 | Не больше 5 заявок подряд, затем холд (`marketplace.account.seller-application.hold`, по умолчанию PT3M). |
| D9 | В Фазе B только запись в outbox. Воркер — фаза F. |
| D10 | `@PreAuthorize` — последний шаг REST. |

### Принятый риск (решение №5)
Redis Pub/Sub — fire-and-forget. Митигация — G4 (сверка через Keycloak Admin REST).

### Принятые решения по сценарию 2 (ревью 2026-09-16)
- Консьюмер `USER_UPDATED` потребляет канал строго по порядку (`SimpleAsyncTaskExecutor`, concurrencyLimit=1): иначе два события одного пользователя применялись бы в обратном порядке.
- `REGISTER` приходит раньше JIT — консьюмер делает тот же `INSERT … ON CONFLICT DO NOTHING`, а не игнорирует событие.
- SPI публикует после коммита транзакции Keycloak (`enlistAfterCompletion`), JSON — через `JsonSerialization` из keycloak-core; listener включён в `realm-export.json`.
- `@EnableCaching` снят до фазы I (нет `CacheManager` → контекст не стартовал).

## Next Action
Фаза B в коде закрыта, все IT на Docker зелёные (127 тестов). Осталось собрать и скопировать SPI-jar в `docker/keycloak/providers/` и вручную проверить сценарий 2 на compose-стеке.

Дальше — Фаза C (Product).
