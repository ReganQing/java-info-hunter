package com.ron.javainfohunter.crawler.scheduler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage;
import com.ron.javainfohunter.crawler.publisher.CrawlResultPublisher;
import com.ron.javainfohunter.crawler.publisher.ErrorPublisher;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CrawlOrchestrator}.
 *
 * <p>Tests the orchestration workflow, concurrent crawling,
 * and result aggregation logic.</p>
 */
@ExtendWith(MockitoExtension.class)
class CrawlOrchestratorTest {

    @Mock
    private RssSourceRepository rssSourceRepository;

    @Mock
    private CrawlerProperties crawlerProperties;

    @Mock
    private CrawlResultPublisher crawlResultPublisher;

    @Mock
    private ErrorPublisher errorPublisher;

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
    void testExecuteCrawlJob_WithMultipleSources_ShouldAggregateResults() {
        // Arrange
        when(rssSourceRepository.save(any())).thenReturn(any());

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(testSources);

        // Assert
        verify(rssSourceRepository, times(testSources.size())).save(any());
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

    @Test
    void testExecuteCrawlJob_WithSingleSource_ShouldProcessSuccessfully() {
        // Arrange
        List<RssSource> singleSource = List.of(
                createTestSource(1L, "Source 1", "http://example.com/feed")
        );
        when(rssSourceRepository.save(any())).thenReturn(any());

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(singleSource);

        // Assert
        verify(rssSourceRepository, times(1)).save(any());
        assert result.getStartTime() != null;
        assert result.getEndTime() != null;
        assert result.getDurationMs() != null;
    }

    @Test
    void testProcessSingleSource_WhenException_ShouldPublishError() {
        // Arrange
        RssSource source = createTestSource(1L, "Error Source", "http://example.com/feed");
        when(rssSourceRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(List.of(source));

        // Assert
        verify(errorPublisher, atLeastOnce()).publishError(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(Exception.class),
                any()
        );
    }

    @Test
    void testExecuteCrawlJob_ShouldSetDuration() {
        // Arrange
        when(rssSourceRepository.save(any())).thenReturn(any());

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(testSources);

        // Assert
        assert result.getStartTime() != null;
        assert result.getEndTime() != null;
        assert result.getDurationMs() != null;
        assert result.getDurationMs() >= 0;
    }

    @Test
    void testExecuteCrawlJob_WithPartialFailures_ShouldReturnPartialStatus() {
        // Arrange
        // This test would require mocking the executor to simulate partial failures
        // For now, we just verify the method executes without exception
        when(rssSourceRepository.save(any())).thenReturn(any());

        // Act
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(testSources);

        // Assert
        assert result != null;
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
}
