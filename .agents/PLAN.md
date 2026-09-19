# План разработки после фазы B

Источник истины по статусу — `TASKS.md`. Этот файл — порядок и зависимости, не чеклист.

Дорожки: **[I]** пишет агент, **[D]** пишет владелец (решение №1). Спека домена — до SQL и entity, как на account (D1–D10).

---

## Сейчас

Фаза B в коде закрыта: статусы продавца, JIT, outbox-запись, Redis `USER_UPDATED`, 127 тестов на Docker зелёные. `AccountView.canTrade()` / `canSell()` уже есть — сценарий 3 может читать account, не трогая его внутренности.

Не закрыто по B (без этого C на compose-стеке будет «полуживым»):

1. **B9 [I]** Собрать SPI-jar и положить в `docker/keycloak/providers/` (сейчас только `.gitkeep`). Без jar Keycloak пишет warning, сценарий 2 молчит.
2. **B10 [I+ручное]** JWT → строка в `market_place.accounts` (сценарий 1).
3. **B11 [I+ручное]** Смена профиля в Keycloak → снапшот в `accounts` (сценарий 2).

Jar в git не коммитим. Outbox-воркера нет (D9 / фаза F): `approveSeller` пишет `SELLER_ROLE_GRANTED`, но JWT роль `SELLER` ещё не появится.

---

## Почему не прыгаем в Order

Сценарий 3: `order` → `AccountFacade` + `ProductFacade` + `PaymentFacade` → outbox.

| Порт | Состояние | Блокер для D |
|---|---|---|
| `AccountView.canTrade()` | готов | — |
| `ProductFacade` цены и атомарный резерв | пустой класс | фаза C |
| `PaymentFacade` / эскроу | нет | фаза E |
| Воркер outbox | нет | фаза F (уведомления; заказ создаётся и без него) |

Order без склада и цены — зелёные тесты ни о чём. Сначала Product.

---

## Две дорожки после B

```
          ┌─ C Product [D+I] ─► D Order [D] ─► E Payment [D]
B ────────┤
          └─ F Outbox worker + Keycloak Admin [I]   (параллельно с C)
                    │
                    └─► G качество / сверка / нагрузка
```

F можно начинать сразу: B уже пишет в `outbox_messages`. Пока F нет, админ «одобрил продавца» только в БД — `@PreAuthorize("hasRole('SELLER')")` на каталоге не взлетит. Пока F не готов, REST продукта опирается на `canSell()` в сервисе, JWT-роль — последним шагом (как D10).

---

## Фаза C — Product

Контракт ещё не написан. **C0 обязателен** — иначе снова получим CRUD, который потом выкинем.

### C0 [D] Спека

Зафиксировать до кода (формат как `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`):

- Кто создаёт карточку: только `canSell()`, или ещё модерация?
- Склад: три корзины `available` / `reserved` / `quarantine` — как в `TASKS.md`, или иначе?
- Резерв **без** `SELECT FOR UPDATE`: `UPDATE … WHERE available >= :qty RETURNING` / `SKIP LOCKED` на строке склада.
- Цена: текущая + история; кто видит историю.
- Картинки: объект в MinIO, в БД — ключ, не байты.
- Кэш: ключ, TTL, что инвалидирует (запись продавца, резерв — нет).
- Идемпотентность резерва: ключ от заказа или от покупателя+товара.

Без ответов SQL не пишем. `02-product.sql` сейчас заглушка `id UUID PK`.

### Дальше по порядку

| ID | Дорожка | Суть | Критерий |
|---|---|---|---|
| C1 | [D] | `02-product.sql` по спеке, `--rollback`, `splitStatements:false` на триггерах | `LiquibaseMigrationTest` знает новые таблицы/индексы |
| C2 | [D] | Переезд `ProductEntity` из `api/` в `entity/`; rich-инварианты, не сеттеры статуса склада | freeze-стор ArchUnit теряет строку про entity в `api` для product (order останется) |
| C3 | [D] | Репозиторий: атомарный резерв/освобождение одним UPDATE | IT на гонке двух резервов: сумма не больше `available` |
| C4 | [D] | `ProductFacade`: цена, наличие, `reserve`/`release` для order. Entity наружу нет | ArchUnit `product_internals_are_hidden` |
| C5 | [I] | MinIO: бакет, presigned PUT/GET, конфиг из compose (`mp-minio`) | Тест без живого MinIO моком; ручной прогон на compose |
| C6 | [I] | `CacheManager` + `@EnableCaching`. Кэш каталога, не склада | Контекст стартует; `@Cacheable` на чтении витрины |
| C7 | [D] | REST витрина + карточка продавца. `@PreAuthorize` — последним | Как B-REST / D10 |
| C8 | [I] | Testcontainers MinIO не тащим, пока C5 не стабилен; IT склада — Postgres | `./gradlew test` зелёный |

`ProductFacade` пустой — это вход для C4, не удалять.

---

## Фаза D — Order

После C4 (`ProductFacade` резервирует). `03-order.sql` тоже заглушка.

- Снимок цены в позиции (не JOIN к каталогу после создания).
- Статусы: Java-методы сущности + `CHECK` в БД.
- Оркестрация сценария 3: `canTrade` → цена/резерв → (E) холд → `CREATED` + outbox `ORDER_CREATED`.
- До E шаг оплаты — заглушка порта `PaymentFacade`, не Stripe.

---

## Фаза E — Payment

После D с заказом в `CREATED`. Stripe PaymentIntent `capture_method: manual`, идемпотентность, вебхуки, Resilience4j. Сценарий 4 появляется вместе с кодом, не раньше.

---

## Фаза F — Outbox worker

Можно параллельно с C.

- Поллер `FOR UPDATE SKIP LOCKED`, порядок `aggregate_id` + `created_at`.
- Доставка account-событий в Keycloak Admin: `SELLER_ROLE_*`, `ACCOUNT_DISABLED/ENABLED/DELETED`.
- Каналы: Redis (уже транспорт), email, WebSocket — когда появится потребитель.
- Не доставлено ≠ откат доменной транзакции (outbox уже в той же ACID-записи). Retry / `DEAD` — в схеме outbox уже есть колонки.

---

## Фаза G — качество

После того как C и F дают сквозной контур.

- G1 обнулить freeze-стор (сейчас: Product/Order entity в `api/`).
- G2 IT на каждый сценарий `SCENARIOS.md`.
- G3 Grafana уже в compose — дашборды и пиннинг виртуальных потоков на Hikari.
- G4 сверка проекций с Keycloak Admin REST (митигация Redis fire-and-forget).
- G5 нагрузка.

---

## Следующий шаг

Не C1. Сначала **B9** (jar в providers), затем **C0** — спека product. SQL и entity продукта без C0 не пишем.
