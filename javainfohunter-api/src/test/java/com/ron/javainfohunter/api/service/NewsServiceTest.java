package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.request.NewsQueryRequest;
import com.ron.javainfohunter.api.dto.response.NewsResponse;
import com.ron.javainfohunter.api.dto.response.SimilarNewsResponse;
import com.ron.javainfohunter.api.exception.BusinessException;
import com.ron.javainfohunter.api.exception.ResourceNotFoundException;
import com.ron.javainfohunter.api.service.impl.NewsServiceImpl;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NewsService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("News Service Tests")
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News testNews;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        testNews = News.builder()
                .id(1L)
                .title("Test News Article")
                .summary("This is a test summary")
                .category("Technology")
                .topics(new String[]{"AI", "Machine Learning"})
                .keywords(new String[]{"artificial intelligence", "tech"})
                .tags(new String[]{"ai", "technology"})
                .sentiment(News.Sentiment.POSITIVE)
                .sentimentScore(new BigDecimal("0.75"))
                .importanceScore(new BigDecimal("0.85"))
                .readingTimeMinutes(5)
                .language("en")
                .slug("test-news-article")
                .featuredImageUrl("https://example.com/image.jpg")
                .viewCount(1000L)
                .likeCount(50)
                .shareCount(10)
                .isPublished(true)
                .isFeatured(false)
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("Get news - default query")
    void testGetNews_Default() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findByIsPublishedTrue(any(Pageable.class))).thenReturn(newsPage);

        NewsQueryRequest request = NewsQueryRequest.builder().build();
        Page<NewsResponse> response = newsService.getNews(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(testNews.getTitle(), response.getContent().get(0).getTitle());

        verify(newsRepository, times(1)).findByIsPublishedTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Get news - with category filter")
    void testGetNews_WithCategoryFilter() {
        String category = "Technology";
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findByCategoryAndIsPublishedTrue(eq(category), any(Pageable.class)))
                .thenReturn(newsPage);

        NewsQueryRequest request = NewsQueryRequest.builder()
                .category(category)
                .build();
        Page<NewsResponse> response = newsService.getNews(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(category, response.getContent().get(0).getCategory());

        verify(newsRepository, times(1)).findByCategoryAndIsPublishedTrue(eq(category), any(Pageable.class));
    }

    @Test
    @DisplayName("Get news - with sentiment filter")
    void testGetNews_WithSentimentFilter() {
        String sentiment = "POSITIVE";
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findBySentiment(eq(News.Sentiment.POSITIVE), any(Pageable.class)))
                .thenReturn(newsPage);

        NewsQueryRequest request = NewsQueryRequest.builder()
                .sentiment(sentiment)
                .build();
        Page<NewsResponse> response = newsService.getNews(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("POSITIVE", response.getContent().get(0).getSentiment());

        verify(newsRepository, times(1)).findBySentiment(eq(News.Sentiment.POSITIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("Get news by ID - found")
    void testGetNewsById_Found() {
        when(newsRepository.findById(1L)).thenReturn(Optional.of(testNews));

        NewsResponse response = newsService.getNewsById(1L);

        assertNotNull(response);
        assertEquals(testNews.getId(), response.getId());
        assertEquals(testNews.getTitle(), response.getTitle());

        verify(newsRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get news by ID - not found")
    void testGetNewsById_NotFound() {
        when(newsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> newsService.getNewsById(999L));

        verify(newsRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Search news")
    void testSearchNews() {
        String query = "artificial intelligence";
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.fullTextSearch(eq(query), any(Pageable.class))).thenReturn(newsPage);

        Page<NewsResponse> response = newsService.searchNews(query, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());

        verify(newsRepository, times(1)).fullTextSearch(eq(query), any(Pageable.class));
    }

    @Test
    @DisplayName("Get similar news")
    void testGetSimilarNews() {
        Long newsId = 1L;
        int limit = 5;
        Pageable pageable = PageRequest.of(0, limit);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));

        when(newsRepository.existsById(newsId)).thenReturn(true);
        when(newsRepository.findSimilarNews(eq(newsId), any(Pageable.class))).thenReturn(newsPage);

        List<SimilarNewsResponse> response = newsService.getSimilarNews(newsId, limit);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testNews.getId(), response.get(0).getId());

        verify(newsRepository, times(1)).existsById(newsId);
        verify(newsRepository, times(1)).findSimilarNews(eq(newsId), any(Pageable.class));
    }

    @Test
    @DisplayName("Get trending news")
    void testGetTrendingNews() {
        int limit = 10;
        Instant since = now.minusSeconds(86400); // 24 hours ago
        Pageable pageable = PageRequest.of(0, limit);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findTrendingNews(any(Instant.class), any(Pageable.class))).thenReturn(newsPage);

        List<NewsResponse> response = newsService.getTrendingNews(limit);

        assertNotNull(response);
        assertEquals(1, response.size());

        verify(newsRepository, times(1)).findTrendingNews(any(Instant.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Get news by category")
    void testGetNewsByCategory() {
        String category = "Technology";
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findByCategoryAndIsPublishedTrue(eq(category), any(Pageable.class)))
                .thenReturn(newsPage);

        Page<NewsResponse> response = newsService.getNewsByCategory(category, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(category, response.getContent().get(0).getCategory());

        verify(newsRepository, times(1)).findByCategoryAndIsPublishedTrue(eq(category), any(Pageable.class));
    }

    @Test
    @DisplayName("Get news - with date range filter")
    void testGetNews_WithDateRange() {
        Instant startDate = now.minusSeconds(86400);
        Instant endDate = now;
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(
                eq(startDate), eq(endDate), any(Pageable.class))).thenReturn(newsPage);

        NewsQueryRequest request = NewsQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
        Page<NewsResponse> response = newsService.getNews(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());

        verify(newsRepository, times(1)).findByPublishedAtBetweenOrderByPublishedAtDesc(
                eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    @DisplayName("Get news - with all filters")
    void testGetNews_WithAllFilters() {
        String category = "Technology";
        String sentiment = "POSITIVE";
        Instant startDate = now.minusSeconds(86400);
        Instant endDate = now;
        Pageable pageable = PageRequest.of(0, 20);

        // When both category and sentiment are provided, we should use the filters method
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        when(newsRepository.findByFilters(
                eq(category),
                eq(News.Sentiment.POSITIVE),
                eq(true),
                any(),
                any(Pageable.class)
        )).thenReturn(newsPage);

        NewsQueryRequest request = NewsQueryRequest.builder()
                .category(category)
                .sentiment(sentiment)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        Page<NewsResponse> response = newsService.getNews(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("Get news - invalid sentiment value")
    void testGetNews_InvalidSentiment() {
        String invalidSentiment = "INVALID_SENTIMENT";

        NewsQueryRequest request = NewsQueryRequest.builder()
                .sentiment(invalidSentiment)
                .build();

        // Should throw BusinessException for invalid sentiment
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            newsService.getNews(request);
        });

        assertTrue(exception.getMessage().contains("Invalid sentiment value"));
        assertTrue(exception.getMessage().contains(invalidSentiment));
    }

    @Test
    @DisplayName("Get news - invalid sentiment with category")
    void testGetNews_InvalidSentimentWithCategory() {
        String category = "Technology";
        String invalidSentiment = "HAPPY"; // Not a valid sentiment

        NewsQueryRequest request = NewsQueryRequest.builder()
                .category(category)
                .sentiment(invalidSentiment)
                .build();

        // Should throw BusinessException for invalid sentiment
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            newsService.getNews(request);
        });

        assertTrue(exception.getMessage().contains("Invalid sentiment value"));
        assertTrue(exception.getMessage().contains(invalidSentiment));
    }

    @Test
    @DisplayName("Get news - case insensitive sentiment parsing")
    void testGetNews_CaseInsensitiveSentiment() {
        String sentiment = "positive"; // lowercase
        Pageable pageable = PageRequest.of(0, 20);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));

        when(newsRepository.findBySentiment(eq(News.Sentiment.POSITIVE), any(Pageable.class)))
                .thenReturn(newsPage);

        NewsQueryRequest request = NewsQueryRequest.builder()
                .sentiment(sentiment)
                .build();
        Page<NewsResponse> response = newsService.getNews(request);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("POSITIVE", response.getContent().get(0).getSentiment());

        verify(newsRepository, times(1)).findBySentiment(eq(News.Sentiment.POSITIVE), any(Pageable.class));
    }
}
