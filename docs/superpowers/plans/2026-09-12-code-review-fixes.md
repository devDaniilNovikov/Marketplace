# Исправления по код-ревью фазы B (Account) — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Закрыть четыре замечания `/code-review` по незакоммиченным изменениям домена `account` и одну смежную инфраструктурную проблему, найденную при их проверке, — каждое отдельным коммитом.

**Architecture:** Все правки точечные, архитектура не меняется. Единственное содержательное изменение — `GlobalExceptionHandler.handleUnexpected` начинает возвращать `ResponseEntity<ProblemDetail>` вместо голого `ProblemDetail`, чтобы пробрасывать заголовки спринговых `ErrorResponse` (`Allow`, `Accept`) и работать с любым `HttpStatusCode`, а не только с константами `HttpStatus`. Остальное — синхронизация индекса git, удаление лишнего поля сущности, фиксация инварианта уникальности и путь стора ArchUnit.

**Tech Stack:** Java 25, Spring Boot 4.1 (Spring Framework 7), JUnit 5 + AssertJ (из `spring-boot-starter-*-test`), ArchUnit 1.4.1, Gradle Kotlin DSL.

**Spec:** Отдельного spec-файла нет — источником служит отчёт `/code-review` от 2026-09-12, продублированный в разделе «Исходные замечания» ниже. Каждая задача ссылается на номер замечания.

## Global Constraints

- Комментарии, Javadoc и сообщения коммитов — на русском, стиль как в существующих коммитах: `<тип>(<задача>): <суть>` (см. `git log`).
- Правило из `.agents/MEMORY.md`, решение №6: **1 задача = 1 коммит** в `main`, remote и PR нет.
- Правило из `.agents/MEMORY.md`, решение №1: дорожка **[I]** (ядро, тесты, конфиги) — ИИ-агент пишет код сам; дорожка **[D]** (домен `account`) — владелец проекта. Задачи 3 и 4 трогают домен — они помечены **[D]**, агент выполняет их только по прямому подтверждению владельца.
- Правило из `.agents/MEMORY.md`, решение №4: в `accounts` нет колонки `role`; проекции из Keycloak называются `*_snapshot`. Поле `email` без суффикса `_snapshot` этому решению противоречит.
- Даты — только `java.time.Instant` (уже соблюдается в `GlobalExceptionHandler`, не менять).
- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate **не** проверяет `UNIQUE` при валидации, поэтому `@Column(unique = true)` на сущности документирует контракт, но не создаёт ограничение. Ограничение в БД — задача B2.
- Полный `./gradlew test` требует Docker (Testcontainers в `LiquibaseMigrationTest`). Во всех задачах тесты запускаются **с фильтром `--tests`**, чтобы не зависеть от Docker-демона.
- Каждый коммит заканчивается строками:
  ```
  Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01JgcEap37CptcQDJwk7DXFy
  ```

---

## Исходные замечания ревью (все — Low, критичных багов нет)

| # | Файл | Суть | Проверено |
|---|------|------|-----------|
| R1 | `account/api/mapper/AccountMapper.java` (индекс) | Индекс и рабочее дерево расходятся: в индексе лежат пустые `api.mapper.AccountMapper`, `core.concurrency.ConcurrencyConfig` и `AccountNotFoundException`/`AccountAlreadyExistsException`, **не** наследующие `RuntimeException`. `git commit` без `-a` даст некомпилируемый коммит. | ✅ `git show :…/AccountNotFoundException.java` → `public class AccountNotFoundException {}` |
| R2 | `core/GlobalExceptionHandler.java:181` | `HttpStatus.valueOf(int)` бросает `IllegalArgumentException` на нестандартном 4xx (например, `ResponseStatusException(HttpStatusCode.valueOf(499))`); заголовки `ErrorResponse` (`Allow` для 405, `Accept` для 415) теряются. | ✅ Код читает `HttpStatus.valueOf(errorResponse.getStatusCode().value())`, `getHeaders()` не используется |
| R3 | `account/api/repository/AccountRepository.java:16` | `Optional<AccountEntity> findByUsername` предполагает уникальность `user_name`, но ни сущность, ни миграции её не гарантируют → `IncorrectResultSizeDataAccessException` → 500. | ✅ `@Column(nullable = false, name = "user_name")` без `unique`; миграции для `market_place.accounts` нет вообще |
| R4 | `account/api/AccountEntity.java:32` | Поле `email` добавлено без changeset и нигде не используется; конфликтует с `email VARCHAR(255) NOT NULL UNIQUE` в `01-account.sql` при `ddl-auto: validate`; нарушает решение №4 (`*_snapshot`). | ✅ Поле есть в сущности, отсутствует в `AccountResponse` и в маппере |
| X1 | `src/test/resources/archunit.properties` | **Найдено при проверке R1, в отчёте ревью нет.** Комментарий в properties утверждает, что стор `FreezingArchRule` лежит в `src/test/resources/archunit_store`, но `freeze.store.default.path` не задан — стор создаётся в **корне проекта** (`./archunit_store/`, untracked, создан сегодня в 19:48). В git его нет, `.gitignore` его не исключает. На чистом клоне стор пересоздаётся с `allowStoreCreation=true` — все текущие нарушения замораживаются заново, и обещание «валит сборку на любом новом нарушении» не выполняется. | ✅ `git ls-files \| grep archunit` → только `archunit.properties` |

