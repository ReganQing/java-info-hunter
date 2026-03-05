package com.ron.javainfohunter.crawler.exception;

/**
 * Exception thrown when message publishing fails after all retry attempts.
 *
 * <p>This exception indicates that a message could not be published to RabbitMQ
 * after the maximum number of retry attempts.</p>
 */
public class PublishException extends RuntimeException {

    /**
     * The message that failed to publish.
     */
    private final Object failedMessage;

    /**
     * Number of retry attempts made.
     */
    private final int attempts;

    /**
     * Creates a new PublishException.
     *
     * @param message the error message
     * @param failedMessage the message that failed to publish
     * @param attempts number of retry attempts made
     */
    public PublishException(String message, Object failedMessage, int attempts) {
        super(message);
        this.failedMessage = failedMessage;
        this.attempts = attempts;
    }

    /**
     * Creates a new PublishException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param failedMessage the message that failed to publish
     * @param attempts number of retry attempts made
     */
    public PublishException(String message, Throwable cause, Object failedMessage, int attempts) {
        super(message, cause);
        this.failedMessage = failedMessage;
        this.attempts = attempts;
    }

    /**
     * Gets the message that failed to publish.
     *
     * @return the failed message
     */
    public Object getFailedMessage() {
        return failedMessage;
    }

    /**
     * Gets the number of retry attempts made.
     *
     * @return number of attempts
     */
    public int getAttempts() {
        return attempts;
    }
}
