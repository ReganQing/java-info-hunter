package com.ron.javainfohunter.crawler.handler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage;
import com.ron.javainfohunter.crawler.metrics.CrawlMetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.sql.SQLException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CrawlErrorHandler.
 */
@ExtendWith(MockitoExtension.class)
class CrawlErrorHandlerTest {

    @Mock
    private CrawlerProperties crawlerProperties;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private CrawlMetricsCollector metricsCollector;

    private CrawlErrorHandler errorHandler;
    private CrawlerProperties.Retry retryConfig;

    @BeforeEach
    void setUp() {
        errorHandler = new CrawlErrorHandler(crawlerProperties, rabbitTemplate, metricsCollector);

        // Setup default retry configuration
        retryConfig = new CrawlerProperties.Retry();
        retryConfig.setMaxAttempts(3);
        retryConfig.setInitialDelay(1000);
        retryConfig.setBackoffMultiplier(2.0);
        retryConfig.setMaxDelay(60000);

        when(crawlerProperties.getRetry()).thenReturn(retryConfig);
    }

    @Test
    void testClassifyConnectionError() {
        IOException exception = new IOException("Connection refused");
        ErrorType errorType = errorHandler.classifyError(exception);

        assertEquals(ErrorType.CONNECTION_ERROR, errorType);
    }

    @Test
    void testClassifyTimeoutError() {
        SocketTimeoutException exception = new SocketTimeoutException("Read timed out");
        ErrorType errorType = errorHandler.classifyError(exception);

        assertEquals(ErrorType.CONNECTION_ERROR, errorType);
    }

    @Test
    void testClassifyDatabaseError() {
        SQLException exception = new SQLException("Connection failed");
        ErrorType errorType = errorHandler.classifyError(exception);

        assertEquals(ErrorType.DATABASE_ERROR, errorType);
    }

    @Test
    void testClassifyValidationError() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid URL");
        ErrorType errorType = errorHandler.classifyError(exception);

        assertEquals(ErrorType.VALIDATION_ERROR, errorType);
    }

    @Test
    void testClassifyUnknownError() {
        RuntimeException exception = new RuntimeException("Unknown error");
        ErrorType errorType = errorHandler.classifyError(exception);

        assertEquals(ErrorType.UNKNOWN_ERROR, errorType);
    }

    @Test
    void testClassifyNullError() {
        ErrorType errorType = errorHandler.classifyError(null);

        assertEquals(ErrorType.UNKNOWN_ERROR, errorType);
    }

    @Test
    void testIsRetryableForConnectionError() {
        assertTrue(errorHandler.isRetryable(ErrorType.CONNECTION_ERROR));
    }

    @Test
    void testIsRetryableForParseError() {
        assertFalse(errorHandler.isRetryable(ErrorType.PARSE_ERROR));
    }

    @Test
    void testIsRetryableForDatabaseError() {
        assertTrue(errorHandler.isRetryable(ErrorType.DATABASE_ERROR));
    }

    @Test
    void testIsRetryableForValidationError() {
        assertFalse(errorHandler.isRetryable(ErrorType.VALIDATION_ERROR));
    }

    @Test
    void testExtractContext() {
        String sourceUrl = "https://example.com/rss";
        IOException exception = new IOException("Connection refused");

        Map<String, Object> context = errorHandler.extractContext(sourceUrl, exception);

        assertEquals(sourceUrl, context.get("sourceUrl"));
        assertEquals("java.io.IOException", context.get("exceptionClass"));
        assertEquals("Connection refused", context.get("exceptionMessage"));
        assertNotNull(context.get("timestamp"));
    }

    @Test
    void testHandleConnectionError() {
        String sourceUrl = "https://example.com/rss";
        IOException exception = new IOException("Connection refused");

        errorHandler.handleError(sourceUrl, exception, null);

        // Verify metrics were recorded
        verify(metricsCollector).recordCrawlFailure(eq(sourceUrl), eq(ErrorType.CONNECTION_ERROR), eq(0L));

        // Verify error was published to RabbitMQ
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture()
        );

        CrawlErrorMessage message = messageCaptor.getValue();
        assertEquals(sourceUrl, message.getRssSourceUrl());
        assertEquals(CrawlErrorMessage.ErrorType.CONNECTION_ERROR, message.getErrorType());
        assertEquals("Connection refused", message.getErrorMessage());
        assertTrue(message.getRetryable());
    }

    @Test
    void testHandleParseError() {
        String sourceUrl = "https://example.com/rss";
        // Use a RuntimeException with message indicating parse error
        Exception exception = new RuntimeException("Invalid XML format in RSS feed");

        errorHandler.handleError(sourceUrl, exception, null);

        // Verify metrics were recorded (will be UNKNOWN_ERROR for generic RuntimeException)
        verify(metricsCollector).recordCrawlFailure(eq(sourceUrl), eq(ErrorType.UNKNOWN_ERROR), eq(0L));

        // Verify error was published
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture()
        );

        CrawlErrorMessage message = messageCaptor.getValue();
        assertEquals(CrawlErrorMessage.ErrorType.UNKNOWN, message.getErrorType());
        assertFalse(message.getRetryable());
    }

    @Test
    void testHandleErrorWithRetryAttempt() {
        String sourceUrl = "https://example.com/rss";
        IOException exception = new IOException("Connection refused");
        Map<String, Object> context = Map.of("retryAttempt", 2);

        errorHandler.handleError(sourceUrl, exception, context);

        // Verify error was published with retry information
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture()
        );

        CrawlErrorMessage message = messageCaptor.getValue();
        assertEquals(2, message.getRetryAttempt());
    }

    @Test
    void testHandleRetryError() {
        String sourceUrl = "https://example.com/rss";
        IOException exception = new IOException("Connection refused");

        errorHandler.handleRetryError(sourceUrl, exception, 1, null);

        // Verify metrics were recorded
        verify(metricsCollector).recordCrawlFailure(eq(sourceUrl), eq(ErrorType.CONNECTION_ERROR), eq(0L));

        // Verify error was published with retry information
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate).convertAndSend(
            anyString(),
            anyString(),
            messageCaptor.capture()
        );

        CrawlErrorMessage message = messageCaptor.getValue();
        assertEquals(1, message.getRetryAttempt());
    }

    @Test
    void testHandleErrorWithCustomContext() {
        String sourceUrl = "https://example.com/rss";
        IOException exception = new IOException("Connection refused");
        Map<String, Object> customContext = Map.of(
            "customField", "customValue",
            "articleCount", 10
        );

        errorHandler.handleError(sourceUrl, exception, customContext);

        // Verify custom context is included
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate).convertAndSend(
            anyString(),
            anyString(),
            messageCaptor.capture()
        );

        CrawlErrorMessage message = messageCaptor.getValue();
        Map<String, Object> messageContext = message.getContext();
        assertNotNull(messageContext);
        assertEquals("customValue", messageContext.get("customField"));
        assertEquals(10, messageContext.get("articleCount"));
    }

    @Test
    void testHandleErrorWhenRabbitMQFails() {
        String sourceUrl = "https://example.com/rss";
        IOException exception = new IOException("Connection refused");

        // Simulate RabbitMQ failure
        doThrow(new RuntimeException("RabbitMQ unavailable"))
            .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        // Should not throw exception
        assertDoesNotThrow(() -> errorHandler.handleError(sourceUrl, exception, null));

        // Metrics should still be recorded
        verify(metricsCollector).recordCrawlFailure(eq(sourceUrl), eq(ErrorType.CONNECTION_ERROR), eq(0L));
    }
}
