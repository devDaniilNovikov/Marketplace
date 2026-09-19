# Phase 3: Product - Context

**Gathered:** 2026-09-16
**Status:** Ready for planning — **C0 спека обязательна до SQL**

<domain>
## Phase Boundary

Каталог, склад, цена, картинки, кэш витрины, ProductFacade для order. Заказ и оплата — другие фазы.
</domain>

<decisions>
## Implementation Decisions

Зафиксировано заранее (остальное — C0):

- **D-01:** Резерв без `SELECT FOR UPDATE` (один UPDATE … WHERE available >= qty)
- **D-02:** Склад: available / reserved / quarantine — подтвердить в C0
- **D-03:** Картинки в MinIO, в БД ключ
- **D-04:** Кэш витрины, не склада; здесь возвращается `@EnableCaching`
- **D-05:** REST `@PreAuthorize` последним; до фазы 4 опираться на `canSell()`
- **D-06:** `ProductEntity` уходит из `api/` в `entity/`

### Claude's Discretion

Только после C0. SQL/entity без спеки не писать.
</decisions>

<canonical_refs>
## Canonical References

- `.agents/PLAN.md` § Фаза C
- `.agents/TASKS.md` C0–C8
- `.agents/CLAUDE.md` — [D] не генерировать домен без команды
- Образец спеки: `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`
</canonical_refs>

<code_context>
## Existing Code Insights

- Заглушка `02-product.sql` (`id UUID PK`)
- `ProductEntity` в `api/`, пустые Facade/Service
- `AccountView.canSell()` готов
- MinIO в compose: порты 9000/9001
</code_context>

<deferred>
Order — Phase 5. Outbox delivery — Phase 4.
</deferred>
