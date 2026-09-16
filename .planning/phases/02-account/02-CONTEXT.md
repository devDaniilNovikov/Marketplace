# Phase 2: Account - Context

**Gathered:** 2026-09-16
**Status:** Complete (код); ручной прогон — 2.1

<domain>
## Phase Boundary

Проекция аккаунта маркетплейса. Роли и профиль — Keycloak. В БД business_status, banned, snapshots, version, deleted_at.
</domain>

<decisions>
## Implementation Decisions

Скопированы D1–D10 из спеки. Дополнительно:

- Consumer serial (`concurrencyLimit=1`)
- REGISTER делает тот же JIT INSERT
- SPI `enlistAfterCompletion` + JsonSerialization
- Профиль только `applyProfile`, сеттеров нет
- `seller_applications` SMALLINT ↔ `@JdbcTypeCode`
</decisions>

<canonical_refs>
## Canonical References

- `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`
- `.agents/SCENARIOS.md` §1–2
- `.agents/MEMORY.md`
</canonical_refs>

<code_context>
## Existing Code Insights

`AccountView.canTrade/canSell` — вход для product/order. `AccountOutboxEvents` — вход для фазы 4.
</code_context>

<deferred>
## Deferred Ideas

Доставка outbox и Keycloak disable — Phase 4. Compose-проверка — 2.1.
</deferred>
