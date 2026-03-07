package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Similar News Response DTO
 *
 * Response model for similar news articles.
 * Used in similarity search and recommendation features.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarNewsResponse {

    /**
     * Primary key
     */
    private Long id;

    /**
     * News title
     */
    private String title;

    /**
     * News summary
     */
    private String summary;

    /**
     * Content category
     */
    private String category;

    /**
     * Similarity score (0.0 to 1.0)
     */
    private Double similarityScore;

    /**
     * Number of shared topics/tags
     */
    private Integer sharedTagsCount;

    /**
     * Publication timestamp
     */
    private Instant publishedAt;

    /**
     * Featured image URL
     */
    private String featuredImageUrl;
}
