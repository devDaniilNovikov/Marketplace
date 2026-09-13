package dn.marketplace.core;

import dn.marketplace.core.exception.BusinessRuleViolationException;
import dn.marketplace.core.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Единая трансляция исключений в ответы RFC 7807 (Problem Details).
 * <p>
 * Два правила, из-за которых этот класс переписан:
 * <ol>
 *   <li>HTTP-статус несёт смысл. Прежняя версия возвращала 200 на любую ошибку,
 *       положив реальный код в тело — клиент и любой промежуточный прокси
 *       считали такой ответ успехом.</li>
 *   <li>Наружу для 5xx уходит только correlation id. Текст исключения, стектрейс
 *       и описание запроса пишутся в лог: {@code e.getMessage()} у SQL- и
 *       Hibernate-ошибок содержит имена таблиц, колонок и куски запроса.</li>
 * </ol>
 * Порядок методов значения не имеет: Spring выбирает самый специфичный тип.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final URI TYPE_VALIDATION = URI.create("urn:marketplace:error:validation");
    private static final URI TYPE_BUSINESS_RULE = URI.create("urn:marketplace:error:business-rule");
    private static final URI TYPE_CONFLICT = URI.create("urn:marketplace:error:conflict");
    private static final URI TYPE_INTERNAL = URI.create("urn:marketplace:error:internal");

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Ресурс не найден", e.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Нарушено бизнес-правило", e.getMessage(), TYPE_BUSINESS_RULE);
    }

    /**
     * Валидация {@code @Valid} на теле запроса. Поля отдаём поимённо —
     * это контракт API, а не внутренняя деталь.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ProblemDetail handleBodyValidation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        org.springframework.validation.FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "некорректное значение" : fe.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Ошибка валидации",
                "Запрос не прошёл валидацию", TYPE_VALIDATION);
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Валидация {@code @Validated} на параметрах контроллера — например
     * {@code @Max(100)} на pageSize.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleParamValidation(ConstraintViolationException e) {
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        jakarta.validation.ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Ошибка валидации",
                "Параметры запроса не прошли валидацию", TYPE_VALIDATION);
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * До этого хендлера долетают только отказы из {@code @PreAuthorize} на методах
     * контроллеров: отказы на уровне фильтров Spring Security обрабатывает сама
     * цепочка и до MVC они не доходят.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Доступ запрещён", "Недостаточно прав для этой операции", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Требуется аутентификация", "Токен отсутствует или невалиден", null);
    }

    /**
     * Проигранная гонка оптимистичной блокировки (@Version). Для клиента это
     * штатный повторяемый исход, а не сбой: 409 и "повторите операцию".
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("Конфликт оптимистичной блокировки", e);
        return problem(HttpStatus.CONFLICT, "Конфликт параллельного изменения",
                "Данные изменились параллельно, повторите операцию", TYPE_CONFLICT);
    }

    /**
     * Сработал CHECK или UNIQUE на стороне БД. Текст ограничения наружу не отдаём:
     * он раскрывает имена таблиц и колонок.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException e) {
        String correlationId = newCorrelationId();
        log.warn("Нарушение целостности данных [correlationId={}]", correlationId, e);

        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Конфликт данных",
                "Операция нарушает ограничение целостности", TYPE_CONFLICT);
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

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

    private String newCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
