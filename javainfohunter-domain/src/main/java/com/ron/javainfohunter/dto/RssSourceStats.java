package com.ron.javainfohunter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * RSS Source Statistics DTO
 *
 * Data Transfer Object for RSS source statistics.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceStats {
    private Long id;
    private String name;
    private String category;
    private Long totalArticles;
    private Long failedCrawls;
    private Instant lastCrawledAt;
}