---

## Структура файлов

| Действие | Путь | Ответственность |
|----------|------|-----------------|
| Изменить | `src/main/java/dn/marketplace/core/GlobalExceptionHandler.java` | Трансляция исключений в RFC 7807; правится только catch-all и `problem()` |
| Создать | `src/test/java/dn/marketplace/core/GlobalExceptionHandlerTest.java` | Юнит-тесты catch-all без Spring-контекста |
| Изменить | `src/main/java/dn/marketplace/account/api/AccountEntity.java` | Удалить `email`, объявить уникальность `username` |
| Изменить | `.agents/TASKS.md` | Уточнить B2: частичный `UNIQUE` на `user_name` |
| Изменить | `src/test/resources/archunit.properties` | Явный путь стора |
| Переместить | `archunit_store/` → `src/test/resources/archunit_store/` | Список замороженных нарушений — техдолг под контролем git |

---

### Task 1: Синхронизировать индекс и закоммитить WIP фазы B (R1) **[D — коммит делает владелец или агент по подтверждению]**

Замечание R1 — процессное: код в рабочем дереве корректен, некорректен только индекс. Задача фиксирует фактическое состояние одним коммитом, чтобы дальнейшие правки ложились на чистое дерево.

**Files:**
- Stage: всё под `src/` и `build.gradle.kts` (без `archunit_store/` — он обрабатывается в Task 5)

**Interfaces:**
- Consumes: —
- Produces: чистое рабочее дерево (`git status --short` показывает только `?? archunit_store/`); последующие задачи опираются на это.

- [ ] **Step 1: Убедиться, что расхождение действительно есть**

Run: `git show :src/main/java/dn/marketplace/account/api/exception/AccountNotFoundException.java`
Expected: `public class AccountNotFoundException {` **без** `extends RuntimeException` — это то, что попало бы в коммит.

- [ ] **Step 2: Синхронизировать индекс с рабочим деревом**

```bash
git add -A -- src build.gradle.kts
git status --short
```
Expected: строк `AM`/`AD` больше нет; `api/mapper/AccountMapper.java` и `core/concurrency/ConcurrencyConfig.java` показаны как `D`; `?? archunit_store/` остаётся.

- [ ] **Step 3: Проверить, что закоммиченное состояние компилируется**

Run: `./gradlew compileJava compileTestJava --quiet`
Expected: BUILD SUCCESSFUL, без вывода ошибок.

- [ ] **Step 4: Прогнать ArchUnit (Docker не нужен)**

Run: `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest'`
Expected: BUILD SUCCESSFUL. Примечание: этот прогон пересоздаст `./archunit_store/` в корне — это ожидаемо до Task 5.

- [ ] **Step 5: Коммит**

Сообщение — предложение; владелец проекта вправе переформулировать под свою трактовку задач B6/B7.

```bash
git commit -m "$(cat <<'EOF'
feat(B6,B7): DTO без Entity, MapStruct-маппер в service, контроллер под /api/v1 с @PreAuthorize

AccountListResponse/AccountMapResponse отдают AccountResponse вместо AccountEntity.
AccountMapper переехал из api/mapper в service и стал package-private.
Доменные исключения получили @ResponseStatus, catch-all в GlobalExceptionHandler
читает аннотацию вместо перечисления типов.
Контроллер: явные @RequestParam, @Min/@Max на пагинации, @PreAuthorize, префикс /api/v1.
Удалены пустые заглушки api.mapper.AccountMapper и core.concurrency.ConcurrencyConfig.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01JgcEap37CptcQDJwk7DXFy
EOF
)"
```

