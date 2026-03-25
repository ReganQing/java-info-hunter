package com.ron.javainfohunter.api.controller;

import com.ron.javainfohunter.api.dto.ApiResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import com.ron.javainfohunter.api.dto.response.CrawlTriggerResponse;
import com.ron.javainfohunter.api.dto.response.ResourceUsageResponse;
import com.ron.javainfohunter.api.dto.response.SystemMetricsResponse;
import com.ron.javainfohunter.api.dto.response.SystemStatusResponse;
import com.ron.javainfohunter.api.service.AgentService;
import com.ron.javainfohunter.api.service.NewsService;
import com.ron.javainfohunter.api.service.RssSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin Controller
 *
 * REST API endpoints for system management and operations.
 * Provides administrative functions for monitoring and controlling the system.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "System Management", description = "System management and operations")
public class AdminController {

    private final RssSourceService rssSourceService;
    private final NewsService newsService;
    private final AgentService agentService;

    private static final int MAX_BATCH_SIZE = 100;

    @PostMapping("/crawl/trigger")
    @Operation(summary = "Trigger full crawl", description = "Trigger crawl for all active RSS sources")
    public ResponseEntity<ApiResponse<CrawlTriggerResponse>> triggerFullCrawl() {
        log.info("Triggering full crawl for all active sources");

        try {
            int sourcesTriggered = 0;
            StringBuilder taskIds = new StringBuilder();
            int page = 0;
            boolean hasMore = true;

            // Process sources in batches to handle large numbers efficiently
            while (hasMore) {
                Pageable pageable = PageRequest.of(page, MAX_BATCH_SIZE);
                Page<com.ron.javainfohunter.api.dto.response.RssSourceResponse> activeSources =
                        rssSourceService.getSources(null, true, pageable);

                for (com.ron.javainfohunter.api.dto.response.RssSourceResponse source : activeSources.getContent()) {
                    try {
                        Map<String, Object> result = rssSourceService.triggerCrawl(source.getId());
                        if (result.containsKey("taskId") && result.get("taskId") != null) {
                            if (taskIds.length() > 0) {
                                taskIds.append(",");
                            }
                            taskIds.append(result.get("taskId"));
                            sourcesTriggered++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to trigger crawl for source {}: {}", source.getId(), e.getMessage());
                    }
                }

                hasMore = !activeSources.isLast();
                page++;
            }

            CrawlTriggerResponse response = CrawlTriggerResponse.builder()
                    .triggered(true)
                    .message(String.format("Triggered crawl for %d sources", sourcesTriggered))
                    .sourcesTriggered(sourcesTriggered)
                    .triggeredAt(Instant.now())
                    .taskIds(taskIds.toString())
                    .estimatedArticles(sourcesTriggered * 10) // Rough estimate
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "Full crawl triggered successfully"));

        } catch (Exception e) {
            log.error("Failed to trigger full crawl", e);

            CrawlTriggerResponse response = CrawlTriggerResponse.builder()
                    .triggered(false)
                    .message("Failed to trigger crawl: " + e.getMessage())
                    .sourcesTriggered(0)
                    .triggeredAt(Instant.now())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }

