package com.ron.javainfohunter.crawler.metrics;

import com.ron.javainfohunter.crawler.handler.ErrorType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe metrics collector for crawler operations.
 *
 * <p>This component collects and aggregates performance metrics for crawler operations,
 * including:</p>
 * <ul>
 *   <li>Total crawl counts (success/failure)</li>
 *   <li>Article processing counts (new/duplicate/failed)</li>
 *   <li>Timing statistics (average, min, max, percentiles)</li>
 *   <li>Error counts by type</li>
 *   <li>Source-specific statistics</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses ConcurrentHashMap for source-specific metrics</li>
 *   <li>Uses AtomicLong and LongAdder for counters</li>
 *   <li>Lock-free reads for high performance</li>
 * </ul>
 *
 * @see CrawlSourceMetrics
 * @see CrawlMetricsSummary
 */
@Slf4j
@Component
public class CrawlMetricsCollector {

    // Global counters
    private final AtomicLong totalCrawls = new AtomicLong(0);
    private final AtomicLong successfulCrawls = new AtomicLong(0);
    private final AtomicLong failedCrawls = new AtomicLong(0);

    // Article counters
    private final LongAdder totalArticlesCrawled = new LongAdder();
    private final LongAdder newArticles = new LongAdder();
    private final LongAdder duplicateArticles = new LongAdder();
    private final LongAdder failedArticles = new LongAdder();

    // Timing statistics (in milliseconds)
    private final LongAdder totalCrawlDuration = new LongAdder();
    private final AtomicLong minCrawlDuration = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxCrawlDuration = new AtomicLong(0);

    // Error counts by type
    private final ConcurrentHashMap<ErrorType, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    // Source-specific metrics
    private final ConcurrentHashMap<String, CrawlSourceMetrics> sourceMetrics = new ConcurrentHashMap<>();

    // Start time
    private final Instant startTime = Instant.now();

    public CrawlMetricsCollector() {
        // Initialize error counters
        for (ErrorType errorType : ErrorType.values()) {
            errorCounts.put(errorType, new AtomicLong(0));
        }
    }

    /**
     * Record the start of a crawl operation.
     *
     * <p>This should be called at the beginning of each crawl operation
     * to track timing information.</p>
     *
     * @param sourceUrl the RSS source URL being crawled
     * @return crawl start time (use with recordCrawlSuccess/recordCrawlFailure)
     */
    public long recordCrawlStart(String sourceUrl) {
        totalCrawls.incrementAndGet();
        return System.currentTimeMillis();
    }

    /**
     * Record a successful crawl operation.
     *
     * @param sourceUrl the RSS source URL
     * @param itemCount number of items crawled
     * @param durationMs crawl duration in milliseconds
     */
    public void recordCrawlSuccess(String sourceUrl, int itemCount, long durationMs) {
        successfulCrawls.incrementAndGet();
        totalArticlesCrawled.add(itemCount);

        // Update timing statistics
        totalCrawlDuration.add(durationMs);
        updateMinMax(durationMs);

        // Update source-specific metrics
        getSourceMetrics(sourceUrl).recordSuccess(itemCount, durationMs);
    }

    /**
     * Record a failed crawl operation.
     *
     * @param sourceUrl the RSS source URL
     * @param errorType the type of error that occurred
     * @param durationMs crawl duration in milliseconds
     */
    public void recordCrawlFailure(String sourceUrl, ErrorType errorType, long durationMs) {
        failedCrawls.incrementAndGet();

        // Update error count
        if (errorType != null) {
            AtomicLong counter = errorCounts.get(errorType);
            if (counter != null) {
                counter.incrementAndGet();
            }
        }

        // Update timing statistics (failed crawls still have duration)
        totalCrawlDuration.add(durationMs);
        updateMinMax(durationMs);

        // Update source-specific metrics
        getSourceMetrics(sourceUrl).recordFailure(errorType, durationMs);
    }

    /**
     * Record article processing results.
     *
     * @param newCount number of new articles
     * @param duplicateCount number of duplicate articles
     * @param failedCount number of failed articles
     */
    public void recordArticleStats(int newCount, int duplicateCount, int failedCount) {
        newArticles.add(newCount);
        duplicateArticles.add(duplicateCount);
        failedArticles.add(failedCount);
    }

    /**
     * Get a summary of all metrics.
     *
     * @return metrics summary
     */
    public CrawlMetricsSummary getSummary() {
        long total = totalCrawls.get();
        long success = successfulCrawls.get();
        long failure = failedCrawls.get();
        long totalDuration = totalCrawlDuration.sum();

        return CrawlMetricsSummary.builder()
            .startTime(startTime)
            .totalCrawls(total)
            .successfulCrawls(success)
            .failedCrawls(failure)
            .successRate(total > 0 ? (double) success / total : 0.0)
            .totalArticlesCrawled(totalArticlesCrawled.sum())
            .newArticles(newArticles.sum())
            .duplicateArticles(duplicateArticles.sum())
            .failedArticles(failedArticles.sum())
            .averageCrawlDuration(total > 0 ? totalDuration / total : 0)
            .minCrawlDuration(minCrawlDuration.get())
            .maxCrawlDuration(maxCrawlDuration.get())
            .errorCounts(getErrorCounts())
            .sourceCount(sourceMetrics.size())
            .build();
    }

    /**
     * Get metrics for a specific source.
     *
     * @param sourceUrl the RSS source URL
     * @return source-specific metrics
     */
    public CrawlSourceMetrics getSourceSummary(String sourceUrl) {
        CrawlSourceMetrics metrics = sourceMetrics.get(sourceUrl);
        return metrics != null ? metrics : new CrawlSourceMetrics(sourceUrl);
    }

