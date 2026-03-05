package com.ron.javainfohunter.crawler.scheduler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlErrorMessage.ErrorType;
import com.ron.javainfohunter.crawler.publisher.CrawlResultPublisher;
import com.ron.javainfohunter.crawler.publisher.ErrorPublisher;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task for automatic RSS feed crawling.
 *
 * <p>This component uses Spring's {@link Scheduled} annotation to trigger
 * periodic crawling of RSS feeds. It implements a robust scheduling mechanism
 * with the following features:</p>
 *
 * <ul>
 *   <li>Configurable scheduling interval via {@code crawler.scheduler.fixed-rate}</li>
 *   <li>Respects the {@code scheduler.enabled} property for runtime control</li>
 *   <li>Filters sources based on their {@code crawl_interval_seconds}</li>
 *   <li>Error isolation prevents scheduler failures from stopping subsequent runs</li>
 *   <li>Comprehensive logging for monitoring and debugging</li>
 * </ul>
 *
 * <p><b>Schedule Configuration:</b></p>
 * <pre>
 * javainfohunter:
 *   crawler:
 *     scheduler:
 *       enabled: true
 *       initial-delay: 30000  # 30 seconds
 *       fixed-rate: 3600000   # 1 hour
 * </pre>
 *
 * <p><b>Workflow:</b></p>
 * <ol>
 *   <li>Check if scheduler is enabled</li>
 *   <li>Fetch active RSS sources from database</li>
 *   <li>Filter sources due for crawling (based on last_crawled_at)</li>
 *   <li>Trigger crawl orchestrator for concurrent processing</li>
 *   <li>Publish crawl results statistics</li>
 *   <li>Handle and log any errors</li>
 * </ol>
 *
 * @see org.springframework.scheduling.annotation.Scheduled
 * @see CrawlOrchestrator
 */
@Slf4j
@Component
public class CrawlScheduler {

    private final RssSourceRepository rssSourceRepository;
    private final CrawlerProperties crawlerProperties;
    private final CrawlOrchestrator crawlOrchestrator;
    private final CrawlResultPublisher crawlResultPublisher;
    private final ErrorPublisher errorPublisher;

    @Autowired
    public CrawlScheduler(
            RssSourceRepository rssSourceRepository,
            CrawlerProperties crawlerProperties,
            CrawlOrchestrator crawlOrchestrator,
            CrawlResultPublisher crawlResultPublisher,
            ErrorPublisher errorPublisher) {
        this.rssSourceRepository = rssSourceRepository;
        this.crawlerProperties = crawlerProperties;
        this.crawlOrchestrator = crawlOrchestrator;
        this.crawlResultPublisher = crawlResultPublisher;
        this.errorPublisher = errorPublisher;
    }

