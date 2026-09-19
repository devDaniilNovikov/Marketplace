# Общий контекст агентского цикла

**Режим автопилота.** Правило «не генерировать код без явной просьбы» из `AGENTS.md`
и `.agents/CLAUDE.md` на этот прогон снято. Все остальные правила проекта действуют.

## Стек и команды

Java 25, Spring Boot 4.1, Gradle Wrapper (Kotlin DSL), один модуль.
Postgres + Liquibase, Redis, Keycloak (SSOT для identity), MinIO, Stripe.

    ./gradlew compileJava compileTestJava
    ./gradlew test -PfastTests=true    # без Docker
    ./gradlew check               # полный прогон, нужен Docker

Модуля `keycloak-spi` **не существует**. Каталога `.planning/` не существует.

## Границы

Запрещено менять из автопилота:
`.github/**`, `gradle/wrapper/**`, `AGENTS.md`, `CLAUDE.md`,
`.agents/CLAUDE.md`, `.agents/MEMORY.md`, `.agents/SCENARIOS.md`.

Миграции в `src/main/resources/db/changelog/` — только добавление новых файлов
и новых changeset. Изменение или удаление применённого changeset отклоняется merge-джобом.

Любой тест, которому нужен контейнер, обязан нести `@Tag("it")` — напрямую или
через наследование от `dn.marketplace.support.AbstractIntegrationTest`.

## Дорожки задач

`.agents/TASKS.md` размечен: **[I]** — инфраструктура, делает агент;
**[D]** — домены и бизнес-логика, делает владелец проекта.
Задача без пометки **[I]** в автопилот не берётся — вернуть её человеку.
