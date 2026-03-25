package com.ron.javainfohunter.crawler.service;

import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CrawlCoordinator.
 *
 * <p>These tests verify the coordinator's ability to orchestrate
 * concurrent crawling operations using virtual threads.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class CrawlCoordinatorTest {

    @Mock
    private RssFeedCrawler rssFeedCrawler;

    @Mock
    private RssSourceService rssSourceService;

    @Mock
    private RssSourceRepository rssSourceRepository;

    private CrawlCoordinator crawlCoordinator;

    @BeforeEach
    void setUp() {
        crawlCoordinator = new CrawlCoordinator(rssFeedCrawler, rssSourceService);
    }

    @Test
    void testCrawlSources_EmptyList() {
        // Test crawling an empty list of sources
        List<RawContentMessage> result = crawlCoordinator.crawlSources(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify no crawler calls were made
        verifyNoInteractions(rssFeedCrawler);
        verifyNoInteractions(rssSourceService);
    }

    @Test
    void testCrawlSources_NullList() {
        // Test crawling a null list of sources
        List<RawContentMessage> result = crawlCoordinator.crawlSources(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify no crawler calls were made
        verifyNoInteractions(rssFeedCrawler);
        verifyNoInteractions(rssSourceService);
    }

    @Test
    void testCrawlSources_SuccessfulCrawl() {
        // Create test RSS sources
        RssSource source1 = createTestRssSource(1L, "Source 1", "http://example.com/feed1");
        RssSource source2 = createTestRssSource(2L, "Source 2", "http://example.com/feed2");

        // Create mock crawl results
        CrawlResult result1 = createMockCrawlResult(1L, "Source 1", "http://example.com/feed1", 5);
        CrawlResult result2 = createMockCrawlResult(2L, "Source 2", "http://example.com/feed2", 3);

        // Mock the crawler behavior
        when(rssFeedCrawler.crawlFeed(eq(source1.getUrl()), eq(source1.getId())))
                .thenReturn(result1);
        when(rssFeedCrawler.crawlFeed(eq(source2.getUrl()), eq(source2.getId())))
                .thenReturn(result2);

        // Mock the service behavior
        doNothing().when(rssSourceService).updateLastCrawled(anyLong(), anyInt());

        // Execute crawl
        List<RawContentMessage> results = crawlCoordinator.crawlSources(Arrays.asList(source1, source2));

        // Verify results
        assertNotNull(results);
        assertEquals(8, results.size()); // 5 + 3 articles

        // Verify crawler was called for each source
        verify(rssFeedCrawler, times(1)).crawlFeed(source1.getUrl(), source1.getId());
        verify(rssFeedCrawler, times(1)).crawlFeed(source2.getUrl(), source2.getId());

        // Verify service updates were called
        verify(rssSourceService, times(1)).updateLastCrawled(1L, 5);
        verify(rssSourceService, times(1)).updateLastCrawled(2L, 3);
    }

    @Test
    void testCrawlSingleSourceSync_Success() {
        // Create test RSS source
        RssSource source = createTestRssSource(1L, "Test Source", "http://example.com/feed");

        // Create mock crawl result
        CrawlResult mockResult = createMockCrawlResult(
                1L,
                "Test Source",
                "http://example.com/feed",
                10
        );

        // Mock the crawler behavior
        when(rssFeedCrawler.crawlFeed(eq(source.getUrl()), eq(source.getId())))
                .thenReturn(mockResult);

        // Mock the service behavior
        doNothing().when(rssSourceService).updateLastCrawled(anyLong(), anyInt());

        // Execute crawl
        CrawlResult result = crawlCoordinator.crawlSingleSourceSync(source);

        // Verify result
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1L, result.getRssSourceId());
        assertEquals("Test Source", result.getRssSourceName());
        assertEquals(10, result.getTotalItems());

        // Verify crawler was called
        verify(rssFeedCrawler, times(1)).crawlFeed(source.getUrl(), source.getId());

        // Verify service update was called
        verify(rssSourceService, times(1)).updateLastCrawled(1L, 10);
    }

    @Test
    void testCrawlSingleSourceSync_Failure() {
        // Create test RSS source
        RssSource source = createTestRssSource(1L, "Test Source", "http://example.com/feed");

        // Create mock failure result
        CrawlResult mockResult = CrawlResult.failure(
                1L,
                "http://example.com/feed",
                "Connection timeout"
        );

        // Mock the crawler behavior
        when(rssFeedCrawler.crawlFeed(eq(source.getUrl()), eq(source.getId())))
                .thenReturn(mockResult);

        // Mock the service behavior
        doNothing().when(rssSourceService).updateFailedCrawl(anyLong());

        // Execute crawl
        CrawlResult result = crawlCoordinator.crawlSingleSourceSync(source);

        // Verify result
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Connection timeout", result.getErrorMessage());

        // Verify crawler was called
        verify(rssFeedCrawler, times(1)).crawlFeed(source.getUrl(), source.getId());

        // Verify service update was called
        verify(rssSourceService, times(1)).updateFailedCrawl(1L);
    }

    @Test
    void testCrawlSources_ErrorIsolation() {
        // Create test RSS sources
        RssSource source1 = createTestRssSource(1L, "Source 1", "http://example.com/feed1");
        RssSource source2 = createTestRssSource(2L, "Source 2", "http://example.com/feed2");

        // Create mock results - one success, one failure
        CrawlResult result1 = createMockCrawlResult(1L, "Source 1", "http://example.com/feed1", 5);
        CrawlResult result2 = CrawlResult.failure(2L, "http://example.com/feed2", "Parse error");

        // Mock the crawler behavior
        when(rssFeedCrawler.crawlFeed(eq(source1.getUrl()), eq(source1.getId())))
                .thenReturn(result1);
        when(rssFeedCrawler.crawlFeed(eq(source2.getUrl()), eq(source2.getId())))
                .thenReturn(result2);

        // Mock the service behavior
        doNothing().when(rssSourceService).updateLastCrawled(anyLong(), anyInt());
        doNothing().when(rssSourceService).updateFailedCrawl(anyLong());

        // Execute crawl
        List<RawContentMessage> results = crawlCoordinator.crawlSources(Arrays.asList(source1, source2));

        // Verify results - should only contain messages from successful source
        assertNotNull(results);
        assertEquals(5, results.size());

        // Verify both crawls were attempted
        verify(rssFeedCrawler, times(1)).crawlFeed(source1.getUrl(), source1.getId());
        verify(rssFeedCrawler, times(1)).crawlFeed(source2.getUrl(), source2.getId());

        // Verify appropriate service updates
        verify(rssSourceService, times(1)).updateLastCrawled(1L, 5);
        verify(rssSourceService, times(1)).updateFailedCrawl(2L);
    }

    @Test
    void testGetStatistics() {
        // Mock the service to return a specific count
        when(rssSourceService.getActiveSourcesCount()).thenReturn(42L);

        // Get statistics
        String stats = crawlCoordinator.getStatistics();

        // Verify
        assertNotNull(stats);
        assertTrue(stats.contains("42"));
        assertTrue(stats.contains("active RSS sources"));

        verify(rssSourceService, times(1)).getActiveSourcesCount();
    }

    /**
     * Helper method to create a test RssSource.
     */
    private RssSource createTestRssSource(Long id, String name, String url) {
        return RssSource.builder()
                .id(id)
                .name(name)
                .url(url)
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Helper method to create a mock CrawlResult.
     */
    private CrawlResult createMockCrawlResult(Long rssSourceId, String rssSourceName,
                                               String feedUrl, int itemCount) {
        List<RawContentMessage> messages = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            messages.add(RawContentMessage.builder()
                    .title("Article " + i)
                    .link(feedUrl + "/article/" + i)
                    .rssSourceId(rssSourceId)
                    .rssSourceName(rssSourceName)
                    .rssSourceUrl(feedUrl)
                    .build());
        }

        return CrawlResult.builder()
                .success(true)
                .rssSourceId(rssSourceId)
                .rssSourceName(rssSourceName)
                .feedUrl(feedUrl)
                .totalItems(itemCount)
                .newItems(itemCount)
                .rawContentMessages(messages)
                .build();
    }
}
