package com.ron.javainfohunter.repository;

import com.ron.javainfohunter.entity.RssSource;
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
 * Repository for RSS Source entities
 *
 * Provides data access operations for RSS subscription sources.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface RssSourceRepository extends JpaRepository<RssSource, Long> {

    /**
     * Find all active RSS sources
     *
     * @return List of active RSS sources
     */
    List<RssSource> findByIsActiveTrue();

    /**
     * Find active RSS sources with pagination
     *
     * @param isActive Active status
     * @param pageable Pagination parameters
     * @return Page of RSS sources
     */
    Page<RssSource> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Find RSS sources by category
     *
     * @param category Category name
     * @return List of RSS sources in the category
     */
    List<RssSource> findByCategory(String category);

    /**
     * Find RSS sources by category with pagination
     *
     * @param category Category name
     * @param pageable Pagination parameters
     * @return Page of RSS sources
     */
    Page<RssSource> findByCategory(String category, Pageable pageable);

    /**
     * Find RSS source by URL
     *
     * @param url RSS feed URL
     * @return Optional containing the RSS source if found
     */
    Optional<RssSource> findByUrl(String url);

    /**
     * Find RSS sources that are due for crawling
     * (active sources where last_crawled_at is null or older than crawl_interval_seconds)
     *
     * @return List of RSS sources due for crawling
     */
    @Query("SELECT rs FROM RssSource rs WHERE rs.isActive = true " +
           "AND (rs.lastCrawledAt IS NULL OR rs.lastCrawledAt < :threshold)")
    List<RssSource> findSourcesDueForCrawling(@Param("threshold") Instant threshold);

    /**
     * Find RSS sources by tag
     *
     * @param tag Tag name
     * @return List of RSS sources with the specified tag
     */
    @Query("SELECT rs FROM RssSource rs JOIN rs.tags tag WHERE tag = :tag")
    List<RssSource> findByTag(@Param("tag") String tag);

    /**
     * Find RSS sources by multiple tags (AND logic)
     *
     * @param tags List of tag names
     * @return List of RSS sources containing all specified tags
     */
    @Query("SELECT rs FROM RssSource rs JOIN rs.tags tag WHERE tag IN :tags " +
           "GROUP BY rs HAVING COUNT(tag) = :tagCount")
    List<RssSource> findByTagsContainingAll(@Param("tags") List<String> tags, @Param("tagCount") long tagCount);

    /**
     * Find RSS sources by category and active status
     *
     * @param category Category name
     * @param isActive Active status
     * @return List of RSS sources
     */
    List<RssSource> findByCategoryAndIsActive(String category, Boolean isActive);

    /**
     * Count active RSS sources
     *
     * @return Number of active sources
     */
    long countByIsActiveTrue();

    /**
     * Count RSS sources by category
     *
     * @param category Category name
     * @return Number of sources in the category
     */
    long countByCategory(String category);

    /**
     * Find RSS sources sorted by last crawled date (oldest first)
     *
     * @param isActive Active status filter
     * @param pageable Pagination parameters
     * @return Page of RSS sources ordered by last crawled date
     */
    Page<RssSource> findByIsActiveOrderByLastCrawledAtAsc(Boolean isActive, Pageable pageable);

    /**
     * Find RSS sources that haven't been crawled in a specific time period
     *
     * @param since Time threshold
     * @return List of stale RSS sources
     */
    @Query("SELECT rs FROM RssSource rs WHERE rs.isActive = true " +
           "AND rs.lastCrawledAt IS NOT NULL AND rs.lastCrawledAt < :since")
    List<RssSource> findStaleSources(@Param("since") Instant since);

    /**
     * Search RSS sources by name or description (case-insensitive)
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching RSS sources
     */
    @Query("SELECT rs FROM RssSource rs WHERE LOWER(rs.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(rs.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<RssSource> searchByNameOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Get RSS source statistics
     *
     * @return List of objects with source statistics
     */
    @Query("SELECT new com.ron.javainfohunter.dto.RssSourceStats(" +
           "rs.id, rs.name, rs.category, rs.totalArticles, rs.failedCrawls, rs.lastCrawledAt) " +
           "FROM RssSource rs WHERE rs.isActive = true")
    List<Object> getSourceStatistics();

    /**
     * Find RSS sources with high failure rate
     *
     * @param failureRateThreshold Failure rate threshold (0-100)
     * @return List of RSS sources with failure rate above threshold
     */
    @Query("SELECT rs FROM RssSource rs WHERE rs.isActive = true " +
           "AND rs.totalArticles > 0 AND (rs.failedCrawls * 100.0 / rs.totalArticles) > :threshold")
    List<RssSource> findSourcesWithHighFailureRate(@Param("threshold") double failureRateThreshold);

    /**
     * Bulk update last crawled timestamp
     *
     * @param ids List of RSS source IDs
     * @param lastCrawledAt New last crawled timestamp
     */
    @Query("UPDATE RssSource rs SET rs.lastCrawledAt = :lastCrawledAt WHERE rs.id IN :ids")
    void bulkUpdateLastCrawledAt(@Param("ids") List<Long> ids, @Param("lastCrawledAt") Instant lastCrawledAt);

    /**
     * Find all RSS sources with their article counts
     *
     * @return List of RSS sources with article counts
     */
    @Query("SELECT rs, COUNT(rc.id) FROM RssSource rs " +
           "LEFT JOIN RawContent rc ON rs.id = rc.rssSource.id " +
           "WHERE rs.isActive = true " +
           "GROUP BY rs")
    List<Object[]> findAllWithArticleCount();
}
