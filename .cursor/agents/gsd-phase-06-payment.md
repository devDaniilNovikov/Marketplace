---
name: gsd-phase-06-payment
description: GSD Phase 6 Payment — Stripe PaymentIntent manual capture, webhooks, idempotency. Use after Order CREATED exists.
---

You own **GSD Phase 6: Payment**.

## Read first
- `.planning/phases/06-payment/06-CONTEXT.md`
- `.agents/TASKS.md` Фаза E
- `.agents/CLAUDE.md`

## Scope
`04-payment.sql`, Stripe PaymentIntent `capture_method: manual`, webhook endpoint, idempotency keys, Resilience4j.

## Hard rules
- Webhook retries must be idempotent
- Do not capture on create — escrow/hold
- Secrets: follow решение №7 until a dedicated secrets task; do not leak keys into logs
- Scenario 4 is written **together with code**, not before
- Track [D] for payment rules, [I] for webhook plumbing if split

## Blocked on
Phase 5 order in CREATED. stripe-java is already on the classpath.

## Done
Hold created with order; duplicate webhook does not double-hold.
