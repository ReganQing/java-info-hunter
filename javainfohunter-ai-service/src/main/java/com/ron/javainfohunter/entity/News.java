package com.ron.javainfohunter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * News Entity
 *
 * Represents processed and enriched news articles after AI analysis.
 * Contains AI-generated summaries, topics, sentiment, and metadata.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "news", indexes = {
    @Index(name = "idx_news_raw_content", columnList = "raw_content_id"),
    @Index(name = "idx_news_published", columnList = "is_published, published_at"),
    @Index(name = "idx_news_category", columnList = "category"),
    @Index(name = "idx_news_sentiment", columnList = "sentiment"),
    @Index(name = "idx_news_importance", columnList = "importance_score"),
    @Index(name = "idx_news_slug", columnList = "slug"),
    @Index(name = "idx_news_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class News {

    /**
     * Sentiment enum
     */
    public enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }

    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to raw content (one-to-one)
     */
    @NotNull(message = "Raw content ID cannot be null")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_content_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_news_raw_content"))
    private RawContent rawContent;

    /**
     * Enriched title (may be different from original)
     */
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * AI-generated summary
     */
    @NotBlank(message = "Summary cannot be blank")
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    /**
     * Cleaned and structured full content
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String fullContent;

    /**
     * AI-extracted topics (PostgreSQL array)
     */
    @Column(columnDefinition = "text[]")
    private String[] topics;

    /**
     * AI-extracted keywords (PostgreSQL array)
     */
    @Column(columnDefinition = "text[]")
    private String[] keywords;

    /**
     * Sentiment classification
     */
    @Enumerated(EnumType.STRING)
    @Size(max = 20, message = "Sentiment must not exceed 20 characters")
    @Column(length = 20)
    private Sentiment sentiment;

    /**
     * Sentiment score from -1.0 (negative) to 1.0 (positive)
     */
    @DecimalMin(value = "-1.0", message = "Sentiment score must be at least -1.0")
    @DecimalMax(value = "1.0", message = "Sentiment score must be at most 1.0")
    @Column(precision = 3, scale = 2, name = "sentiment_score")
    private BigDecimal sentimentScore;

    /**
     * AI-rated importance score from 0.0 to 1.0
     */
    @DecimalMin(value = "0.0", message = "Importance score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Importance score must be at most 1.0")
    @Column(precision = 3, scale = 2, name = "importance_score")
    private BigDecimal importanceScore;

    /**
     * AI-assigned category
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Column(length = 100)
    private String category;

    /**
     * AI-assigned tags (PostgreSQL array)
     */
    @Column(columnDefinition = "text[]")
    private String[] tags;

    /**
     * Content language (ISO 639-1 code)
     */
    @NotBlank(message = "Language cannot be blank")
    @Size(max = 10, message = "Language must not exceed 10 characters")
    @Column(nullable = false, length = 10)
    private String language = "en";

    /**
     * Estimated reading time in minutes
     */
    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    /**
     * URL-friendly slug for SEO
     */
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    @Column(unique = true, length = 255)
    private String slug;

    /**
     * Featured image URL
     */
    @Size(max = 2048, message = "Featured image URL must not exceed 2048 characters")
    @Column(name = "featured_image_url", length = 2048)
    private String featuredImageUrl;

    /**
     * Number of views
     */
    @NotNull(message = "View count cannot be null")
    @Column(nullable = false)
    private Long viewCount = 0L;

    /**
     * Number of likes
     */
    @NotNull(message = "Like count cannot be null")
    @Column(nullable = false)
    private Integer likeCount = 0;

    /**
     * Number of shares
     */
    @NotNull(message = "Share count cannot be null")
    @Column(nullable = false)
    private Integer shareCount = 0;

    /**
     * Whether this news is published
     */
    @NotNull(message = "Published status cannot be null")
    @Column(nullable = false, name = "is_published")
    private Boolean isPublished = false;

    /**
     * Whether this news is featured
     */
    @NotNull(message = "Featured status cannot be null")
    @Column(nullable = false, name = "is_featured")
    private Boolean isFeatured = false;

    /**
     * Timestamp when news was published
     */
    @Column(name = "published_at")
    private Instant publishedAt;

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
     * Lifecycle callback: Set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (isPublished && publishedAt == null) {
            publishedAt = now;
        }
    }

    /**
     * Lifecycle callback: Update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        // Auto-publish if marked as published but no publish date
        if (isPublished && publishedAt == null) {
            publishedAt = Instant.now();
        }
    }

    /**
     * Check if news is published
     *
     * @return true if published
     */
    public boolean isPublished() {
        return isPublished != null && isPublished;
    }

    /**
     * Check if news is featured
     *
     * @return true if featured
     */
    public boolean isFeatured() {
        return isFeatured != null && isFeatured;
    }

    /**
     * Publish the news
     */
    public void publish() {
        this.isPublished = true;
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
    }

    /**
     * Unpublish the news
     */
    public void unpublish() {
        this.isPublished = false;
    }

    /**
     * Mark as featured
     */
    public void markAsFeatured() {
        this.isFeatured = true;
    }

    /**
     * Unmark as featured
     */
    public void unmarkAsFeatured() {
        this.isFeatured = false;
    }

    /**
     * Increment view count
     */
    public void incrementViewCount() {
        if (viewCount == null) {
            viewCount = 0L;
        }
        viewCount++;
    }

    /**
     * Increment like count
     */
    public void incrementLikeCount() {
        if (likeCount == null) {
            likeCount = 0;
        }
        likeCount++;
    }

    /**
     * Decrement like count (unlike)
     */
    public void decrementLikeCount() {
        if (likeCount != null && likeCount > 0) {
            likeCount--;
        }
    }

    /**
     * Increment share count
     */
    public void incrementShareCount() {
        if (shareCount == null) {
            shareCount = 0;
        }
        shareCount++;
    }

    /**
     * Calculate engagement score
     *
     * @return Engagement score (views + 10*likes + 20*shares)
     */
    public Long getEngagementScore() {
        long views = viewCount != null ? viewCount : 0L;
        int likes = likeCount != null ? likeCount : 0;
        int shares = shareCount != null ? shareCount : 0;
        return views + (likes * 10) + (shares * 20);
    }

    /**
     * Check if content has high importance
     *
     * @return true if importance score >= 0.7
     */
    public boolean isHighImportance() {
        return importanceScore != null && importanceScore.compareTo(new BigDecimal("0.7")) >= 0;
    }

    /**
     * Get sentiment description
     *
     * @return Human-readable sentiment description
     */
    public String getSentimentDescription() {
        if (sentiment == null) {
            return "Unknown";
        }
        return sentiment.toString().toLowerCase();
    }
}