---

### Task 2: `GlobalExceptionHandler` — любой `HttpStatusCode` и заголовки `ErrorResponse` (R2) **[I]**

**Files:**
- Modify: `src/main/java/dn/marketplace/core/GlobalExceptionHandler.java:140-200` (метод `handleUnexpected`, `declaredClientError`, `problem`)
- Test: `src/test/java/dn/marketplace/core/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: существующие `problem(...)`, `newCorrelationId()`, константы `TYPE_*`.
- Produces: `public ResponseEntity<ProblemDetail> handleUnexpected(Exception e)` — сигнатура меняется с `ProblemDetail` на `ResponseEntity<ProblemDetail>`; `private ProblemDetail problem(HttpStatusCode status, String title, String detail, URI type)` — параметр `HttpStatus` расширен до `HttpStatusCode` (все существующие вызовы с `HttpStatus.*` компилируются без изменений, т.к. `HttpStatus implements HttpStatusCode`).

- [ ] **Step 1: Написать падающий тест**

Создать `src/test/java/dn/marketplace/core/GlobalExceptionHandlerTest.java`:

```java
package dn.marketplace.core;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Catch-all хендлер проверяется без Spring-контекста: всё, что здесь важно,
 * решается внутри одного метода, а поднимать MVC ради четырёх ассертов дорого.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** Локальный аналог доменного исключения: ядро не должно зависеть от account даже в тестах. */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class DomainNotFound extends RuntimeException {
        DomainNotFound(String message) {
            super(message);
        }
    }

    @Test
    void errorResponse_сохраняет_статус_и_заголовки() {
        var e = new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        // Allow — часть контракта 405 по RFC 9110, его нельзя терять
        assertThat(response.getHeaders().get(HttpHeaders.ALLOW)).containsExactly("GET");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(405);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void errorResponse_с_нестандартным_кодом_не_бросает_исключение() {
        var e = new ResponseStatusException(HttpStatusCode.valueOf(499), "нестандартный код");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(e);

        assertThat(response.getStatusCode().value()).isEqualTo(499);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("нестандартный код");
    }

    @Test
    void responseStatus_на_исключении_уважается() {
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(new DomainNotFound("нет такого"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("нет такого");
    }

    @Test
    void неожиданное_исключение_прячет_текст_и_отдаёт_correlationId() {
        var e = new IllegalStateException("relation market_place.accounts does not exist");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).doesNotContain("accounts");
        assertThat(response.getBody().getProperties()).containsKey("correlationId");
    }
}
```

- [ ] **Step 2: Убедиться, что тест падает**

Run: `./gradlew test --tests 'dn.marketplace.core.GlobalExceptionHandlerTest'`
Expected: **ошибка компиляции** `incompatible types: ProblemDetail cannot be converted to ResponseEntity<ProblemDetail>` — текущая сигнатура `handleUnexpected` возвращает `ProblemDetail`. Это и есть «красный» шаг: тест описывает новый контракт.

- [ ] **Step 3: Реализовать**

В `src/main/java/dn/marketplace/core/GlobalExceptionHandler.java`:

Добавить импорты:
```java
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
```

Заменить `handleUnexpected`, `declaredClientError` и `problem` целиком (Javadoc сохранить, добавив абзац про заголовки):

```java
    /**
     * Последний рубеж. Наружу — только correlation id, по которому ошибку
     * находят в логе.
     * <p>
     * Сначала проверяем, не объявило ли исключение свой 4xx-статус само: этот
     * catch-all срабатывает раньше {@code ResponseStatusExceptionResolver},
     * и без такой проверки доменный 404/409 превращался бы в 500.
     * <p>
     * Возвращаем {@link ResponseEntity}, а не голый {@link ProblemDetail}: для
     * спринговых {@link ErrorResponse} нужно пробросить заголовки
     * ({@code Allow} у 405, {@code Accept} у 415) — это часть контракта HTTP.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        ResponseEntity<ProblemDetail> declared = declaredClientError(e);
        if (declared != null) {
            return declared;
        }

        String correlationId = newCorrelationId();
        log.error("Необработанная ошибка [correlationId={}]", correlationId, e);

        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка",
                "Внутренняя ошибка сервера. Сообщите correlationId в поддержку.", TYPE_INTERNAL);
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Исключения, которые сами знают свой клиентский статус:
     * <ul>
     *   <li>доменные с {@code @ResponseStatus(4xx)} — ядро не знает их типов,
     *       поэтому читаем аннотацию, а не перечисляем классы;</li>
     *   <li>спринговые {@link ErrorResponse}: несовпадение типа path-переменной
     *       ({@code /accounts/abc}), отсутствующий {@code @RequestParam},
     *       неподдерживаемый метод, валидация параметров без {@code @Validated}.</li>
     * </ul>
     * Возвращает {@code null}, если исключение ничего о себе не заявляет —
     * тогда это действительно 500.
     */
    private ResponseEntity<ProblemDetail> declaredClientError(Exception e) {
        ResponseStatus declared = AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
        if (declared != null && declared.code().is4xxClientError()) {
            String title = declared.reason().isEmpty() ? declared.code().getReasonPhrase() : declared.reason();
            return ResponseEntity.status(declared.code())
                    .body(problem(declared.code(), title, e.getMessage(), null));
        }

        if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            // HttpStatusCode, а не HttpStatus.valueOf(int): последний бросает
            // IllegalArgumentException на любом коде без константы, и тогда
            // Spring выбрасывает весь advice — клиент получает не-RFC7807 тело.
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            ProblemDetail problem = problem(status, "Некорректный запрос",
                    detail == null ? reasonPhrase(status) : detail,
                    status.value() == HttpStatus.BAD_REQUEST.value() ? TYPE_VALIDATION : null);
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders())
                    .body(problem);
        }

        return null;
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known == null ? "Client Error" : known.getReasonPhrase();
    }

    private ProblemDetail problem(HttpStatusCode status, String title, String detail, URI type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (type != null) {
            problem.setType(type);
        }
        // Instant, а не LocalDateTime: правило "только UTC" действует и в ответах API
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
```

Остальные `@ExceptionHandler`-методы класса не трогать: они по-прежнему возвращают `ProblemDetail`, Spring допускает смешанные типы возврата.

- [ ] **Step 4: Убедиться, что тест зелёный**

Run: `./gradlew test --tests 'dn.marketplace.core.GlobalExceptionHandlerTest'`
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Прогнать ArchUnit — ядро не должно получить новых зависимостей**

Run: `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest'`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Коммит**

```bash
git add src/main/java/dn/marketplace/core/GlobalExceptionHandler.java \
        src/test/java/dn/marketplace/core/GlobalExceptionHandlerTest.java
git commit -m "$(cat <<'EOF'
fix(A6): catch-all не ронялся на нестандартном 4xx и пробрасывает заголовки ErrorResponse

HttpStatus.valueOf(int) бросал IllegalArgumentException на любом коде без
константы — Spring выбрасывал весь advice, и клиент получал не-RFC7807 тело
без correlationId. Теперь работаем с HttpStatusCode напрямую.

Заголовки ErrorResponse (Allow у 405, Accept у 415) раньше терялись:
handleUnexpected возвращает ResponseEntity<ProblemDetail> и копирует их.

Первый юнит-тест хендлера — без Spring-контекста.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01JgcEap37CptcQDJwk7DXFy
EOF
)"
```

---

### Task 3: Убрать поле `email` из `AccountEntity` (R4) **[D — только по подтверждению владельца]**

Обоснование: поле нигде не читается (нет ни в `AccountResponse`, ни в `AccountMapper`), changeset для него нет, а имя противоречит решению №4 (`email_snapshot`). По YAGNI поле уходит; вернётся как `email_snapshot` вместе с миграцией в B2/B3. Если владелец хочет оставить поле — тогда вместо этой задачи нужен changeset в `01-account.sql` с колонкой `email_snapshot VARCHAR(255) NULL`, и это уже задача B2.

**Files:**
- Modify: `src/main/java/dn/marketplace/account/api/AccountEntity.java:31-32`

**Interfaces:**
- Consumes: —
- Produces: `AccountEntity` без поля `email`; никто из потребителей не меняется.

- [ ] **Step 1: Убедиться, что поле не используется**

Run: `grep -rn "getEmail\|setEmail\|\.email\b" src/main/java/dn/marketplace/account`
Expected: пусто. Если что-то найдено — остановиться и показать владельцу.

- [ ] **Step 2: Удалить поле**

В `AccountEntity.java` удалить две строки:
```java
    @Column(name = "email")
    private String email;
```
Итоговый класс:
```java
@Entity
@Table(schema = "market_place",name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,name = "account_status")
    private AccountStatus status;

    @Column(nullable = false,name = "user_name")
    private String username;
}
```

- [ ] **Step 3: Компиляция и маппер**

Run: `./gradlew compileJava --quiet`
Expected: BUILD SUCCESSFUL. MapStruct с `unmappedTargetPolicy = ERROR` проверяет только цель (`AccountResponse`), поэтому удаление поля источника ошибок не даёт. Если появится `Unmapped target property` — значит, где-то поле всё же читалось, вернуться к Step 1.

- [ ] **Step 4: ArchUnit**

Run: `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest'`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Коммит**

```bash
git add src/main/java/dn/marketplace/account/api/AccountEntity.java
git commit -m "$(cat <<'EOF'
refactor(B3): убрать неиспользуемое поле email из AccountEntity

Поле добавлено без changeset и нигде не читается; при ddl-auto: validate
оно лишь расширяло разрыв между сущностью и 01-account.sql. По решению №4
проекция из Keycloak называется email_snapshot и появится вместе с миграцией в B2.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01JgcEap37CptcQDJwk7DXFy
EOF
)"
```

---

### Task 4: Зафиксировать уникальность `user_name` (R3) **[D — только по подтверждению владельца]**

Обоснование: Keycloak гарантирует уникальность username в пределах realm, поэтому `Optional<AccountEntity> findByUsername` корректен по смыслу, но БД этого не знает. Настоящее ограничение — частичный индекс `WHERE deleted_at IS NULL` — появится в B2. Здесь: (а) объявить контракт на сущности, (б) внести явный пункт в B2, чтобы индекс не забыли. Менять репозиторий на `findFirstByUsername` **не нужно** — это замаскировало бы дубли вместо того, чтобы их запретить.

**Files:**
- Modify: `src/main/java/dn/marketplace/account/api/AccountEntity.java` (аннотация на `username`)
- Modify: `.agents/TASKS.md` (строка задачи B2)

**Interfaces:**
- Consumes: `AccountEntity` из Task 3 (без `email`).
- Produces: `@Column(nullable = false, name = "user_name", unique = true)`.

- [ ] **Step 1: Объявить уникальность на сущности**

В `AccountEntity.java` заменить
```java
    @Column(nullable = false,name = "user_name")
    private String username;
```
на
```java
    /**
     * Уникален в пределах realm Keycloak (SSOT). {@code unique = true} — контракт
     * для читателя: при {@code ddl-auto: validate} Hibernate его не проверяет,
     * реальный частичный UNIQUE-индекс ({@code WHERE deleted_at IS NULL}) — задача B2.
     */
    @Column(nullable = false, name = "user_name", unique = true)
    private String username;
```

- [ ] **Step 2: Уточнить B2 в бэклоге**

В `.agents/TASKS.md` строку
```
- [ ] **B2** `01-account.sql` до V4 по строгому SSOT: `*_snapshot` (nullable), `business_status`,
      `banned`, `version`, `deleted_at`, частичные UNIQUE-индексы, триггер `set_updated_at`,
      `--rollback` в каждом changeset. Колонки `role` нет (решение №4).
```
заменить на
```
- [ ] **B2** `01-account.sql` до V4 по строгому SSOT: `*_snapshot` (nullable), `business_status`,
      `banned`, `version`, `deleted_at`, частичные UNIQUE-индексы (обязательно на `user_name` —
      иначе `findByUsername` при дубле даёт `IncorrectResultSizeDataAccessException` → 500),
      триггер `set_updated_at`, `--rollback` в каждом changeset. Колонки `role` нет (решение №4).
```

- [ ] **Step 3: Компиляция**

Run: `./gradlew compileJava --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Коммит**

```bash
git add src/main/java/dn/marketplace/account/api/AccountEntity.java .agents/TASKS.md
git commit -m "$(cat <<'EOF'
docs(B2,B3): зафиксировать уникальность user_name на сущности и в бэклоге

findByUsername возвращает Optional, но ни сущность, ни миграции уникальность
не объявляли: два аккаунта с одним username дали бы
IncorrectResultSizeDataAccessException и 500. Контракт объявлен на @Column,
частичный UNIQUE-индекс явно вписан в задачу B2.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01JgcEap37CptcQDJwk7DXFy
EOF
)"
```

---

### Task 5: Стор `FreezingArchRule` — под контроль git (X1) **[I]**

Обоснование: без стора в git каждый чистый клон замораживает текущие нарушения заново, и ArchUnit перестаёт быть «списком техдолга, который только уменьшается». Комментарий в `archunit.properties` уже обещает путь `src/test/resources/archunit_store` — надо сделать его правдой.

**Files:**
- Modify: `src/test/resources/archunit.properties`
- Move: `archunit_store/` → `src/test/resources/archunit_store/`

**Interfaces:**
- Consumes: —
- Produces: свойство `freeze.store.default.path=src/test/resources/archunit_store`; закоммиченный стор.

- [ ] **Step 1: Воспроизвести проблему**

Run: `git ls-files src/test/resources/archunit_store; ls archunit_store`
Expected: первая команда пуста, вторая показывает `stored.rules` и файлы-UUID — стор живёт в корне и не отслеживается.

- [ ] **Step 2: Задать путь стора явно**

`src/test/resources/archunit.properties` привести к виду:
```properties
# Часть правил нарушена кодом, написанным до их введения (см. MEMORY.md, решение №2:
# "чиним по ходу"). FreezingArchRule записывает эти нарушения в стор и пропускает их,
# но валит сборку на ЛЮБОМ новом нарушении. Файлы стора — это видимый список техдолга:
# он должен только уменьшаться по мере выполнения задач B1-B6.
#
# Путь задан явно: по умолчанию ArchUnit кладёт стор в рабочий каталог JVM
# (корень проекта), где он не попадал в git — и на чистом клоне нарушения
# замораживались заново, обнуляя смысл правила.
freeze.store.default.path=src/test/resources/archunit_store
freeze.store.default.allowStoreCreation=true

