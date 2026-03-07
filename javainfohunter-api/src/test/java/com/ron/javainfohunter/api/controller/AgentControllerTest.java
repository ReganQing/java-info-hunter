package com.ron.javainfohunter.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.response.AgentExecutionResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import com.ron.javainfohunter.api.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AgentController
 */
@WebMvcTest(AgentController.class)
@DisplayName("Agent Controller Tests")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentService agentService;

    private AgentExecutionResponse testExecutionResponse;
    private AgentStatsResponse testStatsResponse;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        testExecutionResponse = AgentExecutionResponse.builder()
                .id(1L)
                .agentId("crawler-agent")
                .agentName("Crawler Agent")
                .agentType("ToolCallAgent")
                .executionId("exec-123")
                .taskType("crawl")
                .status("COMPLETED")
                .totalSteps(5)
                .startTime(now)
                .endTime(now.plusSeconds(30))
                .durationMilliseconds(30000)
                .tokensUsed(1000)
                .build();

        testStatsResponse = AgentStatsResponse.builder()
                .totalExecutions(100L)
                .runningExecutions(5L)
                .completedExecutions(90L)
                .failedExecutions(5L)
                .averageDurationMs(37500.0)
                .totalTokensUsed(50000L)
                .build();
    }

    @Test
    @DisplayName("Get executions - success")
    void testGetExecutions_Success() throws Exception {
        when(agentService.getExecutions(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(testExecutionResponse))
        );

        mockMvc.perform(get("/api/v1/agents/executions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].agentId").value("crawler-agent"))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Get execution by ID - success")
    void testGetExecutionById_Success() throws Exception {
        when(agentService.getExecutionById(1L)).thenReturn(testExecutionResponse);

        mockMvc.perform(get("/api/v1/agents/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.executionId").value("exec-123"));
    }

    @Test
    @DisplayName("Get execution by ID - not found")
    void testGetExecutionById_NotFound() throws Exception {
        when(agentService.getExecutionById(999L))
                .thenThrow(new com.ron.javainfohunter.api.exception.ResourceNotFoundException("Agent Execution", 999L));

        mockMvc.perform(get("/api/v1/agents/executions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Agent Execution not found with id: 999"));
    }

    @Test
    @DisplayName("Get stats - success")
    void testGetStats_Success() throws Exception {
        when(agentService.getExecutionStats()).thenReturn(testStatsResponse);

        mockMvc.perform(get("/api/v1/agents/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalExecutions").value(100))
                .andExpect(jsonPath("$.data.runningExecutions").value(5))
                .andExpect(jsonPath("$.data.completedExecutions").value(90))
                .andExpect(jsonPath("$.data.failedExecutions").value(5));
    }
}
