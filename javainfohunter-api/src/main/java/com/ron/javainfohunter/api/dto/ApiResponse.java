package com.ron.javainfohunter.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified API Response Wrapper
 *
 * Standardizes all API responses with success status, message, data, and timestamp.
 * Provides factory methods for common response scenarios.
 *
 * @param <T> Type of data being returned
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * Indicates whether the operation was successful
     */
    private Boolean success;

    /**
     * Human-readable message describing the result
     */
    private String message;

    /**
     * Response data payload (nullable on error)
     */
    private T data;

    /**
     * Response timestamp
     */
    private Instant timestamp;

    /**
     * Create a successful response with data
     *
     * @param data Response data
     * @param <T>  Type of data
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a successful response with custom message
     *
     * @param data    Response data
     * @param message Custom message
     * @param <T>     Type of data
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response
     *
     * @param message Error message
     * @param <T>     Type of data (should be null for errors)
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with custom data
     *
     * @param message Error message
     * @param data    Error data (e.g., validation errors)
     * @param <T>     Type of data
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Check if the response is successful
     * Convenience method that delegates to getSuccess()
     *
     * @return true if success is true, false otherwise
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}
