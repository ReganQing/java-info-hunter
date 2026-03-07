package com.ron.javainfohunter.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceNotFoundException
 */
class ResourceNotFoundExceptionTest {

    @Test
    void testConstructorWithResourceNameAndId() {
        ResourceNotFoundException exception = new ResourceNotFoundException("RSS Source", 1L);

        assertEquals("RSS Source not found with id: 1", exception.getMessage());
        assertTrue(exception instanceof BusinessException);
    }

    @Test
    void testConstructorWithResourceNameAndStringId() {
        ResourceNotFoundException exception = new ResourceNotFoundException("News", "abc-123");

        assertEquals("News not found with id: abc-123", exception.getMessage());
    }

    @Test
    void testConstructorWithCustomMessage() {
        String message = "Custom not found message";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithNullResourceName() {
        ResourceNotFoundException exception = new ResourceNotFoundException(null, 1L);

        assertEquals("Resource not found with id: 1", exception.getMessage());
    }

    @Test
    void testConstructorWithNullId() {
        ResourceNotFoundException exception = new ResourceNotFoundException("RSS Source", (Long) null);

        assertEquals("RSS Source not found with id: null", exception.getMessage());
    }

    @Test
    void testIsBusinessException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Test", 1L);

        assertTrue(exception instanceof BusinessException);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testCanBeThrownWithoutCatch() {
        assertThrows(ResourceNotFoundException.class, () -> {
            throw new ResourceNotFoundException("Test", 1L);
        });
    }
}
