package com.ron.javainfohunter.api.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiResponse
 */
class ApiResponseTest {

    @Test
    void testSuccessResponseWithData() {
        String data = "Test Data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals("Operation completed successfully", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testSuccessResponseWithNullData() {
        ApiResponse<String> response = ApiResponse.success(null);

        assertTrue(response.isSuccess());
        assertEquals("Operation completed successfully", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testErrorResponse() {
        String errorMessage = "Resource not found";
        ApiResponse<String> response = ApiResponse.error(errorMessage);

        assertFalse(response.isSuccess());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testErrorResponseWithNullMessage() {
        ApiResponse<String> response = ApiResponse.error(null);

        assertFalse(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testBuilderPattern() {
        Instant now = Instant.now();
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Custom message")
                .data("Custom data")
                .timestamp(now)
                .build();

        assertTrue(response.isSuccess());
        assertEquals("Custom message", response.getMessage());
        assertEquals("Custom data", response.getData());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testNoArgsConstructor() {
        ApiResponse<String> response = new ApiResponse<>();

        assertFalse(response.isSuccess()); // isSuccess() returns false when success is null
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        Instant now = Instant.now();
        ApiResponse<String> response = new ApiResponse<>(true, "Test", "Data", now);

        assertTrue(response.isSuccess());
        assertEquals("Test", response.getMessage());
        assertEquals("Data", response.getData());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testSetters() {
        ApiResponse<String> response = new ApiResponse<>();
        Instant now = Instant.now();

        response.setSuccess(true);
        response.setMessage("Test message");
        response.setData("Test data");
        response.setTimestamp(now);

        assertTrue(response.isSuccess());
        assertEquals("Test message", response.getMessage());
        assertEquals("Test data", response.getData());
        assertEquals(now, response.getTimestamp());
    }
}
