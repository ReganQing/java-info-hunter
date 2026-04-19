package com.ron.javainfohunter.crawler.controller;

import com.ron.javainfohunter.crawler.dto.CrawlResultMessage;
import com.ron.javainfohunter.crawler.scheduler.CrawlOrchestrator;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller for manually triggering crawl jobs.
 * This controller is for development and testing purposes only.
 */
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlOrchestrator crawlOrchestrator;
    private final RssSourceRepository rssSourceRepository;

    @Value("${javainfohunter.crawler.api-key:}")
    private String apiKey;

    private ResponseEntity<Map<String, Object>> unauthorizedResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "Unauthorized: invalid or missing API key");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    private boolean isApiValid(String requestApiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return true; // No API key configured, allow all
        }
        return apiKey.equals(requestApiKey);
    }

    /**
     * Manually trigger a crawl job for all active RSS sources.
     *
     * @return crawl result
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerCrawl(
            @RequestHeader(value = "X-API-Key", required = false) String requestApiKey) {
        log.info("Manual crawl trigger requested");

        if (!isApiValid(requestApiKey)) {
            return unauthorizedResponse();
        }

        List<RssSource> sources = rssSourceRepository.findByIsActiveTrue();
        log.info("Found {} active sources for manual crawl", sources.size());

        if (sources.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "No active RSS sources found");
            result.put("triggeredAt", Instant.now());
            return ResponseEntity.ok(result);
        }

        CrawlResultMessage crawlResult = crawlOrchestrator.executeCrawlJob(sources);

        Map<String, Object> result = new HashMap<>();
        result.put("success", crawlResult.getStatus() == CrawlResultMessage.CrawlStatus.SUCCESS ||
                         crawlResult.getStatus() == CrawlResultMessage.CrawlStatus.PARTIAL);
        result.put("message", "Manual crawl completed");
        result.put("articlesCrawled", crawlResult.getArticlesCrawled());
        result.put("newArticles", crawlResult.getNewArticles());
        result.put("duplicateArticles", crawlResult.getDuplicateArticles());
        result.put("failedArticles", crawlResult.getFailedArticles());
        result.put("status", crawlResult.getStatus());
        result.put("triggeredAt", Instant.now());
        result.put("durationMs", crawlResult.getDurationMs());

        return ResponseEntity.ok(result);
    }

    /**
     * Trigger crawl for a specific source by ID.
     *
     * @param sourceId the RSS source ID
     * @return crawl result
     */
    @PostMapping("/trigger/{sourceId}")
    public ResponseEntity<Map<String, Object>> triggerCrawlForSource(
            @PathVariable Long sourceId,
            @RequestHeader(value = "X-API-Key", required = false) String requestApiKey) {
        log.info("Manual crawl trigger requested for source: {}", sourceId);

        if (!isApiValid(requestApiKey)) {
            return unauthorizedResponse();
        }

        return rssSourceRepository.findById(sourceId)
                .map(source -> {
                    CrawlResultMessage crawlResult = crawlOrchestrator.executeCrawlJob(List.of(source));

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", crawlResult.getStatus() == CrawlResultMessage.CrawlStatus.SUCCESS ||
                                     crawlResult.getStatus() == CrawlResultMessage.CrawlStatus.PARTIAL);
                    result.put("message", "Crawl completed for source: " + source.getName());
                    result.put("sourceId", sourceId);
                    result.put("sourceName", source.getName());
                    result.put("articlesCrawled", crawlResult.getArticlesCrawled());
                    result.put("newArticles", crawlResult.getNewArticles());
                    result.put("duplicateArticles", crawlResult.getDuplicateArticles());
                    result.put("failedArticles", crawlResult.getFailedArticles());
                    result.put("status", crawlResult.getStatus());
                    result.put("triggeredAt", Instant.now());
                    result.put("durationMs", crawlResult.getDurationMs());

                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "RSS source not found: " + sourceId);
                    return ResponseEntity.notFound().build();
                });
    }
}
