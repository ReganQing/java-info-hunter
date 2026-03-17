package com.ron.javainfohunter.crawler.service;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.exception.FeedConnectionException;
import com.ron.javainfohunter.crawler.exception.FeedParseException;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.repository.RawContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSS/Atom Feed Crawler Service using Rome library.
 *
 * <p>This service is responsible for fetching and parsing RSS/Atom feeds
 * from various sources. It handles both RSS and Atom formats automatically
 * through the Rome library's unified interface.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Automatic format detection (RSS 0.9, 1.0, 2.0, Atom 0.3, 1.0)</li>
 *   <li>Content deduplication using SHA-256 hashing against database</li>
 *   <li>Timeout handling and connection pooling</li>
 *   <li>Error handling for malformed feeds and network issues</li>
 *   <li>Metadata extraction (author, category, tags, publication date)</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see com.rometools.rome.io.SyndFeedInput
 * @see com.rometools.rome.feed.synd.SyndFeed
 */
@Slf4j
@Service
public class RssFeedCrawler {

    private final CrawlerProperties crawlerProperties;
    private final RawContentRepository rawContentRepository;

    public RssFeedCrawler(CrawlerProperties crawlerProperties,
                          RawContentRepository rawContentRepository) {
        this.crawlerProperties = crawlerProperties;
        this.rawContentRepository = rawContentRepository;
    }

