package com.ron.javainfohunter.crawler.handler;

/**
 * Enumeration of error types that can occur during crawler operations.
 *
 * <p>This enum classifies different types of errors to enable appropriate
 * handling strategies, retry logic, and monitoring.</p>
 *
 * <p><b>Error Classification:</b></p>
 * <ul>
 *   <li><b>CONNECTION_ERROR</b>: Network issues, timeouts, DNS failures</li>
 *   <li><b>PARSE_ERROR</b>: Invalid RSS/Atom format, malformed XML</li>
 *   <li><b>DATABASE_ERROR</b>: Database connection, constraint violations</li>
 *   <li><b>QUEUE_ERROR</b>: RabbitMQ publishing failures</li>
 *   <li><b>SCHEDULER_ERROR</b>: Task scheduling failures</li>
 *   <li><b>VALIDATION_ERROR</b>: Invalid input data, missing fields</li>
 *   <li><b>ENCODING_ERROR</b>: Character encoding issues</li>
 *   <li><b>UNKNOWN_ERROR</b>: Uncategorized errors</li>
 * </ul>
 *
 * @see CrawlErrorHandler
 */
public enum ErrorType {

    /**
     * Network connection errors.
     * <p>Includes: timeouts, connection refused, DNS failures, SSL errors</p>
     * <p><b>Retryable:</b> Yes</p>
     */
    CONNECTION_ERROR("Connection Error", true, 5),

    /**
     * RSS/Atom feed parsing errors.
     * <p>Includes: malformed XML, invalid feed structure, missing required elements</p>
     * <p><b>Retryable:</b> No (feed structure issues won't change on retry)</p>
     */
    PARSE_ERROR("Parse Error", false, 0),

    /**
     * Database operation errors.
     * <p>Includes: connection failures, constraint violations, transaction errors</p>
     * <p><b>Retryable:</b> Yes (for transient errors)</p>
     */
    DATABASE_ERROR("Database Error", true, 3),

    /**
     * Message queue publishing errors.
     * <p>Includes: RabbitMQ connection failures, exchange not found, queue full</p>
     * <p><b>Retryable:</b> Yes</p>
     */
    QUEUE_ERROR("Queue Error", true, 3),

    /**
     * Scheduler execution errors.
     * <p>Includes: task scheduling failures, thread pool exhaustion</p>
     * <p><b>Retryable:</b> No (scheduler will retry automatically)</p>
     */
    SCHEDULER_ERROR("Scheduler Error", false, 0),

    /**
     * Data validation errors.
     * <p>Includes: missing required fields, invalid data formats</p>
     * <p><b>Retryable:</b> No (data won't become valid on retry)</p>
     */
    VALIDATION_ERROR("Validation Error", false, 0),

    /**
     * Character encoding errors.
     * <p>Includes: unsupported encoding, malformed byte sequences</p>
     * <p><b>Retryable:</b> No</p>
     */
    ENCODING_ERROR("Encoding Error", false, 0),

    /**
     * Unknown or uncategorized errors.
     * <p>Catch-all for errors that don't fit other categories</p>
     * <p><b>Retryable:</b> No (conservative approach)</p>
     */
    UNKNOWN_ERROR("Unknown Error", false, 0);

    private final String displayName;
    private final boolean retryable;
    private final int defaultMaxRetries;

    ErrorType(String displayName, boolean retryable, int defaultMaxRetries) {
        this.displayName = displayName;
        this.retryable = retryable;
        this.defaultMaxRetries = defaultMaxRetries;
    }

    /**
     * Get the human-readable display name for this error type.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determine if this error type is retryable.
     *
     * @return true if retry should be attempted
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Get the default maximum retry attempts for this error type.
     *
     * @return default max retries
     */
    public int getDefaultMaxRetries() {
        return defaultMaxRetries;
    }
}
