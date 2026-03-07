package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.request.RssSourceRequest;
import com.ron.javainfohunter.api.dto.response.RssSourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * RSS Source Service Interface
 *
 * Business logic for RSS source management operations.
 * Handles CRUD operations and crawl triggering for RSS sources.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public interface RssSourceService {

    /**
     * Create a new RSS source
     *
     * @param request RSS source creation request
     * @return Created RSS source response
     */
    RssSourceResponse createSource(RssSourceRequest request);

    /**
     * Get paginated list of RSS sources with optional filters
     *
     * @param category Optional category filter
     * @param isActive Optional active status filter
     * @param pageable Pagination information
     * @return Paginated list of RSS sources
     */
    Page<RssSourceResponse> getSources(String category, Boolean isActive, Pageable pageable);

    /**
     * Get RSS source by ID
     *
     * @param id RSS source ID
     * @return RSS source response
     * @throws com.ron.javainfohunter.api.exception.ResourceNotFoundException if source not found
     */
    RssSourceResponse getSourceById(Long id);

    /**
     * Update existing RSS source
     *
     * @param id      RSS source ID
     * @param request RSS source update request
     * @return Updated RSS source response
     * @throws com.ron.javainfohunter.api.exception.ResourceNotFoundException if source not found
     */
    RssSourceResponse updateSource(Long id, RssSourceRequest request);

    /**
     * Delete RSS source by ID
     *
     * @param id RSS source ID
     * @throws com.ron.javainfohunter.api.exception.ResourceNotFoundException if source not found
     */
    void deleteSource(Long id);

    /**
     * Trigger manual crawl for RSS source
     *
     * @param id RSS source ID
     * @return Crawl result information
     * @throws com.ron.javainfohunter.api.exception.ResourceNotFoundException if source not found
     */
    Map<String, Object> triggerCrawl(Long id);
}
