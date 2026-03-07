package com.ron.javainfohunter.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * News Query Request DTO
 *
 * Request model for querying news with filters.
 * Supports filtering by category, sentiment, date range, and sorting.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsQueryRequest {

    /**
     * Filter by category
     */
    private String category;

    /**
     * Filter by sentiment (POSITIVE, NEGATIVE, NEUTRAL)
     */
    private String sentiment;

    /**
     * Filter by start date
     */
    private Instant startDate;

    /**
     * Filter by end date
     */
    private Instant endDate;

    /**
     * Sort field (publishedAt, importanceScore, createdAt)
     */
    @Builder.Default
    private String sortBy = "publishedAt";

    /**
     * Sort direction (ASC, DESC)
     */
    @Builder.Default
    private String sortDirection = "DESC";

    /**
     * Page number (0-based)
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size
     */
    @Builder.Default
    private int size = 20;
}
