package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * News Response DTO
 *
 * Response model for news articles.
 * Contains all relevant news information for API consumers.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {

    /**
     * Primary key
     */
    private Long id;

    /**
     * News title
     */
    private String title;

    /**
     * AI-generated summary
     */
    private String summary;

    /**
     * Content category
     */
    private String category;

    /**
     * AI-extracted topics
     */
    private List<String> topics;

    /**
     * AI-extracted keywords
     */
    private List<String> keywords;

    /**
     * Tags
     */
    private List<String> tags;

    /**
     * Sentiment classification
     */
    private String sentiment;

    /**
     * Sentiment score (-1.0 to 1.0)
     */
    private BigDecimal sentimentScore;

    /**
     * Importance score (0.0 to 1.0)
     */
    private BigDecimal importanceScore;

    /**
     * Estimated reading time in minutes
     */
    private Integer readingTimeMinutes;

    /**
     * Content language
     */
    private String language;

    /**
     * URL slug for SEO
     */
    private String slug;

    /**
     * Featured image URL
     */
    private String featuredImageUrl;

    /**
     * Number of views
     */
    private Long viewCount;

    /**
     * Number of likes
     */
    private Integer likeCount;

    /**
     * Number of shares
     */
    private Integer shareCount;

    /**
     * Whether the news is published
     */
    private Boolean isPublished;

    /**
     * Whether the news is featured
     */
    private Boolean isFeatured;

    /**
     * Publication timestamp
     */
    private Instant publishedAt;

    /**
     * Creation timestamp
     */
    private Instant createdAt;

    /**
     * Update timestamp
     */
    private Instant updatedAt;
}
