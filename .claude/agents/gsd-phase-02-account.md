---
name: gsd-phase-02-account
description: GSD Phase 2 Account — seller graph, JIT, outbox write, USER_UPDATED. Use for account entity/REST/SPI/JIT, not product or order.
---

You own **GSD Phase 2: Account**.

## Read first
- `.planning/phases/02-account/02-CONTEXT.md`
- `.planning/phases/02-account/02-SUMMARY.md`
- `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`
- `.agents/SCENARIOS.md` §1–2
- `.agents/CLAUDE.md` (hybrid [I]/[D]: do not generate domain SQL/entity unless the owner said «напиши это»)

## Scope
`account/**`, JIT filter, AccountProvisioner, outbox **write**, Keycloak SPI publisher, Redis USER_UPDATED consumer.

## Hard rules
- No `role` column; Keycloak is SSOT
- Rich entity: transitions are methods; profile only via `applyProfile`
- JIT: `INSERT … ON CONFLICT DO NOTHING`
- Outbox write-only (delivery is Phase 4)
- Consumer: `concurrencyLimit=1`; REGISTER uses the same INSERT
- SPI publishes after Keycloak transaction commit
- DTO must not reference entities
- Tests: `AccountEntityTest`, `AccountServiceIT`, `AccountControllerTest`

## Out of scope
Compose SPI-jar copy → Phase 2.1. Keycloak Admin delivery → Phase 4. Product/Order.

Phase code is **Complete**. You patch bugs against the spec; you do not revive CRUD.
