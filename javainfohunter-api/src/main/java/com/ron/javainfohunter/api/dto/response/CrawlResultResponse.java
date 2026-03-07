package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Crawl Result Response DTO
 *
 * Response model for manual crawl trigger operations.
 * Contains information about the triggered crawl task.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResultResponse {

    /**
     * ID of the RSS source being crawled
     */
    private Long sourceId;

    /**
     * Name of the RSS source
     */
    private String sourceName;

    /**
     * URL of the RSS source
     */
    private String sourceUrl;

    /**
     * Status of the crawl trigger
     */
    private String status;

    /**
     * Message describing the result
     */
    private String message;

    /**
     * Estimated articles to crawl (if available)
     */
    private Integer estimatedArticles;

    /**
     * Timestamp when the crawl was triggered
     */
    private Instant triggeredAt;

    /**
     * Task ID for tracking the crawl operation
     */
    private String taskId;
}
