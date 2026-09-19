# GSD phase subagents

Каждая фаза имеет агента в `.claude/agents/` и копию в `.cursor/agents/`.

| Phase | Agent | Status |
|-------|--------|--------|
| 1 Infra | `gsd-phase-01-infra` | Complete |
| 2 Account | `gsd-phase-02-account` | Complete |
| 2.1 Compose | `gsd-phase-02-account-compose` | Next |
| 3 Product | `gsd-phase-03-product` | Next after 2.1 / C0 |
| 4 Outbox | `gsd-phase-04-outbox` | Parallel with 3 |
| 5 Order | `gsd-phase-05-order` | After 3 |
| 6 Payment | `gsd-phase-06-payment` | After 5 |
| 7 Quality | `gsd-phase-07-quality` | After 3+4 |

Cursor: `@gsd-phase-03-product`. Claude/GSD: `subagent_type="gsd-phase-03-product"`.

Планирование фазы: `/gsd-plan-phase 2.1` или `/gsd-plan-phase 3`.
Тесты по SUMMARY: `/gsd-add-tests 2`.
