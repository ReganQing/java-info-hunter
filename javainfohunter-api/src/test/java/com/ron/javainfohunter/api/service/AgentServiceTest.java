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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(agentExecutionRepository.count()).thenReturn(4L);
        when(agentExecutionRepository.countByStatus(AgentExecution.ExecutionStatus.RUNNING)).thenReturn(1L);
        when(agentExecutionRepository.countByStatus(AgentExecution.ExecutionStatus.COMPLETED)).thenReturn(2L);
        when(agentExecutionRepository.countByStatus(AgentExecution.ExecutionStatus.FAILED)).thenReturn(1L);
        when(agentExecutionRepository.getAverageDuration()).thenReturn(37500.0);
        when(agentExecutionRepository.getTotalTokens()).thenReturn(2500L);
        when(agentExecutionRepository.getTotalCost()).thenReturn(new BigDecimal("0.05"));
        lenient().when(agentExecutionRepository.getCountByAgentTypeRaw())
                .thenReturn(List.of(new Object[]{"ToolCallAgent", 2L}, new Object[]{"ReActAgent", 1L}));

        AgentStatsResponse response = agentService.getExecutionStats();

        assertNotNull(response);
        assertEquals(4L, response.getTotalExecutions());
        assertEquals(1L, response.getRunningExecutions());
        assertEquals(2L, response.getCompletedExecutions());
        assertEquals(1L, response.getFailedExecutions());
        assertEquals(37500.0, response.getAverageDurationMs());
        assertEquals(2500L, response.getTotalTokensUsed());

        verify(agentExecutionRepository, never()).findAll();
    }
}
