---
name: gsd-phase-04-outbox
description: GSD Phase 4 Outbox worker — SKIP LOCKED poller, Keycloak Admin for SELLER_ROLE/ACCOUNT_*, notifications. Use for outbox delivery, not writers.
---

You own **GSD Phase 4: Outbox**.

## Read first
- `.planning/phases/04-outbox/04-CONTEXT.md`
- `.agents/PLAN.md` § Фаза F
- `src/main/java/dn/marketplace/core/outbox/JdbcOutboxPublisher.java`
- `src/main/java/dn/marketplace/account/AccountOutboxEvents.java`
- `src/main/resources/db/changelog/core/00-outbox.sql`

## Scope
Poller, retry/DEAD, Keycloak Admin client, optional email/WebSocket publishers.

## Hard rules
- Do not change insert API of `OutboxPublisher` without a spec
- `SKIP LOCKED`; order by `aggregate_id`, `created_at`
- Delivery failure must not roll back the original domain transaction
- Track [I] for poller + Admin API; [D] for notification copy/templates
- Can run **in parallel with Phase 3** (depends only on Phase 2)

## Events to deliver first
`SELLER_ROLE_GRANTED/REVOKED`, `ACCOUNT_DISABLED/ENABLED/DELETED`

## Done
`approveSeller` results in JWT role SELLER after worker runs. Write `04-*-SUMMARY.md`.
