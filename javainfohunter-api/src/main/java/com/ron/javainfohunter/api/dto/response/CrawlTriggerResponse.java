package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Crawl Trigger Response DTO
 *
 * Response model for crawl trigger operations.
 * Contains information about triggered crawl tasks.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlTriggerResponse {

    /**
     * Whether the crawl was triggered successfully
     */
    private Boolean triggered;

    /**
     * Status message
     */
    private String message;

    /**
     * Number of sources triggered
     */
    private Integer sourcesTriggered;

    /**
     * Trigger timestamp
     */
    private Instant triggeredAt;

    /**
     * Task IDs for tracking
     */
    private String taskIds;

    /**
     * Estimated articles to crawl
     */
    private Integer estimatedArticles;
}
