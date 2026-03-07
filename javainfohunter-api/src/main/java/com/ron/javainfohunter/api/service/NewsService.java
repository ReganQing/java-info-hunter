package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.request.NewsQueryRequest;
import com.ron.javainfohunter.api.dto.response.NewsResponse;
import com.ron.javainfohunter.api.dto.response.SimilarNewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * News Service Interface
 *
 * Business logic for news query and search operations.
 * Handles filtering, searching, and recommendation features.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public interface NewsService {

    /**
     * Get paginated news with filters
     *
     * @param request Query request with filters
     * @return Paginated list of news
     */
    Page<NewsResponse> getNews(NewsQueryRequest request);

    /**
     * Get news by ID
     *
     * @param id News ID
     * @return News response
     * @throws com.ron.javainfohunter.api.exception.ResourceNotFoundException if news not found
     */
    NewsResponse getNewsById(Long id);

    /**
     * Full-text search news
     *
     * @param query    Search query
     * @param pageable Pagination information
     * @return Paginated list of matching news
     */
    Page<NewsResponse> searchNews(String query, Pageable pageable);

    /**
     * Get similar news based on shared topics/tags
     *
     * @param id   News ID
     * @param limit Maximum number of similar news to return
     * @return List of similar news
     */
    List<SimilarNewsResponse> getSimilarNews(Long id, int limit);

    /**
     * Get trending news (high engagement in last 24 hours)
     *
     * @param limit Maximum number of trending news to return
     * @return List of trending news
     */
    List<NewsResponse> getTrendingNews(int limit);

    /**
     * Get news by category
     *
     * @param category Category name
     * @param pageable Pagination information
     * @return Paginated list of news in the category
     */
    Page<NewsResponse> getNewsByCategory(String category, Pageable pageable);
}
