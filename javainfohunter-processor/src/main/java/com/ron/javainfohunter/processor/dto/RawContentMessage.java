package com.ron.javainfohunter.processor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Raw content message received from the crawler module.
 *
 * <p>This DTO mirrors the crawler module's {@code com.ron.javainfohunter.crawler.dto.RawContentMessage}
 * to ensure compatibility when consuming messages from the crawler.</p>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * RSS Feed Crawler → Raw Content Queue → Content Processor Consumer
 * </pre>
 *
 * @author JavaInfoHunter
 * @since 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawContentMessage {

    /**
     * Unique identifier from RSS feed (guid).
     */
    @JsonProperty("guid")
    private String guid;

    /**
     * Article title.
     */
    @JsonProperty("title")
    private String title;

    /**
     * Article link/URL.
     */
    @JsonProperty("link")
    private String link;

    /**
     * Raw content (HTML or plain text).
     */
    @JsonProperty("rawContent")
    private String rawContent;

    /**
     * Content hash for deduplication (SHA-256).
     */
    @JsonProperty("contentHash")
    private String contentHash;

    /**
     * RSS source ID (database primary key).
     */
    @JsonProperty("rssSourceId")
    private Long rssSourceId;

    /**
     * RSS source name.
     */
    @JsonProperty("rssSourceName")
    private String rssSourceName;

    /**
     * RSS source URL.
     */
    @JsonProperty("rssSourceUrl")
    private String rssSourceUrl;

    /**
     * Author (optional).
     */
    @JsonProperty("author")
    private String author;

    /**
     * Publication date (optional).
     */
    @JsonProperty("publishDate")
    private Instant publishDate;

    /**
     * Crawl timestamp.
     */
    @JsonProperty("crawlDate")
    private Instant crawlDate;

    /**
     * Content category.
     */
    @JsonProperty("category")
    private String category;

    /**
     * Tags associated with the content.
     */
    @JsonProperty("tags")
    private String[] tags;

    /**
     * Additional metadata (flexible schema).
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Message priority (1-10, higher = more important).
     */
    @JsonProperty("priority")
    private Integer priority;

    /**
     * Maximum retry attempts.
     */
    @JsonProperty("maxRetries")
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Current retry count.
     */
    @JsonProperty("retryCount")
    @Builder.Default
    private Integer retryCount = 0;

}
