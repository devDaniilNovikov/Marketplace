# Repository Guidelines

## Project Structure

- `src/main/java/dn/marketplace/` is a package-by-feature modular monolith. Domains expose cross-domain behavior through root-level `*Facade` types.
- Keep controllers and DTOs in `<domain>/api`, persistence in `<domain>/entity` and `<domain>/repository`, and business logic in `<domain>/service`.
- `src/test/java/` mirrors production packages. Migrations live in `src/main/resources/db/changelog/`; never rewrite an applied migration.
- `keycloak-spi/` is a separate Java 17 Gradle module. The main application uses Java 25.
- Local services are defined in `docker-compose.yml` and `docker/`. See `.agents/SCENARIOS.md` for cross-domain flows and `docs/` or `.planning/` for decisions and plans.

## Package Manager and Commands

- Use the Gradle Wrapper, not a system Gradle installation.
- `./gradlew bootRun` — run the Spring Boot application.
- `./gradlew compileJava` — compile sources.
- `./gradlew test` — run JUnit tests; integration tests require Docker.
- `./gradlew check` — run tests and verification tasks before a PR.
- `./gradlew :keycloak-spi:test` — test the SPI module.
- `./gradlew :keycloak-spi:installProviders` — stage SPI dependencies for Keycloak.
- `docker compose up -d` — start infrastructure.

## File-Scoped Verification

| Task | Command |
| --- | --- |
| Test one class | `./gradlew test --tests 'dn.marketplace.account.service.UserUpdatedHandlerTest'` |
| Test one method | `./gradlew test --tests 'package.ClassName.methodName'` |
| Architecture rules | `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest'` |

## Coding and Testing Conventions

- Follow nearby style: four-space indentation, `UpperCamelCase` types, `lowerCamelCase` members, and lowercase packages.
- Use `java.time.Instant` for system timestamps. Keep Keycloak authoritative for identity and use the transactional outbox for reliable asynchronous domain effects.
- Name focused tests `*Test` and integration tests `*IT`. Cover rules, failures, migrations, and module boundaries; no coverage threshold exists.
- Do not expose JPA entities through APIs. Preserve domain isolation and validate it with ArchUnit.

## Codex Workflow

- Before editing code symbols, run GitNexus upstream impact analysis. Treat `UNKNOWN` as unresolved and confirm with `rg`; warn before HIGH or CRITICAL changes.
- Before committing, run GitNexus `detect-changes --scope all`; partial or truncated output is not a clean result.
- Preserve unrelated changes and stage explicit paths only. In mentoring mode, do not generate implementation code without an explicit request.

## Commits and Pull Requests

- Follow the observed history: `feat(B): ...`, `fix(A6): ...`, `test(A8): ...`, `docs(scope): ...`, or `build(scope): ...`.
- Keep one task per commit and PR. State scope, verification, linked phase/issue, and migration or operational impact.

## Commit Attribution

- AI-authored commits must include `Co-Authored-By: Codex <noreply@openai.com>`.
