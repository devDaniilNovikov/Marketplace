---
phase: 02-account
plan: 00
subsystem: account
tags: [keycloak, jit, outbox, redis, seller]
requires:
  - phase: 01-infra
    provides: Liquibase, JWT filter port, outbox table
provides:
  - AccountEntity rich transitions
  - JIT INSERT ON CONFLICT
  - REST /api/v1/accounts
  - USER_UPDATED consumer + SPI
affects: [product, order, outbox]
requirements-completed: [ACCT-01, ACCT-02, ACCT-03, ACCT-04, ACCT-05, ACCT-06]
coverage:
  - id: D1
    description: Переходы продавца и холд 5 заявок
    requirement: ACCT-02
    verification:
      - kind: unit
        ref: dn.marketplace.account.entity.AccountEntityTest
        status: pass
    human_judgment: false
  - id: D2
    description: JIT идемпотентен, approve пишет outbox
    requirement: ACCT-01
    verification:
      - kind: integration
        ref: dn.marketplace.account.service.AccountServiceIT
        status: pass
    human_judgment: false
  - id: D3
    description: JWT → строка accounts на compose
    requirement: ACCT-07
    verification: []
    human_judgment: true
    rationale: Нужен живой Keycloak и SPI-jar (фаза 2.1)
completed: 2026-09-16
status: complete
---

# Phase 2: Account Summary

Домен account по SSOT: статусы продавца, JIT, outbox-запись, Redis USER_UPDATED. 127 тестов зелёные.

## Files Changed

- `src/main/java/dn/marketplace/account/**`
- `src/main/resources/db/changelog/domain/01-account.sql`
- `src/main/java/dn/marketplace/core/security/JitProvisioningFilter.java`
- `src/main/java/dn/marketplace/core/outbox/**`
- `keycloak-spi/**`
- `src/test/java/dn/marketplace/account/**`
- `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`

## Accomplishments

- Rich entity, D1–D10
- CRUD-черновик удалён
- SPI публикует после commit Keycloak; consumer concurrencyLimit=1
- `@EnableCaching` снят до фазы 3
