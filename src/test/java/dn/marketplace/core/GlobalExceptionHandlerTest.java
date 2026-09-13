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