    /**
     * Scheduled method for automatic RSS feed crawling.
     *
     * <p>This method is executed periodically based on the configured
     * {@code fixed-rate} interval. It orchestrates the complete crawling
     * workflow for all RSS sources due for crawling.</p>
     *
     * <p><b>Scheduling Details:</b></p>
     * <ul>
     *   <li>Fixed rate: Configurable via {@code crawler.scheduler.fixed-rate}</li>
     *   <li>Initial delay: Configurable via {@code crawler.scheduler.initial-delay}</li>
     *   <li>Execution: Method runs in a virtual thread for efficiency</li>
     * </ul>
     *
     * <p><b>Error Handling:</b></p>
     * Errors are caught and logged but do not prevent subsequent scheduled runs.
     * This ensures the scheduler remains resilient even when individual crawl jobs fail.
     *
     * @see org.springframework.scheduling.annotation.Scheduled#fixedRateString
     * @see org.springframework.scheduling.annotation.Scheduled#initialDelayString
     */
    @Scheduled(
        fixedRateString = "${javainfohunter.crawler.scheduler.fixed-rate}",
        initialDelayString = "${javainfohunter.crawler.scheduler.initial-delay}"
    )
    public void scheduleCrawling() {
        // Check if scheduler is enabled
        if (!crawlerProperties.getScheduler().isEnabled()) {
            log.debug("Scheduler is disabled, skipping crawl job");
            return;
        }

        log.info("=================================================");
        log.info("Starting scheduled crawl job");
        log.info("=================================================");

        try {
            // Fetch sources due for crawling
            List<RssSource> sources = fetchSourcesDueForCrawling();
            log.info("Found {} sources due for crawling", sources.size());

            if (sources.isEmpty()) {
                log.info("No sources due for crawling at this time");
                return;
            }

            // Log source details
            sources.forEach(source ->
                log.debug("Source: {} ({}) - Last crawled: {}",
                    source.getName(),
                    source.getUrl(),
                    source.getLastCrawledAt())
            );

            // Execute crawl job
            var result = crawlOrchestrator.executeCrawlJob(sources);

            // Publish results
            crawlResultPublisher.publishCrawlResult(result);

            // Log summary
            log.info("Crawl job completed:");
            log.info("  - New articles: {}", result.getNewArticles());
            log.info("  - Duplicate articles: {}", result.getDuplicateArticles());
            log.info("  - Failed articles: {}", result.getFailedArticles());
            log.info("  - Duration: {} ms", result.getDurationMs());

            if (result.getErrorMessage() != null) {
                log.warn("Crawl job had errors: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Scheduled crawl job failed unexpectedly", e);
            errorPublisher.publishError("scheduler", e, ErrorType.SCHEDULER_ERROR);
        } finally {
            log.info("=================================================");
            log.info("Scheduled crawl job finished");
            log.info("=================================================");
        }
    }

    /**
     * Fetch RSS sources that are due for crawling.
     *
     * <p>A source is considered due for crawling if:</p>
     * <ul>
     *   <li>It is active ({@code isActive = true})</li>
     *   <li>It has never been crawled ({@code last_crawled_at IS NULL})</li>
     *   <li>OR the crawl interval has elapsed since last crawl</li>
     * </ul>
     *
     * <p>The crawl interval is calculated as:</p>
     * <pre>
     * now > last_crawled_at + crawl_interval_seconds
     * </pre>
     *
     * <p><b>Performance Note:</b></p>
     * Uses a database query with indexed columns ({@code is_active}, {@code last_crawled_at})
     * for efficient filtering of sources.
     *
     * @return List of RSS sources due for crawling
     * @see RssSource#isDueForCrawling()
     */
    private List<RssSource> fetchSourcesDueForCrawling() {
        try {
            // Get all active sources
            List<RssSource> activeSources = rssSourceRepository.findByIsActiveTrue();
            log.debug("Found {} active RSS sources", activeSources.size());

            // Filter sources due for crawling
            // We do this in Java to use the entity method isDueForCrawling()
            Instant now = Instant.now();
            List<RssSource> dueSources = activeSources.stream()
                    .filter(this::shouldCrawl)
                    .toList();

            log.debug("Filtered to {} sources due for crawling", dueSources.size());

            return dueSources;

        } catch (Exception e) {
            log.error("Failed to fetch sources due for crawling", e);
            errorPublisher.publishError("scheduler", e, ErrorType.DATABASE_ERROR);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Determine if a specific RSS source should be crawled.
     *
     * <p>This method checks if a source is due for crawling based on:</p>
     * <ul>
     *   <li>Active status</li>
     *   <li>Last crawled timestamp</li>
     *   <li>Crawl interval configuration</li>
     * </ul>
     *
     * <p><b>Crawl Logic:</b></p>
     * <pre>
     * if (never crawled) → crawl
     * if (now > last_crawled + interval) → crawl
     * else → skip
     * </pre>
     *
     * @param source The RSS source to check
     * @return true if the source should be crawled, false otherwise
     */
    private boolean shouldCrawl(RssSource source) {
        // Check if source is active
        if (!source.getIsActive()) {
            log.trace("Source {} is not active, skipping", source.getName());
            return false;
        }

        // Check if never crawled (always crawl new sources)
        if (source.getLastCrawledAt() == null) {
            log.debug("Source {} has never been crawled, scheduling", source.getName());
            return true;
        }

        // Check if crawl interval has elapsed
        Instant lastCrawled = source.getLastCrawledAt();
        Instant nextCrawlTime = lastCrawled.plusSeconds(source.getCrawlIntervalSeconds());
        Instant now = Instant.now();

        if (now.isAfter(nextCrawlTime)) {
            long minutesSinceCrawl = TimeUnit.MILLISECONDS.toMinutes(
                now.toEpochMilli() - lastCrawled.toEpochMilli()
            );
            log.debug("Source {} is due for crawling (last crawled {} minutes ago)",
                    source.getName(), minutesSinceCrawl);
            return true;
        }

        log.trace("Source {} is not due for crawling yet", source.getName());
        return false;
    }
}
