package com.ron.javainfohunter.crawler.handler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage;
import com.ron.javainfohunter.crawler.metrics.CrawlMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Centralized error handler for crawler operations.
 *
 * <p>This component provides unified error handling for all crawler operations,
 * including:</p>
 * <ul>
 *   <li>Error classification by type</li>
 *   <li>Retry eligibility determination</li>
 *   <li>Error context extraction</li>
 *   <li>Error logging with appropriate levels</li>
 *   <li>Error publishing to RabbitMQ for monitoring</li>
 *   <li>Metrics recording</li>
 * </ul>
 *
 * <p><b>Error Handling Workflow:</b></p>
 * <pre>
 * Exception occurs
 *     ↓
 * CrawlErrorHandler.handleError()
 *     ↓
 * Classify error type → ErrorType
 *     ↓
 * Check retry eligibility
 *     ↓
 * If retryable:
 *     RetryHandler.executeWithRetry()
 *     ↓
 *     If still fails after retries:
 *         Log error
 *         Publish to error queue
 *         Record in metrics
 * Else:
 *     Log error
 *     Publish to error queue
 *     Record in metrics
 * </pre>
 *
 * @see ErrorType
 * @see RetryHandler
 * @see CrawlMetricsCollector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlErrorHandler {

    private final CrawlerProperties crawlerProperties;
    private final RabbitTemplate rabbitTemplate;
    private final CrawlMetricsCollector metricsCollector;

    /**
     * Handle a crawler error with comprehensive processing.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Classify the error type</li>
     *   <li>Extract error context</li>
     *   <li>Determine retry eligibility</li>
     *   <li>Log the error with appropriate level</li>
     *   <li>Publish error message to RabbitMQ</li>
     *   <li>Record error in metrics</li>
     * </ol>
     *
     * @param sourceUrl the RSS source URL that caused the error
     * @param e the exception that occurred
     * @param context additional context information (optional), should include "durationMs" if available
     */
    public void handleError(String sourceUrl, Exception e, Map<String, Object> context) {
        ErrorType errorType = classifyError(e);
        Map<String, Object> enrichedContext = extractContext(sourceUrl, e);

        // Merge provided context with extracted context
        if (context != null && !context.isEmpty()) {
            enrichedContext.putAll(context);
        }

        boolean retryable = isRetryable(errorType);
        int retryAttempt = context != null ? (int) context.getOrDefault("retryAttempt", 0) : 0;
        int maxRetries = crawlerProperties.getRetry().getMaxAttempts();

        // Extract duration from context if available
        long durationMs = 0;
        if (context != null) {
            Object durationObj = context.get("durationMs");
            if (durationObj instanceof Number) {
                durationMs = ((Number) durationObj).longValue();
            }
        }

        // Log the error
        logError(sourceUrl, errorType, e, retryAttempt);

        // Create error message
        CrawlErrorMessage errorMessage = buildErrorMessage(
            sourceUrl, errorType, e, enrichedContext, retryable, retryAttempt, maxRetries
        );

        // Publish to error queue
        publishError(errorMessage);

        // Record in metrics with actual duration
        metricsCollector.recordCrawlFailure(sourceUrl, errorType, durationMs);
    }

    /**
     * Handle a crawler error during a retry attempt.
     *
     * @param sourceUrl the RSS source URL
     * @param e the exception
     * @param retryAttempt current retry attempt number
     * @param context additional context
     */
    public void handleRetryError(String sourceUrl, Exception e, int retryAttempt, Map<String, Object> context) {
        if (context == null) {
            context = Map.of();
        }

        Map<String, Object> retryContext = Map.of("retryAttempt", retryAttempt);
        // Merge contexts
        Map<String, Object> mergedContext = new java.util.HashMap<>(context);
        mergedContext.putAll(retryContext);

        handleError(sourceUrl, e, mergedContext);
    }

    /**
     * Classify an exception into an ErrorType.
     *
     * <p>Classification rules:</p>
     * <ul>
     *   <li>IOException/TimeoutException → CONNECTION_ERROR</li>
     *   <li>SQLException → DATABASE_ERROR</li>
     *   <li>AMQP exceptions → QUEUE_ERROR</li>
     *   <li>IllegalStateException/IllegalArgumentException → VALIDATION_ERROR</li>
     *   <li>Other RuntimeException → UNKNOWN_ERROR</li>
     * </ul>
     *
     * @param e the exception to classify
     * @return classified ErrorType
     */
    protected ErrorType classifyError(Exception e) {
        if (e == null) {
            return ErrorType.UNKNOWN_ERROR;
        }

        // Check exception type and its causes
        Throwable cause = e;
        int depth = 0;
        while (cause != null && depth < 5) {
            if (cause instanceof java.io.IOException) {
                if (cause instanceof java.net.SocketTimeoutException ||
                    cause instanceof TimeoutException) {
                    return ErrorType.CONNECTION_ERROR;
                }
                if (cause.getMessage() != null &&
                    (cause.getMessage().contains("Connection refused") ||
                     cause.getMessage().contains("Connection reset") ||
                     cause.getMessage().contains("No route to host") ||
                     cause.getMessage().contains("SSL"))) {
                    return ErrorType.CONNECTION_ERROR;
                }
            }

            if (cause instanceof SQLException) {
                return ErrorType.DATABASE_ERROR;
            }

            if (cause instanceof org.springframework.amqp.AmqpException ||
                cause instanceof org.springframework.amqp.AmqpConnectException ||
                cause instanceof org.springframework.amqp.AmqpIOException) {
                return ErrorType.QUEUE_ERROR;
            }

            if (cause instanceof com.rometools.rome.io.XmlReaderException ||
                (cause.getMessage() != null && cause.getMessage().contains("Invalid RSS"))) {
                return ErrorType.PARSE_ERROR;
            }

            if (cause instanceof java.nio.charset.CharacterCodingException ||
                (cause.getMessage() != null && cause.getMessage().contains("Character encoding"))) {
                return ErrorType.ENCODING_ERROR;
            }

            cause = cause.getCause();
            depth++;
        }

        // Check for specific exception types
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
            return ErrorType.VALIDATION_ERROR;
        }

        if (e instanceof java.io.IOException) {
            return ErrorType.CONNECTION_ERROR;
        }

        return ErrorType.UNKNOWN_ERROR;
    }

    /**
     * Determine if an error type is retryable.
     *
     * @param errorType the error type to check
     * @return true if retry should be attempted
     */
    protected boolean isRetryable(ErrorType errorType) {
        return errorType != null && errorType.isRetryable();
    }

    /**
     * Extract meaningful context from the error.
     *
     * @param sourceUrl the RSS source URL
     * @param e the exception
     * @return context map with extracted information
     */
    protected Map<String, Object> extractContext(String sourceUrl, Exception e) {
        Map<String, Object> context = new java.util.HashMap<>();

        context.put("sourceUrl", sourceUrl);
        context.put("exceptionClass", e.getClass().getName());
        context.put("exceptionMessage", e.getMessage());

        // Extract root cause
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        context.put("rootCauseClass", rootCause.getClass().getName());
        context.put("rootCauseMessage", rootCause.getMessage());

        // Add timestamp
        context.put("timestamp", Instant.now().toString());

        return context;
    }

    /**
     * Log error with appropriate level based on error type.
     *
     * @param sourceUrl the source URL
     * @param errorType the error type
     * @param e the exception
     * @param retryAttempt current retry attempt
     */
    private void logError(String sourceUrl, ErrorType errorType, Exception e, int retryAttempt) {
        String retryInfo = retryAttempt > 0 ? String.format(" (Retry attempt %d)", retryAttempt) : "";

        switch (errorType) {
            case CONNECTION_ERROR:
                log.warn("Connection error crawling {}{}: {}", sourceUrl, retryInfo, e.getMessage());
                break;

            case PARSE_ERROR:
                log.error("Parse error crawling {}{} - Invalid feed format: {}", sourceUrl, retryInfo, e.getMessage());
                break;

            case DATABASE_ERROR:
                log.error("Database error processing {}{}: {}", sourceUrl, retryInfo, e.getMessage(), e);
                break;

            case QUEUE_ERROR:
                log.error("Queue error publishing for {}{}: {}", sourceUrl, retryInfo, e.getMessage());
                break;

            case VALIDATION_ERROR:
                log.warn("Validation error for {}{}: {}", sourceUrl, retryInfo, e.getMessage());
                break;

            case ENCODING_ERROR:
                log.warn("Encoding error for {}{}: {}", sourceUrl, retryInfo, e.getMessage());
                break;

            case SCHEDULER_ERROR:
                log.error("Scheduler error for {}{}: {}", sourceUrl, retryInfo, e.getMessage(), e);
                break;

            case UNKNOWN_ERROR:
            default:
                log.error("Unknown error crawling {}{}: {}", sourceUrl, retryInfo, e.getMessage(), e);
                break;
        }
    }

    /**
     * Build a CrawlErrorMessage from error information.
     *
     * @param sourceUrl the source URL
     * @param errorType the error type
     * @param e the exception
     * @param context error context
     * @param retryable whether error is retryable
     * @param retryAttempt current retry attempt
     * @param maxRetries maximum retry attempts
     * @return CrawlErrorMessage
     */
    private CrawlErrorMessage buildErrorMessage(
        String sourceUrl,
        ErrorType errorType,
        Exception e,
        Map<String, Object> context,
        boolean retryable,
        int retryAttempt,
        int maxRetries
    ) {
        // Convert ErrorType to CrawlErrorMessage.ErrorType
        CrawlErrorMessage.ErrorType messageErrorType = convertErrorType(errorType);

        // Calculate retry delay
        Long retryDelayMs = null;
        if (retryable && retryAttempt < maxRetries) {
            retryDelayMs = calculateRetryDelay(retryAttempt);
        }

        return CrawlErrorMessage.builder()
            .rssSourceUrl(sourceUrl)
            .errorType(messageErrorType)
            .errorMessage(e.getMessage())
            .errorTrace(getStackTrace(e))
            .errorTime(Instant.now())
            .retryable(retryable)
            .retryDelayMs(retryDelayMs)
            .retryAttempt(retryAttempt)
            .maxRetries(maxRetries)
            .context(context)
            .build();
    }

    /**
     * Convert handler ErrorType to message ErrorType.
     *
     * @param handlerErrorType handler error type
     * @return message error type
     */
    private CrawlErrorMessage.ErrorType convertErrorType(ErrorType handlerErrorType) {
        if (handlerErrorType == null) {
            return CrawlErrorMessage.ErrorType.UNKNOWN;
        }

        return switch (handlerErrorType) {
            case CONNECTION_ERROR -> CrawlErrorMessage.ErrorType.CONNECTION_ERROR;
            case PARSE_ERROR -> CrawlErrorMessage.ErrorType.PARSE_ERROR;
            case DATABASE_ERROR -> CrawlErrorMessage.ErrorType.DATABASE_ERROR;
            case QUEUE_ERROR -> CrawlErrorMessage.ErrorType.QUEUE_ERROR;
            case VALIDATION_ERROR -> CrawlErrorMessage.ErrorType.VALIDATION_ERROR;
            case ENCODING_ERROR -> CrawlErrorMessage.ErrorType.ENCODING_ERROR;
            default -> CrawlErrorMessage.ErrorType.UNKNOWN;
        };
    }

    /**
     * Calculate retry delay using exponential backoff.
     *
     * @param retryAttempt current retry attempt (0-based)
     * @return delay in milliseconds
     */
    private long calculateRetryDelay(int retryAttempt) {
        CrawlerProperties.Retry retryConfig = crawlerProperties.getRetry();
        long delay = (long) (retryConfig.getInitialDelay() *
                           Math.pow(retryConfig.getBackoffMultiplier(), retryAttempt));
        return Math.min(delay, retryConfig.getMaxDelay());
    }

    /**
     * Get stack trace as string.
     *
     * @param e the exception
     * @return stack trace string
     */
    private String getStackTrace(Exception e) {
        if (e == null) {
            return null;
        }

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Publish error message to RabbitMQ.
     *
     * @param errorMessage the error message to publish
     */
    private void publishError(CrawlErrorMessage errorMessage) {
        try {
            rabbitTemplate.convertAndSend(
                com.ron.javainfohunter.crawler.config.RabbitMQConfig.CRAWLER_EXCHANGE,
                com.ron.javainfohunter.crawler.config.RabbitMQConfig.CRAWL_ERROR_ROUTING_KEY,
                errorMessage
            );
            log.debug("Published error message to queue: {}", errorMessage.getErrorType());
        } catch (Exception e) {
            log.error("Failed to publish error message to RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
