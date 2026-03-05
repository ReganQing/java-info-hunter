package com.ron.javainfohunter.crawler.exception;

/**
 * Exception thrown when a connection to an RSS/Atom feed fails.
 *
 * <p>This exception indicates network-related issues such as timeouts,
 * connection refusals, or HTTP errors (4xx, 5xx).</p>
 *
 * <p><b>Common causes:</b></p>
 * <ul>
 *   <li>Network timeout</li>
 *   <li>Connection refused</li>
 *   <li>HTTP 404 Not Found</li>
 *   <li>HTTP 5xx Server Error</li>
 *   <li>DNS resolution failure</li>
 *   <li>SSL/TLS certificate issues</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public class FeedConnectionException extends RuntimeException {

    /**
     * URL of the feed that failed to connect
     */
    private final String feedUrl;

    /**
     * HTTP status code (if applicable)
     */
    private final Integer httpStatusCode;

    /**
     * Whether this error is retryable
     */
    private final boolean retryable;

    /**
     * Construct a new FeedConnectionException with the specified detail message
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     */
    public FeedConnectionException(String message, String feedUrl) {
        this(message, feedUrl, null, true);
    }

    /**
     * Construct a new FeedConnectionException with the specified detail message and HTTP status code
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     * @param httpStatusCode the HTTP status code (if applicable)
     */
    public FeedConnectionException(String message, String feedUrl, Integer httpStatusCode) {
        this(message, feedUrl, httpStatusCode, isRetryableForStatusCode(httpStatusCode));
    }

    /**
     * Construct a new FeedConnectionException with the specified detail message and cause
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     * @param cause the cause of the exception
     */
    public FeedConnectionException(String message, String feedUrl, Throwable cause) {
        this(message, feedUrl, null, true, cause);
    }

    /**
     * Construct a new FeedConnectionException with all parameters
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     * @param httpStatusCode the HTTP status code (if applicable)
     * @param retryable whether this error is retryable
     */
    public FeedConnectionException(String message, String feedUrl, Integer httpStatusCode, boolean retryable) {
        super(message);
        this.feedUrl = feedUrl;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    /**
     * Construct a new FeedConnectionException with all parameters and cause
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     * @param httpStatusCode the HTTP status code (if applicable)
     * @param retryable whether this error is retryable
     * @param cause the cause of the exception
     */
    public FeedConnectionException(String message, String feedUrl, Integer httpStatusCode,
                                   boolean retryable, Throwable cause) {
        super(message, cause);
        this.feedUrl = feedUrl;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    /**
     * Determine if an error is retryable based on HTTP status code
     *
     * @param statusCode the HTTP status code
     * @return true if retryable, false otherwise
     */
    private static boolean isRetryableForStatusCode(Integer statusCode) {
        if (statusCode == null) {
            return true; // Network errors are generally retryable
        }
        // Retry on 5xx errors, 429 (Too Many Requests), and 408 (Request Timeout)
        // Don't retry on 4xx client errors (except 429 and 408)
        return statusCode >= 500 || statusCode == 429 || statusCode == 408;
    }

    /**
     * Get the URL of the feed that failed to connect
     *
     * @return the feed URL
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * Get the HTTP status code (if applicable)
     *
     * @return the HTTP status code, or null if not applicable
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Check if this error is retryable
     *
     * @return true if retryable, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return "FeedConnectionException{" +
                "feedUrl='" + feedUrl + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                ", retryable=" + retryable +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
