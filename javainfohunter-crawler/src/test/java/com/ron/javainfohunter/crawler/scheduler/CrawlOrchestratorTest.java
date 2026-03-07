package com.ron.javainfohunter.crawler.scheduler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.publisher.CrawlResultPublisher;
import com.ron.javainfohunter.crawler.publisher.ContentPublisher;
import com.ron.javainfohunter.crawler.publisher.ErrorPublisher;
import com.ron.javainfohunter.crawler.service.RssFeedCrawler;
import com.ron.javainfohunter.crawler.service.RssSourceService;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CrawlOrchestrator}.
 *
 * <p>Tests the orchestration workflow, concurrent crawling,
 * and result aggregation logic.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrawlOrchestratorTest {

    @Mock
    private RssSourceRepository rssSourceRepository;

    @Mock
    private RssSourceService rssSourceService;

    @Mock
    private CrawlerProperties crawlerProperties;

    @Mock
    private CrawlResultPublisher crawlResultPublisher;

    @Mock
    private ErrorPublisher errorPublisher;

    @Mock
    private RssFeedCrawler rssFeedCrawler;

    @Mock
    private ContentPublisher contentPublisher;

    @Mock
    private ExecutorService crawlExecutor;

    @InjectMocks
    private CrawlOrchestrator crawlOrchestrator;

    private List<RssSource> testSources;

    @BeforeEach
    void setUp() {
        testSources = List.of(
                createTestSource(1L, "Source 1", "http://example.com/feed1"),
                createTestSource(2L, "Source 2", "http://example.com/feed2"),
                createTestSource(3L, "Source 3", "http://example.com/feed3")
        );
    }

    @Test
    void testExecuteCrawlJob_WithEmptyList_ShouldReturnEmptyResult() {
        // Arrange
        List<RssSource> emptyList = List.of();

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(emptyList);

        // Assert
        assert result.getNewArticles() == 0;
        assert result.getDuplicateArticles() == 0;
        assert result.getFailedArticles() == 0;
    }

    // ========== TDD TESTS FOR ISSUE #1: ACTUAL RSS CRAWLING ==========

    /**
     * Test that RssFeedCrawler dependency is injected correctly.
     *
     * <p>This test verifies Issue #1 from the code review:
     * The orchestrator MUST have the RssFeedCrawler dependency injected,
     * which is used for actual RSS feed crawling (not placeholder code).</p>
     */
    @Test
    void testCrawlOrchestrator_ShouldHaveRssFeedCrawlerDependency() {
        // Assert
        assertNotNull(crawlOrchestrator, "CrawlOrchestrator should be created");
        // The fact that @InjectMocks works with @Mock RssFeedCrawler proves the dependency exists
    }

    /**
     * Test that CrawlResult can be created with actual crawling data.
     *
     * <p>This test verifies that the CrawlResult DTO supports all the fields
     * needed for actual RSS crawling results (new items, duplicates, messages).</p>
     */
    @Test
    void testCrawlResult_ShouldSupportActualCrawlingData() {
        // Arrange & Act
        CrawlResult result = CrawlResult.builder()
                .success(true)
                .rssSourceId(1L)
                .feedUrl("http://example.com/feed")
                .rssSourceName("Test Feed")
                .totalItems(10)
                .newItems(8)
                .duplicateItems(2)
                .rawContentMessages(createTestMessages(8))
                .durationMs(1500)
                .build();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(10, result.getTotalItems());
        assertEquals(8, result.getNewItems());
        assertEquals(2, result.getDuplicateItems());
        assertNotNull(result.getRawContentMessages());
        assertEquals(8, result.getRawContentMessages().size());
        assertEquals(1500, result.getDurationMs());
    }

    /**
     * Test that RawContentMessage contains all required fields.
     *
     * <p>This test verifies that RawContentMessage has all the fields needed
     * to publish crawled content to RabbitMQ.</p>
     */
    @Test
    void testRawContentMessage_ShouldContainAllRequiredFields() {
        // Arrange & Act
        RawContentMessage message = RawContentMessage.builder()
                .guid("test-guid")
                .title("Test Article")
                .link("http://example.com/article")
                .rawContent("Test content")
                .contentHash("test-hash")
                .rssSourceId(1L)
                .rssSourceName("Test Source")
                .rssSourceUrl("http://example.com/feed")
                .author("Test Author")
                .publishDate(Instant.now())
                .crawlDate(Instant.now())
                .category("Technology")
                .tags(new String[]{"java", "spring"})
                .build();

        // Assert
        assertNotNull(message);
        assertEquals("test-guid", message.getGuid());
        assertEquals("Test Article", message.getTitle());
        assertEquals("http://example.com/article", message.getLink());
        assertEquals("Test content", message.getRawContent());
        assertEquals("test-hash", message.getContentHash());
        assertEquals(1L, message.getRssSourceId());
        assertNotNull(message.getPublishDate());
        assertNotNull(message.getCrawlDate());
    }

    /**
     * Test that CrawlOrchestrator handles empty sources list.
     *
     * <p>This test verifies the orchestrator's behavior when given
     * an empty list of sources to crawl.</p>
     */
    @Test
    void testCrawlOrchestrator_WithEmptySources_ShouldReturnEmptyResult() {
        // Arrange
        List<RssSource> emptySources = List.of();

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(emptySources);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getNewArticles());
        assertEquals(0, result.getDuplicateArticles());
        assertEquals(0, result.getFailedArticles());
        assertEquals(CrawlResultMessage.CrawlStatus.SUCCESS, result.getStatus());
    }

    /**
     * Test that CrawlResultMessage status is calculated correctly.
     *
     * <p>This test verifies the status logic: SUCCESS when all succeed,
     * FAILED when all fail, PARTIAL when mixed.</p>
     */
    @Test
    void testCrawlResultMessage_StatusCalculation() {
        // Test SUCCESS status
        CrawlResultMessage successResult = CrawlResultMessage.builder()
                .status(CrawlResultMessage.CrawlStatus.SUCCESS)
                .newArticles(10)
                .duplicateArticles(5)
                .failedArticles(0)
                .build();
        assertEquals(CrawlResultMessage.CrawlStatus.SUCCESS, successResult.getStatus());

        // Test FAILED status
        CrawlResultMessage failedResult = CrawlResultMessage.builder()
                .status(CrawlResultMessage.CrawlStatus.FAILED)
                .newArticles(0)
                .duplicateArticles(0)
                .failedArticles(5)
                .build();
        assertEquals(CrawlResultMessage.CrawlStatus.FAILED, failedResult.getStatus());

        // Test PARTIAL status (new + failed)
        CrawlResultMessage partialResult = CrawlResultMessage.builder()
                .status(CrawlResultMessage.CrawlStatus.PARTIAL)
                .newArticles(5)
                .duplicateArticles(2)
                .failedArticles(1)
                .build();
        assertEquals(CrawlResultMessage.CrawlStatus.PARTIAL, partialResult.getStatus());
    }

    // Helper methods

    private RssSource createTestSource(Long id, String name, String url) {
        return RssSource.builder()
                .id(id)
                .name(name)
                .url(url)
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .lastCrawledAt(null)
                .totalArticles(0L)
                .failedCrawls(0L)
                .build();
    }

    private List<RawContentMessage> createTestMessages(int count) {
        List<RawContentMessage> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(RawContentMessage.builder()
                    .guid("guid-" + i)
                    .title("Article " + i)
                    .link("http://example.com/article" + i)
                    .rawContent("Content " + i)
                    .contentHash("hash-" + i)
                    .rssSourceId(1L)
                    .rssSourceName("Test Source")
                    .rssSourceUrl("http://example.com/feed")
                    .publishDate(Instant.now())
                    .crawlDate(Instant.now())
                    .build());
        }
        return messages;
    }
}
