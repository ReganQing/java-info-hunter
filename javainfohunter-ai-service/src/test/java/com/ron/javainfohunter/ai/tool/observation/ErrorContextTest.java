package com.ron.javainfohunter.ai.tool.observation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorContextTest {

    @Test
    void constructor_setsAllFields() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.PARSE,
            "Invalid HTML structure",
            "HTML content appears malformed",
            false,
            "Check HTML source"
        );

        assertEquals(ErrorType.PARSE, ctx.type());
        assertEquals("Invalid HTML structure", ctx.rootCause());
        assertEquals("HTML content appears malformed", ctx.userMessage());
        assertFalse(ctx.retryable());
        assertEquals("Check HTML source", ctx.retryHint());
    }

    @Test
    void retryable_networkError_isTrue() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.NETWORK, "timeout", "Connection timeout", true, "retry after 5s"
        );
        assertTrue(ctx.retryable());
    }

    @Test
    void retryable_parseError_isFalse() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.PARSE, "invalid format", "Bad format", false, "fix input"
        );
        assertFalse(ctx.retryable());
    }

    @Test
    void retryable_rateLimit_isTrue() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.RATE_LIMIT, "too many requests", "Rate limited", true, "wait 60s"
        );
        assertTrue(ctx.retryable());
    }

    @Test
    void retryable_timeout_isTrue() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.TIMEOUT, "execution timeout", "Operation timed out", true, "retry with longer timeout"
        );
        assertTrue(ctx.retryable());
    }

    @Test
    void retryable_validation_isFalse() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.VALIDATION, "missing field", "Required field missing", false, "provide all required fields"
        );
        assertFalse(ctx.retryable());
    }

    @Test
    void retryable_fatal_isFalse() {
        ErrorContext ctx = new ErrorContext(
            ErrorType.FATAL, "internal error", "Unexpected error", false, "report bug"
        );
        assertFalse(ctx.retryable());
    }

    @Test
    void record_equality_works() {
        ErrorContext ctx1 = new ErrorContext(ErrorType.PARSE, "a", "b", false, "c");
        ErrorContext ctx2 = new ErrorContext(ErrorType.PARSE, "a", "b", false, "c");
        assertEquals(ctx1, ctx2);
    }
}
