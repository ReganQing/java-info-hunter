package com.ron.javainfohunter.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Message DTO for raw RSS content to be published to RabbitMQ.
 *
 * <p>This message is published to the {@code crawler.raw.content.queue}
 * and contains the raw content extracted from RSS feeds.</p>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * RSS Feed Crawler → Raw Content Queue → Content Processor
 * </pre>
 *
 * @see com.rometools.rome.feed.synd.SyndEntry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawContentMessage {

    /**
     * Unique identifier from RSS feed (guid).
     */
    private String guid;

    /**
     * Article title.
     */
    private String title;

    /**
     * Article link/URL.
     */
    private String link;

    /**
     * Raw content (HTML or plain text).
     */
    private String rawContent;

    /**
     * Content hash for deduplication (SHA-256).
     */
    private String contentHash;

    /**
     * RSS source ID (database primary key).
     */
    private Long rssSourceId;

    /**
     * RSS source name.
     */
    private String rssSourceName;

    /**
     * RSS source URL.
     */
    private String rssSourceUrl;

    /**
     * Author (optional).
     */
    private String author;

    /**
     * Publication date (optional).
     */
    private Instant publishDate;

    /**
     * Crawl timestamp.
     */
    private Instant crawlDate;

    /**
     * Content category.
     */
    private String category;

    /**
     * Tags associated with the content.
     */
    private String[] tags;

    /**
     * Additional metadata (flexible schema).
     */
    private Map<String, Object> metadata;

    /**
     * Message priority (1-10, higher = more important).
     */
    private Integer priority;

    /**
     * Maximum retry attempts.
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Current retry count.
     */
    @Builder.Default
    private Integer retryCount = 0;

}
