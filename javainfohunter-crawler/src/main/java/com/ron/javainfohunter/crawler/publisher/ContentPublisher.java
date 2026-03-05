package com.ron.javainfohunter.crawler.publisher;

import com.ron.javainfohunter.crawler.config.RabbitMQConfig;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.exception.ConfirmTimeoutException;
import com.ron.javainfohunter.crawler.exception.PublishException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publisher for raw RSS content messages.
 *
 * <p>This publisher handles publishing {@link RawContentMessage} objects to the
 * {@code crawler.raw.content.queue} with the following features:</p>
 *
 * <ul>
 *   <li>Publisher confirms - waits for broker ACK before considering message sent</li>
 *   <li>Automatic retry with exponential backoff (1s, 2s, 4s, 8s, max 5 attempts)</li>
 *   <li>Correlation ID generation for message tracing</li>
 *   <li>Batch publishing support</li>
 *   <li>Error isolation - failures don't stop the publishing process</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. RabbitTemplate is thread-safe,
 * and confirm tracking uses ConcurrentHashMap for concurrent access.</p>
 *
 * @see RawContentMessage
 * @see PublishResult
 */
@Slf4j
@Service
public class ContentPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final Map<String, CompletableFuture<Boolean>> pendingConfirms;

    /**
     * Maximum number of retry attempts for failed publishing.
     */
    private static final int MAX_RETRY_ATTEMPTS = 5;

    /**
     * Base delay for exponential backoff in milliseconds.
     */
    private static final long BASE_RETRY_DELAY_MS = 1000;

    /**
     * Maximum delay between retries in milliseconds.
     */
    private static final long MAX_RETRY_DELAY_MS = 8000;

    /**
     * Timeout for waiting for publisher confirms in milliseconds.
     */
    private static final long CONFIRM_TIMEOUT_MS = 5000;

    @Autowired
    public ContentPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.pendingConfirms = new ConcurrentHashMap<>();

        // Set up confirm callback to complete futures
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                String correlationId = correlationData.getId();
                CompletableFuture<Boolean> future = pendingConfirms.remove(correlationId);
                if (future != null) {
                    future.complete(ack);
                    if (!ack) {
                        log.error("Publisher confirm NACK for correlation ID: {}, cause: {}",
                            correlationId, cause);
                    }
                }
            }
        });
    }

    /**
     * Publishes a single raw content message to RabbitMQ.
     *
     * <p>This method will retry up to 5 times with exponential backoff if publishing fails.
     * It waits for broker confirmation before returning.</p>
     *
     * @param message the raw content message to publish
     * @return true if the message was published successfully, false otherwise
     * @throws PublishException if publishing fails after all retry attempts
     */
    public boolean publishRawContent(RawContentMessage message) {
        if (message == null) {
            log.warn("Attempted to publish null message, skipping");
            return false;
        }

        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++;
            try {
                log.debug("Publishing raw content message (attempt {}/{}): correlationId={}, guid={}",
                    attempt, MAX_RETRY_ATTEMPTS, correlationId, message.getGuid());

                // Create a future to wait for confirmation
                CompletableFuture<Boolean> confirmFuture = new CompletableFuture<>();
                pendingConfirms.put(correlationId, confirmFuture);

                // Send the message
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CRAWLER_EXCHANGE,
                    RabbitMQConfig.RAW_CONTENT_ROUTING_KEY,
                    message,
                    correlationData
                );

                // Wait for confirmation
                Boolean ack = waitForConfirms(correlationId);
                if (ack) {
                    log.info("Successfully published raw content message: correlationId={}, guid={}",
                        correlationId, message.getGuid());
                    return true;
                } else {
                    log.warn("Publisher NACK received for correlationId: {}, attempt {}", correlationId, attempt);
                    lastException = new PublishException(
                        "Broker rejected the message",
                        message,
                        attempt
                    );
                }

            } catch (ConfirmTimeoutException e) {
                log.error("Confirm timeout for correlationId: {} on attempt {}", correlationId, attempt);
                lastException = e;
            } catch (Exception e) {
                log.error("Failed to publish message on attempt {}: {}", attempt, e.getMessage(), e);
                lastException = e;
            } finally {
                // Clean up pending confirm if still present
                pendingConfirms.remove(correlationId);
            }

            // Retry with exponential backoff if not the last attempt
            if (attempt < MAX_RETRY_ATTEMPTS) {
                long delayMs = calculateBackoffDelay(attempt);
                log.info("Retrying in {} ms...", delayMs);
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Thread interrupted during retry delay");
                    throw new PublishException(
                        "Publishing interrupted during retry",
                        message,
                        attempt
                    );
                }
            }
        }

        // All retries exhausted
        log.error("Failed to publish message after {} attempts: guid={}, correlationId={}",
            MAX_RETRY_ATTEMPTS, message.getGuid(), correlationId);
        throw new PublishException(
            String.format("Failed to publish message after %d attempts", MAX_RETRY_ATTEMPTS),
            lastException,
            message,
            MAX_RETRY_ATTEMPTS
        );
    }

    /**
     * Publishes a batch of raw content messages to RabbitMQ.
     *
     * <p>This method publishes all messages in the batch, collecting success/failure statistics.
     * Failed messages don't stop the batch processing - each message is handled independently.</p>
     *
     * @param messages the list of raw content messages to publish
     * @return PublishResult containing statistics and details of failed messages
     */
    public PublishResult publishRawContentBatch(List<RawContentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Attempted to publish empty batch, returning empty result");
            return PublishResult.builder()
                .totalCount(0)
                .successCount(0)
                .failureCount(0)
                .durationMs(0)
                .build();
        }

        long startTime = System.currentTimeMillis();
        PublishResult result = PublishResult.builder()
            .totalCount(messages.size())
            .successCount(0)
            .failureCount(0)
            .build();

        log.info("Starting batch publish of {} raw content messages", messages.size());

        for (RawContentMessage message : messages) {
            try {
                publishRawContent(message);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (PublishException e) {
                log.error("Failed to publish message in batch: guid={}", message.getGuid(), e);
                result.addFailedMessage(
                    message,
                    e.getMessage(),
                    e.getAttempts(),
                    e.getCause()
                );
            } catch (Exception e) {
                log.error("Unexpected error publishing message in batch: guid={}", message.getGuid(), e);
                result.addFailedMessage(
                    message,
                    "Unexpected error: " + e.getMessage(),
                    0,
                    e
                );
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        result.setDurationMs(duration);

        log.info("Batch publish completed: total={}, success={}, failure={}, duration={}ms",
            result.getTotalCount(), result.getSuccessCount(),
            result.getFailureCount(), duration);

        return result;
    }

    /**
     * Waits for publisher confirmation from the broker.
     *
     * @param correlationId the correlation ID to wait for
     * @return true if ACK received, false if NACK received
     * @throws ConfirmTimeoutException if confirmation not received within timeout
     */
    private Boolean waitForConfirms(String correlationId) {
        CompletableFuture<Boolean> future = pendingConfirms.get(correlationId);
        if (future == null) {
            log.warn("No pending confirm found for correlationId: {}", correlationId);
            return false;
        }

        try {
            return future.get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingConfirms.remove(correlationId);
            throw new ConfirmTimeoutException(UUID.fromString(correlationId), CONFIRM_TIMEOUT_MS);
        } catch (Exception e) {
            pendingConfirms.remove(correlationId);
            throw new ConfirmTimeoutException(UUID.fromString(correlationId), CONFIRM_TIMEOUT_MS, e);
        }
    }

    /**
     * Calculates exponential backoff delay for retry attempts.
     *
     * @param attempt the current attempt number (1-based)
     * @return delay in milliseconds
     */
    private long calculateBackoffDelay(int attempt) {
        // Exponential backoff: 1s, 2s, 4s, 8s, 8s, 8s...
        long delay = BASE_RETRY_DELAY_MS * (1L << (attempt - 1));
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    /**
     * Gets the number of messages currently waiting for confirmation.
     *
     * @return count of pending confirms
     */
    public int getPendingConfirmCount() {
        return pendingConfirms.size();
    }
}
