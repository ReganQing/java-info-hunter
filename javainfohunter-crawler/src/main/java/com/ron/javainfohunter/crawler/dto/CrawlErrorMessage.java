package com.ron.javainfohunter.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Message DTO for crawl error notifications.
 *
 * <p>This message is published to the {@code crawler.crawl.error.queue}
 * and contains detailed error information for monitoring and alerting.</p>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * RSS Feed Crawler → Crawl Error Queue → Alerting System
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlErrorMessage {

    /**
     * RSS source ID that caused the error.
     */
    private Long rssSourceId;

    /**
     * RSS source name.
     */
    private String rssSourceName;

    /**
     * RSS source URL.
     */
    private String rssSourceUrl;

    /**
     * Error type (CONNECTION_ERROR, PARSE_ERROR, DATABASE_ERROR, etc.).
     */
    private ErrorType errorType;

    /**
     * Error message.
     */
    private String errorMessage;

    /**
     * Error stack trace.
     */
    private String errorTrace;

    /**
     * Error timestamp.
     */
    private Instant errorTime;

    /**
     * Whether this error is retryable.
     */
    private Boolean retryable;

    /**
     * Suggested retry delay in milliseconds.
     */
    private Long retryDelayMs;

    /**
     * Current retry attempt.
     */
    private Integer retryAttempt;

    /**
     * Maximum retry attempts.
     */
    private Integer maxRetries;

    /**
     * Article GUID that caused the error (if applicable).
     */
    private String articleGuid;

    /**
     * Additional context for debugging.
     */
    private Map<String, Object> context;

    /**
     * Error type enum.
     */
    public enum ErrorType {
        /**
         * Network connection error (timeout, refused, etc.).
         */
        CONNECTION_ERROR,

        /**
         * RSS/Atom feed parsing error.
         */
        PARSE_ERROR,

        /**
         * Database operation error.
         */
        DATABASE_ERROR,

        /**
         * Message queue publishing error.
         */
        QUEUE_ERROR,

        /**
         * Content encoding error.
         */
        ENCODING_ERROR,

        /**
         * Validation error.
         */
        VALIDATION_ERROR,

        /**
         * Unknown error.
         */
        UNKNOWN,

        /**
         * Scheduler error (scheduled task failure).
         */
        SCHEDULER_ERROR
    }

}
