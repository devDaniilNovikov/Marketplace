---
name: gsd-phase-02-account-compose
description: GSD Phase 2.1 — copy Keycloak SPI jar, verify JWT JIT and USER_UPDATED on docker compose. Use for compose/manual scenario 1–2, not domain rewrites.
---

You own **GSD Phase 2.1: Account compose** (ACCT-07).

## Read first
- `.planning/phases/02.1-account-compose/02.1-CONTEXT.md`
- `keycloak-spi/README.md`
- `.agents/SCENARIOS.md` §1–2
- `docker-compose.yml`

## Tasks
1. `./gradlew :keycloak-spi:jar`
2. Copy `marketplace-keycloak-user-events.jar` into `docker/keycloak/providers/` (**do not git-add the jar**)
3. Restart Keycloak; if realm already imported, enable listener in Realm settings → Events
4. Manual: JWT → row in `market_place.accounts`
5. Manual: change profile in Keycloak → snapshots update

## Hard rules
- Do not change AccountEntity / REST unless a compose bug forces a minimal fix
- Redis for SPI: service `redis`, channel `keycloak.events.user`
- Track [I]

## Done
Scenarios 1 and 2 observed on compose. Then write `02.1-SUMMARY.md`.
