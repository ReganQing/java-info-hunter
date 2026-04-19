package com.ron.javainfohunter.ai.tool.impl;

import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.tool.observation.ErrorContext;
import com.ron.javainfohunter.ai.tool.observation.ErrorType;
import com.ron.javainfohunter.ai.tool.observation.ToolObservation;
import com.ron.javainfohunter.ai.tool.observation.ToolStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CoordinatorTools Tests")
class CoordinatorToolsTest {

    private CoordinatorTools tools;
    private AgentManager agentManager;

    @BeforeEach
    void setUp() {
        agentManager = mock(AgentManager.class);
        tools = new CoordinatorTools(agentManager);
    }

    // ==================== delegateTask ====================

    @Test
    @DisplayName("delegateTask - valid input returns SUCCESS")
    void delegateTask_validInput_returnsSuccess() {
        ToolObservation result = tools.delegateTask(
            "task-1", "analyze content",
            "{\"worker1\":\"summarize\",\"worker2\":\"classify\"}",
            60, true
        );

        assertEquals(ToolStatus.SUCCESS, result.status());
        assertTrue(result.summary().contains("task-1"));
        assertTrue(result.details().contains("2"));
        assertTrue(result.details().contains("60"));
    }

    @Test
    @DisplayName("delegateTask - invalid JSON returns FAILURE with VALIDATION error")
    void delegateTask_invalidJson_returnsFailure() {
        ToolObservation result = tools.delegateTask(
            "task-1", "analyze", "{invalid}", 30, false
        );

        assertEquals(ToolStatus.FAILURE, result.status());
        assertNotNull(result.error());
        assertEquals(ErrorType.VALIDATION, result.error().type());
        assertFalse(result.error().retryable());
    }

    // ==================== checkWorkerStatus ====================

    @Test
    @DisplayName("checkWorkerStatus - valid IDs returns SUCCESS with per-worker status")
    void checkWorkerStatus_validIds_returnsSuccess() {
        ToolObservation result = tools.checkWorkerStatus(
            "[\"worker1\",\"worker2\"]"
        );

        assertEquals(ToolStatus.SUCCESS, result.status());
        assertTrue(result.summary().contains("2"));
        assertTrue(result.details().contains("worker1"));
        assertTrue(result.details().contains("worker2"));
    }

    @Test
    @DisplayName("checkWorkerStatus - invalid JSON returns FAILURE")
    void checkWorkerStatus_invalidJson_returnsFailure() {
        ToolObservation result = tools.checkWorkerStatus("[invalid]");

        assertEquals(ToolStatus.FAILURE, result.status());
        assertNotNull(result.error());
        assertEquals(ErrorType.VALIDATION, result.error().type());
    }

    // ==================== aggregateResults ====================

    @Test
    @DisplayName("aggregateResults - returns SUCCESS")
    void aggregateResults_returnsSuccess() {
        ToolObservation result = tools.aggregateResults(
            "[{\"worker1\":\"result1\"}]"
        );

        assertEquals(ToolStatus.SUCCESS, result.status());
        assertNotNull(result.summary());
    }

    // ==================== getAvailableWorkers ====================

    @Test
    @DisplayName("getAvailableWorkers - returns workers from AgentManager")
    void getAvailableWorkers_returnsWorkersFromAgentManager() {
        when(agentManager.getAgentNames()).thenReturn(
            List.of("crawler-agent", "analysis-agent", "summary-agent")
        );

        ToolObservation result = tools.getAvailableWorkers();

        assertEquals(ToolStatus.SUCCESS, result.status());
        assertTrue(result.summary().contains("3"));
        assertTrue(result.details().contains("crawler-agent"));
        assertTrue(result.details().contains("analysis-agent"));
        assertEquals("3", result.artifacts().get("workerCount"));
    }

    @Test
    @DisplayName("getAvailableWorkers - empty agent list returns SUCCESS with 0 workers")
    void getAvailableWorkers_emptyList_returnsSuccess() {
        when(agentManager.getAgentNames()).thenReturn(List.of());

        ToolObservation result = tools.getAvailableWorkers();

        assertEquals(ToolStatus.SUCCESS, result.status());
        assertTrue(result.summary().contains("0"));
        assertEquals("0", result.artifacts().get("workerCount"));
    }

    @Test
    @DisplayName("getAvailableWorkers - AgentManager exception returns FAILURE")
    void getAvailableWorkers_agentManagerException_returnsFailure() {
        when(agentManager.getAgentNames()).thenThrow(new RuntimeException("service unavailable"));

        ToolObservation result = tools.getAvailableWorkers();

        assertEquals(ToolStatus.FAILURE, result.status());
        assertNotNull(result.error());
        assertEquals(ErrorType.FATAL, result.error().type());
        assertTrue(result.error().retryable());
    }
}
