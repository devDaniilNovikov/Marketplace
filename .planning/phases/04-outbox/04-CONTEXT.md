# Phase 4: Outbox - Context

**Gathered:** 2026-09-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Доставка уже записанных outbox-событий. Не менять контракт записи `OutboxPublisher`.
</domain>

<decisions>
## Implementation Decisions

- Поллер `FOR UPDATE SKIP LOCKED`, порядок `aggregate_id` + `created_at`
- Account-события → Keycloak Admin API
- Redis fire-and-forget на публикации не откатывает домен
- Колонки retry/DEAD уже в `00-outbox.sql`
</decisions>

<canonical_refs>
## Canonical References

- `.agents/PLAN.md` § Фаза F
- `src/main/java/dn/marketplace/account/AccountOutboxEvents.java`
- `src/main/resources/db/changelog/core/00-outbox.sql`
</canonical_refs>

<code_context>
## Existing Code Insights

`JdbcOutboxPublisher` пишет PENDING JSONB. Воркера нет. Можно стартовать параллельно с Phase 3.
</code_context>

<deferred>
Сценарий 5 уведомлений — вместе с кодом email/WS.
</deferred>
