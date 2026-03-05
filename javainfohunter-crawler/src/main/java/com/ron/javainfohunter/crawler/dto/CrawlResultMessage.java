package com.ron.javainfohunter.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Message DTO for crawl result notifications.
 *
 * <p>This message is published to the {@code crawler.crawl.result.queue}
 * and contains statistics about the crawl operation.</p>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * RSS Feed Crawler → Crawl Result Queue → Monitoring/Analytics
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResultMessage {

    /**
     * RSS source ID that was crawled.
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
     * Crawl status (SUCCESS, PARTIAL, FAILED).
     */
    private CrawlStatus status;

    /**
     * Number of articles successfully crawled.
     */
    private Integer articlesCrawled;

    /**
     * Number of new articles (not seen before).
     */
    private Integer newArticles;

    /**
     * Number of duplicate articles (already in database).
     */
    private Integer duplicateArticles;

    /**
     * Number of articles that failed to process.
     */
    private Integer failedArticles;

    /**
     * Crawl start timestamp.
     */
    private Instant startTime;

    /**
     * Crawl end timestamp.
     */
    private Instant endTime;

    /**
     * Crawl duration in milliseconds.
     */
    private Long durationMs;

    /**
     * Error message if crawl failed.
     */
    private String errorMessage;

    /**
     * Error stack trace (optional, for debugging).
     */
    private String errorTrace;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;

    /**
     * Crawl status enum.
     */
    public enum CrawlStatus {
        /**
         * Crawl completed successfully.
         */
        SUCCESS,

        /**
         * Crawl partially successful (some articles failed).
         */
        PARTIAL,

        /**
         * Crawl failed completely.
         */
        FAILED,

        /**
         * Crawl skipped (e.g., source not active).
         */
        SKIPPED
    }

}
