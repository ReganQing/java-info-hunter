package com.ron.javainfohunter.api.controller;

import com.ron.javainfohunter.api.dto.ApiResponse;
import com.ron.javainfohunter.api.dto.response.NewsResponse;
import com.ron.javainfohunter.api.dto.response.SimilarNewsResponse;
import com.ron.javainfohunter.api.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * News Controller
 *
 * REST API endpoints for news query and search operations.
 * Provides filtering, searching, and recommendation features.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Validated
@Tag(name = "Content Query", description = "News content query, search, and recommendation")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "Get news list", description = "Get paginated news with optional filters")
    public ResponseEntity<ApiResponse<Page<NewsResponse>>> getNews(
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by sentiment (POSITIVE, NEGATIVE, NEUTRAL)")
            @RequestParam(required = false) String sentiment,
            @Parameter(description = "Filter by start date")
            @RequestParam(required = false) Instant startDate,
            @Parameter(description = "Filter by end date")
            @RequestParam(required = false) Instant endDate,
            @Parameter(description = "Sort field (publishedAt, importanceScore, createdAt)")
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Getting news - page: {}, size: {}, category: {}, sentiment: {}",
                page, size, category, sentiment);

        // Validate sort direction
        if (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid sort direction. Must be ASC or DESC"));
        }

        com.ron.javainfohunter.api.dto.request.NewsQueryRequest request =
                com.ron.javainfohunter.api.dto.request.NewsQueryRequest.builder()
                        .category(category)
                        .sentiment(sentiment)
                        .startDate(startDate)
                        .endDate(endDate)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .size(size)
                        .build();

        Page<NewsResponse> news = newsService.getNews(request);
        return ResponseEntity.ok(ApiResponse.success(news));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get news by ID", description = "Get detailed information about a specific news article")
    public ResponseEntity<ApiResponse<NewsResponse>> getNewsById(
            @Parameter(description = "News ID")
            @PathVariable Long id) {
        log.debug("Getting news by ID: {}", id);
        NewsResponse news = newsService.getNewsById(id);
        return ResponseEntity.ok(ApiResponse.success(news));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search news", description = "Search news by title, summary, or content")
    public ResponseEntity<ApiResponse<Page<NewsResponse>>> searchNews(
            @Parameter(description = "Search query")
            @RequestParam @NotBlank String query,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Searching news with query: {}", query);

        if (query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search query cannot be empty"));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<NewsResponse> results = newsService.searchNews(query, pageable);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Get similar news", description = "Get news similar to the specified article based on topics and tags")
    public ResponseEntity<ApiResponse<List<SimilarNewsResponse>>> getSimilarNews(
            @Parameter(description = "News ID")
            @PathVariable Long id,
            @Parameter(description = "Maximum number of similar news to return")
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {

        log.debug("Getting similar news for: {}", id);
        List<SimilarNewsResponse> similarNews = newsService.getSimilarNews(id, limit);
        return ResponseEntity.ok(ApiResponse.success(similarNews));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending news", description = "Get trending news from the last 24 hours sorted by engagement")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getTrendingNews(
            @Parameter(description = "Maximum number of trending news to return")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.debug("Getting {} trending news", limit);
        List<NewsResponse> trendingNews = newsService.getTrendingNews(limit);
        return ResponseEntity.ok(ApiResponse.success(trendingNews));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get news by category", description = "Get paginated news from a specific category")
    public ResponseEntity<ApiResponse<Page<NewsResponse>>> getNewsByCategory(
            @Parameter(description = "Category name")
            @PathVariable String category,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("Getting news by category: {}", category);
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<NewsResponse> news = newsService.getNewsByCategory(category, pageable);

        return ResponseEntity.ok(ApiResponse.success(news));
    }
}
