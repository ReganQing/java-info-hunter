package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.request.RssSourceRequest;
import com.ron.javainfohunter.api.dto.response.RssSourceResponse;
import com.ron.javainfohunter.api.exception.ResourceNotFoundException;
import com.ron.javainfohunter.api.service.impl.RssSourceServiceImpl;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RssSourceService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RSS Source Service Tests")
class RssSourceServiceTest {

    @Mock
    private RssSourceRepository rssSourceRepository;

    @InjectMocks
    private RssSourceServiceImpl rssSourceService;

    private RssSource testSource;
    private RssSourceRequest testRequest;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        testSource = RssSource.builder()
                .id(1L)
                .name("Tech Blog")
                .url("https://example.com/rss")
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
    void testCreateSource_Success() {
        when(rssSourceRepository.findByUrl(testRequest.getUrl())).thenReturn(Optional.empty());
        when(rssSourceRepository.save(any(RssSource.class))).thenReturn(testSource);

        RssSourceResponse response = rssSourceService.createSource(testRequest);

        assertNotNull(response);
        assertEquals(testSource.getId(), response.getId());
        assertEquals(testSource.getName(), response.getName());
        assertEquals(testSource.getUrl(), response.getUrl());
        assertEquals(testSource.getCategory(), response.getCategory());

        verify(rssSourceRepository, times(1)).findByUrl(testRequest.getUrl());
        verify(rssSourceRepository, times(1)).save(any(RssSource.class));
    }

    @Test
    @DisplayName("Get sources - all sources")
    void testGetSources_All() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<RssSource> sourcePage = new PageImpl<>(List.of(testSource));
        when(rssSourceRepository.findAll(pageable)).thenReturn(sourcePage);

        Page<RssSourceResponse> response = rssSourceService.getSources(null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(testSource.getId(), response.getContent().get(0).getId());

        verify(rssSourceRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Get sources - with category filter")
    void testGetSources_WithCategoryFilter() {
        String category = "Technology";
        Pageable pageable = PageRequest.of(0, 20);
        Page<RssSource> sourcePage = new PageImpl<>(List.of(testSource));
        when(rssSourceRepository.findByCategory(category, pageable)).thenReturn(sourcePage);

        Page<RssSourceResponse> response = rssSourceService.getSources(category, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(category, response.getContent().get(0).getCategory());

        verify(rssSourceRepository, times(1)).findByCategory(category, pageable);
    }

    @Test
    @DisplayName("Get sources - with active filter")
    void testGetSources_WithActiveFilter() {
        Boolean isActive = true;
        Pageable pageable = PageRequest.of(0, 20);
        Page<RssSource> sourcePage = new PageImpl<>(List.of(testSource));
        when(rssSourceRepository.findByIsActive(isActive, pageable)).thenReturn(sourcePage);

        Page<RssSourceResponse> response = rssSourceService.getSources(null, isActive, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(isActive, response.getContent().get(0).getIsActive());

        verify(rssSourceRepository, times(1)).findByIsActive(isActive, pageable);
    }

    @Test
    @DisplayName("Get sources - with both filters")
    void testGetSources_WithBothFilters() {
        String category = "Technology";
        Boolean isActive = true;
        Pageable pageable = PageRequest.of(0, 20);
        when(rssSourceRepository.findByCategoryAndIsActive(category, isActive)).thenReturn(List.of(testSource));

        Page<RssSourceResponse> response = rssSourceService.getSources(category, isActive, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());

        verify(rssSourceRepository, times(1)).findByCategoryAndIsActive(category, isActive);
    }

    @Test
    @DisplayName("Get source by ID - found")
    void testGetSourceById_Found() {
        when(rssSourceRepository.findById(1L)).thenReturn(Optional.of(testSource));

        RssSourceResponse response = rssSourceService.getSourceById(1L);

        assertNotNull(response);
        assertEquals(testSource.getId(), response.getId());
        assertEquals(testSource.getName(), response.getName());

        verify(rssSourceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get source by ID - not found")
    void testGetSourceById_NotFound() {
        when(rssSourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rssSourceService.getSourceById(999L));

        verify(rssSourceRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Update source - success")
    void testUpdateSource_Success() {
        RssSource existingSource = RssSource.builder()
                .id(1L)
                .name("Old Name")
                .url("https://old-url.com/rss")
                .build();

        when(rssSourceRepository.findById(1L)).thenReturn(Optional.of(existingSource));
        when(rssSourceRepository.findByUrl(testRequest.getUrl())).thenReturn(Optional.empty());
        when(rssSourceRepository.save(any(RssSource.class))).thenReturn(testSource);

        RssSourceResponse response = rssSourceService.updateSource(1L, testRequest);

        assertNotNull(response);
        assertEquals(testSource.getId(), response.getId());

        verify(rssSourceRepository, times(1)).findById(1L);
        verify(rssSourceRepository, times(1)).save(any(RssSource.class));
    }

    @Test
    @DisplayName("Update source - not found")
    void testUpdateSource_NotFound() {
        when(rssSourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rssSourceService.updateSource(999L, testRequest));

        verify(rssSourceRepository, times(1)).findById(999L);
        verify(rssSourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete source - success")
    void testDeleteSource_Success() {
        when(rssSourceRepository.findById(1L)).thenReturn(Optional.of(testSource));
        doNothing().when(rssSourceRepository).delete(testSource);

        assertDoesNotThrow(() -> rssSourceService.deleteSource(1L));

        verify(rssSourceRepository, times(1)).findById(1L);
        verify(rssSourceRepository, times(1)).delete(testSource);
    }

    @Test
    @DisplayName("Delete source - not found")
    void testDeleteSource_NotFound() {
        when(rssSourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rssSourceService.deleteSource(999L));

        verify(rssSourceRepository, times(1)).findById(999L);
        verify(rssSourceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Trigger crawl - success")
    void testTriggerCrawl_Success() {
        when(rssSourceRepository.findById(1L)).thenReturn(Optional.of(testSource));

        assertDoesNotThrow(() -> rssSourceService.triggerCrawl(1L));

        verify(rssSourceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Trigger crawl - source not found")
    void testTriggerCrawl_NotFound() {
        when(rssSourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rssSourceService.triggerCrawl(999L));

        verify(rssSourceRepository, times(1)).findById(999L);
    }
}
