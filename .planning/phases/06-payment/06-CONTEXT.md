# Phase 6: Payment - Context

**Gathered:** 2026-09-16
**Status:** Blocked on Phase 5

<domain>
## Phase Boundary

Холд/capture Stripe, вебхуки, идемпотентность. Не каталог и не статусы заказа сверх оплаты.
</domain>

<decisions>
## Implementation Decisions

- PaymentIntent `capture_method: manual`
- Идемпотентность вебхуков
- Resilience4j circuit breaker (зависимость уже в проекте)
</decisions>

<canonical_refs>
## Canonical References

- `.agents/TASKS.md` Фаза E
- `build.gradle.kts` — `com.stripe:stripe-java`
</canonical_refs>

<code_context>
## Existing Code Insights

Stripe SDK подключён, доменного кода нет. `04-payment.sql` ещё не существует.
</code_context>

<deferred>
Сценарий 4 писать вместе с кодом, не раньше.
</deferred>
