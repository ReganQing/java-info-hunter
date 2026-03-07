package com.ron.javainfohunter.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.response.CrawlTriggerResponse;
import com.ron.javainfohunter.api.dto.response.SystemStatusResponse;
import com.ron.javainfohunter.api.service.AgentService;
import com.ron.javainfohunter.api.service.NewsService;
import com.ron.javainfohunter.api.service.RssSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminController
 */
@WebMvcTest(AdminController.class)
@DisplayName("Admin Controller Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RssSourceService rssSourceService;

    @MockBean
    private NewsService newsService;

    @MockBean
    private AgentService agentService;

    private SystemStatusResponse testStatusResponse;
    private CrawlTriggerResponse testCrawlResponse;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        testStatusResponse = SystemStatusResponse.builder()
                .status("HEALTHY")
                .timestamp(now)
                .totalRssSources(50L)
                .activeRssSources(45L)
                .totalNews(10000L)
                .pendingProcessing(100L)
                .uptimeSeconds(86400L)
                .version("0.0.1-SNAPSHOT")
                .build();

        testCrawlResponse = CrawlTriggerResponse.builder()
                .triggered(true)
                .message("Crawl triggered successfully")
                .sourcesTriggered(45)
                .triggeredAt(now)
                .taskIds("task-123,task-456")
                .estimatedArticles(500)
                .build();
    }

    @Test
    @DisplayName("Get system status - success")
    void testGetSystemStatus_Success() throws Exception {
        when(rssSourceService.getSources(eq(null), eq(null), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 1), 50L)
        );
        when(rssSourceService.getSources(eq(null), eq(true), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 1), 45L)
        );
        when(newsService.getNews(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 1), 10000L)
        );
        when(agentService.getExecutionStats()).thenReturn(
                com.ron.javainfohunter.api.dto.response.AgentStatsResponse.builder()
                        .runningExecutions(5L)
                        .build()
        );

        mockMvc.perform(get("/api/v1/admin/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("HEALTHY"))
                .andExpect(jsonPath("$.data.totalRssSources").value(50))
                .andExpect(jsonPath("$.data.activeRssSources").value(45))
                .andExpect(jsonPath("$.data.totalNews").value(10000));
    }

    @Test
    @DisplayName("Trigger full crawl - success")
    void testTriggerFullCrawl_Success() throws Exception {
        Map<String, Object> crawlResult = new HashMap<>();
        crawlResult.put("triggered", true);
        crawlResult.put("message", "Crawl triggered successfully");
        crawlResult.put("sourcesTriggered", 45);
        crawlResult.put("taskId", "task-123");

        when(rssSourceService.triggerCrawl(any())).thenReturn(crawlResult);
        when(rssSourceService.getSources(eq(null), eq(true), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of())
        );

        mockMvc.perform(post("/api/v1/admin/crawl/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.triggered").value(true));
    }

    @Test
    @DisplayName("Trigger full crawl - with pagination loop")
    void testTriggerFullCrawl_WithPagination() throws Exception {
        // Create mock sources
        com.ron.javainfohunter.api.dto.response.RssSourceResponse source1 =
            com.ron.javainfohunter.api.dto.response.RssSourceResponse.builder()
                .id(1L)
                .name("Source 1")
                .url("https://example1.com")
                .isActive(true)
                .build();

        com.ron.javainfohunter.api.dto.response.RssSourceResponse source2 =
            com.ron.javainfohunter.api.dto.response.RssSourceResponse.builder()
                .id(2L)
                .name("Source 2")
                .url("https://example2.com")
                .isActive(true)
                .build();

        Map<String, Object> crawlResult = new HashMap<>();
        crawlResult.put("triggered", true);
        crawlResult.put("message", "Crawl triggered successfully");
        crawlResult.put("taskId", "task-123");

        when(rssSourceService.triggerCrawl(any())).thenReturn(crawlResult);

        // First page has 2 sources, isLast = false
        when(rssSourceService.getSources(eq(null), eq(true), any()))
            .thenReturn(
                new org.springframework.data.domain.PageImpl<>(
                    java.util.List.of(source1, source2),
                    org.springframework.data.domain.PageRequest.of(0, 100),
                    2L
                )
            );

        mockMvc.perform(post("/api/v1/admin/crawl/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.triggered").value(true))
                .andExpect(jsonPath("$.data.sourcesTriggered").value(2));
    }

    @Test
    @DisplayName("Trigger full crawl - empty sources")
    void testTriggerFullCrawl_EmptySources() throws Exception {
        when(rssSourceService.getSources(eq(null), eq(true), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of())
        );

        mockMvc.perform(post("/api/v1/admin/crawl/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.triggered").value(true))
                .andExpect(jsonPath("$.data.sourcesTriggered").value(0));
    }

    @Test
    @DisplayName("Trigger source crawl - success")
    void testTriggerSourceCrawl_Success() throws Exception {
        Map<String, Object> crawlResult = new HashMap<>();
        crawlResult.put("sourceId", 1L);
        crawlResult.put("status", "triggered");

        when(rssSourceService.triggerCrawl(1L)).thenReturn(crawlResult);

        mockMvc.perform(post("/api/v1/admin/crawl/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.triggered").value(true))
                .andExpect(jsonPath("$.data.sourcesTriggered").value(1));
    }
}
