package com.ron.javainfohunter.crawler.handler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RetryHandler.
 */
@ExtendWith(MockitoExtension.class)
class RetryHandlerTest {

    @Mock
    private CrawlerProperties crawlerProperties;

    private RetryHandler retryHandler;
    private CrawlerProperties.Retry retryConfig;

    @BeforeEach
    void setUp() {
        retryConfig = new CrawlerProperties.Retry();
        retryConfig.setMaxAttempts(3);
        retryConfig.setInitialDelay(100);  // Short delay for tests
        retryConfig.setBackoffMultiplier(2.0);
        retryConfig.setMaxDelay(1000);

        when(crawlerProperties.getRetry()).thenReturn(retryConfig);
        retryHandler = new RetryHandler(crawlerProperties);
    }

    @Test
    void testSuccessfulExecutionOnFirstAttempt() {
        String result = retryHandler.executeWithRetry(
            "Test task",
            () -> "Success"
        );

        assertEquals("Success", result);
    }

    @Test
    void testSuccessfulExecutionAfterRetry() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = retryHandler.executeWithRetry(
            "Test task",
            () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new IOException("Temporary failure");
                }
                return "Success";
            }
        );

        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testFailureAfterMaxRetries() {
        IOException exception = new IOException("Persistent failure");

        assertThrows(RuntimeException.class, () -> {
            retryHandler.executeWithRetry(
                "Test task",
                () -> {
                    throw exception;
                }
            );
        });
    }

    @Test
    void testExponentialBackoffCalculation() {
        assertEquals(100, retryHandler.calculateBackoff(0));
        assertEquals(200, retryHandler.calculateBackoff(1));
        assertEquals(400, retryHandler.calculateBackoff(2));
        assertEquals(800, retryHandler.calculateBackoff(3));
    }

    @Test
    void testBackoffCappedAtMaxDelay() {
        // With maxDelay = 1000, attempts beyond a certain point should be capped
        long delay = retryHandler.calculateBackoff(20);
        assertEquals(1000, delay);  // Should be capped at maxDelay
    }

    @Test
    void testCustomRetryCondition() {
        AtomicInteger attempts = new AtomicInteger(0);

        // Only retry on first attempt
        String result = retryHandler.executeWithRetry(
            "Test task",
            () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new IOException("Temporary failure");
                }
                return "Success";
            },
            e -> attempts.get() < 2  // Only retry once
        );

        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testShouldRetryForIOException() {
        IOException exception = new IOException("Connection failed");
        assertTrue(retryHandler.shouldRetry(exception));
    }

    @Test
    void testShouldNotRetryForIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        assertFalse(retryHandler.shouldRetry(exception));
    }

    @Test
    void testShouldNotRetryForIllegalStateException() {
        IllegalStateException exception = new IllegalStateException("Invalid state");
        assertFalse(retryHandler.shouldRetry(exception));
    }

    @Test
    void testShouldNotRetryForInterruptedException() {
        InterruptedException exception = new InterruptedException("Interrupted");
        assertFalse(retryHandler.shouldRetry(exception));
    }

    @Test
    void testShouldRetryForSQLException() {
        java.sql.SQLException exception = new java.sql.SQLException("Database error");
        assertTrue(retryHandler.shouldRetry(exception));
    }

    @Test
    void testShouldRetryForAMQPException() {
        org.springframework.amqp.AmqpException exception =
            new org.springframework.amqp.AmqpException("Queue error");
        assertTrue(retryHandler.shouldRetry(exception));
    }

    @Test
    void testExecuteWithRetryUsingErrorType() {
        CrawlErrorHandler errorHandler = new CrawlErrorHandler(
            crawlerProperties, null, null
        );

        AtomicInteger attempts = new AtomicInteger(0);

        String result = retryHandler.executeWithRetry(
            "Test task",
            () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new IOException("Temporary failure");
                }
                return "Success";
            },
            errorHandler
        );

        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testExecuteWithRetryForRunnable() {
        AtomicInteger attempts = new AtomicInteger(0);

        retryHandler.executeWithRetry(
            "Test task",
            () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new IOException("Temporary failure");
                }
            }
        );

        assertEquals(2, attempts.get());
    }

    @Test
    void testGetMaxRetryAttempts() {
        assertEquals(3, retryHandler.getMaxRetryAttempts());
    }

    @Test
    void testGetInitialDelay() {
        assertEquals(100, retryHandler.getInitialDelay());
    }

    @Test
    void testGetBackoffMultiplier() {
        assertEquals(2.0, retryHandler.getBackoffMultiplier(), 0.001);
    }

    @Test
    void testGetMaxDelay() {
        assertEquals(1000, retryHandler.getMaxDelay());
    }

    @Test
    void testRetryWithDifferentConfiguration() {
        retryConfig.setMaxAttempts(5);
        retryConfig.setInitialDelay(50);
        retryConfig.setBackoffMultiplier(3.0);

        assertEquals(5, retryHandler.calculateBackoff(0));
        assertEquals(150, retryHandler.calculateBackoff(1));
        assertEquals(450, retryHandler.calculateBackoff(2));
    }

    @Test
    void testExceptionPreservedInRuntimeException() {
        IOException originalException = new IOException("Original error");

        try {
            retryHandler.executeWithRetry(
                "Test task",
                () -> {
                    throw originalException;
                }
            );
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("Original error", e.getCause().getMessage());
        }
    }

    @Test
    void testTaskNameInExceptionMessage() {
        try {
            retryHandler.executeWithRetry(
                "My Custom Task",
                () -> {
                    throw new IOException("Error");
                }
            );
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("My Custom Task"));
        }
    }

    @Test
    void testZeroMaxAttempts() {
        retryConfig.setMaxAttempts(0);

        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            retryHandler.executeWithRetry(
                "Test task",
                () -> {
                    attempts.incrementAndGet();
                    throw new IOException("Error");
                }
            );
        });

        assertEquals(1, attempts.get());  // Should only attempt once
    }

    @Test
    void testVeryShortDelays() {
        retryConfig.setInitialDelay(1);
        retryConfig.setBackoffMultiplier(1.5);

        long start = System.currentTimeMillis();
        try {
            retryHandler.executeWithRetry(
                "Test task",
                () -> {
                    throw new IOException("Error");
                }
            );
        } catch (Exception e) {
            // Expected
        }
        long duration = System.currentTimeMillis() - start;

        // With maxAttempts=3 and short delays, should complete quickly
        assertTrue(duration < 1000, "Test took too long: " + duration + "ms");
    }
}
