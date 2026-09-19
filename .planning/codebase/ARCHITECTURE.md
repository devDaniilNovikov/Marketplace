# Architecture

**Analysis Date:** 2026-09-16

## Pattern Overview

**Overall:** Модульный монолит, package-by-feature

**Key Characteristics:**
- Домены `account`, `product`, `order` + `core`
- Наружу домена — `*Facade` и `api/` (controller, dto)
- Entity/repository public, изоляция ArchUnit (решение №9)
- Синхронно: Facade; асинхронно: transactional outbox + Redis Pub/Sub

## Layers

**api:** REST, DTO, enums, события контракта (`UserUpdatedEvent`)
**service:** оркестрация, транзакции readOnly на классе
**entity:** rich model, переходы методами
**repository:** Spring Data + native INSERT ON CONFLICT
**core:** security JIT, outbox, Redis, Clock, RFC 7807

## Data Flow

**HTTP + JWT:**
1. Resource server валидирует JWT Keycloak
2. `JitProvisioningFilter` → `AccountProvisioner.provision(sub, username)`
3. Контроллер / `@PreAuthorize`
4. Сервис → entity methods → outbox в той же транзакции

**USER_UPDATED:**
1. Keycloak SPI после commit транзакции публикует JSON в Redis
2. Listener concurrencyLimit=1
3. Handler: INSERT ON CONFLICT + applyProfile, @Version

## Key Abstractions

**AccountView:** проекция для других доменов (canTrade/canSell)
**OutboxPublisher:** обязательная запись JSONB
**AccountProvisioner:** порт JIT в core, реализация в account

## Entry Points

**MarketplaceApplication:** порт 3000, `@EnableRetry/Async/Scheduling/WebSocket`
**keycloak-spi:** EventListenerProvider `marketplace-user-updated`

## Error Handling

`BusinessRuleViolationException` → 422
`ResourceNotFoundException` → 404
catch-all сохраняет заголовки ErrorResponse

---
*Architecture analysis: 2026-09-16*
