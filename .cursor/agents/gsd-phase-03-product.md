---
name: gsd-phase-03-product
description: GSD Phase 3 Product — catalog, inventory reserve, MinIO, catalog cache, ProductFacade. Use for product domain. Spec C0 before any SQL.
---

You own **GSD Phase 3: Product**.

## Read first
- `.planning/phases/03-product/03-CONTEXT.md`
- `.agents/PLAN.md` § Фаза C
- `.agents/TASKS.md` C0–C8
- `.agents/CLAUDE.md` — domain [D]: no SQL/entity/mapper unless the owner said «напиши это»
- Account sample spec: `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`

## Order of work
1. **C0 spec** (blocking) — who sells, three stock buckets, reserve UPDATE, price history, MinIO key, cache key/TTL, reserve idempotency
2. C1 `02-product.sql` with `--rollback`
3. C2 `ProductEntity` moves `api/` → `entity/`
4. C3 atomic reserve, race IT
5. C4 `ProductFacade` for order (no entity leak)
6. C5 MinIO presign [I]
7. C6 CacheManager + `@EnableCaching` on catalog only [I]
8. C7 REST; `@PreAuthorize` last. Until Phase 4, enforce `canSell()` in service

## Hard rules
- No `SELECT FOR UPDATE` for stock
- Do not cache stock
- ArchUnit: product internals hidden; entity not in `api/`
- Follow account patterns: Instant, rich entity, Liquibase style
- `AccountView.canSell()` is the seller gate

## Blocked on
Phase 2.1 is not a code blocker. Phase 4 is not a code blocker for Facade; it is a blocker for JWT role SELLER.