    /**
     * Get error counts by type.
     *
     * @return map of error type to count
     */
    public java.util.Map<ErrorType, Long> getErrorCounts() {
        java.util.Map<ErrorType, Long> counts = new java.util.HashMap<>();
        for (java.util.Map.Entry<ErrorType, AtomicLong> entry : errorCounts.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().get());
        }
        return counts;
    }

    /**
     * Reset all metrics.
     *
     * <p><b>Warning:</b> This will clear all collected metrics.
     * Use only for testing or manual reset scenarios.</p>
     */
    public void reset() {
        totalCrawls.set(0);
        successfulCrawls.set(0);
        failedCrawls.set(0);

        totalArticlesCrawled.reset();
        newArticles.reset();
        duplicateArticles.reset();
        failedArticles.reset();

        totalCrawlDuration.reset();
        minCrawlDuration.set(Long.MAX_VALUE);
        maxCrawlDuration.set(0);

        for (AtomicLong counter : errorCounts.values()) {
            counter.set(0);
        }

        sourceMetrics.clear();
        log.info("Metrics reset");
    }

    /**
     * Get or create source-specific metrics.
     *
     * @param sourceUrl the RSS source URL
     * @return source metrics
     */
    private CrawlSourceMetrics getSourceMetrics(String sourceUrl) {
        return sourceMetrics.computeIfAbsent(sourceUrl, CrawlSourceMetrics::new);
    }

    /**
     * Update min/max duration statistics.
     *
     * @param duration the duration to update with
     */
    private void updateMinMax(long duration) {
        // Update minimum
        long currentMin = minCrawlDuration.get();
        while (duration < currentMin && !minCrawlDuration.compareAndSet(currentMin, duration)) {
            currentMin = minCrawlDuration.get();
        }

        // Update maximum
        long currentMax = maxCrawlDuration.get();
        while (duration > currentMax && !maxCrawlDuration.compareAndSet(currentMax, duration)) {
            currentMax = maxCrawlDuration.get();
        }
    }

    /**
     * Source-specific metrics.
     */
    @Data
    public static class CrawlSourceMetrics {
        private final String sourceUrl;
        private final AtomicLong totalCrawls = new AtomicLong(0);
        private final AtomicLong successfulCrawls = new AtomicLong(0);
        private final AtomicLong failedCrawls = new AtomicLong(0);
        private final LongAdder totalArticles = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxDuration = new AtomicLong(0);
        private final ConcurrentHashMap<ErrorType, AtomicLong> errorCounts = new ConcurrentHashMap<>();

        public CrawlSourceMetrics(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            // Initialize error counters
            for (ErrorType errorType : ErrorType.values()) {
                errorCounts.put(errorType, new AtomicLong(0));
            }
        }

        public void recordSuccess(int itemCount, long durationMs) {
            totalCrawls.incrementAndGet();
            successfulCrawls.incrementAndGet();
            totalArticles.add(itemCount);
            totalDuration.add(durationMs);
            updateMinMax(durationMs);
        }

        public void recordFailure(ErrorType errorType, long durationMs) {
            totalCrawls.incrementAndGet();
            failedCrawls.incrementAndGet();
            totalDuration.add(durationMs);
            updateMinMax(durationMs);

            if (errorType != null) {
                AtomicLong counter = errorCounts.get(errorType);
                if (counter != null) {
                    counter.incrementAndGet();
                }
            }
        }

        public long getTotalCrawls() {
            return totalCrawls.get();
        }

        public long getSuccessfulCrawls() {
            return successfulCrawls.get();
        }

        public long getFailedCrawls() {
            return failedCrawls.get();
        }

        public double getSuccessRate() {
            long total = totalCrawls.get();
            return total > 0 ? (double) successfulCrawls.get() / total : 0.0;
        }

        public long getTotalArticles() {
            return totalArticles.sum();
        }

        public long getAverageDuration() {
            long total = totalCrawls.get();
            return total > 0 ? totalDuration.sum() / total : 0;
        }

        public long getMinDuration() {
            long min = minDuration.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getMaxDuration() {
            return maxDuration.get();
        }

        public java.util.Map<ErrorType, Long> getErrorCounts() {
            java.util.Map<ErrorType, Long> counts = new java.util.HashMap<>();
            for (java.util.Map.Entry<ErrorType, AtomicLong> entry : errorCounts.entrySet()) {
                counts.put(entry.getKey(), entry.getValue().get());
            }
            return counts;
        }

        private void updateMinMax(long duration) {
            long currentMin = minDuration.get();
            while (duration < currentMin && !minDuration.compareAndSet(currentMin, duration)) {
                currentMin = minDuration.get();
            }

            long currentMax = maxDuration.get();
            while (duration > currentMax && !maxDuration.compareAndSet(currentMax, duration)) {
                currentMax = maxDuration.get();
            }
        }
    }

    /**
     * Metrics summary snapshot.
     */
    @Data
    @lombok.Builder
    public static class CrawlMetricsSummary {
        private Instant startTime;
        private long totalCrawls;
        private long successfulCrawls;
        private long failedCrawls;
        private double successRate;
        private long totalArticlesCrawled;
        private long newArticles;
        private long duplicateArticles;
        private long failedArticles;
        private long averageCrawlDuration;
        private long minCrawlDuration;
        private long maxCrawlDuration;
        private java.util.Map<ErrorType, Long> errorCounts;
        private int sourceCount;

        /**
         * Get uptime in human-readable format.
         *
         * @return uptime string
         */
        public String getUptime() {
            long uptimeSeconds = java.time.Duration.between(startTime, Instant.now()).getSeconds();
            long hours = uptimeSeconds / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;
            return String.format("%d hours %d minutes", hours, minutes);
        }
    }
}
