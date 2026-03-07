package com.ron.javainfohunter.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.response.NewsResponse;
import com.ron.javainfohunter.api.dto.response.SimilarNewsResponse;
import com.ron.javainfohunter.api.service.NewsService;
import com.ron.javainfohunter.entity.News;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for NewsController
 */
@WebMvcTest(NewsController.class)
@DisplayName("News Controller Tests")
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsService newsService;

    private NewsResponse testResponse;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        testResponse = NewsResponse.builder()
                .id(1L)
                .title("Test News Article")
                .summary("This is a test summary")
                .category("Technology")
                .topics(Arrays.asList("AI", "Machine Learning"))
                .keywords(Arrays.asList("artificial intelligence", "tech"))
                .tags(Arrays.asList("ai", "technology"))
                .sentiment("POSITIVE")
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
    @DisplayName("Get news - success")
    void testGetNews_Success() throws Exception {
        when(newsService.getNews(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(testResponse))
        );

        mockMvc.perform(get("/api/v1/news")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].title").value("Test News Article"));
    }

    @Test
    @DisplayName("Get news - with filters")
    void testGetNews_WithFilters() throws Exception {
        when(newsService.getNews(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(testResponse))
        );

        mockMvc.perform(get("/api/v1/news")
                        .param("category", "Technology")
                        .param("sentiment", "POSITIVE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].category").value("Technology"))
                .andExpect(jsonPath("$.data.content[0].sentiment").value("POSITIVE"));
    }

    @Test
    @DisplayName("Get news by ID - success")
    void testGetNewsById_Success() throws Exception {
        when(newsService.getNewsById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Test News Article"));
    }

    @Test
    @DisplayName("Get news by ID - not found")
    void testGetNewsById_NotFound() throws Exception {
        when(newsService.getNewsById(999L))
                .thenThrow(new com.ron.javainfohunter.api.exception.ResourceNotFoundException("News", 999L));

        mockMvc.perform(get("/api/v1/news/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("News not found with id: 999"));
    }

    @Test
    @DisplayName("Search news - success")
    void testSearchNews_Success() throws Exception {
        when(newsService.searchNews(eq("artificial intelligence"), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(testResponse))
        );

        mockMvc.perform(get("/api/v1/news/search")
                        .param("query", "artificial intelligence")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L));
    }

    @Test
    @DisplayName("Search news - empty query")
    void testSearchNews_EmptyQuery() throws Exception {
        mockMvc.perform(get("/api/v1/news/search")
                        .param("query", "")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Get similar news - success")
    void testGetSimilarNews_Success() throws Exception {
        SimilarNewsResponse similarNews = SimilarNewsResponse.builder()
                .id(2L)
                .title("Similar Article")
                .summary("Similar content")
                .category("Technology")
                .similarityScore(0.85)
                .sharedTagsCount(3)
                .publishedAt(now)
                .build();

        when(newsService.getSimilarNews(1L, 5)).thenReturn(List.of(similarNews));

        mockMvc.perform(get("/api/v1/news/1/similar")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2L))
                .andExpect(jsonPath("$.data[0].similarityScore").value(0.85));
    }

    @Test
    @DisplayName("Get trending news - success")
    void testGetTrendingNews_Success() throws Exception {
        when(newsService.getTrendingNews(10)).thenReturn(List.of(testResponse));

        mockMvc.perform(get("/api/v1/news/trending")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("Test News Article"));
    }

    @Test
    @DisplayName("Get news by category - success")
    void testGetNewsByCategory_Success() throws Exception {
        when(newsService.getNewsByCategory(eq("Technology"), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(testResponse))
        );

        mockMvc.perform(get("/api/v1/news/category/Technology")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].category").value("Technology"));
    }

    @Test
    @DisplayName("Get news - invalid sort direction")
    void testGetNews_InvalidSortDirection() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                        .param("sortBy", "publishedAt")
                        .param("sortDirection", "INVALID")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Get news - page size exceeds maximum")
    void testGetNews_PageSizeExceedsMaximum() throws Exception {
        // The @Max(100) validation should reject sizes greater than 100
        mockMvc.perform(get("/api/v1/news")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get news - page size below minimum")
    void testGetNews_PageSizeBelowMinimum() throws Exception {
        // The @Min(1) validation should reject sizes less than 1
        mockMvc.perform(get("/api/v1/news")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get news - page number negative")
    void testGetNews_PageNumberNegative() throws Exception {
        // The @Min(0) validation should reject negative page numbers
        mockMvc.perform(get("/api/v1/news")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
