# ADR-0001: Partial DDD в модульном монолите

**Status:** Proposed  
**Date:** 2026-09-16  
**Deciders:** владелец домена (гибрид [D]/[I])

## Context

Marketplace — HighLoad-монолит: покупатель оформляет заказ, продавец выставляет товар после проверки. Account уже с rich entity и Facade. Дальше — Product (C0 до SQL), Order, Payment.

Нужно зафиксировать **уровень** DDD: иначе либо вернёмся к CRUD, либо раздуем event store / микросервисы, которые уже вне скоупа (не Kafka, не 2PC, один владелец).

## Decision Drivers

- Инварианты сделки критичны: `canTrade`, атомарный резерв, снимок цены.
- Команда из одного владельца — нет коллизий моделей между командами.
- Стек и границы уже выбраны: пакеты `account`/`product`/`order`/`core`, ArchUnit, outbox.

## Considered Options

### 1. Simple modular (анемичные сущности, правила в сервисах)
- Плюс: быстрее набросать REST.
- Минус: account так уже писали и выкинули; инварианты размазываются по сервисам.

### 2. Partial DDD (рекомендация)
- Плюс: агрегаты держат инварианты; Facade — контракт; outbox — интеграция; один деплой.
- Минус: спека (C0) обязательна до SQL; нельзя «просто таблицу».

### 3. Full DDD (event sourcing, CQRS-хранилище, саги, отдельные БД)
- Плюс: полная история, независимый деплой контекстов.
- Минус: некому это оперировать; ломает «без 2PC» и текущий outbox; откладывает core value.

## Decision

Принимаем **partial DDD** в модульном монолите.

- Bounded context = пакет фичи, не сервис.
- Core-агрегаты: rich entity, переходы методами, 422 на нарушение.
- Между контекстами только `*Facade` и outbox; entity наружу нет (ArchUnit).
- Evented-паттерны (CQRS/event store/saga) — только если инвариант нельзя удержать одной транзакцией Postgres. Сейчас такого нет.

## Consequences

**Good:** C0 Product и спека Order копируют формат account (D1–D10), не CRUD.  
**Bad:** SQL/entity без спеки запрещены — медленнее старт фазы C.  
**Mitigation:** воркфлоу DDD шаг 2 (стратегия) и шаг 4 (тактика) закрывают C0 до Liquibase.

## Related

- `.agents/MEMORY.md` решения №1, №9, D6, D9
- `docs/ddd/01-fit-and-scope.md`
- Образец: `docs/superpowers/specs/2026-09-16-account-business-rules-design.md`
