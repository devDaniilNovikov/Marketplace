package dn.marketplace.core;


import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(WebRequest webRequest,
                                                       Exception e) {
        log.info("Headers from request is: {}",webRequest.getHeaderNames());
        return ResponseEntity.ok(
                ApiResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(webRequest.getDescription(true))
                .details(e.getMessage())
                .path(webRequest.getContextPath())
                .build()
        );


    }
}
