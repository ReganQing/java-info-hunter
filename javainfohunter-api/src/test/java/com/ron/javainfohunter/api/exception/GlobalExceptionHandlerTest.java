package com.ron.javainfohunter.api.exception;

import com.ron.javainfohunter.api.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleBusinessException() {
        BusinessException exception = new BusinessException("Business rule violated");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Business rule violated", body.getMessage());
        assertNull(body.getData());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("News", 1L);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("News not found with id: 1", body.getMessage());
        assertNull(body.getData());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("objectName", "fieldName", "should not be null");
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<ApiResponse<Map<String, String>>> response =
                exceptionHandler.handleMethodArgumentNotValid(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Map<String, String>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Validation failed", body.getMessage());
        assertNotNull(body.getData());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void testHandleMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");
        when(exception.getValue()).thenReturn("invalid");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleTypeMismatch(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertTrue(body.getMessage().contains("Invalid parameter"));
        assertNotNull(body.getTimestamp());
    }

    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("Unexpected error");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("An unexpected error occurred", body.getMessage());
        assertNull(body.getData());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("Runtime error");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("An unexpected error occurred", body.getMessage());
        assertNull(body.getData());
        assertNotNull(body.getTimestamp());
    }
}