    /**
     * Crawl an RSS/Atom feed and extract all items.
     *
     * <p>This method fetches the feed from the specified URL, parses it,
     * and extracts all items with their metadata. It handles both RSS and Atom
     * formats automatically.</p>
     *
     * @param url the URL of the RSS/Atom feed
     * @param rssSourceId the database ID of the RSS source
     * @return CrawlResult containing extracted items and statistics
     */
    public CrawlResult crawlFeed(String url, Long rssSourceId) {
        long startTime = System.currentTimeMillis();
        log.debug("Starting crawl for feed: {} (source ID: {})", url, rssSourceId);

        try {
            // Validate URL
            validateUrl(url);

            // Fetch and parse the feed
            SyndFeed feed = fetchFeed(url);

            // Extract feed metadata
            String feedTitle = feed.getTitle();
            log.debug("Feed title: {}", feedTitle);

            // Extract all entries
            List<SyndEntry> entries = feed.getEntries();
            if (entries == null || entries.isEmpty()) {
                log.warn("Feed {} has no entries", url);
                return CrawlResult.builder()
                        .success(true)
                        .rssSourceId(rssSourceId)
                        .feedUrl(url)
                        .rssSourceName(feedTitle)
                        .totalItems(0)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Limit entries to max articles per feed
            int maxItems = Math.min(entries.size(), crawlerProperties.getFeed().getMaxArticlesPerFeed());
            List<SyndEntry> limitedEntries = entries.subList(0, maxItems);
            log.debug("Processing {} entries (limited from {})", maxItems, entries.size());

            // Check for duplicates and collect only new content
            Set<String> existingHashes = findExistingContentHashes(limitedEntries);
            log.debug("Found {} existing hashes out of {} entries", existingHashes.size(), limitedEntries.size());

            // Convert entries to RawContentMessage, filtering duplicates
            List<RawContentMessage> messages = new ArrayList<>();
            int duplicateCount = 0;
            for (SyndEntry entry : limitedEntries) {
                try {
                    String contentHash = computeContentHash(entry.getTitle(), extractContent(entry));

                    // Skip if duplicate content detected
                    if (existingHashes.contains(contentHash)) {
                        duplicateCount++;
                        log.trace("Skipping duplicate entry: {} (hash: {})", entry.getTitle(), contentHash);
                        continue;
                    }

                    RawContentMessage message = convertToMessage(entry, rssSourceId, feedTitle, url, contentHash);
                    messages.add(message);

                    log.trace("Extracted entry: {} (hash: {})", entry.getTitle(), contentHash);
                } catch (Exception e) {
                    log.error("Failed to process entry: {}", entry.getTitle(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully crawled feed {}: {} new, {} duplicates in {}ms",
                    url, messages.size(), duplicateCount, duration);

            return CrawlResult.builder()
                    .success(true)
                    .rssSourceId(rssSourceId)
                    .rssSourceName(feedTitle)
                    .feedUrl(url)
                    .totalItems(messages.size() + duplicateCount)
                    .newItems(messages.size())
                    .duplicateItems(duplicateCount)
                    .rawContentMessages(messages)
                    .durationMs(duration)
                    .build();

        } catch (FeedConnectionException e) {
            log.error("Connection error fetching feed {}: {}", url, e.getMessage());
            return CrawlResult.failure(rssSourceId, url, "Connection error: " + e.getMessage(), e);
        } catch (FeedParseException e) {
            log.error("Parse error processing feed {}: {}", url, e.getMessage());
            return CrawlResult.failure(rssSourceId, url, "Parse error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error crawling feed {}", url, e);
            return CrawlResult.failure(rssSourceId, url, "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch and parse an RSS/Atom feed from the specified URL.
     *
     * <p>This method handles HTTP connection with timeouts, validates the response,
     * and parses the feed using Rome's SyndFeedInput.</p>
     *
     * @param url the URL of the feed
     * @return parsed SyndFeed object
     * @throws IOException if network error occurs
     * @throws FeedException if feed parsing fails
     * @throws FeedConnectionException if HTTP error occurs
     * @throws FeedParseException if feed format is invalid
     */
    private SyndFeed fetchFeed(String url) throws IOException, FeedException {
        HttpURLConnection connection = null;
        try {
            URL feedUrl = new URL(url);

            // Configure connection with timeouts
            connection = (HttpURLConnection) feedUrl.openConnection();
            connection.setConnectTimeout(crawlerProperties.getFeed().getConnectionTimeout());
            connection.setReadTimeout(crawlerProperties.getFeed().getReadTimeout());
            connection.setRequestProperty("User-Agent", crawlerProperties.getFeed().getUserAgent());
            connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml");

            // Parse the feed - XmlReader handles connection automatically
            // This avoids the "Already connected" error from setting request properties after connect()
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(connection));

            log.debug("Successfully fetched and parsed feed: {} (type: {})", url, feed.getFeedType());

            return feed;

        } catch (IOException e) {
            if (e instanceof java.net.SocketTimeoutException) {
                throw new FeedConnectionException(
                        "Connection timeout while fetching feed",
                        url,
                        null,
                        true,
                        e
                );
            }
            throw new FeedConnectionException(
                    "Network error while fetching feed: " + e.getMessage(),
                    url,
                    null,
                    true,
                    e
            );
        } catch (FeedException e) {
            throw new FeedParseException(
                    "Failed to parse feed: " + e.getMessage(),
                    url,
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Extract content from a SyndEntry.
     *
     * <p>This method tries multiple content fields in order of preference:
     * content -> description -> summary</p>
     *
     * @param entry the syndication entry
     * @return the content as a string, or empty string if not found
     */
    private String extractContent(SyndEntry entry) {
        // Try content first
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            SyndContent content = entry.getContents().get(0);
            if (content != null && content.getValue() != null) {
                return content.getValue();
            }
        }

        // Try description
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            return entry.getDescription().getValue();
        }

        // Return empty string if no content found
        return "";
    }

    /**
     * Validate that the URL is safe to use.
     *
     * <p>This method checks that:</p>
     * <ul>
     *   <li>URL is not null or empty</li>
     *   <li>URL uses http or https protocol only</li>
     *   <li>URL is well-formed</li>
     * </ul>
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if URL is invalid
     */
    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            URL javaUrl = new URL(url);
            String protocol = javaUrl.getProtocol();

            // Only allow http and https protocols
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new IllegalArgumentException(
                    "Unsupported protocol: " + protocol + ". Only http and https are allowed."
                );
            }

        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }

    /**
     * Compute SHA-256 hash of title + content for deduplication.
     *
     * <p>This method creates a unique hash based on the article title and content
     * to detect duplicate articles across different RSS sources.</p>
     *
     * @param title the article title
     * @param content the article content
     * @return hexadecimal string representation of the SHA-256 hash
     */
    private String computeContentHash(String title, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                    crawlerProperties.getDeduplication().getHashAlgorithm()
            );

            String combined = (title != null ? title : "") + (content != null ? content : "");
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm not found: {}", crawlerProperties.getDeduplication().getHashAlgorithm(), e);
            throw new RuntimeException("Failed to compute content hash", e);
        }
    }

    /**
     * Convert a SyndEntry to a RawContentMessage.
     *
     * <p>This method extracts all available metadata from the syndication entry
     * and converts it to a message suitable for publishing to RabbitMQ.</p>
     *
     * @param entry the syndication entry
     * @param rssSourceId the RSS source database ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param contentHash the pre-computed content hash
     * @return RawContentMessage ready for publishing
     */
    private RawContentMessage convertToMessage(SyndEntry entry, Long rssSourceId,
                                               String rssSourceName, String rssSourceUrl, String contentHash) {
        // Extract publication date
        Instant publishDate = null;
        if (entry.getPublishedDate() != null) {
            publishDate = entry.getPublishedDate().toInstant();
        } else if (entry.getUpdatedDate() != null) {
            publishDate = entry.getUpdatedDate().toInstant();
        }

        // Extract categories/tags
        String[] tags = null;
        if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
            tags = entry.getCategories().stream()
                    .map(category -> category.getName())
                    .filter(name -> name != null && !name.isEmpty())
                    .toArray(String[]::new);
        }

        // Extract author
        String author = entry.getAuthor();
        if (author == null || author.isEmpty()) {
            if (entry.getAuthors() != null && !entry.getAuthors().isEmpty()) {
                author = entry.getAuthors().get(0).getEmail();
            }
        }

        return RawContentMessage.builder()
                .guid(entry.getUri() != null ? entry.getUri() : entry.getLink())
                .title(entry.getTitle())
                .link(entry.getLink())
                .rawContent(extractContent(entry))
                .contentHash(contentHash)
                .rssSourceId(rssSourceId)
                .rssSourceName(rssSourceName)
                .rssSourceUrl(rssSourceUrl)
                .author(author)
                .publishDate(publishDate)
                .crawlDate(Instant.now())
                .category(extractCategory(entry))
                .tags(tags)
                .build();
    }

    /**
     * Extract the primary category from a SyndEntry.
     *
     * @param entry the syndication entry
     * @return the category name, or null if not found
     */
    private String extractCategory(SyndEntry entry) {
        if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
            return entry.getCategories().get(0).getName();
        }
        return null;
    }

    /**
     * Find existing content hashes for the given entries.
     *
     * <p>This method performs bulk duplicate checking by computing hashes for all entries
     * and querying the database for existing matches using a single batch query.
     * This fixes the N+1 query problem by using {@code findExistingContentHashes()}
     * which uses an IN clause instead of individual queries.</p>
     *
     * @param entries the syndication entries to check
     * @return set of existing content hashes
     */
    private Set<String> findExistingContentHashes(List<SyndEntry> entries) {
        if (!crawlerProperties.getDeduplication().isEnabled()) {
            return Set.of();
        }

        // Compute hashes for all entries first
        Set<String> hashesToCheck = ConcurrentHashMap.newKeySet();
        for (SyndEntry entry : entries) {
            try {
                String contentHash = computeContentHash(entry.getTitle(), extractContent(entry));
                hashesToCheck.add(contentHash);
            } catch (Exception e) {
                log.trace("Failed to compute hash for entry: {}", entry.getTitle());
            }
        }

        // Single batch query using IN clause (N+1 fix)
        if (hashesToCheck.isEmpty()) {
            return Set.of();
        }

        try {
            Set<String> existingHashes = rawContentRepository.findExistingContentHashes(hashesToCheck);
            // Return thread-safe set
            Set<String> threadSafeSet = ConcurrentHashMap.newKeySet();
            threadSafeSet.addAll(existingHashes);
            return threadSafeSet;
        } catch (Exception e) {
            log.warn("Error checking for duplicate hashes: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Check if a single content hash already exists in the database.
     *
     * <p>This is a convenience method for single-item duplicate checking.
     * Note: The @Transactional annotation is intentionally omitted from this
     * public method as it doesn't work reliably without proper proxy configuration.
     * The repository method handles its own transaction.</p>
     *
     * @param contentHash the content hash to check
     * @return true if the content hash already exists, false otherwise
     */
    public boolean isDuplicateContent(String contentHash) {
        if (!crawlerProperties.getDeduplication().isEnabled()) {
            return false;
        }

        try {
            return rawContentRepository.findByContentHash(contentHash).isPresent();
        } catch (Exception e) {
            log.warn("Error checking for duplicate hash {}: {}", contentHash, e.getMessage());
            return false;
        }
    }
}
