package com.ron.javainfohunter.crawler.scheduler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage.CrawlStatus;
import com.ron.javainfohunter.crawler.publisher.CrawlResultPublisher;
import com.ron.javainfohunter.crawler.publisher.ErrorPublisher;
import com.ron.javainfohunter.crawler.publisher.ErrorPublisher;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage.ErrorType;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrator for the complete RSS feed crawling workflow.
 *
 * <p>This service coordinates the entire crawling process, including:
 * <ul>
 *   <li>Fetching RSS sources due for crawling</li>
 *   <li>Executing concurrent crawl jobs using virtual threads</li>
 *   <li>Aggregating results from multiple sources</li>
 *   <li>Publishing crawl statistics</li>
 *   <li>Handling errors at orchestration level</li>
 * </ul>
 *
 * <p><b>Virtual Thread Integration:</b></p>
 * <pre>
 * Uses Java 21 virtual threads for concurrent crawling:
 * - Each RSS source is crawled in a separate virtual thread
 * - Enables massive concurrency without platform thread overhead
 * - Ideal for I/O-bound RSS feed operations
 * </pre>
 *
 * @see CrawlScheduler
 * @see CrawlResultMessage
 */
@Slf4j
@Service
public class CrawlOrchestrator {

    private final RssSourceRepository rssSourceRepository;
    private final CrawlerProperties crawlerProperties;
    private final CrawlResultPublisher crawlResultPublisher;
    private final ErrorPublisher errorPublisher;
    private final ExecutorService crawlExecutor;

    @Autowired
    public CrawlOrchestrator(
            RssSourceRepository rssSourceRepository,
            CrawlerProperties crawlerProperties,
            CrawlResultPublisher crawlResultPublisher,
            ErrorPublisher errorPublisher,
            @Qualifier("crawlExecutor") ExecutorService crawlExecutor) {
        this.rssSourceRepository = rssSourceRepository;
        this.crawlerProperties = crawlerProperties;
        this.crawlResultPublisher = crawlResultPublisher;
        this.errorPublisher = errorPublisher;
        this.crawlExecutor = crawlExecutor;
    }

    /**
     * Execute a crawl job for multiple RSS sources.
     *
     * <p>This method orchestrates concurrent crawling of multiple RSS sources
     * using virtual threads. Each source is processed independently, and
     * failures are isolated to prevent cascading errors.</p>
     *
     * <p><b>Workflow:</b></p>
     * <ol>
     *   <li>Submit crawl tasks for all sources concurrently</li>
     *   <li>Wait for all tasks to complete (with timeout)</li>
     *   <li>Aggregate results from all sources</li>
     *   <li>Publish aggregate statistics</li>
     *   <li>Handle any orchestration-level errors</li>
     * </ol>
     *
     * @param sources List of RSS sources to crawl
     * @return Aggregate crawl result message
     */
    public CrawlResultMessage executeCrawlJob(List<RssSource> sources) {
        log.info("Starting crawl orchestration for {} sources", sources.size());

        Instant startTime = Instant.now();
        CrawlResultBuilder resultBuilder = new CrawlResultBuilder();

        // Create a list of futures for concurrent execution
        List<CompletableFuture<Void>> futures = sources.stream()
                .map(source -> CompletableFuture.runAsync(
                        () -> processSingleSource(source, resultBuilder),
                        crawlExecutor
                ))
                .toList();

        // Wait for all tasks to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.MINUTES); // 30-minute timeout
        } catch (TimeoutException e) {
            log.error("Crawl job timed out after 30 minutes");
            resultBuilder.addError("Crawl job timed out");
        } catch (InterruptedException e) {
            log.error("Crawl job interrupted", e);
            Thread.currentThread().interrupt();
            resultBuilder.addError("Crawl job interrupted: " + e.getMessage());
        } catch (ExecutionException e) {
            log.error("Error during crawl job execution", e);
            resultBuilder.addError("Execution error: " + e.getCause().getMessage());
        }

        // Build the final result
        CrawlResultMessage result = resultBuilder.build();
        result.setStartTime(startTime);
        result.setEndTime(Instant.now());
        result.setDurationMs(result.getEndTime().toEpochMilli() - result.getStartTime().toEpochMilli());

