package com.ron.javainfohunter.repository;

import com.ron.javainfohunter.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for News entities
 *
 * Provides data access operations for processed and enriched news articles.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * Find all published news
     *
     * @return List of published news
     */
    List<News> findByIsPublishedTrue();

    /**
     * Find published news with pagination
     *
     * @param pageable Pagination parameters
     * @return Page of published news
     */
    Page<News> findByIsPublishedTrue(Pageable pageable);

    /**
     * Find published news ordered by published date
     *
     * @param pageable Pagination parameters
     * @return Page of published news ordered by published date
     */
    Page<News> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    /**
     * Find featured news
     *
     * @return List of featured news
     */
    List<News> findByIsFeaturedTrue();

    /**
     * Find featured and published news
     *
     * @param pageable Pagination parameters
     * @return Page of featured published news
     */
    Page<News> findByIsFeaturedTrueAndIsPublishedTrue(Pageable pageable);

    /**
     * Find news by category
     *
     * @param category Category name
     * @param pageable Pagination parameters
     * @return Page of news in the category
     */
    Page<News> findByCategory(String category, Pageable pageable);

    /**
     * Find published news by category
     *
     * @param category Category name
     * @param pageable Pagination parameters
     * @return Page of published news in the category
     */
    Page<News> findByCategoryAndIsPublishedTrue(String category, Pageable pageable);

    /**
     * Find news by sentiment
     *
     * @param sentiment Sentiment value
     * @param pageable Pagination parameters
     * @return Page of news with the specified sentiment
     */
    Page<News> findBySentiment(News.Sentiment sentiment, Pageable pageable);

    /**
     * Find news by slug
     *
     * @param slug URL slug
     * @return Optional containing the news if found
     */
    Optional<News> findBySlug(String slug);

    /**
     * Find news by raw content ID
     *
     * @param rawContentId Raw content ID
     * @return Optional containing the news if found
     */
    Optional<News> findByRawContentId(Long rawContentId);

    /**
     * Find news with importance score above threshold
     *
     * @param threshold Importance score threshold
     * @param pageable Pagination parameters
     * @return Page of high-importance news
     */
    Page<News> findByImportanceScoreGreaterThanEqual(BigDecimal threshold, Pageable pageable);

    /**
     * Find published high-importance news
     *
     * @param threshold Importance score threshold
     * @param pageable Pagination parameters
     * @return Page of published high-importance news
     */
    @Query("SELECT n FROM News n WHERE n.isPublished = true AND n.importanceScore >= :threshold")
    Page<News> findPublishedHighImportanceNews(@Param("threshold") BigDecimal threshold, Pageable pageable);

    /**
     * Find news by tag
     *
     * @param tag Tag name
     * @return List of news with the specified tag
     */
    @Query("SELECT n FROM News n JOIN n.tags tag WHERE tag = :tag")
    List<News> findByTag(@Param("tag") String tag);

    /**
     * Find published news by tag with pagination
     *
     * @param tag Tag name
     * @param pageable Pagination parameters
     * @return Page of published news with the specified tag
     */
    @Query("SELECT n FROM News n JOIN n.tags tag WHERE tag = :tag AND n.isPublished = true")
    Page<News> findPublishedByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Find news by topic
     *
     * @param topic Topic name
     * @return List of news with the specified topic
     */
    @Query("SELECT n FROM News n JOIN n.topics topic WHERE topic = :topic")
    List<News> findByTopic(@Param("topic") String topic);

    /**
     * Find news by keyword
     *
     * @param keyword Keyword name
     * @return List of news with the specified keyword
     */
    @Query("SELECT n FROM News n JOIN n.keywords keyword WHERE keyword = :keyword")
    List<News> findByKeyword(@Param("keyword") String keyword);

    /**
     * Search news by title or summary (case-insensitive)
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching news
     */
    @Query("SELECT n FROM News n WHERE n.isPublished = true AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<News> searchPublished(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find news published after a specific date
     *
     * @param publishedAt Publication date threshold
     * @param pageable Pagination parameters
     * @return Page of news published after the date
     */
    Page<News> findByPublishedAtAfterOrderByPublishedAtDesc(Instant publishedAt, Pageable pageable);

    /**
     * Find news published within a date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of news within the date range
     */
    Page<News> findByPublishedAtBetweenOrderByPublishedAtDesc(Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Find recent published news
     *
     * @param since Date threshold
     * @param pageable Pagination parameters
     * @return Page of recent published news
     */
    Page<News> findByIsPublishedTrueAndPublishedAtAfterOrderByPublishedAtDesc(boolean isPublished, Instant since, Pageable pageable);

    /**
     * Count published news by category
     *
     * @param category Category name
     * @return Number of published news in the category
     */
    long countByCategoryAndIsPublishedTrue(String category);

    /**
     * Count news by sentiment
     *
     * @param sentiment Sentiment value
     * @return Number of news with the sentiment
     */
    long countBySentiment(News.Sentiment sentiment);

    /**
     * Find top news by engagement (views + likes + shares)
     *
     * @param pageable Pagination parameters
     * @return Page of top news
     */
    @Query("SELECT n FROM News n WHERE n.isPublished = true " +
           "ORDER BY (n.viewCount + n.likeCount * 10 + n.shareCount * 20) DESC")
    Page<News> findTopNewsByEngagement(Pageable pageable);

    /**
     * Find top news by importance score
     *
     * @param pageable Pagination parameters
     * @return Page of top news
     */
    Page<News> findByIsPublishedTrueOrderByImportanceScoreDesc(Pageable pageable);

    /**
     * Find news by language
     *
     * @param language Language code
     * @param pageable Pagination parameters
     * @return Page of news in the specified language
     */
    Page<News> findByLanguageAndIsPublishedTrue(String language, Pageable pageable);

    /**
     * Find news with reading time within range
     *
     * @param minMinutes Minimum reading time
     * @param maxMinutes Maximum reading time
     * @param pageable Pagination parameters
     * @return Page of news within the reading time range
     */
    Page<News> findByReadingTimeMinutesBetweenAndIsPublishedTrue(
        Integer minMinutes, Integer maxMinutes, Pageable pageable
    );

    /**
     * Find news by multiple filters
     *
     * @param category Category (optional)
     * @param sentiment Sentiment (optional)
     * @param isPublished Published status (optional)
     * @param minImportance Minimum importance score (optional)
     * @param pageable Pagination parameters
     * @return Page of filtered news
     */
    @Query("SELECT n FROM News n WHERE " +
           "(:category IS NULL OR n.category = :category) AND " +
           "(:sentiment IS NULL OR n.sentiment = :sentiment) AND " +
           "(:isPublished IS NULL OR n.isPublished = :isPublished) AND " +
           "(:minImportance IS NULL OR n.importanceScore >= :minImportance)")
    Page<News> findByFilters(
        @Param("category") String category,
        @Param("sentiment") News.Sentiment sentiment,
        @Param("isPublished") Boolean isPublished,
        @Param("minImportance") BigDecimal minImportance,
        Pageable pageable
    );

    /**
     * Find trending news (high engagement in last 24 hours)
     *
     * @param since Date threshold (typically 24 hours ago)
     * @param pageable Pagination parameters
     * @return Page of trending news
     */
    @Query("SELECT n FROM News n WHERE n.isPublished = true AND n.publishedAt > :since " +
           "ORDER BY (n.viewCount + n.likeCount * 10 + n.shareCount * 20) DESC")
    Page<News> findTrendingNews(@Param("since") Instant since, Pageable pageable);

    /**
     * Get news statistics by category
     *
     * @return List of objects with category statistics
     */
    @Query("SELECT n.category, COUNT(n) FROM News n WHERE n.isPublished = true " +
           "GROUP BY n.category")
    List<Object[]> getStatisticsByCategory();

    /**
     * Get news statistics by sentiment
     *
     * @return List of objects with sentiment statistics
     */
    @Query("SELECT n.sentiment, COUNT(n), AVG(n.sentimentScore) FROM News n " +
           "WHERE n.isPublished = true GROUP BY n.sentiment")
    List<Object[]> getStatisticsBySentiment();

    /**
     * Find news without slug (for slug generation)
     *
     * @return List of news without slug
     */
    @Query("SELECT n FROM News n WHERE n.slug IS NULL AND n.isPublished = true")
    List<News> findNewsWithoutSlug();

    /**
     * Find news from specific RSS sources
     *
     * @param rssSourceIds List of RSS source IDs
     * @param pageable Pagination parameters
     * @return Page of news from the specified sources
     */
    @Query("SELECT n FROM News n JOIN n.rawContent rc WHERE rc.rssSource.id IN :rssSourceIds")
    Page<News> findByRssSourceIds(@Param("rssSourceIds") List<Long> rssSourceIds, Pageable pageable);

    /**
     * Full-text search on news content
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching news
     */
    @Query("SELECT n FROM News n WHERE n.isPublished = true AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.fullContent) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<News> fullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find similar news by tags (shared tags)
     *
     * @param newsId Reference news ID
     * @param pageable Pagination parameters
     * @return Page of similar news
     */
    @Query("SELECT n FROM News n JOIN n.tags tag WHERE tag IN " +
           "(SELECT t FROM News n2 JOIN n2.tags t WHERE n2.id = :newsId) " +
           "AND n.id != :newsId AND n.isPublished = true " +
           "GROUP BY n ORDER BY COUNT(tag) DESC")
    Page<News> findSimilarNews(@Param("newsId") Long newsId, Pageable pageable);

    /**
     * Find unpublished news ready for review
     *
     * @param pageable Pagination parameters
     * @return Page of unpublished news
     */
    @Query("SELECT n FROM News n WHERE n.isPublished = false " +
           "AND n.summary IS NOT NULL AND n.summary != '' " +
           "ORDER BY n.createdAt DESC")
    Page<News> findUnpublishedReadyForReview(Pageable pageable);
}
