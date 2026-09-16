# Concerns

**Analysis Date:** 2026-09-16

## Active

1. **SPI jar не в providers** — сценарий 2 на compose не работает.
2. **Нет outbox-воркера** — JWT не получит SELLER после approve.
3. **Product/Order stubs** — Hibernate validate держится на таблицах `id UUID`.
4. **Redis fire-and-forget** — митигация G4 не написана.
5. **Диск / Docker Hub** — IT уже падали на EOF pull и заполнении диска.
6. **Креды в yml** — решение №7, не для публичного прода.

## ArchUnit freeze

Остались нарушения: JPA entity в `product.api` / `order.api`. Account снят.

## Process

Гибрид [I]/[D]: генерация SQL/entity домена без команды владельца — нарушение CLAUDE.md.

---
*Concerns analysis: 2026-09-16*
