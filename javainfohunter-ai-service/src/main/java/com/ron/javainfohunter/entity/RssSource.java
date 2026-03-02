package com.ron.javainfohunter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * RSS Subscription Source Entity
 *
 * Represents an RSS feed that the system monitors for new content.
 * Includes crawling configuration, statistics, and categorization.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "rss_sources", indexes = {
    @Index(name = "idx_rss_sources_active", columnList = "is_active"),
    @Index(name = "idx_rss_sources_category", columnList = "category"),
    @Index(name = "idx_rss_sources_last_crawled", columnList = "last_crawled_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSource {

    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for the RSS source
     */
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * URL of the RSS feed
     */
    @NotBlank(message = "URL cannot be blank")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    /**
     * Optional description of the RSS source
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Category for grouping related sources
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Column(length = 100)
    private String category;

    /**
     * Array of tags for flexible categorization and filtering
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rss_source_tags", joinColumns = @JoinColumn(name = "rss_source_id"))
    @Column(name = "tag")
    private List<String> tags;

    /**
     * Crawling interval in seconds (minimum 60s recommended)
     */
    @NotNull(message = "Crawl interval cannot be null")
    @Min(value = 60, message = "Crawl interval must be at least 60 seconds")
    @Column(nullable = false, name = "crawl_interval_seconds")
    private Integer crawlIntervalSeconds = 3600; // Default: 1 hour

    /**
     * Whether this source is actively being crawled
     */
    @NotNull(message = "Active status cannot be null")
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Maximum number of retry attempts on failure
     */
    @NotNull(message = "Max retries cannot be null")
    @Min(value = 0, message = "Max retries cannot be negative")
    @Column(nullable = false, name = "max_retries")
    private Integer maxRetries = 3;

    /**
     * Backoff delay between retries in seconds
     */
    @NotNull(message = "Retry backoff cannot be null")
    @Min(value = 0, message = "Retry backoff cannot be negative")
    @Column(nullable = false, name = "retry_backoff_seconds")
    private Integer retryBackoffSeconds = 60;

    /**
     * Content language (ISO 639-1 code)
     */
    @Size(max = 10, message = "Language must not exceed 10 characters")
    @Column(length = 10)
    private String language = "en";

    /**
     * Timezone for date parsing (IANA timezone ID)
     */
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    @Column(length = 50)
    private String timezone = "UTC";

    /**
     * Timestamp when this record was created
     */
    @NotNull(message = "Created at cannot be null")
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    /**
     * Timestamp when this record was last updated
     */
    @NotNull(message = "Updated at cannot be null")
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    /**
     * Timestamp of the last successful crawl
     */
    @Column(name = "last_crawled_at")
    private Instant lastCrawledAt;

    /**
     * Total number of articles collected from this source
     */
    @NotNull(message = "Total articles cannot be null")
    @Column(nullable = false)
    private Long totalArticles = 0L;

    /**
     * Number of failed crawl attempts
     */
    @NotNull(message = "Failed crawls cannot be null")
    @Column(nullable = false)
    private Long failedCrawls = 0L;

    /**
     * Lifecycle callback: Set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Lifecycle callback: Update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculate the failure rate as a percentage
     *
     * @return Failure rate (0-100), or null if no articles
     */
    public Double getFailureRate() {
        if (totalArticles == 0) {
            return null;
        }
        return (failedCrawls * 100.0) / totalArticles;
    }

    /**
     * Check if this source is due for crawling
     *
     * @return true if the source is active and the crawl interval has elapsed
     */
    public boolean isDueForCrawling() {
        if (!isActive || lastCrawledAt == null) {
            return true;
        }
        return Instant.now().isAfter(lastCrawledAt.plusSeconds(crawlIntervalSeconds));
    }

    /**
     * Increment the article count
     */
    public void incrementArticleCount() {
        totalArticles++;
    }

    /**
     * Increment the failed crawl count
     */
    public void incrementFailedCrawlCount() {
        failedCrawls++;
    }

    /**
     * Update the last crawled timestamp
     */
    public void updateLastCrawled() {
        lastCrawledAt = Instant.now();
    }
}
