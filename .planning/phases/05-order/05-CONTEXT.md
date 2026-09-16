# Phase 5: Order - Context

**Gathered:** 2026-09-16
**Status:** Blocked on Phase 3 ProductFacade

<domain>
## Phase Boundary

Оформление заказа (сценарий 3). Stripe — заглушка порта до Phase 6.
</domain>

<decisions>
## Implementation Decisions

- `canTrade()` до резерва
- Снимок цены в позиции
- Статусы: методы сущности + CHECK
- `ORDER_CREATED` в outbox в той же транзакции
- `OrderEntity` из `api/` в `entity/`
</decisions>

<canonical_refs>
## Canonical References

- `.agents/SCENARIOS.md` §3
- `.agents/PLAN.md` § Фаза D
</canonical_refs>

<code_context>
## Existing Code Insights

`03-order.sql` и `OrderEntity` — заглушки id. `OrderFacade` пустой.
</code_context>

<deferred>
Stripe — Phase 6.
</deferred>
