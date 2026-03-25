package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resource Usage Response DTO
 *
 * Response model for system resource usage information.
 * Provides CPU, memory, and disk usage statistics.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUsageResponse {

    /**
     * CPU usage percentage (0-100)
     */
    private Double cpuPercent;

    /**
     * Memory used in bytes
     */
    private Long memoryUsed;

    /**
     * Total memory in bytes
     */
    private Long memoryTotal;

    /**
     * Disk used in bytes
     */
    private Long diskUsed;

    /**
     * Total disk in bytes
     */
    private Long diskTotal;
}
