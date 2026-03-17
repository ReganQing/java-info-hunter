package com.ron.javainfohunter.api.service.impl;

import com.ron.javainfohunter.api.dto.request.NewsQueryRequest;
import com.ron.javainfohunter.api.dto.response.NewsResponse;
import com.ron.javainfohunter.api.dto.response.SimilarNewsResponse;
import com.ron.javainfohunter.api.exception.BusinessException;
import com.ron.javainfohunter.api.exception.ResourceNotFoundException;
import com.ron.javainfohunter.api.service.NewsService;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * News Service Implementation
 *
 * Implements business logic for news query operations.
 * Provides filtering, searching, and recommendation features.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    @Override
    public Page<NewsResponse> getNews(NewsQueryRequest request) {
        log.debug("Getting news with filters: {}", request);

        Pageable pageable = createPageable(request);
        Page<News> newsPage;

        // Build query based on filters
        if (request.getCategory() != null && request.getSentiment() != null) {
            // Use complex filter query
            News.Sentiment sentiment = parseSentiment(request.getSentiment());
            newsPage = newsRepository.findByFilters(
                    request.getCategory(),
                    sentiment,
                    true, // isPublished
                    null, // minImportance
                    pageable
            );
        } else if (request.getCategory() != null && request.getStartDate() != null && request.getEndDate() != null) {
            // Category + date range
            newsPage = newsRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(
                    request.getStartDate(),
                    request.getEndDate(),
                    pageable
            );
        } else if (request.getCategory() != null) {
            // Category only
            newsPage = newsRepository.findByCategoryAndIsPublishedTrue(
                    request.getCategory(),
                    pageable
            );
        } else if (request.getSentiment() != null) {
            // Sentiment only
            News.Sentiment sentiment = parseSentiment(request.getSentiment());
            newsPage = newsRepository.findBySentiment(sentiment, pageable);
        } else if (request.getStartDate() != null && request.getEndDate() != null) {
            // Date range only
            newsPage = newsRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(
                    request.getStartDate(),
                    request.getEndDate(),
                    pageable
            );
        } else {
            // Default: all published news
            newsPage = newsRepository.findByIsPublishedTrue(pageable);
        }

        return newsPage.map(this::toResponse);
    }

    @Override
    public NewsResponse getNewsById(Long id) {
        log.debug("Getting news by ID: {}", id);
        News news = newsRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", id));
        return toResponse(news);
    }

    @Override
    public Page<NewsResponse> searchNews(String query, Pageable pageable) {
        log.debug("Searching news with query: {}", query);
        Page<News> newsPage = newsRepository.fullTextSearch(query, pageable);
        return newsPage.map(this::toResponse);
    }

    @Override
    public List<SimilarNewsResponse> getSimilarNews(Long id, int limit) {
        log.debug("Getting similar news for: {}", id);

        // Verify the news exists
        if (!newsRepository.existsById(id)) {
            throw new ResourceNotFoundException("News", id);
        }

        Pageable pageable = PageRequest.of(0, limit);
        Page<News> similarNews = newsRepository.findSimilarNews(id, pageable);

        return similarNews.getContent().stream()
                .map(this::toSimilarResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NewsResponse> getTrendingNews(int limit) {
        log.debug("Getting {} trending news", limit);

        // Get news from last 24 hours
        Instant since = Instant.now().minusSeconds(86400);
        Pageable pageable = PageRequest.of(0, limit);

        Page<News> trendingNews = newsRepository.findTrendingNews(since, pageable);

        return trendingNews.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NewsResponse> getNewsByCategory(String category, Pageable pageable) {
        log.debug("Getting news by category: {}", category);
        Page<News> newsPage = newsRepository.findByCategoryAndIsPublishedTrue(category, pageable);
        return newsPage.map(this::toResponse);
    }

    /**
     * Create Pageable from request
     */
    private Pageable createPageable(NewsQueryRequest request) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    /**
     * Convert News entity to NewsResponse DTO
     */
    private NewsResponse toResponse(News news) {
        // Extract sourceName and url from rawContent if available
        String sourceName = null;
        String url = null;

        if (news.getRawContent() != null) {
            if (news.getRawContent().getRssSource() != null) {
                sourceName = news.getRawContent().getRssSource().getName();
            }
            url = news.getRawContent().getLink();
        }

        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategory())
                .topics(news.getTopics() != null ? Arrays.asList(news.getTopics()) : null)
                .keywords(news.getKeywords() != null ? Arrays.asList(news.getKeywords()) : null)
                .tags(news.getTags() != null ? Arrays.asList(news.getTags()) : null)
                .sentiment(news.getSentiment() != null ? news.getSentiment().name() : null)
                .sentimentScore(news.getSentimentScore())
                .importanceScore(news.getImportanceScore())
                .readingTimeMinutes(news.getReadingTimeMinutes())
                .language(news.getLanguage())
                .slug(news.getSlug())
                .featuredImageUrl(news.getFeaturedImageUrl())
                .viewCount(news.getViewCount())
                .likeCount(news.getLikeCount())
                .shareCount(news.getShareCount())
                .isPublished(news.getIsPublished())
                .isFeatured(news.getIsFeatured())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .sourceName(sourceName)
                .url(url)
                .build();
    }

    /**
     * Convert News entity to SimilarNewsResponse DTO
     */
    private SimilarNewsResponse toSimilarResponse(News news) {
        return SimilarNewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategory())
                .similarityScore(0.0) // Placeholder - actual similarity would come from vector search
                .sharedTagsCount(news.getTopics() != null ? news.getTopics().length : 0)
                .publishedAt(news.getPublishedAt())
                .featuredImageUrl(news.getFeaturedImageUrl())
                .build();
    }

    /**
     * Parse sentiment string to enum with validation
     *
     * @param sentiment Sentiment string (case-insensitive)
     * @return Parsed Sentiment enum
     * @throws BusinessException if sentiment value is invalid
     */
    private News.Sentiment parseSentiment(String sentiment) {
        try {
            return News.Sentiment.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid sentiment value: " + sentiment +
                    ". Must be one of: " + Arrays.asList(News.Sentiment.values())
            );
        }
    }
}
