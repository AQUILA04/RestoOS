package com.resto.core.exception;

import com.resto.core.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response<Map<String, Object>>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "error.bad.request", "ILLEGAL_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Response<Map<String, Object>>> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "error.conflict", "ILLEGAL_STATE", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "error.validation", "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "error.forbidden", "ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Map<String, Object>>> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal", "INTERNAL_ERROR", ex.getMessage());
    }

    private ResponseEntity<Response<Map<String, Object>>> build(HttpStatus status, String message, String code, String details) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("details", details);
        Response<Map<String, Object>> body = Response.<Map<String, Object>>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .service("RESTO-OS")
                .data(data)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
