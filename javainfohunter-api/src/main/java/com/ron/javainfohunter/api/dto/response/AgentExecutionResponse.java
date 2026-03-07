package com.ron.javainfohunter.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Agent Execution Response DTO
 *
 * Response model for agent execution information.
 * Contains execution details, status, and performance metrics.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionResponse {

    /**
     * Primary key
     */
    private Long id;

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * Agent name
     */
    private String agentName;

    /**
     * Agent type
     */
    private String agentType;

    /**
     * Unique execution identifier
     */
    private String executionId;

    /**
     * Task type
     */
    private String taskType;

    /**
     * Execution status
     */
    private String status;

    /**
     * Input data
     */
    private Map<String, Object> inputData;

    /**
     * Output data
     */
    private Map<String, Object> outputData;

    /**
     * Error message
     */
    private String errorMessage;

    /**
     * Total steps taken
     */
    private Integer totalSteps;

    /**
     * Execution start time
     */
    private Instant startTime;

    /**
     * Execution end time
     */
    private Instant endTime;

    /**
     * Duration in milliseconds
     */
    private Integer durationMilliseconds;

    /**
     * Tokens used
     */
    private Integer tokensUsed;

    /**
     * Estimated cost in USD
     */
    private String estimatedCostUsd;
}
