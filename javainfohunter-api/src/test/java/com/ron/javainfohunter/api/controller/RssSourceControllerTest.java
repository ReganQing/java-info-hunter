package com.ron.javainfohunter.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.request.RssSourceRequest;
import com.ron.javainfohunter.api.dto.response.RssSourceResponse;
import com.ron.javainfohunter.api.service.RssSourceService;
import com.ron.javainfohunter.entity.RssSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RssSourceController
 */
@WebMvcTest(RssSourceController.class)
@DisplayName("RSS Source Controller Tests")
class RssSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RssSourceService rssSourceService;

    private RssSourceResponse testResponse;
    private RssSourceRequest testRequest;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        testResponse = RssSourceResponse.builder()
                .id(1L)
                .name("Tech Blog")
                .url("https://example.com/rss")
                .description("A tech blog")
                .category("Technology")
                .tags(Arrays.asList("tech", "programming"))
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .language("en")
                .timezone("UTC")
                .lastCrawledAt(now)
                .totalArticles(100L)
                .failedCrawls(5L)
                .failureRate(5.0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        testRequest = RssSourceRequest.builder()
                .name("Tech Blog")
                .url("https://example.com/rss")
                .description("A tech blog")
                .category("Technology")
                .tags(Arrays.asList("tech", "programming"))
                .crawlIntervalSeconds(3600)
                .isActive(true)
                .language("en")
                .timezone("UTC")
                .build();
    }

    @Test
    @DisplayName("Create RSS source - success")
    void testCreateSource_Success() throws Exception {
        when(rssSourceService.createSource(any(RssSourceRequest.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/rss-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Tech Blog"))
                .andExpect(jsonPath("$.data.url").value("https://example.com/rss"));
    }

    @Test
    @DisplayName("Create RSS source - validation error (missing name)")
    void testCreateSource_ValidationError() throws Exception {
        RssSourceRequest invalidRequest = RssSourceRequest.builder()
                .url("https://example.com/rss")
                .build();

        mockMvc.perform(post("/api/v1/rss-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Create RSS source - validation error (invalid URL)")
    void testCreateSource_InvalidUrl() throws Exception {
        RssSourceRequest invalidRequest = RssSourceRequest.builder()
                .name("Test")
                .url("invalid-url")
                .build();

        mockMvc.perform(post("/api/v1/rss-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Get sources - success")
    void testGetSources_Success() throws Exception {
        when(rssSourceService.getSources(eq(null), eq(null), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(testResponse))
        );

        mockMvc.perform(get("/api/v1/rss-sources")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].name").value("Tech Blog"));
    }

    @Test
    @DisplayName("Get sources - with category filter")
    void testGetSources_WithFilters() throws Exception {
        when(rssSourceService.getSources(eq("Technology"), eq(null), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(testResponse))
        );

        mockMvc.perform(get("/api/v1/rss-sources")
                        .param("page", "0")
                        .param("size", "20")
                        .param("category", "Technology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].category").value("Technology"));
    }

    @Test
    @DisplayName("Get source by ID - success")
    void testGetSource_Success() throws Exception {
        when(rssSourceService.getSourceById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/rss-sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Tech Blog"));
    }

    @Test
    @DisplayName("Get source by ID - not found")
    void testGetSource_NotFound() throws Exception {
        when(rssSourceService.getSourceById(999L))
                .thenThrow(new com.ron.javainfohunter.api.exception.ResourceNotFoundException("RSS Source", 999L));

        mockMvc.perform(get("/api/v1/rss-sources/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("RSS Source not found with id: 999"));
    }

    @Test
    @DisplayName("Update source - success")
    void testUpdateSource_Success() throws Exception {
        when(rssSourceService.updateSource(eq(1L), any(RssSourceRequest.class))).thenReturn(testResponse);

        mockMvc.perform(put("/api/v1/rss-sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Tech Blog"));
    }

    @Test
    @DisplayName("Update source - not found")
    void testUpdateSource_NotFound() throws Exception {
        when(rssSourceService.updateSource(eq(999L), any(RssSourceRequest.class)))
                .thenThrow(new com.ron.javainfohunter.api.exception.ResourceNotFoundException("RSS Source", 999L));

        mockMvc.perform(put("/api/v1/rss-sources/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Delete source - success")
    void testDeleteSource_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/rss-sources/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Delete source - not found")
    void testDeleteSource_NotFound() throws Exception {
        org.mockito.Mockito.doThrow(new com.ron.javainfohunter.api.exception.ResourceNotFoundException("RSS Source", 999L))
                .when(rssSourceService).deleteSource(999L);

        mockMvc.perform(delete("/api/v1/rss-sources/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Trigger crawl - success")
    void testTriggerCrawl_Success() throws Exception {
        Map<String, Object> crawlResult = new HashMap<>();
        crawlResult.put("sourceId", 1L);
        crawlResult.put("sourceName", "Tech Blog");
        crawlResult.put("status", "triggered");
        crawlResult.put("message", "Crawl task triggered successfully");

        when(rssSourceService.triggerCrawl(1L)).thenReturn(crawlResult);

        mockMvc.perform(post("/api/v1/rss-sources/1/crawl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceId").value(1L))
                .andExpect(jsonPath("$.data.status").value("triggered"));
    }

    @Test
    @DisplayName("Trigger crawl - source not found")
    void testTriggerCrawl_NotFound() throws Exception {
        when(rssSourceService.triggerCrawl(999L))
                .thenThrow(new com.ron.javainfohunter.api.exception.ResourceNotFoundException("RSS Source", 999L));

        mockMvc.perform(post("/api/v1/rss-sources/999/crawl"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
