package com.ron.javainfohunter.api.controller;

import com.ron.javainfohunter.api.dto.ApiResponse;
import com.ron.javainfohunter.api.dto.request.RssSourceRequest;
import com.ron.javainfohunter.api.dto.response.RssSourceResponse;
import com.ron.javainfohunter.api.service.RssSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RSS Source Controller
 *
 * REST API endpoints for managing RSS subscription sources.
 * Provides CRUD operations and manual crawl triggering.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rss-sources")
@RequiredArgsConstructor
@Tag(name = "RSS Source Management", description = "RSS subscription source CRUD operations")
public class RssSourceController {

    private final RssSourceService rssSourceService;

    @PostMapping
    @Operation(summary = "Create RSS source", description = "Create a new RSS subscription source")
    public ResponseEntity<ApiResponse<RssSourceResponse>> createSource(
            @Valid @RequestBody RssSourceRequest request) {
        log.info("Creating RSS source: {}", request.getName());
        RssSourceResponse response = rssSourceService.createSource(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "RSS source created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get RSS sources", description = "Get paginated list of RSS sources with optional filters")
    public ResponseEntity<ApiResponse<Page<RssSourceResponse>>> getSources(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive) {
        log.debug("Getting RSS sources - page: {}, size: {}, category: {}, isActive: {}",
                page, size, category, isActive);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RssSourceResponse> sources = rssSourceService.getSources(category, isActive, pageable);

        return ResponseEntity.ok(ApiResponse.success(sources));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get RSS source by ID", description = "Get detailed information about a specific RSS source")
    public ResponseEntity<ApiResponse<RssSourceResponse>> getSource(
            @Parameter(description = "RSS source ID")
            @PathVariable Long id) {
        log.debug("Getting RSS source: {}", id);
        RssSourceResponse response = rssSourceService.getSourceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update RSS source", description = "Update an existing RSS source")
    public ResponseEntity<ApiResponse<RssSourceResponse>> updateSource(
            @Parameter(description = "RSS source ID")
            @PathVariable Long id,
            @Valid @RequestBody RssSourceRequest request) {
        log.info("Updating RSS source: {}", id);
        RssSourceResponse response = rssSourceService.updateSource(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "RSS source updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete RSS source", description = "Delete an RSS source")
    public ResponseEntity<ApiResponse<Void>> deleteSource(
            @Parameter(description = "RSS source ID")
            @PathVariable Long id) {
        log.info("Deleting RSS source: {}", id);
        rssSourceService.deleteSource(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/{id}/crawl")
    @Operation(summary = "Trigger manual crawl", description = "Manually trigger a crawl task for the RSS source")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerCrawl(
            @Parameter(description = "RSS source ID")
            @PathVariable Long id) {
        log.info("Triggering crawl for RSS source: {}", id);
        Map<String, Object> result = rssSourceService.triggerCrawl(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Crawl task triggered successfully"));
    }
}
