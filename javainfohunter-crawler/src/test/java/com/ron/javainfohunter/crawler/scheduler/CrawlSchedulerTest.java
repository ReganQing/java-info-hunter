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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CrawlScheduler}.
 *
 * <p>Tests the scheduling logic, source filtering, and error handling
 * of the RSS feed crawl scheduler.</p>
 */
@ExtendWith(MockitoExtension.class)
class CrawlSchedulerTest {

    @Mock
    private RssSourceRepository rssSourceRepository;

    @Mock
    private CrawlerProperties crawlerProperties;

    @Mock
    private CrawlOrchestrator crawlOrchestrator;

    @Mock
    private CrawlResultPublisher crawlResultPublisher;

    @Mock
    private ErrorPublisher errorPublisher;

    @InjectMocks
    private CrawlScheduler crawlScheduler;

    private CrawlerProperties.Scheduler schedulerConfig;

    @BeforeEach
    void setUp() {
        schedulerConfig = new CrawlerProperties.Scheduler();
        schedulerConfig.setEnabled(true);
        schedulerConfig.setInitialDelay(1000);
        schedulerConfig.setFixedRate(5000);

        when(crawlerProperties.getScheduler()).thenReturn(schedulerConfig);
    }

    @Test
    void testScheduleCrawling_WhenEnabled_ShouldExecute() {
        // Arrange
        RssSource source1 = createTestSource(1L, "Source 1", "http://example.com/feed1", true, null);
        RssSource source2 = createTestSource(2L, "Source 2", "http://example.com/feed2", true, Instant.now().minusSeconds(7200));

        when(rssSourceRepository.findByIsActiveTrue()).thenReturn(List.of(source1, source2));

        CrawlResultMessage result = CrawlResultMessage.builder()
                .status(CrawlResultMessage.CrawlStatus.SUCCESS)
                .newArticles(5)
                .duplicateArticles(2)
                .failedArticles(0)
                .build();
        when(crawlOrchestrator.executeCrawlJob(any())).thenReturn(result);

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(crawlOrchestrator).executeCrawlJob(any());
        verify(crawlResultPublisher).publishCrawlResult(any());
    }

    @Test
    void testScheduleCrawling_WhenDisabled_ShouldSkip() {
        // Arrange
        schedulerConfig.setEnabled(false);

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(rssSourceRepository, never()).findByIsActiveTrue();
        verify(crawlOrchestrator, never()).executeCrawlJob(any());
        verify(crawlResultPublisher, never()).publishCrawlResult(any());
    }

    @Test
    void testScheduleCrawling_WhenNoSourcesDue_ShouldSkip() {
        // Arrange
        RssSource source = createTestSource(1L, "Source 1", "http://example.com/feed", true, Instant.now());
        when(rssSourceRepository.findByIsActiveTrue()).thenReturn(List.of(source));

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(crawlOrchestrator, never()).executeCrawlJob(any());
        verify(crawlResultPublisher, never()).publishCrawlResult(any());
    }

    @Test
    void testScheduleCrawling_WhenException_ShouldPublishError() {
        // Arrange
        when(rssSourceRepository.findByIsActiveTrue()).thenThrow(new RuntimeException("Database error"));

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(errorPublisher).publishError(eq("scheduler"), any(), any());
        verify(crawlOrchestrator, never()).executeCrawlJob(any());
    }

    @Test
    void testShouldCrawl_WhenNeverCrawled_ShouldExecute() {
        // Arrange
        RssSource source = createTestSource(1L, "Source 1", "http://example.com/feed", true, null);
        when(rssSourceRepository.findByIsActiveTrue()).thenReturn(List.of(source));

        CrawlResultMessage result = CrawlResultMessage.builder()
                .status(CrawlResultMessage.CrawlStatus.SUCCESS)
                .newArticles(1)
                .build();
        when(crawlOrchestrator.executeCrawlJob(any())).thenReturn(result);

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        // The scheduler should have found this source and attempted to crawl
        verify(crawlOrchestrator, times(1)).executeCrawlJob(any());
    }

    @Test
    void testShouldCrawl_WhenIntervalElapsed_ShouldReturnTrue() {
        // Arrange
        Instant lastCrawled = Instant.now().minusSeconds(7200); // 2 hours ago
        RssSource source = createTestSource(1L, "Source 1", "http://example.com/feed", true, lastCrawled);
        source.setCrawlIntervalSeconds(3600); // 1 hour interval

        when(rssSourceRepository.findByIsActiveTrue()).thenReturn(List.of(source));

        CrawlResultMessage result = CrawlResultMessage.builder()
                .status(CrawlResultMessage.CrawlStatus.SUCCESS)
                .newArticles(1)
                .build();
        when(crawlOrchestrator.executeCrawlJob(any())).thenReturn(result);

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(crawlOrchestrator).executeCrawlJob(any());
    }

    @Test
    void testShouldCrawl_WhenIntervalNotElapsed_ShouldReturnFalse() {
        // Arrange
        Instant lastCrawled = Instant.now().minusSeconds(1800); // 30 minutes ago
        RssSource source = createTestSource(1L, "Source 1", "http://example.com/feed", true, lastCrawled);
        source.setCrawlIntervalSeconds(3600); // 1 hour interval

        when(rssSourceRepository.findByIsActiveTrue()).thenReturn(List.of(source));

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(crawlOrchestrator, never()).executeCrawlJob(any());
    }

    @Test
    void testShouldCrawl_WhenInactive_ShouldReturnFalse() {
        // Arrange
        RssSource source = createTestSource(1L, "Source 1", "http://example.com/feed", false, null);
        when(rssSourceRepository.findByIsActiveTrue()).thenReturn(List.of());

        // Act
        crawlScheduler.scheduleCrawling();

        // Assert
        verify(crawlOrchestrator, never()).executeCrawlJob(any());
    }

    // Helper methods

    private RssSource createTestSource(Long id, String name, String url, boolean isActive, Instant lastCrawled) {
        return RssSource.builder()
                .id(id)
                .name(name)
                .url(url)
                .isActive(isActive)
                .crawlIntervalSeconds(3600)
                .lastCrawledAt(lastCrawled)
                .build();
    }
}
