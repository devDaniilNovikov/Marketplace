# Conventions

**Analysis Date:** 2026-09-16

## Language

- Комментарии, Javadoc, коммиты — русский
- Сообщения коммитов: `тип(область): суть` (`feat(B):`, `fix(A8):`)
- 1 задача = 1 коммит

## Domain code

- Домен **[D]** пишет владелец; агент — по прямой команде (`.agents/CLAUDE.md`)
- Инфра **[I]** пишет агент
- Спека домена до SQL (урок CRUD фазы B)

## Persistence

- Liquibase formatted SQL, `--rollback`, триггеры `splitStatements:false`
- Таблицы во множественном числе, схема `market_place`
- `@Version Long` + `Persistable.isNew()` для assigned UUID
- Soft delete: `deleted_at` + частичный UNIQUE

## Time

Запрещены `LocalDateTime`, `OffsetDateTime`, `java.util.Date`, `Timestamp` (ArchUnit).

## Mapping

MapStruct в `api/mapper`, DTO не ссылаются на Entity.

---
*Conventions analysis: 2026-09-16*
