package com.ron.javainfohunter.crawler.scheduler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage.CrawlStatus;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.publisher.CrawlResultPublisher;
import com.ron.javainfohunter.crawler.publisher.ContentPublisher;
import com.ron.javainfohunter.crawler.publisher.ErrorPublisher;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage.ErrorType;
import com.ron.javainfohunter.crawler.service.RssFeedCrawler;
import com.ron.javainfohunter.crawler.service.RssSourceService;
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
 *   <li>Publishing crawl statistics and raw content</li>
 *   <li>Handling errors at orchestration level</li>
 *   <li>Bulk updating source metadata for efficiency</li>
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
 * @see RssFeedCrawler
 * @see ContentPublisher
 */
@Slf4j
@Service
public class CrawlOrchestrator {

    private static final int MAX_CAUSE_DEPTH = 5;

    private final RssSourceRepository rssSourceRepository;
    private final RssSourceService rssSourceService;
    private final CrawlerProperties crawlerProperties;
    private final CrawlResultPublisher crawlResultPublisher;
    private final ErrorPublisher errorPublisher;
    private final RssFeedCrawler rssFeedCrawler;
    private final ContentPublisher contentPublisher;
    private final ExecutorService crawlExecutor;

    @Autowired
    public CrawlOrchestrator(
            RssSourceRepository rssSourceRepository,
            RssSourceService rssSourceService,
            CrawlerProperties crawlerProperties,
            CrawlResultPublisher crawlResultPublisher,
            ErrorPublisher errorPublisher,
            RssFeedCrawler rssFeedCrawler,
            ContentPublisher contentPublisher,
            @Qualifier("crawlExecutor") ExecutorService crawlExecutor) {
        this.rssSourceRepository = rssSourceRepository;
        this.rssSourceService = rssSourceService;
        this.crawlerProperties = crawlerProperties;
        this.crawlResultPublisher = crawlResultPublisher;
        this.errorPublisher = errorPublisher;
        this.rssFeedCrawler = rssFeedCrawler;
        this.contentPublisher = contentPublisher;
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

        // Track source IDs for bulk update
        List<Long> successfullyCrawledSourceIds = new CopyOnWriteArrayList<>();

        // Create a list of futures for concurrent execution
        List<CompletableFuture<Void>> futures = sources.stream()
                .map(source -> CompletableFuture.runAsync(
                        () -> processSingleSource(source, resultBuilder, successfullyCrawledSourceIds),
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

        // Bulk update last crawled timestamps for efficiency
        if (!successfullyCrawledSourceIds.isEmpty()) {
            try {
                rssSourceService.bulkUpdateLastCrawled(successfullyCrawledSourceIds);
                log.debug("Bulk updated last_crawled_at for {} sources", successfullyCrawledSourceIds.size());
            } catch (Exception e) {
                log.error("Failed to bulk update last_crawled_at timestamps", e);
            }
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
    private void processSingleSource(RssSource source, CrawlResultBuilder resultBuilder,
                                      List<Long> successfullyCrawledSourceIds) {
        log.debug("Processing source: {} ({})", source.getName(), source.getUrl());

        Instant sourceStartTime = Instant.now();
        int newArticles = 0;
        int duplicateArticles = 0;
        int failedArticles = 0;

        try {
            // Use RssFeedCrawler to fetch and parse the RSS feed
            CrawlResult crawlResult = rssFeedCrawler.crawlFeed(source.getUrl(), source.getId());

            if (crawlResult.isSuccess()) {
                // Publish raw content messages to RabbitMQ
                List<RawContentMessage> messages = crawlResult.getRawContentMessages();
                boolean allPublishingSucceeded = true;
                if (messages != null && !messages.isEmpty()) {
                    for (RawContentMessage message : messages) {
                        try {
                            contentPublisher.publishRawContent(message);
                            newArticles++;
                        } catch (Exception e) {
                            log.warn("Failed to publish message: {}", message.getGuid(), e);
                            failedArticles++;
                            allPublishingSucceeded = false;
                        }
                    }
                }

                duplicateArticles = crawlResult.getDuplicateItems();
                log.debug("Source {} completed: {} new, {} duplicates published",
                        source.getName(), newArticles, duplicateArticles);

                // Only add to bulk update list if all messages were published successfully
                if (allPublishingSucceeded) {
                    successfullyCrawledSourceIds.add(source.getId());
                }

            } else {
                log.warn("Source {} crawl failed: {}",
                        source.getName(), crawlResult.getErrorMessage());
                failedArticles++;

                // Publish error message based on error type
                Throwable throwable = crawlResult.getException();
                ErrorType errorType = determineErrorType(throwable);

                // Convert Throwable to Exception for publishError
                Exception exception = throwable instanceof Exception
                    ? (Exception) throwable
                    : new Exception(throwable.getMessage(), throwable);

                errorPublisher.publishError(
                        source.getId(),
                        source.getName(),
                        source.getUrl(),
                        exception,
                        errorType
                );
            }

        } catch (Throwable e) {
            log.error("Failed to process source: {} ({})", source.getName(), source.getUrl(), e);
            failedArticles++;

            // Convert Throwable to Exception for publishError
            Exception exception = e instanceof Exception
                ? (Exception) e
                : new Exception(e.getMessage(), e);

            // Publish error message
            errorPublisher.publishError(
                    source.getId(),
                    source.getName(),
                    source.getUrl(),
                    exception,
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
     * Determine error type from exception.
     *
     * @param e the exception
     * @return the error type
     */
    private ErrorType determineErrorType(Throwable e) {
        if (e == null) {
            return ErrorType.UNKNOWN;
        }

        Throwable cause = e;
        int depth = 0;
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            if (cause instanceof java.io.IOException) {
                if (cause instanceof java.net.SocketTimeoutException ||
                    cause instanceof java.util.concurrent.TimeoutException) {
                    return ErrorType.CONNECTION_ERROR;
                }
                if (cause.getMessage() != null &&
                    (cause.getMessage().contains("Connection refused") ||
                     cause.getMessage().contains("Connection reset") ||
                     cause.getMessage().contains("No route to host"))) {
                    return ErrorType.CONNECTION_ERROR;
                }
            }

            if (cause instanceof com.rometools.rome.io.FeedException ||
                cause instanceof com.rometools.rome.io.XmlReaderException) {
                return ErrorType.PARSE_ERROR;
            }

            cause = cause.getCause();
            depth++;
        }

        return ErrorType.UNKNOWN;
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
