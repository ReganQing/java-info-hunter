package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * RSS Source Response DTO
 *
 * Response model for RSS source operations.
 * Contains all relevant source information for API consumers.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceResponse {

    /**
     * Primary key
     */
    private Long id;

    /**
     * Human-readable name for the RSS source
     */
    private String name;

    /**
     * URL of the RSS feed
     */
    private String url;

    /**
     * Optional description of the RSS source
     */
    private String description;

    /**
     * Category for grouping related sources
     */
    private String category;

    /**
     * Array of tags for flexible categorization and filtering
     */
    private List<String> tags;

    /**
     * Whether this source is actively being crawled
     */
    private Boolean isActive;

    /**
     * Crawling interval in seconds
     */
    private Integer crawlIntervalSeconds;

    /**
     * Maximum number of retry attempts on failure
     */
    private Integer maxRetries;

    /**
     * Backoff delay between retries in seconds
     */
    private Integer retryBackoffSeconds;

    /**
     * Content language (ISO 639-1 code)
     */
    private String language;

    /**
     * Timezone for date parsing (IANA timezone ID)
     */
    private String timezone;

    /**
     * Timestamp of the last successful crawl
     */
    private Instant lastCrawledAt;

    /**
     * Total number of articles collected from this source
     */
    private Long totalArticles;

    /**
     * Number of failed crawl attempts
     */
    private Long failedCrawls;

    /**
     * Failure rate as percentage (0-100)
     */
    private Double failureRate;

    /**
     * Timestamp when this record was created
     */
    private Instant createdAt;

    /**
     * Timestamp when this record was last updated
     */
    private Instant updatedAt;
}
