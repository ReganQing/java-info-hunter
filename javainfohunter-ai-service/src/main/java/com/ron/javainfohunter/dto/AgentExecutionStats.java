package com.ron.javainfohunter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent Execution Statistics DTO
 *
 * Data Transfer Object for agent execution statistics.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionStats {
    private String agentId;
    private Long totalExecutions;
    private Long completedCount;
    private Long failedCount;
    private Double avgDurationSeconds;
    private Long totalTokens;
    private Double totalCostUsd;
}
