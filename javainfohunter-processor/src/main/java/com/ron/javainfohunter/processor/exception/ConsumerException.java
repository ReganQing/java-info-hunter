package com.ron.javainfohunter.processor.exception;

import lombok.Getter;

/**
 * Exception thrown when message consumption fails.
 *
 * <p>This exception indicates that a message could not be processed
 * after being consumed from RabbitMQ.</p>
 */
@Getter
public class ConsumerException extends RuntimeException {

    /**
     * The content hash of the message that failed processing.
     */
    private final String contentHash;

    /**
     * Creates a new ConsumerException.
     *
     * @param message the error message
     * @param contentHash the content hash of the failed message
     */
    public ConsumerException(String message, String contentHash) {
        super(message);
        this.contentHash = contentHash;
    }

    /**
     * Creates a new ConsumerException with a cause.
     *
     * @param message the error message
     * @param contentHash the content hash of the failed message
     * @param cause the underlying cause
     */
    public ConsumerException(String message, String contentHash, Throwable cause) {
        super(message, cause);
        this.contentHash = contentHash;
    }
}
