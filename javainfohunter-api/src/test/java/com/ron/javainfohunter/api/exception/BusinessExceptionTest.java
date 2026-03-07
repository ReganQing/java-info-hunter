package com.ron.javainfohunter.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessException
 */
class BusinessExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Business logic error";
        BusinessException exception = new BusinessException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Business logic error";
        Throwable cause = new RuntimeException("Root cause");
        BusinessException exception = new BusinessException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCauseOnly() {
        Throwable cause = new RuntimeException("Root cause");
        BusinessException exception = new BusinessException(cause);

        assertEquals(cause.getClass().getName() + ": " + cause.getMessage(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testIsRuntimeException() {
        BusinessException exception = new BusinessException("Test");

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testCanBeThrownWithoutCatch() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException("Test exception");
        });
    }
}
