package com.ron.javainfohunter.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.response.AgentExecutionResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import com.ron.javainfohunter.api.exception.ResourceNotFoundException;
import com.ron.javainfohunter.api.service.impl.AgentServiceImpl;
import com.ron.javainfohunter.entity.AgentExecution;
import com.ron.javainfohunter.repository.AgentExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent Service Tests")
class AgentServiceTest {

    @Mock
    private AgentExecutionRepository agentExecutionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AgentServiceImpl agentService;

    private AgentExecution testExecution;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        testExecution = AgentExecution.builder()
                .id(1L)
                .agentId("crawler-agent")
                .agentName("Crawler Agent")
                .agentType("ToolCallAgent")
                .executionId("exec-123")
                .taskType("crawl")
                .status(AgentExecution.ExecutionStatus.COMPLETED)
                .inputData("{\"url\": \"https://example.com\"}")
                .outputData("{\"articles\": 10}")
                .totalSteps(5)
                .startTime(now)
                .endTime(now.plusSeconds(30))
                .durationMilliseconds(30000)
                .tokensUsed(1000)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("Get executions - success")
    void testGetExecutions_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AgentExecution> executionPage = new PageImpl<>(List.of(testExecution));
        when(agentExecutionRepository.findAll(pageable)).thenReturn(executionPage);

        Page<AgentExecutionResponse> response = agentService.getExecutions(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(testExecution.getAgentId(), response.getContent().get(0).getAgentId());

        verify(agentExecutionRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Get execution by ID - found")
    void testGetExecutionById_Found() {
        when(agentExecutionRepository.findById(1L)).thenReturn(Optional.of(testExecution));

        AgentExecutionResponse response = agentService.getExecutionById(1L);

        assertNotNull(response);
        assertEquals(testExecution.getId(), response.getId());
        assertEquals(testExecution.getExecutionId(), response.getExecutionId());

        verify(agentExecutionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get execution by ID - not found")
    void testGetExecutionById_NotFound() {
        when(agentExecutionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> agentService.getExecutionById(999L));

        verify(agentExecutionRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Get execution stats - success")
    void testGetExecutionStats_Success() {
        List<AgentExecution> completedExecutions = Arrays.asList(
                testExecution,
                AgentExecution.builder()
                        .id(2L)
                        .agentType("ToolCallAgent")
                        .status(AgentExecution.ExecutionStatus.COMPLETED)
                        .durationMilliseconds(45000)
                        .tokensUsed(1500)
                        .build()
        );

        List<AgentExecution> runningExecutions = Arrays.asList(
                AgentExecution.builder()
                        .id(3L)
                        .agentType("ReActAgent")
                        .status(AgentExecution.ExecutionStatus.RUNNING)
                        .build()
        );

        List<AgentExecution> failedExecutions = Arrays.asList(
                AgentExecution.builder()
                        .id(4L)
                        .agentType("BaseAgent")
                        .status(AgentExecution.ExecutionStatus.FAILED)
                        .build()
        );

        when(agentExecutionRepository.findAll()).thenReturn(Arrays.asList(
                testExecution,
                completedExecutions.get(1),
                runningExecutions.get(0),
                failedExecutions.get(0)
        ));

        AgentStatsResponse response = agentService.getExecutionStats();

        assertNotNull(response);
        assertEquals(4L, response.getTotalExecutions());
        assertEquals(1L, response.getRunningExecutions());
        assertEquals(2L, response.getCompletedExecutions());
        assertEquals(1L, response.getFailedExecutions());
        assertEquals(37500.0, response.getAverageDurationMs());

        verify(agentExecutionRepository, times(1)).findAll();
    }
}
