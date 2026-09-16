# Сценарии взаимодействия доменов (SCENARIOS.md)

Процессы, затрагивающие два и более домена. Распределённых транзакций (2PC) нет.
Общение: синхронно через `*Facade` (чтение/агрегация) либо асинхронно через Transactional Outbox
(JSON-событие в `outbox_messages` в той же локальной ACID-транзакции).

Транспорт событий Keycloak → монолит — **Redis Pub/Sub** (решение №5). Kafka в стек не входит.

---

## 1. Инициализация аккаунта (JIT-Provisioning)

**Тип:** Синхронный (инфраструктурный, до слоя бизнес-логики)
**Узлы:** `JitProvisioningFilter` → `AccountProvisioner` / `account`

1. Клиент делает HTTP-запрос к защищённому эндпоинту с валидным JWT Keycloak.
2. Фильтр извлекает `sub` (UUID) и `preferred_username`.
3. Домен `account` выполняет
   `INSERT INTO market_place.accounts (id, user_name) VALUES (...) ON CONFLICT (id) DO NOTHING`.
4. К моменту контроллера строка с этим `id` существует. Предварительного SELECT нет.

---

## 2. Синхронизация профиля (Eventual Consistency)

**Тип:** Асинхронный
**Узлы:** `Keycloak SPI` → `Redis Pub/Sub` (`marketplace.keycloak.events-channel`) → `account`

1. Пользователь меняет email или имя в Keycloak (SSOT).
2. SPI публикует JSON `USER_UPDATED` (`accountId`, `username`, `email`, `firstName`, `lastName`).
3. Консьюмер монолита обновляет `username` и `*_snapshot`. На `deleted_at IS NOT NULL` событие игнорируется.
4. Запись с инкрементом `@Version` (защита от lost update).

Риск: Redis Pub/Sub — fire-and-forget. Митигация — задача G4.

---

## 3. Оформление заказа (Order Orchestration)

**Тип:** Синхронный оркестратор + асинхронные сайд-эффекты
**Узлы:** `order` → `AccountFacade` (canTrade), `ProductFacade` (цены/наличие), `PaymentFacade` (эскроу) → Outbox

1. Покупатель нажимает «Оформить заказ».
2. `order` проверяет `AccountView.canTrade()`; забаненный и удалённый не покупают.
3. `order` запрашивает актуальные цены и наличие через `ProductFacade` и атомарно резервирует.
4. `order` вызывает `PaymentFacade` для холдирования (Stripe Escrow, `capture_method: manual`).
5. Заказ → `CREATED`. В той же транзакции в `outbox_messages`:

```json
{
  "event_type": "ORDER_CREATED",
  "aggregate_id": "uuid-заказа",
  "payload": {
    "order_id": "uuid",
    "buyer_id": "uuid",
    "total_amount": 1500.00
  }
}
```

6. Воркер фазы F читает outbox (`SKIP LOCKED`) и публикует уведомления. Сценарии 4 (Payment)
   и 5 (Notification) описываются, когда появятся Фазы E и F — не раньше кода.
