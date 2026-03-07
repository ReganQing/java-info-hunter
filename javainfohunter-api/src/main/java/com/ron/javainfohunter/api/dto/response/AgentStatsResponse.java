package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent Statistics Response DTO
 *
 * Response model for agent execution statistics.
 * Provides aggregated metrics and performance data.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatsResponse {

    /**
     * Total number of executions
     */
    private Long totalExecutions;

    /**
     * Number of currently running executions
     */
    private Long runningExecutions;

    /**
     * Number of completed executions
     */
    private Long completedExecutions;

    /**
     * Number of failed executions
     */
    private Long failedExecutions;

    /**
     * Average duration in milliseconds
     */
    private Double averageDurationMs;

    /**
     * Executions by agent type
     */
    private Map<String, Long> executionsByAgentType;

    /**
     * Executions by status
     */
    private Map<String, Long> executionsByStatus;

    /**
     * Total tokens used
     */
    private Long totalTokensUsed;

    /**
     * Total estimated cost in USD
     */
    private String totalEstimatedCostUsd;
}
