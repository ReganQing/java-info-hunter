package com.ron.javainfohunter.crawler.metrics;

import com.ron.javainfohunter.crawler.handler.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CrawlMetricsCollector.
 */
class CrawlMetricsCollectorTest {

    private CrawlMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        metricsCollector = new CrawlMetricsCollector();
    }

    @Test
    void testRecordCrawlStart() {
        String sourceUrl = "https://example.com/rss";
        long startTime = metricsCollector.recordCrawlStart(sourceUrl);

        assertTrue(startTime > 0);
        assertEquals(1, metricsCollector.getSummary().getTotalCrawls());
    }

    @Test
    void testRecordCrawlSuccess() {
        String sourceUrl = "https://example.com/rss";
        int itemCount = 10;
        long durationMs = 1000;

        metricsCollector.recordCrawlSuccess(sourceUrl, itemCount, durationMs);

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(1, summary.getSuccessfulCrawls());
        assertEquals(10, summary.getTotalArticlesCrawled());
        assertEquals(1000, summary.getAverageCrawlDuration());
        assertEquals(1000, summary.getMinCrawlDuration());
        assertEquals(1000, summary.getMaxCrawlDuration());
    }

    @Test
    void testRecordCrawlFailure() {
        String sourceUrl = "https://example.com/rss";
        ErrorType errorType = ErrorType.CONNECTION_ERROR;
        long durationMs = 500;

        metricsCollector.recordCrawlFailure(sourceUrl, errorType, durationMs);

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(1, summary.getFailedCrawls());
        assertEquals(0.0, summary.getSuccessRate());

        // Check error counts
        assertTrue(summary.getErrorCounts().containsKey(errorType));
        assertEquals(1L, summary.getErrorCounts().get(errorType));
    }

    @Test
    void testRecordArticleStats() {
        metricsCollector.recordArticleStats(5, 3, 1);

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(5, summary.getNewArticles());
        assertEquals(3, summary.getDuplicateArticles());
        assertEquals(1, summary.getFailedArticles());
    }

    @Test
    void testSourceSpecificMetrics() {
        String sourceUrl1 = "https://example1.com/rss";
        String sourceUrl2 = "https://example2.com/rss";

        metricsCollector.recordCrawlSuccess(sourceUrl1, 10, 1000);
        metricsCollector.recordCrawlSuccess(sourceUrl1, 15, 1500);
        metricsCollector.recordCrawlFailure(sourceUrl2, ErrorType.CONNECTION_ERROR, 500);

        CrawlMetricsCollector.CrawlSourceMetrics source1Metrics =
            metricsCollector.getSourceSummary(sourceUrl1);
        CrawlMetricsCollector.CrawlSourceMetrics source2Metrics =
            metricsCollector.getSourceSummary(sourceUrl2);

        // Source 1 metrics
        assertEquals(2, source1Metrics.getTotalCrawls());
        assertEquals(2, source1Metrics.getSuccessfulCrawls());
        assertEquals(0, source1Metrics.getFailedCrawls());
        assertEquals(25, source1Metrics.getTotalArticles());
        assertEquals(1.0, source1Metrics.getSuccessRate());

        // Source 2 metrics
        assertEquals(1, source2Metrics.getTotalCrawls());
        assertEquals(0, source2Metrics.getSuccessfulCrawls());
        assertEquals(1, source2Metrics.getFailedCrawls());
    }

    @Test
    void testTimingStatistics() {
        String sourceUrl = "https://example.com/rss";

        metricsCollector.recordCrawlSuccess(sourceUrl, 10, 500);
        metricsCollector.recordCrawlSuccess(sourceUrl, 10, 1000);
        metricsCollector.recordCrawlSuccess(sourceUrl, 10, 1500);

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(500, summary.getMinCrawlDuration());
        assertEquals(1500, summary.getMaxCrawlDuration());
        assertEquals(1000, summary.getAverageCrawlDuration());
    }

    @Test
    void testSuccessRateCalculation() {
        String sourceUrl = "https://example.com/rss";

        metricsCollector.recordCrawlSuccess(sourceUrl, 10, 1000);
        metricsCollector.recordCrawlSuccess(sourceUrl, 10, 1000);
        metricsCollector.recordCrawlFailure(sourceUrl, ErrorType.CONNECTION_ERROR, 500);

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(3, summary.getTotalCrawls());
        assertEquals(2, summary.getSuccessfulCrawls());
        assertEquals(1, summary.getFailedCrawls());
        assertEquals(2.0 / 3.0, summary.getSuccessRate(), 0.001);
    }

    @Test
    void testReset() {
        String sourceUrl = "https://example.com/rss";

        metricsCollector.recordCrawlSuccess(sourceUrl, 10, 1000);
        metricsCollector.recordArticleStats(5, 3, 1);
        metricsCollector.recordCrawlFailure(sourceUrl, ErrorType.CONNECTION_ERROR, 500);

        metricsCollector.reset();

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(0, summary.getTotalCrawls());
        assertEquals(0, summary.getSuccessfulCrawls());
        assertEquals(0, summary.getFailedCrawls());
        assertEquals(0, summary.getTotalArticlesCrawled());
        assertEquals(0, summary.getSourceCount());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    String sourceUrl = "https://example" + threadId + ".com/rss";
                    for (int j = 0; j < operationsPerThread; j++) {
                        metricsCollector.recordCrawlSuccess(sourceUrl, 1, 100);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(threadCount * operationsPerThread, summary.getSuccessfulCrawls());
        assertEquals(threadCount * operationsPerThread, summary.getTotalArticlesCrawled());
        assertEquals(threadCount, summary.getSourceCount());
    }

    @Test
    void testConcurrentErrorCounting() throws InterruptedException {
        int threadCount = 5;
        int errorsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < errorsPerThread; j++) {
                        metricsCollector.recordCrawlFailure(
                            "https://example.com/rss",
                            ErrorType.CONNECTION_ERROR,
                            100
                        );
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        assertEquals(threadCount * errorsPerThread, summary.getFailedCrawls());
        assertEquals((long) threadCount * errorsPerThread,
            summary.getErrorCounts().get(ErrorType.CONNECTION_ERROR));
    }

    @Test
    void testGetSummaryForNonExistentSource() {
        String sourceUrl = "https://nonexistent.com/rss";
        CrawlMetricsCollector.CrawlSourceMetrics metrics =
            metricsCollector.getSourceSummary(sourceUrl);

        assertNotNull(metrics);
        assertEquals(sourceUrl, metrics.getSourceUrl());
        assertEquals(0, metrics.getTotalCrawls());
    }

    @Test
    void testErrorCountsByType() {
        metricsCollector.recordCrawlFailure("url1", ErrorType.CONNECTION_ERROR, 100);
        metricsCollector.recordCrawlFailure("url2", ErrorType.PARSE_ERROR, 200);
        metricsCollector.recordCrawlFailure("url3", ErrorType.CONNECTION_ERROR, 300);

        var errorCounts = metricsCollector.getErrorCounts();

        assertEquals(2L, errorCounts.get(ErrorType.CONNECTION_ERROR));
        assertEquals(1L, errorCounts.get(ErrorType.PARSE_ERROR));
        assertEquals(0L, errorCounts.get(ErrorType.DATABASE_ERROR));
    }

    @Test
    void testUptimeFormatting() {
        CrawlMetricsCollector.CrawlMetricsSummary summary = metricsCollector.getSummary();

        String uptime = summary.getUptime();
        assertNotNull(uptime);
        assertTrue(uptime.contains("hours") || uptime.contains("minutes") || uptime.contains("seconds"));
    }

    @Test
    void testMultipleErrorTypesInSourceMetrics() {
        String sourceUrl = "https://example.com/rss";

        metricsCollector.recordCrawlFailure(sourceUrl, ErrorType.CONNECTION_ERROR, 100);
        metricsCollector.recordCrawlFailure(sourceUrl, ErrorType.PARSE_ERROR, 200);
        metricsCollector.recordCrawlFailure(sourceUrl, ErrorType.DATABASE_ERROR, 300);

        CrawlMetricsCollector.CrawlSourceMetrics sourceMetrics =
            metricsCollector.getSourceSummary(sourceUrl);

        assertEquals(3, sourceMetrics.getTotalCrawls());
        assertEquals(3, sourceMetrics.getFailedCrawls());

        var errorCounts = sourceMetrics.getErrorCounts();
        assertEquals(1L, errorCounts.get(ErrorType.CONNECTION_ERROR));
        assertEquals(1L, errorCounts.get(ErrorType.PARSE_ERROR));
        assertEquals(1L, errorCounts.get(ErrorType.DATABASE_ERROR));
    }
}
