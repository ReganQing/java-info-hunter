package com.ron.javainfohunter.crawler.exception;

/**
 * Exception thrown when an RSS/Atom feed cannot be parsed.
 *
 * <p>This exception indicates that the feed format is invalid,
 * malformed, or not supported by the Rome library.</p>
 *
 * <p><b>Common causes:</b></p>
 * <ul>
 *   <li>Malformed XML in the feed</li>
 *   <li>Invalid RSS/Atom format version</li>
 *   <li>Missing required feed elements</li>
 *   <li>Encoding issues</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public class FeedParseException extends RuntimeException {

    /**
     * URL of the feed that failed to parse
     */
    private final String feedUrl;

    /**
     * Construct a new FeedParseException with the specified detail message
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     */
    public FeedParseException(String message, String feedUrl) {
        super(message);
        this.feedUrl = feedUrl;
    }

    /**
     * Construct a new FeedParseException with the specified detail message and cause
     *
     * @param message the detail message
     * @param feedUrl the URL of the feed that failed
     * @param cause the cause of the exception
     */
    public FeedParseException(String message, String feedUrl, Throwable cause) {
        super(message, cause);
        this.feedUrl = feedUrl;
    }

    /**
     * Get the URL of the feed that failed to parse
     *
     * @return the feed URL
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    @Override
    public String toString() {
        return "FeedParseException{" +
                "feedUrl='" + feedUrl + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
