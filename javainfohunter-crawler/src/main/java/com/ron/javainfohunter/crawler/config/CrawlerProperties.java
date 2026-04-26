package com.ron.javainfohunter.crawler.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

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
 *     publisher:
 *       cleanup-interval-ms: 300000  # 5 minutes - cleanup interval for stale confirms
 *       stale-confirm-age-ms: 120000 # 2 minutes - age before a confirm is considered stale
 * </pre>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "javainfohunter.crawler")
public class CrawlerProperties {

    /**
     * Enable/disable the crawler module.
     */
    private boolean enabled = true;

    /**
     * Scheduler configuration for periodic crawling.
     */
    @Valid
    private Scheduler scheduler = new Scheduler();

    /**
     * Feed processing configuration.
     */
    @Valid
    private Feed feed = new Feed();

    /**
     * Content processing configuration.
     */
    @Valid
    private Processing processing = new Processing();

    /**
     * Retry configuration.
     */
    @Valid
    private Retry retry = new Retry();

    /**
     * Deduplication configuration.
     */
    @Valid
    private Deduplication deduplication = new Deduplication();

    /**
     * Publisher configuration for message publishing.
     */
    @Valid
    private Publisher publisher = new Publisher();

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
        @Min(0)
        private long initialDelay = 30000;

        /**
         * Fixed rate between crawls (milliseconds).
         * Default: 1 hour
         */
        @Min(1000)
        private long fixedRate = 3600000;

        /**
         * Maximum number of RSS sources crawled concurrently.
         * Keeps virtual-thread fan-out bounded on developer machines.
         */
        @Min(1)
        @Max(32)
        private int maxConcurrentSources = 8;
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
        @Min(1)
        @Max(500)
        private int maxArticlesPerFeed = 100;

        /**
         * Connection timeout for fetching RSS feeds (milliseconds).
         * Default: 30 seconds
         */
        @Min(1000)
        @Max(120000)
        private int connectionTimeout = 30000;

        /**
         * Read timeout for fetching RSS feeds (milliseconds).
         * Default: 60 seconds
         */
        @Min(1000)
        @Max(300000)
        private int readTimeout = 60000;

        /**
         * User-Agent header for HTTP requests.
         * Some feeds require specific user agents.
         */
        @NotBlank
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
        @Min(1)
        @Max(500)
        private int batchSize = 50;

        /**
         * Maximum number of retry attempts for failed processing.
         * @deprecated Use {@link Retry#maxAttempts} instead
         */
        @Min(0)
        @Max(10)
        @Deprecated
        private int maxRetries = 3;

        /**
         * Backoff time between retries (milliseconds).
         * @deprecated Use {@link Retry#initialDelay} and {@link Retry#backoffMultiplier} instead
         */
        @Min(100)
        @Max(300000)
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
        @Min(0)
        @Max(10)
        private int maxAttempts = 3;

        /**
         * Initial backoff delay in milliseconds.
         * Default: 1000ms (1 second)
         */
        @Min(100)
        @Max(60000)
        private long initialDelay = 1000;

        /**
         * Exponential backoff multiplier.
         * Default: 2.0 (doubles delay each retry: 1s, 2s, 4s, ...)
         */
        @DecimalMin("1.0")
        @DecimalMax("10.0")
        private double backoffMultiplier = 2.0;

        /**
         * Maximum backoff delay in milliseconds.
         * Default: 60000ms (1 minute)
         */
        @Min(1000)
        @Max(300000)
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
        @NotBlank
        private String hashAlgorithm = "SHA-256";
    }

    /**
     * Publisher configuration properties.
     */
    @Data
    public static class Publisher {
        /**
         * Cleanup interval for stale pending confirms (milliseconds).
         * <p>This controls how often the publisher checks for and removes
         * stale pending confirms that haven't received broker confirmation.</p>
         * <p>Default: 300000ms (5 minutes)</p>
         */
        @Min(1000)
        @Max(900000)
        private long cleanupIntervalMs = 300000;

        /**
         * Maximum age for pending confirms before cleanup (milliseconds).
         * <p>Stale confirms older than this duration are removed during cleanup.</p>
         * <p>Default: 120000ms (2 minutes)</p>
         */
        @Min(1000)
        @Max(600000)
        private long staleConfirmAgeMs = 120000;
    }

}
