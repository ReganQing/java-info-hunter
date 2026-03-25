package com.ron.javainfohunter.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * System Status Response DTO
 *
 * Response model for system health and status information.
 * Provides overall system health and component status.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {

    /**
     * Overall system status (HEALTHY, DEGRADED, DOWN)
     */
    private String status;

    /**
     * Status timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * Total RSS sources
     */
    private Long totalRssSources;

    /**
     * Active RSS sources
     */
    private Long activeRssSources;

    /**
     * Total news articles
     */
    private Long totalNews;

    /**
     * Pending processing items
     */
    private Long pendingProcessing;

    /**
     * Service status map with detailed service information
     */
    private Map<String, Object> services;

    /**
     * Uptime in seconds
     */
    private Long uptimeSeconds;

    /**
     * Version information
     */
    private String version;

    /**
     * Resource usage information
     */
    private ResourceUsageResponse resources;

    /**
     * System metrics
     */
    private SystemMetricsResponse metrics;
}
