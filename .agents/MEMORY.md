# AI Agent Context Memory (HighLoad Marketplace)

## 🧠 Текущий статус (Current State)
- **Базовый пакет Java:** `dn.marketplace`
- **Текущая фаза:** Фаза 2 (Реализация домена `account`).
- **Последнее успешное действие:** Утверждена Production-Ready схема БД `01-account.sql` (V4). Разрешены конфликты Soft Delete vs UNIQUE, внедрены триггеры, разделены changeset-ы для безопасного `--rollback`.

## 🏗 Архитектурные решения (Guardrails)
- **Модульный монолит (Package-by-Feature):** Домены изолированы (`api`, `entity`, `repository`, `service`). Инкапсуляция через `package-private`. Общение через `*Facade`.
- **Даты и Время:** Исключительно `java.time.Instant` в Java и `TIMESTAMP WITH TIME ZONE` в БД (хранение строго в UTC). Использование `OffsetDateTime` запрещено.
- **Интеграция с Keycloak (SSOT):**
    - Разделение прав: Keycloak владеет авторизацией (`roles`, `email`), монолит владеет бизнес-статусом (`business_status`, `banned`).
    - **Provisioning:** Сверхбыстрый JIT (`INSERT ... ON CONFLICT DO NOTHING`) в Security-фильтре.
    - **Синхронизация:** Асинхронное обновление проекций профиля (email, имя) через консьюмер событий Keycloak.
- **Liquibase:** Жесткое правило "1 логический блок = 1 changeset со своим `--rollback`". Для триггеров/функций обязателен атрибут `splitStatements:false`.
- **Удаление (GDPR):** Soft Delete (`deleted_at`). Уникальные индексы делаются частичными (`WHERE deleted_at IS NULL`).

## 🎯 Активная задача (Next Action)
Написание Java-кода для домена `account`:
1. Создание `AccountEntity` (package-private, маппинг `Instant`, `@Version` для Optimistic Locking).
2. Создание `AccountRepository`.