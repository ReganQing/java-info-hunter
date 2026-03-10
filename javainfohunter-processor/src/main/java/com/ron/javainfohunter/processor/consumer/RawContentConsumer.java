package com.ron.javainfohunter.processor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.ron.javainfohunter.processor.dto.RawContentMessage;
import com.ron.javainfohunter.processor.exception.ConsumerException;
import com.ron.javainfohunter.processor.service.ContentRoutingService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer for raw content messages from the crawler module.
 *
 * <p>This consumer listens to the {@code processor.raw.content.queue} and processes
 * raw RSS content messages by routing them to appropriate AI agents.</p>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * Crawler Module → crawler.direct exchange (raw.content) → processor.raw.content.queue → RawContentConsumer → ContentRoutingService
 * </pre>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Manual acknowledgment for reliable message processing</li>
 *   <li>JSON deserialization using Jackson</li>
 *   <li>Error handling with message rejection to DLQ on failure</li>
 *   <li>Message processing metrics tracking</li>
 * </ul>
 *
 * @see ContentRoutingService
 * @see RawContentMessage
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "javainfohunter.processor",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RawContentConsumer {

    private final ContentRoutingService routingService;
    private final ObjectMapper objectMapper;

    /**
     * Counter for successfully processed messages.
     */
    private final AtomicLong processedCount = new AtomicLong(0);

    /**
     * Counter for failed messages.
     */
    private final AtomicLong failedCount = new AtomicLong(0);

    /**
     * Consumes raw content messages from the crawler module.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Extracts the delivery tag for manual acknowledgment</li>
     *   <li>Parses the JSON message body to {@link RawContentMessage}</li>
     *   <li>Routes the message to AI agents via {@link ContentRoutingService}</li>
     *   <li>Acks the message on success</li>
     *   <li>Nacks and rejects on failure (sends to DLQ)</li>
     * </ol>
     *
     * @param message the raw message bytes
     * @param channel the RabbitMQ channel for manual ack
     * @param deliveryTag the delivery tag for acknowledgment
     * @throws ConsumerException if message processing fails
     */
    @RabbitListener(
        queues = "${javainfohunter.processor.queue.input-queue}",
        containerFactory = "rabbitListenerContainerFactory",
        ackMode = "MANUAL"
    )
    public void consumeRawContent(
        byte[] message,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        RawContentMessage contentMessage = null;
        String contentHash = "unknown";

        try {
            // Parse JSON to RawContentMessage
            contentMessage = objectMapper.readValue(
                new String(message, StandardCharsets.UTF_8),
                RawContentMessage.class
            );

            // Extract content hash for logging and error handling
            contentHash = contentMessage.getContentHash();

            log.debug("Received raw content message: guid={}, contentHash={}, title='{}'",
                contentMessage.getGuid(),
                contentHash,
                contentMessage.getTitle()
            );

            // Route to agents for processing
            routingService.routeToAgents(contentMessage);

            // Acknowledge successful processing
            channel.basicAck(deliveryTag, false);
            long processed = processedCount.incrementAndGet();

            log.info("Successfully processed raw content message: contentHash={}, guid={}, totalProcessed={}",
                contentHash,
                contentMessage.getGuid(),
                processed
            );

        } catch (ConsumerException e) {
            // Business logic exception - nack and reject to DLQ
            failedCount.incrementAndGet();
            log.error("Failed to process raw content message (ConsumerException): contentHash={}, error={}",
                contentHash, e.getMessage(), e);

            rejectMessage(channel, deliveryTag, false);

        } catch (Exception e) {
            // Unexpected exception - nack and reject to DLQ
            failedCount.incrementAndGet();
            log.error("Unexpected error consuming raw content message: contentHash={}, error={}",
                contentHash, e.getMessage(), e);

            rejectMessage(channel, deliveryTag, false);
        }
    }

    /**
     * Rejects a message by sending negative acknowledgment and requeuing flag.
     *
     * @param channel the RabbitMQ channel
     * @param deliveryTag the delivery tag to reject
     * @param requeue whether to requeue the message (false sends to DLQ)
     */
    private void rejectMessage(Channel channel, long deliveryTag, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (IOException ioException) {
            log.error("Failed to send NACK for delivery tag: {}", deliveryTag, ioException);
        }
    }

    /**
     * Returns the count of successfully processed messages.
     *
     * @return number of successfully processed messages
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    /**
     * Returns the count of failed messages.
     *
     * @return number of failed messages
     */
    public long getFailedCount() {
        return failedCount.get();
    }

    /**
     * Logs shutdown statistics.
     */
    @PreDestroy
    public void shutdown() {
        log.info("RawContentConsumer shutting down. Total processed: {}, Total failed: {}",
            processedCount.get(), failedCount.get());
    }
}
