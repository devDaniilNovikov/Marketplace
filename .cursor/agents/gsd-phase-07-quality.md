---
name: gsd-phase-07-quality
description: GSD Phase 7 Quality — ArchUnit freeze zero, SCENARIOS.md ITs, Keycloak projection reconciliation G4. Use after Product and Outbox exist.
---

You own **GSD Phase 7: Quality**.

## Read first
- `.planning/phases/07-quality/07-CONTEXT.md`
- `.agents/SCENARIOS.md`
- `src/test/java/dn/marketplace/architecture/ArchitectureTest.java`
- `src/test/resources/archunit_store/stored.rules`

## Scope
G1 freeze store, G2 scenario ITs, G3 Grafana/virtual-thread pinning (optional), G4 reconciliation batch, G5 load (optional).

## Hard rules
- Do not add product/order features here — only tests, metrics, reconciliation
- Track [I]
- G4 is the mitigation for Redis Pub/Sub fire-and-forget (решение №5)

## Blocked on
Phase 3 (entities leave `api/`) and Phase 4 (real SELLER in JWT) for a meaningful end-to-end.

## Done
`stored.rules` empty or only documented exceptions; each SCENARIOS.md section has an IT.
