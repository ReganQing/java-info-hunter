package com.ron.javainfohunter.crawler.service;

import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RssSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing RSS sources and their crawling metadata.
 *
 * <p>This service provides methods to fetch RSS sources that are due for crawling,
 * update their last crawled timestamps, and manage content hashes for deduplication.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Query active RSS sources due for crawling</li>
 *   <li>Update last_crawled_at timestamps</li>
 *   <li>Track crawling statistics (total articles, failed crawls)</li>
 *   <li>Transactional database operations</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@Service
public class RssSourceService {

    private final RssSourceRepository rssSourceRepository;

    public RssSourceService(RssSourceRepository rssSourceRepository) {
        this.rssSourceRepository = rssSourceRepository;
    }

    /**
     * Fetch all active RSS sources that are due for crawling.
     *
     * <p>A source is considered due for crawling if:</p>
 * <ul>
     *   <li>It is active (isActive = true)</li>
     *   <li>It has never been crawled (last_crawled_at is null), OR</li>
     *   <li>The time since last crawl exceeds its crawl_interval_seconds</li>
     * </ul>
     *
     * @return list of RSS sources due for crawling
     */
    @Transactional(readOnly = true)
    public List<RssSource> fetchSourcesDueForCrawling() {
        log.debug("Fetching RSS sources due for crawling");

        // Get all active sources
        List<RssSource> activeSources = rssSourceRepository.findByIsActiveTrue();

        // Filter sources that are due for crawling
        List<RssSource> dueSources = activeSources.stream()
                .filter(RssSource::isDueForCrawling)
                .toList();

        log.info("Found {} RSS sources due for crawling (out of {} active sources)",
                dueSources.size(), activeSources.size());

        return dueSources;
    }

    /**
     * Update the last_crawled_at timestamp for an RSS source.
     *
     * <p>This method also increments the total articles count to track
     * the source's activity over time.</p>
     *
     * @param sourceId the ID of the RSS source
     * @param newArticlesCount the number of new articles found
     */
    @Transactional
    public void updateLastCrawled(Long sourceId, int newArticlesCount) {
        log.debug("Updating last crawled timestamp for source ID: {}", sourceId);

        rssSourceRepository.findById(sourceId).ifPresentOrElse(
                source -> {
                    source.updateLastCrawled();
                    source.setTotalArticles(source.getTotalArticles() + newArticlesCount);
                    rssSourceRepository.save(source);
                    log.debug("Updated source {}: last_crawled_at={}, total_articles={}",
                            sourceId, source.getLastCrawledAt(), source.getTotalArticles());
                },
                () -> log.warn("Source ID {} not found when updating last crawled timestamp", sourceId)
        );
    }

    /**
     * Update the last_crawled_at timestamp and increment failed crawl count.
     *
     * <p>This method is called when a crawl attempt fails, allowing the system
     * to track source reliability and trigger alerts for problematic sources.</p>
     *
     * @param sourceId the ID of the RSS source
     */
    @Transactional
    public void updateFailedCrawl(Long sourceId) {
        log.debug("Recording failed crawl for source ID: {}", sourceId);

        rssSourceRepository.findById(sourceId).ifPresentOrElse(
                source -> {
                    source.updateLastCrawled();
                    source.incrementFailedCrawlCount();
                    rssSourceRepository.save(source);
                    log.debug("Updated source {}: failed_crawls={}", sourceId, source.getFailedCrawls());
                },
                () -> log.warn("Source ID {} not found when recording failed crawl", sourceId)
        );
    }

    /**
     * Get an RSS source by its ID.
     *
     * @param sourceId the ID of the RSS source
     * @return the RSS source, or null if not found
     */
    @Transactional(readOnly = true)
    public RssSource getSourceById(Long sourceId) {
        return rssSourceRepository.findById(sourceId).orElse(null);
    }

    /**
     * Get all active RSS sources.
     *
     * @return list of all active RSS sources
     */
    @Transactional(readOnly = true)
    public List<RssSource> getAllActiveSources() {
        return rssSourceRepository.findByIsActiveTrue();
    }

    /**
     * Get the total count of active RSS sources.
     *
     * @return number of active sources
     */
    @Transactional(readOnly = true)
    public long getActiveSourcesCount() {
        return rssSourceRepository.countByIsActiveTrue();
    }

    /**
     * Find RSS sources with high failure rate.
     *
     * <p>This method can be used to identify problematic sources that may
     * need attention or removal from the system.</p>
     *
     * @param failureRateThreshold the failure rate threshold (0-100)
     * @return list of sources with failure rate above the threshold
     */
    @Transactional(readOnly = true)
    public List<RssSource> findSourcesWithHighFailureRate(double failureRateThreshold) {
        return rssSourceRepository.findSourcesWithHighFailureRate(failureRateThreshold);
    }

    /**
     * Bulk update last crawled timestamp for multiple sources.
     *
     * <p>This is more efficient than updating sources individually when
     * processing multiple sources in parallel.</p>
     *
     * @param sourceIds list of source IDs to update
     */
    @Transactional
    public void bulkUpdateLastCrawled(List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        rssSourceRepository.bulkUpdateLastCrawledAt(sourceIds, now);
        log.debug("Bulk updated last_crawled_at for {} sources", sourceIds.size());
    }
}
