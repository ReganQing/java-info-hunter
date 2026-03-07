package com.ron.javainfohunter.api.exception;

import com.ron.javainfohunter.api.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * Centralized exception handling for all REST controllers.
 * Converts exceptions into standardized API responses with appropriate HTTP status codes.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle rate limit exceeded exceptions
     *
     * @param ex Rate limit exceeded exception
     * @return 429 Too Many Requests with rate limit details
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRateLimitExceeded(
            RateLimitExceededException ex) {
        log.warn("Rate limit exceeded for key '{}': {} requests per {}s",
            ex.getKey(), ex.getLimit(), ex.getWindowSeconds());

        Map<String, Object> details = new HashMap<>();
        details.put("key", ex.getKey());
        details.put("limit", ex.getLimit());
        details.put("windowSeconds", ex.getWindowSeconds());
        details.put("message", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("X-RateLimit-Limit", String.valueOf(ex.getLimit()))
            .header("X-RateLimit-Window", String.valueOf(ex.getWindowSeconds()))
            .header("Retry-After", String.valueOf(ex.getWindowSeconds()))
            .body(ApiResponse.error("Rate limit exceeded", details));
    }

    /**
     * Handle business exceptions
     *
     * @param ex Business exception
     * @return 400 Bad Request with error message
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle resource not found exceptions
     *
     * @param ex Resource not found exception
     * @return 404 Not Found with error message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle validation errors
     *
     * @param ex Method argument not valid exception
     * @return 400 Bad Request with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    /**
     * Handle type mismatch exceptions (e.g., invalid path variable type)
     *
     * @param ex Method argument type mismatch exception
     * @return 400 Bad Request with error message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid parameter '%s': value '%s' is not valid",
                ex.getName(), ex.getValue());
        log.warn("Type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Handle constraint violation exceptions (for @Validated on request parameters)
     *
     * @param ex Constraint violation exception
     * @return 400 Bad Request with validation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> {
                            // Extract parameter name from property path
                            String path = violation.getPropertyPath().toString();
                            return path.substring(path.lastIndexOf('.') + 1);
                        },
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing + "; " + replacement
                ));

        log.warn("Constraint violation: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    /**
     * Handle all other exceptions
     *
     * @param ex Generic exception
     * @return 500 Internal Server Error with generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
