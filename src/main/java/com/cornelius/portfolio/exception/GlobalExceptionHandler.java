package com.cornelius.portfolio.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
            Instant.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
            Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));

        ApiError error = new ApiError(
            Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        ApiError error = new ApiError(
            Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
            "Something went wrong. Please try again later.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
