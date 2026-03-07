package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.response.AgentExecutionResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Agent Service Interface
 *
 * Business logic for agent execution monitoring and statistics.
 * Handles querying agent executions and computing metrics.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public interface AgentService {

    /**
     * Get paginated list of agent executions
     *
     * @param pageable Pagination information
     * @return Paginated list of agent executions
     */
    Page<AgentExecutionResponse> getExecutions(Pageable pageable);

    /**
     * Get agent execution by ID
     *
     * @param id Execution ID
     * @return Agent execution response
     * @throws com.ron.javainfohunter.api.exception.ResourceNotFoundException if execution not found
     */
    AgentExecutionResponse getExecutionById(Long id);

    /**
     * Get agent execution statistics
     *
     * @return Aggregated statistics
     */
    AgentStatsResponse getExecutionStats();
}
