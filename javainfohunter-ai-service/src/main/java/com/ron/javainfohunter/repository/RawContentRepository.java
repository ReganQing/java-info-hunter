package com.ron.javainfohunter.repository;

import com.ron.javainfohunter.entity.RawContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Raw Content entities
 *
 * Provides data access operations for raw content collected from RSS feeds.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface RawContentRepository extends JpaRepository<RawContent, Long> {

    /**
     * Find raw content by processing status
     *
     * @param status Processing status
     * @return List of raw content with the specified status
     */
    List<RawContent> findByProcessingStatus(RawContent.ProcessingStatus status);

    /**
     * Find raw content by processing status with pagination
     *
     * @param status Processing status
     * @param pageable Pagination parameters
     * @return Page of raw content
     */
    Page<RawContent> findByProcessingStatus(RawContent.ProcessingStatus status, Pageable pageable);

    /**
     * Find raw content by RSS source ID
     *
     * @param rssSourceId RSS source ID
     * @return List of raw content from the specified source
     */
    List<RawContent> findByRssSourceId(Long rssSourceId);

    /**
     * Find raw content by RSS source ID with pagination
     *
     * @param rssSourceId RSS source ID
     * @param pageable Pagination parameters
     * @return Page of raw content
     */
    Page<RawContent> findByRssSourceId(Long rssSourceId, Pageable pageable);

    /**
     * Find raw content by content hash
     *
     * @param contentHash Content hash (SHA-256)
     * @return Optional containing the raw content if found
     */
    Optional<RawContent> findByContentHash(String contentHash);

    /**
     * Find raw content by GUID and RSS source ID
     * (Checks for duplicate articles from the same source)
     *
     * @param guid Article GUID
     * @param rssSourceId RSS source ID
     * @return Optional containing the raw content if found
     */
    Optional<RawContent> findByGuidAndRssSourceId(String guid, Long rssSourceId);

    /**
     * Find raw content published after a specific date
     *
     * @param publishDate Publication date threshold
     * @param pageable Pagination parameters
     * @return Page of raw content published after the date
     */
    Page<RawContent> findByPublishDateAfter(Instant publishDate, Pageable pageable);

    /**
     * Find raw content crawled after a specific date
     *
     * @param crawlDate Crawl date threshold
     * @param pageable Pagination parameters
     * @return Page of raw content crawled after the date
     */
    Page<RawContent> findByCrawlDateAfter(Instant crawlDate, Pageable pageable);

    /**
     * Find raw content by RSS source and processing status
     *
     * @param rssSourceId RSS source ID
     * @param status Processing status
     * @return List of raw content
     */
    List<RawContent> findByRssSourceIdAndProcessingStatus(Long rssSourceId, RawContent.ProcessingStatus status);

    /**
     * Find pending content ready for processing
     * (status = PENDING, ordered by crawl date)
     *
     * @param pageable Pagination parameters
     * @return Page of pending content
     */
    Page<RawContent> findByProcessingStatusOrderByCrawlDateAsc(RawContent.ProcessingStatus status, Pageable pageable);

    /**
     * Count raw content by processing status
     *
     * @param status Processing status
     * @return Number of raw content items with the status
     */
    long countByProcessingStatus(RawContent.ProcessingStatus status);

    /**
     * Count raw content by RSS source
     *
     * @param rssSourceId RSS source ID
     * @return Number of raw content items from the source
     */
    long countByRssSourceId(Long rssSourceId);

    /**
     * Find raw content without embedding
     *
     * @return List of raw content without vector embeddings
     */
    @Query("SELECT rc FROM RawContent rc WHERE rc.embedding IS NULL AND rc.processingStatus = 'COMPLETED'")
    List<RawContent> findContentWithoutEmbedding();

    /**
     * Find raw content by author
     *
     * @param author Author name
     * @param pageable Pagination parameters
     * @return Page of raw content by the author
     */
    Page<RawContent> findByAuthor(String author, Pageable pageable);

    /**
     * Search raw content by title (case-insensitive)
     *
     * @param title Title search term
     * @param pageable Pagination parameters
     * @return Page of matching raw content
     */
    Page<RawContent> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Find raw content within a date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of raw content within the date range
     */
    Page<RawContent> findByPublishDateBetween(Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Find recent raw content (last N days)
     *
     * @param since Date threshold
     * @param pageable Pagination parameters
     * @return Page of recent raw content
     */
    Page<RawContent> findByCrawlDateAfterOrderByCrawlDateDesc(Instant since, Pageable pageable);

    /**
     * Find failed content for retry
     *
     * @param maxRetryAge Maximum age of failed content to retry
     * @param pageable Pagination parameters
     * @return Page of failed content eligible for retry
     */
    @Query("SELECT rc FROM RawContent rc WHERE rc.processingStatus = 'FAILED' " +
           "AND rc.updatedAt > :minRetryAge")
    Page<RawContent> findFailedContentForRetry(@Param("minRetryAge") Instant maxRetryAge, Pageable pageable);

    /**
     * Find raw content by link URL
     *
     * @param link Link URL
     * @return Optional containing the raw content if found
     */
    Optional<RawContent> findByLink(String link);

    /**
     * Bulk update processing status
     *
     * @param ids List of raw content IDs
     * @param status New processing status
     */
    @Query("UPDATE RawContent rc SET rc.processingStatus = :status WHERE rc.id IN :ids")
    void bulkUpdateProcessingStatus(@Param("ids") List<Long> ids, @Param("status") RawContent.ProcessingStatus status);

    /**
     * Find raw content with pagination and filters
     *
     * @param rssSourceId RSS source ID (optional)
     * @param status Processing status (optional)
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @param pageable Pagination parameters
     * @return Page of filtered raw content
     */
    @Query("SELECT rc FROM RawContent rc WHERE " +
           "(:rssSourceId IS NULL OR rc.rssSource.id = :rssSourceId) AND " +
           "(:status IS NULL OR rc.processingStatus = :status) AND " +
           "(:startDate IS NULL OR rc.publishDate >= :startDate) AND " +
           "(:endDate IS NULL OR rc.publishDate <= :endDate)")
    Page<RawContent> findByFilters(
        @Param("rssSourceId") Long rssSourceId,
        @Param("status") RawContent.ProcessingStatus status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Get content statistics by processing status
     *
     * @return List of objects with status statistics
     */
    @Query("SELECT rc.processingStatus, COUNT(rc) FROM RawContent rc GROUP BY rc.processingStatus")
    List<Object[]> getStatisticsByStatus();

    /**
     * Find oldest pending content
     *
     * @param limit Maximum number of results
     * @return List of oldest pending content
     */
    @Query("SELECT rc FROM RawContent rc WHERE rc.processingStatus = 'PENDING' " +
           "ORDER BY rc.crawlDate ASC")
    List<RawContent> findOldestPendingContent(@Param("limit") int limit);

    /**
     * Delete old raw content based on date
     *
     * @param beforeDate Delete content older than this date
     * @return Number of deleted records
     */
    @Query("DELETE FROM RawContent rc WHERE rc.crawlDate < :beforeDate " +
           "AND rc.processingStatus = 'COMPLETED' " +
           "AND NOT EXISTS (SELECT 1 FROM News n WHERE n.rawContent.id = rc.id)")
    int deleteOldProcessedContent(@Param("beforeDate") Instant beforeDate);

    /**
     * Find raw content for specific sources in date range
     *
     * @param rssSourceIds List of RSS source IDs
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of raw content
     */
    @Query("SELECT rc FROM RawContent rc WHERE rc.rssSource.id IN :rssSourceIds " +
           "AND rc.publishDate BETWEEN :startDate AND :endDate")
    Page<RawContent> findByRssSourceIdsAndDateRange(
        @Param("rssSourceIds") List<Long> rssSourceIds,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );
}
