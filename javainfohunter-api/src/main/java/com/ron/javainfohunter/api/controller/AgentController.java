package com.ron.javainfohunter.api.controller;

import com.ron.javainfohunter.api.dto.ApiResponse;
import com.ron.javainfohunter.api.dto.response.AgentExecutionResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import com.ron.javainfohunter.api.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Agent Controller
 *
 * REST API endpoints for agent execution monitoring and statistics.
 * Provides visibility into AI agent performance and metrics.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Monitoring", description = "Agent execution monitoring and statistics")
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/executions")
    @Operation(summary = "Get agent executions", description = "Get paginated list of agent execution records")
    public ResponseEntity<ApiResponse<Page<AgentExecutionResponse>>> getExecutions(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting agent executions - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<AgentExecutionResponse> executions = agentService.getExecutions(pageable);

        return ResponseEntity.ok(ApiResponse.success(executions));
    }

    @GetMapping("/executions/{id}")
    @Operation(summary = "Get execution details", description = "Get detailed information about a specific agent execution")
    public ResponseEntity<ApiResponse<AgentExecutionResponse>> getExecutionById(
            @Parameter(description = "Execution ID")
            @PathVariable Long id) {

        log.debug("Getting agent execution by ID: {}", id);

        AgentExecutionResponse execution = agentService.getExecutionById(id);
        return ResponseEntity.ok(ApiResponse.success(execution));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get statistics", description = "Get aggregated agent execution statistics")
    public ResponseEntity<ApiResponse<AgentStatsResponse>> getStats() {
        log.debug("Getting agent execution statistics");

        AgentStatsResponse stats = agentService.getExecutionStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
