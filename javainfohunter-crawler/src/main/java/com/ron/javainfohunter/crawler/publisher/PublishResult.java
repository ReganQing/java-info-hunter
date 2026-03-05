package com.ron.javainfohunter.crawler.publisher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a batch publish operation.
 *
 * <p>This class contains statistics about batch publishing,
 * including success/failure counts and details of failed messages.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishResult {

    /**
     * Total number of messages in the batch.
     */
    private int totalCount;

    /**
     * Number of successfully published messages.
     */
    @Builder.Default
    private int successCount = 0;

    /**
     * Number of failed messages.
     */
    @Builder.Default
    private int failureCount = 0;

    /**
     * List of failed messages with their error details.
     */
    @Builder.Default
    private List<FailedMessage> failedMessages = new ArrayList<>();

    /**
     * Total time taken for the batch operation in milliseconds.
     */
    private long durationMs;

    /**
     * Represents a failed message with error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedMessage {

        /**
         * The message that failed to publish.
         */
        private Object message;

        /**
         * Error message describing the failure.
         */
        private String errorMessage;

        /**
         * Number of retry attempts made.
         */
        private int attempts;

        /**
         * The exception that caused the failure (if any).
         */
        private Throwable exception;
    }

    /**
     * Adds a failed message to this result.
     *
     * @param message the failed message
     * @param errorMessage the error message
     * @param attempts number of attempts made
     * @param exception the exception (optional)
     */
    public void addFailedMessage(Object message, String errorMessage, int attempts, Throwable exception) {
        FailedMessage failedMessage = FailedMessage.builder()
            .message(message)
            .errorMessage(errorMessage)
            .attempts(attempts)
            .exception(exception)
            .build();
        this.failedMessages.add(failedMessage);
        this.failureCount++;
    }

    /**
     * Checks if the batch operation was completely successful.
     *
     * @return true if all messages were published successfully
     */
    public boolean isCompleteSuccess() {
        return failureCount == 0 && successCount == totalCount;
    }

    /**
     * Checks if the batch operation was a complete failure.
     *
     * @return true if all messages failed to publish
     */
    public boolean isCompleteFailure() {
        return successCount == 0 && failureCount == totalCount;
    }

    /**
     * Gets an unmodifiable list of failed messages.
     *
     * @return unmodifiable list of failed messages
     */
    public List<FailedMessage> getFailedMessages() {
        return Collections.unmodifiableList(failedMessages);
    }

    /**
     * Gets the success rate as a percentage (0-100).
     *
     * @return success rate percentage
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (successCount * 100.0) / totalCount;
    }
}