# Исправленное нарушение автоматически удаляется из стора и назад уже не пройдёт.
freeze.store.default.allowStoreUpdate=true
```

- [ ] **Step 3: Перенести существующий стор**

```bash
rm -rf src/test/resources/archunit_store
mv archunit_store src/test/resources/archunit_store
```

- [ ] **Step 4: Прогнать ArchUnit и убедиться, что стор больше не создаётся в корне**

Run: `./gradlew test --tests 'dn.marketplace.architecture.ArchitectureTest' && ls archunit_store 2>&1`
Expected: BUILD SUCCESSFUL; `ls: archunit_store: No such file or directory`. Файлы в `src/test/resources/archunit_store/` могут обновиться (allowStoreUpdate=true) — это штатно.

- [ ] **Step 5: Проверить, что стор не пустой и содержит ожидаемые замороженные правила**

Run: `cat src/test/resources/archunit_store/stored.rules`
Expected: перечислены правила из `ArchitectureTest` (entity в пакете `entity`, repository package-private и т.д.). Если файл пустой — правила не заморозились, вернуться к Step 2 и проверить, что Gradle подхватил `archunit.properties` с classpath (`src/test/resources`).

- [ ] **Step 6: Коммит**

```bash
git add src/test/resources/archunit.properties src/test/resources/archunit_store
git commit -m "$(cat <<'EOF'
test(A7): стор FreezingArchRule лежал в корне проекта и не попадал в git

