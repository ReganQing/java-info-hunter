package com.ron.javainfohunter.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RSS Source Create/Update Request DTO
 *
 * Used for creating and updating RSS subscription sources.
 * Includes validation for required fields and business rules.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceRequest {

    /**
     * Human-readable name for the RSS source
     */
    @NotBlank(message = "Name is required")
    @Pattern(regexp = "^[\\p{Alnum}\\s\\-_\\.]{1,255}$", message = "Name must be 1-255 alphanumeric characters")
    private String name;

    /**
     * URL of the RSS feed
     */
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            message = "Invalid URL format")
    private String url;

    /**
     * Optional description of the RSS source
     */
    private String description;

    /**
     * Category for grouping related sources
     */
    private String category;

    /**
     * Array of tags for flexible categorization and filtering
     */
    private List<String> tags;

    /**
     * Crawling interval in seconds (minimum 60s recommended)
     */
    @Min(value = 60, message = "Crawl interval must be at least 60 seconds")
    private Integer crawlIntervalSeconds;

    /**
     * Whether this source is actively being crawled
     */
    private Boolean isActive;

    /**
     * Content language (ISO 639-1 code)
     */
    private String language;

    /**
     * Timezone for date parsing (IANA timezone ID)
     */
    private String timezone;
}
