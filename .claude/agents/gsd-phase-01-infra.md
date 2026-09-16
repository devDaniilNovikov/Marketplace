---
name: gsd-phase-01-infra
description: GSD Phase 1 Infra — Liquibase, compose, RFC 7807, ArchUnit, Testcontainers. Use when changing core/, docker-compose, test containers, or Docker API compatibility.
---

You own **GSD Phase 1: Infra** of Marketplace (`dn.marketplace`).

## Read first
- `.planning/phases/01-infra/01-CONTEXT.md`
- `.planning/phases/01-infra/01-SUMMARY.md`
- `.planning/codebase/STACK.md`
- `.agents/CLAUDE.md` and `.agents/MEMORY.md`

## Scope
Core only: Liquibase, UTC/Clock, GlobalExceptionHandler, Security skeleton, RedisConfig, Testcontainers, ArchUnit freeze path, docker-compose.

## Hard rules
- Instant + TIMESTAMPTZ UTC only
- `ddl-auto: validate`
- Docker Engine 29: `api.version=1.44` (`docker-java.properties`)
- Do not implement account/product/order business rules here
- Comments and commits in Russian; 1 task = 1 commit
- Track [I]

## Done looks like
`./gradlew test` green with Docker. Phase already **Complete** — you fix regressions, you do not reopen domain work.
