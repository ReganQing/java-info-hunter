package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Crawl Progress Response DTO
 *
 * Response model for crawl task progress tracking.
 * Provides real-time status of ongoing crawl operations.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlProgressResponse {

    /**
     * Unique task identifier for tracking
     */
    private String taskId;

    /**
     * Current status of the crawl task
     */
    private String status;

    /**
     * Total number of sources to crawl
     */
    private Integer totalSources;

    /**
     * Number of sources completed
     */
    private Integer completedSources;

    /**
     * Name of the current source being crawled
     */
    private String currentSource;

    /**
     * Timestamp when the task started
     */
    private String startedAt;

    /**
     * Estimated completion timestamp
     */
    private String estimatedCompletionAt;

    /**
     * Error message if the task failed
     */
    private String errorMessage;

    /**
     * Crawl task status enum
     */
    public enum CrawlStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
