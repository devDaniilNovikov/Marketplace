# Repository Guidelines

These are the operating instructions for AI agents working in this repository.
Both Codex and Claude Code read this file.

## Two Modes

This repository runs in two distinct modes. **Check which one applies before doing anything.**

- **Mentoring mode** — the default for interactive sessions with the owner. The project
  specification and the mentor persona live in `.agents/CLAUDE.md` (loaded via the root
  `CLAUDE.md`). In this mode you do not write implementation code unless asked to.
- **Autopilot mode** — an automated run of the agent loop (`.github/workflows/agent-*.yml`).
  Every autopilot prompt states this explicitly in its first line. In this mode **writing
  implementation code is the job**; the mentoring restriction does not apply.
  The loop, its states and its guard rails are documented in `docs/agent-loop.md`.

An autopilot prompt always overrides this file and `.agents/CLAUDE.md` on this point,
and only on this point. Everything else below applies in both modes.

## Project Structure

- `src/main/java/dn/marketplace/` is a package-by-feature modular monolith. Domains expose
  cross-domain behavior through root-level `*Facade` types.
- Keep controllers and DTOs in `<domain>/api`, persistence in `<domain>/entity` and
  `<domain>/repository`, and business logic in `<domain>/service`.
- `src/test/java/` mirrors production packages. Migrations live in
  `src/main/resources/db/changelog/`; never rewrite an applied migration.
- The build is a **single Gradle module** on Java 25. `settings.gradle.kts` declares
  `rootProject.name` only. There is **no `keycloak-spi/` module** — it is planned (task B8.2)
  but does not exist. Do not run `:keycloak-spi:*` tasks; they will fail.
- Local services are defined in `docker-compose.yml` and `docker/`. See `.agents/SCENARIOS.md`
  for cross-domain flows and `docs/` for decisions and plans. There is no `.planning/` directory.

## Package Manager and Commands

- Use the Gradle Wrapper, not a system Gradle installation.
- `docker compose up -d` — start infrastructure (Postgres, Redis, Keycloak, MinIO, Prometheus, Grafana).
- `./gradlew bootRun` — run the Spring Boot application (port 3000).
- `./gradlew compileJava compileTestJava` — compile.
- `./gradlew test -PfastTests` — tests that do not need Docker. This is the cheap gate.
- `./gradlew test` — all JUnit tests; the `it`-tagged ones start Testcontainers and need Docker.
- `./gradlew check` — full verification. **Run this before opening or updating a PR.**

## File-Scoped Verification

| Task | Command |
| --- | --- |
| Test one class | `./gradlew test --tests 'dn.marketplace.account.service.UserUpdatedHandlerTest'` |
| Test one method | `./gradlew test --tests 'package.ClassName.methodName'` |
| Architecture rules | `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest'` |

`ArchitectureTest` is static analysis and does **not** need Docker.

## Coding and Testing Conventions

- Follow nearby style: four-space indentation, `UpperCamelCase` types, `lowerCamelCase`
  members, lowercase packages. Comments and commit messages in this repository are in Russian —
  match the surrounding files.
- Use `java.time.Instant` for system timestamps. Keep Keycloak authoritative for identity and
  use the transactional outbox for reliable asynchronous domain effects.
- Name focused tests `*Test` and integration tests `*IT`. Any test that needs a container must
  carry `@Tag("it")` — either directly or by extending
  `dn.marketplace.support.AbstractIntegrationTest`, which is annotated and inherited.
  Without the tag it will run in the fast CI job and fail there for lack of Docker.
- Cover rules, failures, migrations, and module boundaries; no coverage threshold exists.
- Do not expose JPA entities through APIs. Preserve domain isolation and validate it with ArchUnit.

## Hard Rules

These are enforced by CI and by the merge guard rails, not just by convention.

- **Never modify or delete an applied Liquibase changeset.** Migrations are append-only; the
  merge job rejects any non-added change under `src/main/resources/db/changelog/`.
- **Never touch `.github/**`, `gradle/wrapper/**` or this file from an autopilot run.**
  The bot identity used by the loop deliberately lacks the `workflows` permission, so such a
  push is rejected by GitHub itself.
- Preserve unrelated changes and stage explicit paths only.
- If GitNexus tooling is available in your environment, run upstream impact analysis before
  editing code symbols and `detect-changes --scope all` before committing; treat `UNKNOWN` as
  unresolved and confirm with `rg`. If the tooling is not available — as in CI — say so in the
  PR body and proceed; do not stall waiting for it.

## Commits and Pull Requests

- Follow the observed history: `feat(B): ...`, `fix(A6): ...`, `test(A8): ...`,
  `docs(scope): ...`, `db(A4): ...`, `infra(A1): ...`, `build(scope): ...`.
- Keep one task per commit and PR.
- The PR body must state: scope, how it was verified, the linked issue (`Closes #N`), and any
  migration or operational impact.

## Commit Attribution

Whoever actually wrote the commit signs it.

- Commits authored by Codex: `Co-Authored-By: Codex <noreply@openai.com>`
- Commits authored by Claude: `Co-Authored-By: Claude <noreply@anthropic.com>`

Model names and version numbers belong only in these attribution trailers — never in commit
subjects, PR titles, PR bodies or code comments.
