package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * System Metrics Response DTO
 *
 * Response model for system performance metrics.
 * Provides time-series data for request rates, error rates, and connection counts.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsResponse {

    /**
     * System uptime in seconds
     */
    private Long uptime;

    /**
     * Request rate metrics over time
     */
    private List<MetricPoint> requestRate;

    /**
     * Error rate metrics over time
     */
    private List<MetricPoint> errorRate;

    /**
     * Number of active connections
     */
    private Integer activeConnections;

    /**
     * Metric point data structure
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricPoint {
        /**
         * Timestamp of the metric
         */
        private String timestamp;

        /**
         * Metric value
         */
        private Double value;
    }
}