freeze.store.default.path не был задан, и ArchUnit создавал archunit_store/
в рабочем каталоге JVM. На чистом клоне стор пересоздавался с нуля — все
текущие нарушения замораживались заново, и правило "новое нарушение валит
сборку" не выполнялось. Путь задан явно, стор закоммичен как список техдолга.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01JgcEap37CptcQDJwk7DXFy
EOF
)"
```

---

## Самопроверка плана

- **Покрытие замечаний:** R1 → Task 1; R2 → Task 2; R4 → Task 3; R3 → Task 4; X1 (найдено при верификации) → Task 5. Непокрытых нет.
- **Плейсхолдеры:** отсутствуют — каждый шаг с кодом содержит сам код, каждый шаг проверки — команду и ожидаемый вывод.
- **Согласованность типов:** `handleUnexpected` → `ResponseEntity<ProblemDetail>` (Task 2, тест и реализация совпадают); `problem(HttpStatusCode, String, String, URI)` — все прежние вызовы передают `HttpStatus`, который реализует `HttpStatusCode`; `AccountEntity` в Task 4 берётся из состояния после Task 3 (без `email`).
- **Порядок:** Task 1 обязателен первым (чистое дерево). Task 2 и Task 5 независимы от Task 3–4 и могут идти без подтверждения владельца (дорожка [I]). Task 3 и Task 4 — дорожка [D], перед выполнением нужно явное «да» владельца.
