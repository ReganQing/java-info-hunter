package com.ron.javainfohunter.api.service.impl;

import com.ron.javainfohunter.api.dto.request.RssSourceRequest;
import com.ron.javainfohunter.api.dto.response.RssSourceResponse;
import com.ron.javainfohunter.api.exception.ResourceNotFoundException;
import com.ron.javainfohunter.api.service.RssSourceService;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RSS Source Service Implementation
 *
 * Implements business logic for RSS source management.
 * Uses repository pattern for data access and provides domain operations.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssSourceServiceImpl implements RssSourceService {

    private final RssSourceRepository rssSourceRepository;

    @Override
    @Transactional
    public RssSourceResponse createSource(RssSourceRequest request) {
        log.info("Creating RSS source: {}", request.getName());

        // Check if URL already exists
        rssSourceRepository.findByUrl(request.getUrl())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("RSS source with URL " + request.getUrl() + " already exists");
                });

        // Create entity from request
        RssSource source = RssSource.builder()
                .name(request.getName())
                .url(request.getUrl())
                .description(request.getDescription())
                .category(request.getCategory())
                .tags(request.getTags() != null ? request.getTags().toArray(new String[0]) : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .crawlIntervalSeconds(request.getCrawlIntervalSeconds() != null ?
                        request.getCrawlIntervalSeconds() : 3600)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .totalArticles(0L)
                .failedCrawls(0L)
                .build();

        RssSource savedSource = rssSourceRepository.save(source);
        log.info("RSS source created with ID: {}", savedSource.getId());

        return toResponse(savedSource);
    }

    @Override
    public Page<RssSourceResponse> getSources(String category, Boolean isActive, Pageable pageable) {
        log.debug("Getting RSS sources with filters - category: {}, isActive: {}", category, isActive);

        Page<RssSource> sources;

        if (category != null && isActive != null) {
            // Use the list method and paginate manually
            List<RssSource> sourceList = rssSourceRepository.findByCategoryAndIsActive(category, isActive);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), sourceList.size());
            List<RssSource> paginatedList = sourceList.subList(start, end);
            sources = new org.springframework.data.domain.PageImpl<>(paginatedList, pageable, sourceList.size());
        } else if (category != null) {
            sources = rssSourceRepository.findByCategory(category, pageable);
        } else if (isActive != null) {
            sources = rssSourceRepository.findByIsActive(isActive, pageable);
        } else {
            sources = rssSourceRepository.findAll(pageable);
        }

        return sources.map(this::toResponse);
    }

    @Override
    public RssSourceResponse getSourceById(Long id) {
        log.debug("Getting RSS source by ID: {}", id);
        RssSource source = rssSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RSS Source", id));
        return toResponse(source);
    }

    @Override
    @Transactional
    public RssSourceResponse updateSource(Long id, RssSourceRequest request) {
        log.info("Updating RSS source: {}", id);

        RssSource source = rssSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RSS Source", id));

        // Check if URL conflicts with another source
        rssSourceRepository.findByUrl(request.getUrl())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("RSS source with URL " + request.getUrl() + " already exists");
                    }
                });

        // Update fields
        source.setName(request.getName());
        source.setUrl(request.getUrl());
        source.setDescription(request.getDescription());
        source.setCategory(request.getCategory());
        source.setTags(request.getTags() != null ? request.getTags().toArray(new String[0]) : null);
        if (request.getIsActive() != null) {
            source.setIsActive(request.getIsActive());
        }
        if (request.getCrawlIntervalSeconds() != null) {
            source.setCrawlIntervalSeconds(request.getCrawlIntervalSeconds());
        }
        if (request.getLanguage() != null) {
            source.setLanguage(request.getLanguage());
        }
        if (request.getTimezone() != null) {
            source.setTimezone(request.getTimezone());
        }

        RssSource updatedSource = rssSourceRepository.save(source);
        log.info("RSS source updated: {}", id);

        return toResponse(updatedSource);
    }

    @Override
    @Transactional
    public void deleteSource(Long id) {
        log.info("Deleting RSS source: {}", id);

        RssSource source = rssSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RSS Source", id));

        rssSourceRepository.delete(source);
        log.info("RSS source deleted: {}", id);
    }

    @Override
    public Map<String, Object> triggerCrawl(Long id) {
        log.info("Triggering crawl for RSS source: {}", id);

        RssSource source = rssSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RSS Source", id));

        // In a real implementation, this would publish a message to RabbitMQ
        // For now, we return a mock response
        String taskId = UUID.randomUUID().toString();

        Map<String, Object> result = new HashMap<>();
        result.put("sourceId", source.getId());
        result.put("sourceName", source.getName());
        result.put("sourceUrl", source.getUrl());
        result.put("status", "triggered");
        result.put("message", "Crawl task triggered successfully");
        result.put("taskId", taskId);
        result.put("triggeredAt", java.time.Instant.now());

        log.info("Crawl task triggered with ID: {} for source: {}", taskId, id);

        // TODO: Publish message to RabbitMQ queue for crawler module
        // Message format: { "sourceId": id, "taskId": taskId, "url": source.getUrl() }

        return result;
    }

    /**
     * Convert RssSource entity to RssSourceResponse DTO
     *
     * @param source RSS source entity
     * @return RSS source response DTO
     */
    private RssSourceResponse toResponse(RssSource source) {
        return RssSourceResponse.builder()
                .id(source.getId())
                .name(source.getName())
                .url(source.getUrl())
                .description(source.getDescription())
                .category(source.getCategory())
                .tags(source.getTags() != null ? List.of(source.getTags()) : null)
                .isActive(source.getIsActive())
                .crawlIntervalSeconds(source.getCrawlIntervalSeconds())
                .maxRetries(source.getMaxRetries())
                .retryBackoffSeconds(source.getRetryBackoffSeconds())
                .language(source.getLanguage())
                .timezone(source.getTimezone())
                .lastCrawledAt(source.getLastCrawledAt())
                .totalArticles(source.getTotalArticles())
                .failedCrawls(source.getFailedCrawls())
                .failureRate(source.getFailureRate())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }
}
