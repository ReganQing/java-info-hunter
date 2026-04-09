package com.ron.javainfohunter.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^(publishedAt|importanceScore|createdAt|updatedAt|viewCount|likeCount)$",
             message = "Invalid sort field. Allowed: publishedAt, importanceScore, createdAt, updatedAt, viewCount, likeCount")
    private String sortBy = "publishedAt";

    /**
     * Sort direction (ASC, DESC)
     */
    @Builder.Default
    @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    private String sortDirection = "DESC";

    /**
     * Page number (0-based)
     */
    @Builder.Default
    @Min(value = 0, message = "Page number must be >= 0")
    private int page = 0;

    /**
     * Page size
     */
    @Builder.Default
    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 100, message = "Page size must be <= 100")
    private int size = 20;
}
