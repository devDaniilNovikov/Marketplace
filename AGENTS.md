# Repository Guidelines

## Project Structure


## Package Manager and Commands

- Use the Gradle Wrapper, not a system Gradle installation.

## File-Scoped Verification

| Task | Command |
| --- | --- |
| Test one class | `./gradlew test --tests 'dn.marketplace.account.service.UserUpdatedHandlerTest'` |
| Test one method | `./gradlew test --tests 'package.ClassName.methodName'` |
| Architecture rules | `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest'` |

## Coding and Testing Conventions

- Do not expose JPA entities through APIs. Preserve domain isolation and validate it with ArchUnit.



## Commits and Pull Requests


## Commit Attribution

