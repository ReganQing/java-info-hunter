package com.ron.javainfohunter.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.response.AgentExecutionResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import com.ron.javainfohunter.api.exception.ResourceNotFoundException;
import com.ron.javainfohunter.api.service.AgentService;
import com.ron.javainfohunter.entity.AgentExecution;
import com.ron.javainfohunter.repository.AgentExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Service Implementation
 *
 * Implements business logic for agent execution monitoring.
 * Provides statistics and execution tracking features.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentExecutionRepository agentExecutionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<AgentExecutionResponse> getExecutions(Pageable pageable) {
        log.debug("Getting agent executions - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<AgentExecution> executions = agentExecutionRepository.findAll(pageable);
        return executions.map(this::toResponse);
    }

    @Override
    public AgentExecutionResponse getExecutionById(Long id) {
        log.debug("Getting agent execution by ID: {}", id);

        AgentExecution execution = agentExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Execution", id));

        return toResponse(execution);
    }

    @Override
    public AgentStatsResponse getExecutionStats() {
        log.debug("Getting agent execution statistics");

        long total = agentExecutionRepository.count();
        long running = agentExecutionRepository.countByStatus(AgentExecution.ExecutionStatus.RUNNING);
        long completed = agentExecutionRepository.countByStatus(AgentExecution.ExecutionStatus.COMPLETED);
        long failed = agentExecutionRepository.countByStatus(AgentExecution.ExecutionStatus.FAILED);

        Double avgDuration = agentExecutionRepository.getAverageDuration();
        long totalTokens = agentExecutionRepository.getTotalTokens();
        BigDecimal totalCost = agentExecutionRepository.getTotalCost();
        Map<String, Long> byAgentType = agentExecutionRepository.getCountByAgentType();

        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("RUNNING", running);
        byStatus.put("COMPLETED", completed);
        byStatus.put("FAILED", failed);

        return AgentStatsResponse.builder()
                .totalExecutions(total)
                .runningExecutions(running)
                .completedExecutions(completed)
                .failedExecutions(failed)
                .averageDurationMs(avgDuration != null ? avgDuration : 0.0)
                .executionsByAgentType(byAgentType)
                .executionsByStatus(byStatus)
                .totalTokensUsed(totalTokens)
                .totalEstimatedCostUsd(totalCost != null ? totalCost.toString() : "0")
                .build();
    }

    /**
     * Convert AgentExecution entity to AgentExecutionResponse DTO
     */
    private AgentExecutionResponse toResponse(AgentExecution execution) {
        Map<String, Object> inputData = parseJson(execution.getInputData());
        Map<String, Object> outputData = parseJson(execution.getOutputData());

        return AgentExecutionResponse.builder()
                .id(execution.getId())
                .agentId(execution.getAgentId())
                .agentName(execution.getAgentName())
                .agentType(execution.getAgentType())
                .executionId(execution.getExecutionId())
                .taskType(execution.getTaskType())
                .status(execution.getStatus().name())
                .inputData(inputData)
                .outputData(outputData)
                .errorMessage(execution.getErrorTrace())
                .totalSteps(execution.getTotalSteps())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .durationMilliseconds(execution.getDurationMilliseconds())
                .tokensUsed(execution.getTokensUsed())
                .estimatedCostUsd(execution.getEstimatedCostUsd() != null ?
                        execution.getEstimatedCostUsd().toString() : null)
                .build();
    }

    /**
     * Parse JSON string to Map
     */
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