    @PostMapping("/crawl-by-category")
    @Operation(summary = "Trigger category crawl", description = "Trigger crawl for all RSS sources in a specific category")
    public ResponseEntity<ApiResponse<CrawlTriggerResponse>> triggerCategoryCrawl(
            @RequestBody Map<String, String> request) {

        String category = request.get("category");
        if (category == null || category.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Category parameter is required"));
        }

        log.info("Triggering crawl for category: {}", category);

        try {
            int sourcesTriggered = 0;
            List<String> taskIdList = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // Get all sources for the specified category
            Pageable pageable = PageRequest.of(0, MAX_BATCH_SIZE);
            Page<com.ron.javainfohunter.api.dto.response.RssSourceResponse> categorySources =
                    rssSourceService.getSources(category, null, pageable);

            for (com.ron.javainfohunter.api.dto.response.RssSourceResponse source : categorySources.getContent()) {
                try {
                    Map<String, Object> result = rssSourceService.triggerCrawl(source.getId());
                    if (result.containsKey("taskId") && result.get("taskId") != null) {
                        taskIdList.add(result.get("taskId").toString());
                        sourcesTriggered++;
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to trigger crawl for source %s: %s",
                            source.getName(), e.getMessage());
                    log.warn(errorMsg);
                    errors.add(errorMsg);
                }
            }

            String taskIds = String.join(",", taskIdList);
            String message;
            if (errors.isEmpty()) {
                message = String.format("Triggered crawl for %d sources in category '%s'",
                        sourcesTriggered, category);
            } else {
                message = String.format("Triggered crawl for %d sources in category '%s' (%d errors)",
                        sourcesTriggered, category, errors.size());
            }

            CrawlTriggerResponse response = CrawlTriggerResponse.builder()
                    .triggered(sourcesTriggered > 0)
                    .message(message)
                    .sourcesTriggered(sourcesTriggered)
                    .triggeredAt(Instant.now())
                    .taskIds(taskIds)
                    .estimatedArticles(sourcesTriggered * 10)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, message));

        } catch (Exception e) {
            log.error("Failed to trigger category crawl", e);

            CrawlTriggerResponse response = CrawlTriggerResponse.builder()
                    .triggered(false)
                    .message("Failed to trigger category crawl: " + e.getMessage())
                    .sourcesTriggered(0)
                    .triggeredAt(Instant.now())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }

    @PostMapping("/crawl/{sourceId}")
    @Operation(summary = "Trigger single source crawl", description = "Trigger crawl for a specific RSS source")
    public ResponseEntity<ApiResponse<CrawlTriggerResponse>> triggerSourceCrawl(
            @Parameter(description = "RSS source ID")
            @PathVariable Long sourceId) {

        log.info("Triggering crawl for source: {}", sourceId);

        Map<String, Object> result = rssSourceService.triggerCrawl(sourceId);

        CrawlTriggerResponse response = CrawlTriggerResponse.builder()
                .triggered(true)
                .message("Crawl triggered successfully")
                .sourcesTriggered(1)
                .triggeredAt(Instant.now())
                .taskIds(result.get("taskId") != null ? result.get("taskId").toString() : UUID.randomUUID().toString())
                .estimatedArticles(10)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Source crawl triggered successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get system status", description = "Get overall system health and status")
    public ResponseEntity<ApiResponse<SystemStatusResponse>> getSystemStatus() {
        log.info("Getting system status");

        // Get counts
        Pageable pageable = PageRequest.of(0, 1);
        long totalSources = rssSourceService.getSources(null, null, pageable).getTotalElements();
        long activeSources = rssSourceService.getSources(null, true, pageable).getTotalElements();
        long totalNews = newsService.getNews(
                com.ron.javainfohunter.api.dto.request.NewsQueryRequest.builder().build()
        ).getTotalElements();

        // Get agent stats
        AgentStatsResponse agentStats = agentService.getExecutionStats();
        long pendingProcessing = agentStats.getRunningExecutions();

        // Determine overall status
        String status = "HEALTHY";
        if (pendingProcessing > 100 || activeSources < totalSources * 0.8) {
            status = "DEGRADED";
        }
        if (activeSources == 0) {
            status = "DOWN";
        }

        // Build services status map
        Map<String, Object> services = new HashMap<>();
        services.put("rssSources", Map.of(
                "total", totalSources,
                "active", activeSources,
                "status", activeSources > 0 ? "UP" : "DOWN"
        ));
        services.put("newsProcessing", Map.of(
                "totalArticles", totalNews,
                "pending", pendingProcessing,
                "status", "UP"
        ));

        Map<String, Object> agentSystemStatus = new HashMap<>();
        agentSystemStatus.put("runningExecutions", agentStats.getRunningExecutions() != null ? agentStats.getRunningExecutions() : 0L);
        agentSystemStatus.put("totalExecutions", agentStats.getTotalExecutions() != null ? agentStats.getTotalExecutions() : 0L);
        agentSystemStatus.put("status", "UP");
        services.put("agentSystem", agentSystemStatus);

        SystemStatusResponse systemStatus = SystemStatusResponse.builder()
                .status(status)
                .timestamp(Instant.now())
                .totalRssSources(totalSources)
                .activeRssSources(activeSources)
                .totalNews(totalNews)
                .pendingProcessing(pendingProcessing)
                .services(services)
                .uptimeSeconds(System.currentTimeMillis() / 1000) // Simplified uptime
                .version("0.0.1-SNAPSHOT")
                .build();

        return ResponseEntity.ok(ApiResponse.success(systemStatus));
    }

    @GetMapping("/resources")
    @Operation(summary = "Get system resources", description = "Get current system resource usage")
    public ResponseEntity<ApiResponse<ResourceUsageResponse>> getResources() {
        Runtime runtime = Runtime.getRuntime();

        ResourceUsageResponse response = ResourceUsageResponse.builder()
                .memoryUsed(runtime.totalMemory() - runtime.freeMemory())
                .memoryTotal(runtime.totalMemory())
                .diskUsed(0L)  // Not available in standard Java API
                .diskTotal(0L) // Not available in standard Java API
                .cpuPercent(0.0) // Not available in standard Java API
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics", description = "Get system performance metrics")
    public ResponseEntity<ApiResponse<SystemMetricsResponse>> getMetrics() {
        SystemMetricsResponse response = SystemMetricsResponse.builder()
                .uptime(System.currentTimeMillis() / 1000)
                .activeConnections(0)
                .requestRate(List.of())
                .errorRate(List.of())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
