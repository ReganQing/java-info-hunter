package com.ron.javainfohunter.crawler.service;

import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import com.ron.javainfohunter.entity.RssSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinator for RSS feed crawling operations.
 *
 * <p>This service orchestrates the crawling workflow, managing concurrent
 * crawling of multiple RSS sources using Java 21 virtual threads.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Concurrent crawling using virtual threads (Java 21)</li>
 *   <li>Error isolation (single source failure doesn't affect others)</li>
 *   <li>Comprehensive logging and statistics</li>
 *   <li>Integration with RssFeedCrawler and RssSourceService</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * This service uses thread-safe collections and atomic counters to safely
 * handle concurrent crawling operations.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@Service
public class CrawlCoordinator {

    private final RssFeedCrawler rssFeedCrawler;
    private final RssSourceService rssSourceService;

    public CrawlCoordinator(RssFeedCrawler rssFeedCrawler, RssSourceService rssSourceService) {
        this.rssFeedCrawler = rssFeedCrawler;
        this.rssSourceService = rssSourceService;
    }

    /**
     * Crawl multiple RSS sources concurrently using virtual threads.
     *
     * <p>This method uses Java 21's virtual threads to achieve high concurrency
     * with minimal resource overhead. Each source is crawled in a separate virtual thread.</p>
     *
     * <p>Error isolation is ensured: a failure in one source does not affect
     * the crawling of other sources.</p>
     *
     * @param sources list of RSS sources to crawl
     * @return list of all raw content messages extracted from all sources
     */
    public List<RawContentMessage> crawlSources(List<RssSource> sources) {
        if (sources == null || sources.isEmpty()) {
            log.info("No sources to crawl");
            return List.of();
        }

        log.info("Starting concurrent crawl of {} RSS sources using virtual threads", sources.size());
        long startTime = System.currentTimeMillis();

        // Thread-safe collections for concurrent operations
        List<RawContentMessage> allMessages = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger totalArticles = new AtomicInteger(0);

        // Create virtual thread executor for concurrent crawling
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit crawl tasks for each source
            List<CompletableFuture<Void>> futures = sources.stream()
                    .map(source -> CompletableFuture.runAsync(
                            () -> crawlSingleSource(source, allMessages),
                            executor
                            ))
                    .toList();

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error("Error during concurrent crawling", e);
        }

        long duration = System.currentTimeMillis() - startTime;

        // Log summary
        log.info("Crawl completed: {} sources successful, {} sources failed, {} total articles in {}ms",
                successCount.get(), failureCount.get(), totalArticles.get(), duration);

        return allMessages;
    }

    /**
     * Crawl a single RSS source and update its metadata.
     *
     * <p>This method handles the complete workflow for a single source:</p>
 * <ol>
     *   <li>Fetch and parse the RSS feed</li>
     *   <li>Extract all articles</li>
     *   <li>Update source metadata (last_crawled_at, article counts)</li>
     *   <li>Handle errors gracefully</li>
     * </ol>
     *
     * @param source the RSS source to crawl
     * @param allMessages thread-safe list to collect extracted messages
     */
    private void crawlSingleSource(RssSource source, List<RawContentMessage> allMessages) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Crawling source: {} ({})", source.getName(), source.getUrl());

            // Crawl the feed
            CrawlResult result = rssFeedCrawler.crawlFeed(source.getUrl(), source.getId());

            long duration = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                // Add messages to the shared list
                allMessages.addAll(result.getRawContentMessages());

                // Update source metadata
                rssSourceService.updateLastCrawled(source.getId(), result.getNewItems());

                log.info("Successfully crawled {} ({}): {} articles in {}ms",
                        source.getName(), source.getUrl(), result.getTotalItems(), duration);

            } else {
                // Handle crawl failure
                rssSourceService.updateFailedCrawl(source.getId());

                log.error("Failed to crawl {} ({}): {}",
                        source.getName(), source.getUrl(), result.getErrorMessage());

                if (result.getException() != null) {
                    log.debug("Exception details:", result.getException());
                }
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Record failure
            rssSourceService.updateFailedCrawl(source.getId());

            log.error("Unexpected error crawling {} ({}): {}",
                    source.getName(), source.getUrl(), e.getMessage(), e);

        }
    }

    /**
     * Crawl a single RSS source and return the result.
     *
     * <p>This is a convenience method for crawling a single source
     * without the complexity of the full coordinator workflow.</p>
     *
     * @param source the RSS source to crawl
     * @return crawl result with extracted messages and statistics
     */
    public CrawlResult crawlSingleSourceSync(RssSource source) {
        log.info("Crawling single source: {} ({})", source.getName(), source.getUrl());
        long startTime = System.currentTimeMillis();

        try {
            // Crawl the feed
            CrawlResult result = rssFeedCrawler.crawlFeed(source.getUrl(), source.getId());

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);

            if (result.isSuccess()) {
                // Update source metadata
                rssSourceService.updateLastCrawled(source.getId(), result.getNewItems());
                log.info("Successfully crawled {}: {} articles in {}ms",
                        source.getName(), result.getTotalItems(), duration);
            } else {
                rssSourceService.updateFailedCrawl(source.getId());
                log.error("Failed to crawl {}: {}", source.getName(), result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            rssSourceService.updateFailedCrawl(source.getId());

            CrawlResult result = CrawlResult.failure(
                    source.getId(),
                    source.getUrl(),
                    "Unexpected error: " + e.getMessage(),
                    e
            );
            result.setDurationMs(duration);

            log.error("Unexpected error crawling {}", source.getName(), e);
            return result;
        }
    }

    /**
     * Crawl all active RSS sources that are due for crawling.
     *
     * <p>This is a convenience method that combines fetching due sources
     * and crawling them concurrently.</p>
     *
     * @return list of all raw content messages extracted
     */
    public List<RawContentMessage> crawlDueSources() {
        log.info("Fetching sources due for crawling");
        List<RssSource> dueSources = rssSourceService.fetchSourcesDueForCrawling();
        return crawlSources(dueSources);
    }

    /**
     * Get statistics about the crawler.
     *
     * @return statistics string with active sources count
     */
    public String getStatistics() {
        long activeCount = rssSourceService.getActiveSourcesCount();
        return String.format("Crawler Statistics: %d active RSS sources", activeCount);
    }
}
