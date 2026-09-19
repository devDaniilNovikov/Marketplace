# Phase 1: Infra - Context

**Gathered:** 2026-09-16
**Status:** Complete

<domain>
## Phase Boundary

Инфраструктура монолита без доменной логики account/product/order.
</domain>

<decisions>
## Implementation Decisions

- **D-01:** Полный стек сразу в compose (решение №3)
- **D-02:** Креды в yml = compose (№7)
- **D-03:** ArchUnit рано, freeze для техдолга (№2)
- **D-04:** Docker API 1.44 в тестах
</decisions>

<canonical_refs>
## Canonical References

- `.agents/MEMORY.md` — решения 1–8
- `.agents/TASKS.md` — Фаза A
- `.planning/codebase/STACK.md`
</canonical_refs>

<code_context>
## Existing Code Insights

Образец для следующих фаз: `core/outbox`, `core/security`, `AbstractIntegrationTest`.
</code_context>

<deferred>
## Deferred Ideas

CacheManager — Phase 3. Outbox worker — Phase 4.
</deferred>
