package com.ron.javainfohunter.crawler.publisher;

import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ErrorPublisher}.
 */
@ExtendWith(MockitoExtension.class)
class ErrorPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ErrorPublisher errorPublisher;

    @BeforeEach
    void setUp() {
        errorPublisher = new ErrorPublisher(rabbitTemplate);
    }

    @Test
    void testPublishError_WithException() {
        // Arrange
        String sourceUrl = "https://example.com/feed";
        Exception exception = new RuntimeException("Test error");
        ErrorType errorType = ErrorType.CONNECTION_ERROR;

        // Act
        assertDoesNotThrow(() -> errorPublisher.publishError(sourceUrl, exception, errorType));

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            any(CrawlErrorMessage.class),
            any(MessagePostProcessor.class)
        );
    }

    @Test
    void testPublishError_WithNullSourceUrl() {
        // Arrange
        Exception exception = new RuntimeException("Test error");
        ErrorType errorType = ErrorType.PARSE_ERROR;

        // Act
        errorPublisher.publishError(null, exception, errorType);

        // Assert
        verify(rabbitTemplate, never()).convertAndSend(
            anyString(),
            anyString(),
            any(),
            any(MessagePostProcessor.class)
        );
    }

    @Test
    void testPublishError_WithNullException() {
        // Arrange
        String sourceUrl = "https://example.com/feed";
        ErrorType errorType = ErrorType.VALIDATION_ERROR;

        // Act
        errorPublisher.publishError(sourceUrl, null, errorType);

        // Assert
        verify(rabbitTemplate, never()).convertAndSend(
            anyString(),
            anyString(),
            any(),
            any(MessagePostProcessor.class)
        );
    }

    @Test
    void testPublishError_WithContext() {
        // Arrange
        String sourceUrl = "https://example.com/feed";
        String errorMessage = "Custom error message";
        ErrorType errorType = ErrorType.DATABASE_ERROR;
        Map<String, Object> context = new HashMap<>();
        context.put("articleCount", 10);
        context.put("duration", 5000L);

        // Act
        assertDoesNotThrow(() -> errorPublisher.publishError(sourceUrl, errorMessage, errorType, context));

        // Assert
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture(),
            any(MessagePostProcessor.class)
        );

        CrawlErrorMessage capturedMessage = messageCaptor.getValue();
        assertEquals(sourceUrl, capturedMessage.getRssSourceUrl());
        assertEquals(errorMessage, capturedMessage.getErrorMessage());
        assertEquals(errorType, capturedMessage.getErrorType());
        assertEquals(context, capturedMessage.getContext());
        assertFalse(capturedMessage.getRetryable());
    }

    @Test
    void testPublishError_WithFullContext() {
        // Arrange
        Long rssSourceId = 123L;
        String rssSourceName = "Test Source";
        String rssSourceUrl = "https://example.com/feed";
        Exception exception = new java.net.SocketTimeoutException("Connection timeout");
        ErrorType errorType = ErrorType.CONNECTION_ERROR;

        // Act
        assertDoesNotThrow(() ->
            errorPublisher.publishError(rssSourceId, rssSourceName, rssSourceUrl, exception, errorType)
        );

        // Assert
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture(),
            any(MessagePostProcessor.class)
        );

        CrawlErrorMessage capturedMessage = messageCaptor.getValue();
        assertEquals(rssSourceId, capturedMessage.getRssSourceId());
        assertEquals(rssSourceName, capturedMessage.getRssSourceName());
        assertEquals(rssSourceUrl, capturedMessage.getRssSourceUrl());
        assertEquals(errorType, capturedMessage.getErrorType());
        assertEquals("Connection timeout", capturedMessage.getErrorMessage());
        assertTrue(capturedMessage.getRetryable(), "Socket timeout should be retryable");
        assertNotNull(capturedMessage.getErrorTrace());
        assertNotNull(capturedMessage.getErrorTime());
        assertEquals(0, capturedMessage.getRetryAttempt());
    }

    @Test
    void testPublishError_WithArticleContext() {
        // Arrange
        Long rssSourceId = 123L;
        String rssSourceName = "Test Source";
        String rssSourceUrl = "https://example.com/feed";
        String articleGuid = "article-guid-456";
        Exception exception = new IllegalArgumentException("Invalid article format");
        ErrorType errorType = ErrorType.VALIDATION_ERROR;

        // Act
        assertDoesNotThrow(() ->
            errorPublisher.publishError(rssSourceId, rssSourceName, rssSourceUrl, articleGuid, exception, errorType)
        );

        // Assert
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture(),
            any(MessagePostProcessor.class)
        );

        CrawlErrorMessage capturedMessage = messageCaptor.getValue();
        assertEquals(rssSourceId, capturedMessage.getRssSourceId());
        assertEquals(articleGuid, capturedMessage.getArticleGuid());
        assertEquals(errorType, capturedMessage.getErrorType());
        assertFalse(capturedMessage.getRetryable(), "Validation errors should not be retryable");
    }

    @Test
    void testPublishRetryError() {
        // Arrange
        Long rssSourceId = 123L;
        String rssSourceName = "Test Source";
        String rssSourceUrl = "https://example.com/feed";
        Exception exception = new java.net.ConnectException("Connection refused");
        ErrorType errorType = ErrorType.CONNECTION_ERROR;
        int currentAttempt = 2;
        int maxAttempts = 5;

        // Act
        assertDoesNotThrow(() ->
            errorPublisher.publishRetryError(rssSourceId, rssSourceName, rssSourceUrl,
                exception, errorType, currentAttempt, maxAttempts)
        );

        // Assert
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture(),
            any(MessagePostProcessor.class)
        );

        CrawlErrorMessage capturedMessage = messageCaptor.getValue();
        assertEquals(currentAttempt, capturedMessage.getRetryAttempt());
        assertEquals(maxAttempts, capturedMessage.getMaxRetries());
        assertTrue(capturedMessage.getRetryable(), "Should be retryable when attempts < max");
        assertNotNull(capturedMessage.getRetryDelayMs());
        assertTrue(capturedMessage.getRetryDelayMs() > 0);
    }

    @Test
    void testPublishRetryError_ExhaustedRetries() {
        // Arrange
        Long rssSourceId = 123L;
        String rssSourceName = "Test Source";
        String rssSourceUrl = "https://example.com/feed";
        Exception exception = new java.net.ConnectException("Connection refused");
        ErrorType errorType = ErrorType.CONNECTION_ERROR;
        int currentAttempt = 5;
        int maxAttempts = 5;

        // Act
        assertDoesNotThrow(() ->
            errorPublisher.publishRetryError(rssSourceId, rssSourceName, rssSourceUrl,
                exception, errorType, currentAttempt, maxAttempts)
        );

        // Assert
        ArgumentCaptor<CrawlErrorMessage> messageCaptor = ArgumentCaptor.forClass(CrawlErrorMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            messageCaptor.capture(),
            any(MessagePostProcessor.class)
        );

        CrawlErrorMessage capturedMessage = messageCaptor.getValue();
        assertEquals(currentAttempt, capturedMessage.getRetryAttempt());
        assertEquals(maxAttempts, capturedMessage.getMaxRetries());
        assertFalse(capturedMessage.getRetryable(), "Should not be retryable when attempts >= max");
    }

    @Test
    void testPublishError_PublishingFailureDoesNotCascade() {
        // Arrange
        String sourceUrl = "https://example.com/feed";
        Exception exception = new RuntimeException("Test error");
        ErrorType errorType = ErrorType.QUEUE_ERROR;

        doThrow(new RuntimeException("RabbitMQ connection failed"))
            .when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(CrawlErrorMessage.class),
                any(MessagePostProcessor.class)
            );

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() ->
            errorPublisher.publishError(sourceUrl, exception, errorType)
        );

        // Verify attempt was made
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            any(CrawlErrorMessage.class),
            any(MessagePostProcessor.class)
        );
    }

    @Test
    void testPublishError_ErrorTypeClassification() {
        // Arrange
        String sourceUrl = "https://example.com/feed";

        // Test various error types
        ErrorType[] errorTypes = {
            ErrorType.CONNECTION_ERROR,
            ErrorType.PARSE_ERROR,
            ErrorType.DATABASE_ERROR,
            ErrorType.QUEUE_ERROR,
            ErrorType.ENCODING_ERROR,
            ErrorType.VALIDATION_ERROR,
            ErrorType.UNKNOWN
        };

        for (ErrorType errorType : errorTypes) {
            Exception exception = new RuntimeException("Test " + errorType);

            // Act
            assertDoesNotThrow(() ->
                errorPublisher.publishError(sourceUrl, exception, errorType)
            );
        }

        // Verify all error types were sent
        verify(rabbitTemplate, times(errorTypes.length)).convertAndSend(
            eq("crawler.direct"),
            eq("crawl.error"),
            any(CrawlErrorMessage.class),
            any(MessagePostProcessor.class)
        );
    }
}
