package com.cornelius.portfolio.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        String summary = fieldErrors.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, "Bad Request", summary, request, fieldErrors);
    }

    // Wrong type in a path variable or query param — e.g. GET /api/projects/abc
    // where an id: Long is expected. Was previously falling through to the
    // generic 500 handler, which misreports a client mistake as a server fault.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex, HttpServletRequest request
    ) {
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "a different type";
        String message = "'%s' is not a valid value for '%s' — expected %s"
            .formatted(ex.getValue(), ex.getName(), expectedType);
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, request, null);
    }

    // Malformed or missing JSON body. Same misclassification problem as above —
    // this is the client sending something we can't parse, not a server error.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedBody(
        HttpMessageNotReadableException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
            "Request body is missing or could not be parsed as valid JSON.", request, null);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
        DataIntegrityViolationException ex, HttpServletRequest request
    ) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict",
            "This would conflict with existing data (e.g. a duplicate name).", request, null);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "Something went wrong. Please try again later.", request, null);
    }

    private ResponseEntity<ApiError> build(
        HttpStatus status, String error, String message, HttpServletRequest request, Map<String, String> fieldErrors
    ) {
        ApiError body = new ApiError(
            Instant.now(), status.value(), error, message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
