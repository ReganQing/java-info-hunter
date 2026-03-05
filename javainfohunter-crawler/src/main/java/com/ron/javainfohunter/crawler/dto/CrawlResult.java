package com.ron.javainfohunter.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a single RSS feed crawl operation.
 *
 * <p>This DTO contains statistics and data from a crawl operation,
 * including successfully parsed items, errors encountered, and
 * deduplication information.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResult {

    /**
     * Whether the crawl operation was successful
     */
    @Builder.Default
    private boolean success = false;

    /**
     * RSS source ID that was crawled
     */
    private Long rssSourceId;

    /**
     * RSS source name
     */
    private String rssSourceName;

    /**
     * RSS feed URL that was crawled
     */
    private String feedUrl;

    /**
     * Total number of items found in the feed
     */
    @Builder.Default
    private int totalItems = 0;

    /**
     * Number of new items (not seen before based on content hash)
     */
    @Builder.Default
    private int newItems = 0;

    /**
     * Number of duplicate items (already processed)
     */
    @Builder.Default
    private int duplicateItems = 0;

    /**
     * Number of items that failed to process
     */
    @Builder.Default
    private int failedItems = 0;

    /**
     * List of raw content messages extracted from the feed
     */
    @Builder.Default
    private List<RawContentMessage> rawContentMessages = new ArrayList<>();

    /**
     * Error message if the crawl failed
     */
    private String errorMessage;

    /**
     * Exception that caused the failure (if applicable)
     */
    private Throwable exception;

    /**
     * Time taken to crawl the feed in milliseconds
     */
    @Builder.Default
    private long durationMs = 0;

    /**
     * Create a failed crawl result
     *
     * @param rssSourceId the RSS source ID
     * @param feedUrl the feed URL
     * @param errorMessage the error message
     * @return a failed CrawlResult
     */
    public static CrawlResult failure(Long rssSourceId, String feedUrl, String errorMessage) {
        return CrawlResult.builder()
                .success(false)
                .rssSourceId(rssSourceId)
                .feedUrl(feedUrl)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create a failed crawl result with exception
     *
     * @param rssSourceId the RSS source ID
     * @param feedUrl the feed URL
     * @param errorMessage the error message
     * @param exception the exception that caused the failure
     * @return a failed CrawlResult
     */
    public static CrawlResult failure(Long rssSourceId, String feedUrl, String errorMessage, Throwable exception) {
        return CrawlResult.builder()
                .success(false)
                .rssSourceId(rssSourceId)
                .feedUrl(feedUrl)
                .errorMessage(errorMessage)
                .exception(exception)
                .build();
    }

    /**
     * Create a successful crawl result
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param feedUrl the feed URL
     * @param rawContentMessages the list of raw content messages
     * @return a successful CrawlResult
     */
    public static CrawlResult success(Long rssSourceId, String rssSourceName, String feedUrl,
                                     List<RawContentMessage> rawContentMessages) {
        return CrawlResult.builder()
                .success(true)
                .rssSourceId(rssSourceId)
                .rssSourceName(rssSourceName)
                .feedUrl(feedUrl)
                .totalItems(rawContentMessages.size())
                .newItems(rawContentMessages.size())
                .rawContentMessages(rawContentMessages)
                .build();
    }

    /**
     * Add a raw content message to the result
     *
     * @param message the raw content message
     */
    public void addRawContentMessage(RawContentMessage message) {
        if (rawContentMessages == null) {
            rawContentMessages = new ArrayList<>();
        }
        rawContentMessages.add(message);
        totalItems++;
    }

    /**
     * Increment the new items count
     */
    public void incrementNewItems() {
        newItems++;
    }

    /**
     * Increment the duplicate items count
     */
    public void incrementDuplicateItems() {
        duplicateItems++;
    }

    /**
     * Increment the failed items count
     */
    public void incrementFailedItems() {
        failedItems++;
    }

    /**
     * Check if the crawl was partially successful
     * (some items succeeded, some failed)
     *
     * @return true if partially successful
     */
    public boolean isPartialSuccess() {
        return success && (failedItems > 0 || newItems > 0);
    }

    /**
     * Get the success rate as a percentage
     *
     * @return success rate (0-100), or 0 if no items
     */
    public double getSuccessRate() {
        if (totalItems == 0) {
            return 0.0;
        }
        return ((totalItems - failedItems) * 100.0) / totalItems;
    }
}
