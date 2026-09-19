# Phase 7: Quality - Context

**Gathered:** 2026-09-16
**Status:** After Phase 3 + 4

<domain>
## Phase Boundary

Качество и наблюдаемость сквозного контура. Новых доменов нет.
</domain>

<decisions>
## Implementation Decisions

- G1: обнулить freeze (product/order не в api/)
- G2: IT на каждый сценарий SCENARIOS.md
- G4: сверка Keycloak Admin REST ↔ accounts
- G3/G5 — можно позже внутри этой фазы
</decisions>

<canonical_refs>
## Canonical References

- `.agents/SCENARIOS.md`
- `src/test/resources/archunit_store/stored.rules`
</canonical_refs>

<code_context>
## Existing Code Insights

Grafana/Prometheus уже в compose. Actuator `/actuator/prometheus` открыт.
</code_context>

<deferred>
Нагрузка G5 — конец фазы, не блокер витрины.
</deferred>
