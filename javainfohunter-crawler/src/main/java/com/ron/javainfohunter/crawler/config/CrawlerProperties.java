package com.ron.javainfohunter.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Crawler module.
 *
 * <p>This class encapsulates all configurable parameters for RSS feed crawling,
 * including scheduler settings, feed processing limits, and retry behavior.</p>
 *
 * <p><b>Configuration Structure:</b></p>
 * <pre>
 * javainfohunter:
 *   crawler:
 *     enabled: true
 *     scheduler:
 *       enabled: true
 *       initial-delay: 30000  # 30 seconds
 *       fixed-rate: 3600000   # 1 hour
 *     feed:
 *       max-articles-per-feed: 100
 *       connection-timeout: 30000
 *       read-timeout: 60000
 *       user-agent: "JavaInfoHunter/1.0"
 *     processing:
 *       batch-size: 50
 *       max-retries: 3
 *       retry-backoff: 60000
 *     deduplication:
 *       enabled: true
 *       hash-algorithm: "SHA-256"
 * </pre>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@Component
@ConfigurationProperties(prefix = "javainfohunter.crawler")
public class CrawlerProperties {

    /**
     * Enable/disable the crawler module.
     */
    private boolean enabled = true;

    /**
     * Scheduler configuration for periodic crawling.
     */
    private Scheduler scheduler = new Scheduler();

    /**
     * Feed processing configuration.
     */
    private Feed feed = new Feed();

    /**
     * Content processing configuration.
     */
    private Processing processing = new Processing();

    /**
     * Retry configuration.
     */
    private Retry retry = new Retry();

    /**
     * Deduplication configuration.
     */
    private Deduplication deduplication = new Deduplication();

    /**
     * Scheduler configuration properties.
     */
    @Data
    public static class Scheduler {
        /**
         * Enable/disable scheduled crawling.
         */
        private boolean enabled = true;

        /**
         * Initial delay before first crawl (milliseconds).
         * Default: 30 seconds
         */
        private long initialDelay = 30000;

        /**
         * Fixed rate between crawls (milliseconds).
         * Default: 1 hour
         */
        private long fixedRate = 3600000;
    }

    /**
     * Feed processing configuration properties.
     */
    @Data
    public static class Feed {
        /**
         * Maximum number of articles to process per feed.
         * Prevents memory issues with large feeds.
         */
        private int maxArticlesPerFeed = 100;

        /**
         * Connection timeout for fetching RSS feeds (milliseconds).
         * Default: 30 seconds
         */
        private int connectionTimeout = 30000;

        /**
         * Read timeout for fetching RSS feeds (milliseconds).
         * Default: 60 seconds
         */
        private int readTimeout = 60000;

        /**
         * User-Agent header for HTTP requests.
         * Some feeds require specific user agents.
         */
        private String userAgent = "JavaInfoHunter/1.0 (+https://github.com/yourusername/javainfohunter)";
    }

    /**
     * Content processing configuration properties.
     */
    @Data
    public static class Processing {
        /**
         * Batch size for processing articles.
         * Affects database batch insert performance.
         */
        private int batchSize = 50;

        /**
         * Maximum number of retry attempts for failed processing.
         * @deprecated Use {@link Retry#maxAttempts} instead
         */
        @Deprecated
        private int maxRetries = 3;

        /**
         * Backoff time between retries (milliseconds).
         * @deprecated Use {@link Retry#initialDelay} and {@link Retry#backoffMultiplier} instead
         */
        @Deprecated
        private long retryBackoff = 60000;
    }

    /**
     * Retry configuration properties.
     */
    @Data
    public static class Retry {
        /**
         * Maximum number of retry attempts.
         * Default: 3
         */
        private int maxAttempts = 3;

        /**
         * Initial backoff delay in milliseconds.
         * Default: 1000ms (1 second)
         */
        private long initialDelay = 1000;

        /**
         * Exponential backoff multiplier.
         * Default: 2.0 (doubles delay each retry: 1s, 2s, 4s, ...)
         */
        private double backoffMultiplier = 2.0;

        /**
         * Maximum backoff delay in milliseconds.
         * Default: 60000ms (1 minute)
         */
        private long maxDelay = 60000;
    }

    /**
     * Deduplication configuration properties.
     */
    @Data
    public static class Deduplication {
        /**
         * Enable/disable content deduplication.
         * Uses SHA-256 hash to detect duplicate content.
         */
        private boolean enabled = true;

        /**
         * Hash algorithm for content deduplication.
         * Supported: SHA-256, SHA-512, MD5
         */
        private String hashAlgorithm = "SHA-256";
    }

}
