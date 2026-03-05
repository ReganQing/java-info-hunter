package com.ron.javainfohunter.crawler.publisher;

import com.ron.javainfohunter.crawler.config.RabbitMQConfig;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publisher for crawl error messages.
 *
 * <p>This publisher handles publishing {@link CrawlErrorMessage} objects to the
 * {@code crawler.crawl.error.queue} with the following features:</p>
 *
 * <ul>
 *   <li>Automatic error type classification</li>
 *   <li>Retry information tracking (currentAttempt, maxAttempts)</li>
 *   <li>Debugging context metadata</li>
 *   <li>Stack trace capture</li>
 *   <li>Graceful error handling (never cascades)</li>
 * </ul>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * RSS Feed Crawler → ErrorPublisher → Crawl Error Queue → Alerting System
 * </pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. RabbitTemplate is thread-safe.</p>
 *
 * @see CrawlErrorMessage
 */
@Slf4j
@Service
public class ErrorPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Default maximum retry attempts for retryable errors.
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * Default retry delay for retryable errors in milliseconds.
     */
    private static final long DEFAULT_RETRY_DELAY_MS = 60000; // 1 minute

    @Autowired
    public ErrorPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes an error message from an exception.
     *
     * <p>This method extracts error information from the exception including
     * message, stack trace, and determines if the error is retryable.</p>
     *
     * @param sourceUrl the source URL that caused the error
     * @param exception the exception that occurred
     * @param errorType the type of error
     */
    public void publishError(String sourceUrl, Exception exception, ErrorType errorType) {
        if (sourceUrl == null || exception == null) {
            log.warn("Attempted to publish error with null sourceUrl or exception, skipping");
            return;
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            CrawlErrorMessage errorMessage = CrawlErrorMessage.builder()
                .rssSourceUrl(sourceUrl)
                .errorType(errorType)
                .errorMessage(exception.getMessage())
                .errorTrace(getStackTrace(exception))
                .errorTime(Instant.now())
                .retryable(isRetryable(exception))
                .retryDelayMs(DEFAULT_RETRY_DELAY_MS)
                .retryAttempt(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .build();

            log.debug("Publishing error message: correlationId={}, sourceUrl={}, errorType={}",
                correlationId, sourceUrl, errorType);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_ERROR_ROUTING_KEY,
                errorMessage,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    return message;
                }
            );

            log.info("Successfully published error message: correlationId={}, sourceUrl={}, errorType={}",
                correlationId, sourceUrl, errorType);

        } catch (Exception e) {
            log.error("Failed to publish error message: correlationId={}, sourceUrl={}, error={}",
                correlationId, sourceUrl, e.getMessage(), e);
            // Never throw - we don't want publishing errors to cascade
        }
    }

    /**
     * Publishes an error message with full context.
     *
     * <p>This method allows providing additional context metadata for debugging
     * and includes retry information.</p>
     *
     * @param sourceUrl the source URL that caused the error
     * @param message the error message
     * @param errorType the type of error
     * @param context additional debugging context (optional)
     */
    public void publishError(String sourceUrl, String message, ErrorType errorType, Map<String, Object> context) {
        if (sourceUrl == null || message == null) {
            log.warn("Attempted to publish error with null sourceUrl or message, skipping");
            return;
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            CrawlErrorMessage errorMessage = CrawlErrorMessage.builder()
                .rssSourceUrl(sourceUrl)
                .errorType(errorType)
                .errorMessage(message)
                .errorTime(Instant.now())
                .retryable(false) // Manual error messages are not retryable by default
                .retryDelayMs(0L)
                .retryAttempt(0)
                .maxRetries(0)
                .context(context)
                .build();

            log.debug("Publishing error message: correlationId={}, sourceUrl={}, errorType={}",
                correlationId, sourceUrl, errorType);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_ERROR_ROUTING_KEY,
                errorMessage,
                msg -> {
                    msg.getMessageProperties().setCorrelationId(correlationId);
                    return msg;
                }
            );

            log.info("Successfully published error message: correlationId={}, sourceUrl={}, errorType={}",
                correlationId, sourceUrl, errorType);

        } catch (Exception e) {
            log.error("Failed to publish error message: correlationId={}, sourceUrl={}, error={}",
                correlationId, sourceUrl, e.getMessage(), e);
            // Never throw - we don't want publishing errors to cascade
        }
    }

    /**
     * Publishes an error message with RSS source context.
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param exception the exception that occurred
     * @param errorType the type of error
     */
    public void publishError(Long rssSourceId, String rssSourceName, String rssSourceUrl,
                            Exception exception, ErrorType errorType) {
        if (exception == null) {
            log.warn("Attempted to publish error with null exception, skipping");
            return;
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            CrawlErrorMessage errorMessage = CrawlErrorMessage.builder()
                .rssSourceId(rssSourceId)
                .rssSourceName(rssSourceName)
                .rssSourceUrl(rssSourceUrl)
                .errorType(errorType)
                .errorMessage(exception.getMessage())
                .errorTrace(getStackTrace(exception))
                .errorTime(Instant.now())
                .retryable(isRetryable(exception))
                .retryDelayMs(DEFAULT_RETRY_DELAY_MS)
                .retryAttempt(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .build();

            log.debug("Publishing error message: correlationId={}, sourceId={}, errorType={}",
                correlationId, rssSourceId, errorType);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_ERROR_ROUTING_KEY,
                errorMessage,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    return message;
                }
            );

            log.info("Successfully published error message: correlationId={}, sourceId={}, errorType={}",
                correlationId, rssSourceId, errorType);

        } catch (Exception e) {
            log.error("Failed to publish error message: correlationId={}, sourceId={}, error={}",
                correlationId, rssSourceId, e.getMessage(), e);
            // Never throw - we don't want publishing errors to cascade
        }
    }

    /**
     * Publishes an error message with article context.
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param articleGuid the article GUID that caused the error
     * @param exception the exception that occurred
     * @param errorType the type of error
     */
    public void publishError(Long rssSourceId, String rssSourceName, String rssSourceUrl,
                            String articleGuid, Exception exception, ErrorType errorType) {
        if (exception == null) {
            log.warn("Attempted to publish error with null exception, skipping");
            return;
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            CrawlErrorMessage errorMessage = CrawlErrorMessage.builder()
                .rssSourceId(rssSourceId)
                .rssSourceName(rssSourceName)
                .rssSourceUrl(rssSourceUrl)
                .articleGuid(articleGuid)
                .errorType(errorType)
                .errorMessage(exception.getMessage())
                .errorTrace(getStackTrace(exception))
                .errorTime(Instant.now())
                .retryable(isRetryable(exception))
                .retryDelayMs(DEFAULT_RETRY_DELAY_MS)
                .retryAttempt(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .build();

            log.debug("Publishing error message: correlationId={}, sourceId={}, articleGuid={}, errorType={}",
                correlationId, rssSourceId, articleGuid, errorType);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_ERROR_ROUTING_KEY,
                errorMessage,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    return message;
                }
            );

            log.info("Successfully published error message: correlationId={}, sourceId={}, articleGuid={}, errorType={}",
                correlationId, rssSourceId, articleGuid, errorType);

        } catch (Exception e) {
            log.error("Failed to publish error message: correlationId={}, sourceId={}, error={}",
                correlationId, rssSourceId, e.getMessage(), e);
            // Never throw - we don't want publishing errors to cascade
        }
    }

    /**
     * Publishes a retry error message with attempt information.
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param exception the exception that occurred
     * @param errorType the type of error
     * @param currentAttempt the current retry attempt
     * @param maxAttempts the maximum retry attempts
     */
    public void publishRetryError(Long rssSourceId, String rssSourceName, String rssSourceUrl,
                                  Exception exception, ErrorType errorType,
                                  int currentAttempt, int maxAttempts) {
        if (exception == null) {
            log.warn("Attempted to publish error with null exception, skipping");
            return;
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            CrawlErrorMessage errorMessage = CrawlErrorMessage.builder()
                .rssSourceId(rssSourceId)
                .rssSourceName(rssSourceName)
                .rssSourceUrl(rssSourceUrl)
                .errorType(errorType)
                .errorMessage(exception.getMessage())
                .errorTrace(getStackTrace(exception))
                .errorTime(Instant.now())
                .retryable(currentAttempt < maxAttempts)
                .retryDelayMs(calculateRetryDelay(currentAttempt))
                .retryAttempt(currentAttempt)
                .maxRetries(maxAttempts)
                .build();

            log.debug("Publishing retry error message: correlationId={}, sourceId={}, attempt={}/{}, errorType={}",
                correlationId, rssSourceId, currentAttempt, maxAttempts, errorType);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_ERROR_ROUTING_KEY,
                errorMessage,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    return message;
                }
            );

            log.info("Successfully published retry error message: correlationId={}, sourceId={}, attempt={}/{}, errorType={}",
                correlationId, rssSourceId, currentAttempt, maxAttempts, errorType);

        } catch (Exception e) {
            log.error("Failed to publish error message: correlationId={}, sourceId={}, error={}",
                correlationId, rssSourceId, e.getMessage(), e);
            // Never throw - we don't want publishing errors to cascade
        }
    }

    /**
     * Extracts stack trace from exception.
     *
     * @param exception the exception
     * @return stack trace as string
     */
    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Determines if an exception is retryable.
     *
     * @param exception the exception
     * @return true if retryable, false otherwise
     */
    private boolean isRetryable(Exception exception) {
        // Network errors are typically retryable
        if (exception instanceof java.io.IOException ||
            exception instanceof java.net.SocketTimeoutException ||
            exception instanceof java.net.ConnectException ||
            exception instanceof java.net.UnknownHostException) {
            return true;
        }
        // Other errors are not retryable by default
        return false;
    }

    /**
     * Calculates retry delay with exponential backoff.
     *
     * @param attempt the current attempt number (1-based)
     * @return delay in milliseconds
     */
    private long calculateRetryDelay(int attempt) {
        // Exponential backoff: 1 minute, 2 minutes, 4 minutes, 8 minutes...
        long baseDelay = 60000; // 1 minute
        long delay = baseDelay * (1L << (attempt - 1));
        return Math.min(delay, 600000); // Max 10 minutes
    }
}
