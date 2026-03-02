package com.ron.javainfohunter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * Raw Content Entity
 *
 * Represents raw content collected from RSS feeds before AI processing.
 * Stores original content, metadata, and processing status.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "raw_content", indexes = {
    @Index(name = "idx_raw_content_rss_source", columnList = "rss_source_id"),
    @Index(name = "idx_raw_content_status", columnList = "processing_status"),
    @Index(name = "idx_raw_content_publish_date", columnList = "publish_date"),
    @Index(name = "idx_raw_content_crawl_date", columnList = "crawl_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawContent {

    /**
     * Processing status enum
     */
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to RSS source
     */
    @NotNull(message = "RSS source ID cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rss_source_id", nullable = false, foreignKey = @ForeignKey(name = "fk_raw_content_rss_source"))
    private RssSource rssSource;

    /**
     * Unique identifier from RSS feed (e.g., GUID)
     */
    @NotBlank(message = "GUID cannot be blank")
    @Size(max = 255, message = "GUID must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String guid;

    /**
     * Article title
     */
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * Article link/URL
     */
    @Size(max = 2048, message = "Link must not exceed 2048 characters")
    @Column(length = 2048)
    private String link;

    /**
     * Raw HTML or plain text content
     */
    @NotBlank(message = "Raw content cannot be blank")
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    /**
     * SHA-256 hash for deduplication
     */
    @NotBlank(message = "Content hash cannot be blank")
    @Size(max = 64, message = "Content hash must be 64 characters")
    @Column(nullable = false, length = 64, unique = true, name = "content_hash")
    private String contentHash;

    /**
     * Content author
     */
    @Size(max = 255, message = "Author must not exceed 255 characters")
    @Column(length = 255)
    private String author;

    /**
     * Original publication date from RSS
     */
    @Column(name = "publish_date")
    private Instant publishDate;

    /**
     * Timestamp when content was crawled
     */
    @NotNull(message = "Crawl date cannot be null")
    @Column(nullable = false, name = "crawl_date")
    private Instant crawlDate;

    /**
     * Processing status
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Processing status cannot be null")
    @Column(nullable = false, length = 20, name = "processing_status")
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    /**
     * Error message if processing failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Vector embedding for semantic search
     * Uses pgvector type with 1536 dimensions (OpenAI embedding size)
     * Note: Requires pgvector extension and proper Hibernate type mapping
     */
    @Lob
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private List<Double> embedding;

    /**
     * Timestamp when embedding was generated
     */
    @Column(name = "embedding_generated_at")
    private Instant embeddingGeneratedAt;

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
        if (crawlDate == null) {
            crawlDate = now;
        }
    }

    /**
     * Lifecycle callback: Update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if content is ready for processing
     *
     * @return true if status is PENDING
     */
    public boolean isReadyForProcessing() {
        return processingStatus == ProcessingStatus.PENDING;
    }

    /**
     * Check if content is currently being processed
     *
     * @return true if status is PROCESSING
     */
    public boolean isProcessing() {
        return processingStatus == ProcessingStatus.PROCESSING;
    }

    /**
     * Check if content processing is complete
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return processingStatus == ProcessingStatus.COMPLETED;
    }

    /**
     * Check if processing failed
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return processingStatus == ProcessingStatus.FAILED;
    }

    /**
     * Mark content as processing
     */
    public void markAsProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.errorMessage = null;
    }

    /**
     * Mark content as completed
     */
    public void markAsCompleted() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.errorMessage = null;
    }

    /**
     * Mark content as failed with error message
     *
     * @param errorMessage Error message describing the failure
     */
    public void markAsFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Check if embedding exists
     *
     * @return true if embedding vector is present
     */
    public boolean hasEmbedding() {
        return embedding != null && !embedding.isEmpty();
    }

    /**
     * Update the embedding vector
     *
     * @param embedding New embedding vector
     */
    public void updateEmbedding(List<Double> embedding) {
        this.embedding = embedding;
        this.embeddingGeneratedAt = Instant.now();
    }
}