        log.info("Crawl orchestration completed: {} new articles, {} duplicates, {} failures",
                result.getNewArticles(),
                result.getDuplicateArticles(),
                result.getFailedArticles());

        return result;
    }

    /**
     * Process a single RSS source.
     *
     * <p>This method is executed in a virtual thread for each RSS source.
     * It handles the complete workflow for a single source:</p>
     * <ul>
     *   <li>Fetch and parse RSS feed</li>
     *   <li>Extract articles</li>
     *   <li>Check for duplicates</li>
     *   <li>Publish raw content messages</li>
     *   <li>Update source statistics</li>
     * </ul>
     *
     * <p><b>Error Isolation:</b></p>
     * Errors are caught and logged at the source level, preventing
     * failures from affecting other sources.
     *
     * @param source The RSS source to process
     * @param resultBuilder Builder for aggregating results
     */
    private void processSingleSource(RssSource source, CrawlResultBuilder resultBuilder) {
        log.debug("Processing source: {} ({})", source.getName(), source.getUrl());

        Instant sourceStartTime = Instant.now();
        int newArticles = 0;
        int duplicateArticles = 0;
        int failedArticles = 0;

        try {
            // TODO: Implement actual RSS feed crawling
            // This is a placeholder that simulates crawling
            // The actual implementation will be provided by Agent-2

            // Simulate crawling delay
            Thread.sleep(100);

            // Simulate finding some articles
            newArticles = (int) (Math.random() * 10);
            duplicateArticles = (int) (Math.random() * 5);

            log.debug("Source {} completed: {} new, {} duplicates",
                    source.getName(), newArticles, duplicateArticles);

            // Update source statistics
            source.updateLastCrawled();
            source.incrementArticleCount();
            rssSourceRepository.save(source);

        } catch (Exception e) {
            log.error("Failed to process source: {} ({})", source.getName(), source.getUrl(), e);
            failedArticles = 1;

            // Publish error message
            errorPublisher.publishError(
                    source.getId(),
                    source.getName(),
                    source.getUrl(),
                    e,
                    ErrorType.CONNECTION_ERROR
            );
        }

        // Add to result builder
        resultBuilder.addSourceResult(
                source.getId(),
                source.getName(),
                source.getUrl(),
                newArticles,
                duplicateArticles,
                failedArticles,
                sourceStartTime
        );
    }

    /**
     * Builder class for aggregating crawl results.
     */
    private static class CrawlResultBuilder {
        private final AtomicInteger totalNewArticles = new AtomicInteger(0);
        private final AtomicInteger totalDuplicateArticles = new AtomicInteger(0);
        private final AtomicInteger totalFailedArticles = new AtomicInteger(0);
        private final StringBuilder errorMessages = new StringBuilder();

        public void addSourceResult(Long sourceId, String sourceName, String sourceUrl,
                                   int newArticles, int duplicateArticles, int failedArticles,
                                   Instant sourceStartTime) {
            totalNewArticles.addAndGet(newArticles);
            totalDuplicateArticles.addAndGet(duplicateArticles);
            totalFailedArticles.addAndGet(failedArticles);

            log.debug("Source result: {} - {} new, {} duplicates, {} failures ({}ms)",
                    sourceName,
                    newArticles,
                    duplicateArticles,
                    failedArticles,
                    Instant.now().toEpochMilli() - sourceStartTime.toEpochMilli());
        }

        public void addError(String errorMessage) {
            if (errorMessages.length() > 0) {
                errorMessages.append("; ");
            }
            errorMessages.append(errorMessage);
        }

        public CrawlResultMessage build() {
            CrawlStatus status = CrawlStatus.SUCCESS;
            if (totalFailedArticles.get() > 0 && totalNewArticles.get() > 0) {
                status = CrawlStatus.PARTIAL;
            } else if (totalFailedArticles.get() > 0 && totalNewArticles.get() == 0) {
                status = CrawlStatus.FAILED;
            }

            return CrawlResultMessage.builder()
                    .status(status)
                    .articlesCrawled(totalNewArticles.get() + totalDuplicateArticles.get())
                    .newArticles(totalNewArticles.get())
                    .duplicateArticles(totalDuplicateArticles.get())
                    .failedArticles(totalFailedArticles.get())
                    .errorMessage(errorMessages.length() > 0 ? errorMessages.toString() : null)
                    .build();
        }
    }
}
