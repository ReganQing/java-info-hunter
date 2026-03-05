package com.ron.javainfohunter.crawler.exception;

import java.util.UUID;

/**
 * Exception thrown when RabbitMQ broker does not confirm message publication within timeout.
 *
 * <p>This exception indicates that a publisher confirm was not received from the broker
 * within the configured timeout period.</p>
 */
public class ConfirmTimeoutException extends RuntimeException {

    /**
     * Correlation ID of the unconfirmed message.
     */
    private final UUID correlationId;

    /**
     * Timeout in milliseconds.
     */
    private final long timeoutMs;

    /**
     * Creates a new ConfirmTimeoutException.
     *
     * @param correlationId the correlation ID of the unconfirmed message
     * @param timeoutMs the timeout in milliseconds
     */
    public ConfirmTimeoutException(UUID correlationId, long timeoutMs) {
        super(String.format("Publisher confirm timeout for correlation ID %s after %d ms",
            correlationId, timeoutMs));
        this.correlationId = correlationId;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Creates a new ConfirmTimeoutException with a cause.
     *
     * @param correlationId the correlation ID of the unconfirmed message
     * @param timeoutMs the timeout in milliseconds
     * @param cause the underlying cause
     */
    public ConfirmTimeoutException(UUID correlationId, long timeoutMs, Throwable cause) {
        super(String.format("Publisher confirm timeout for correlation ID %s after %d ms",
            correlationId, timeoutMs), cause);
        this.correlationId = correlationId;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Gets the correlation ID of the unconfirmed message.
     *
     * @return the correlation ID
     */
    public UUID getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
}
