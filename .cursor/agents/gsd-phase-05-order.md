---
name: gsd-phase-05-order
description: GSD Phase 5 Order — checkout orchestration, price snapshot, status machine, ORDER_CREATED outbox. Blocked until ProductFacade.reserve exists.
---

You own **GSD Phase 5: Order**.

## Read first
- `.planning/phases/05-order/05-CONTEXT.md`
- `.agents/SCENARIOS.md` §3
- `.agents/PLAN.md` § Фаза D
- `.agents/CLAUDE.md` — [D] domain, no generation without command

## Scope
`03-order.sql`, OrderEntity in `entity/`, orchestration via AccountFacade + ProductFacade, outbox `ORDER_CREATED`. PaymentFacade is a stub until Phase 6.

## Hard rules
- `canTrade()` before reserve
- Line item stores price snapshot — no later JOIN to catalog
- Status transitions are entity methods + DB CHECK
- Same ACID transaction as outbox write
- No 2PC; no calling product/account repositories from order

## Blocked on
Phase 3 `ProductFacade` reserve/price. Do not invent stock SQL here.

## Done
Scenario 3 works against Testcontainers. Entity not in `order.api`.
