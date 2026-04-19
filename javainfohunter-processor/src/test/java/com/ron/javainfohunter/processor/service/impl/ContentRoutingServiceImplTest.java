package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.service.ResultAggregator;
import com.ron.javainfohunter.dto.RawContentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentRoutingServiceImplTest {

    private static final String CONTENT_HASH = "test-hash-123";

    @Mock
    private ResultAggregator resultAggregator;

    @Mock
    private AgentProcessor mockProcessor;

    private ProcessorProperties properties;
    private ContentRoutingServiceImpl service;
    private RawContentMessage contentMessage;

    @BeforeEach
    void setUp() throws Exception {
        properties = new ProcessorProperties();
        contentMessage = new RawContentMessage();
        contentMessage.setContentHash(CONTENT_HASH);
        contentMessage.setTitle("Test Article");
    }

    @Test
    void shutdown_shouldBeCalledOnDestroy() throws Exception {
        service = new ContentRoutingServiceImpl(properties, List.of(), resultAggregator);
        ExecutorService executor = getExecutorViaReflection(service);
        assertNotNull(executor);
        assertFalse(executor.isShutdown());

        service.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    void routeToAgents_shouldUseCountDownLatchNotPolling() throws Exception {
        when(mockProcessor.getAgentType()).thenReturn(AgentResult.AgentType.ANALYSIS);
        when(mockProcessor.process(any())).thenReturn(
                AgentResult.builder()
                        .agentType(AgentResult.AgentType.ANALYSIS)
                        .contentHash(CONTENT_HASH)
                        .success(true)
                        .durationMs(50L)
                        .build()
        );

        service = new ContentRoutingServiceImpl(properties, List.of(mockProcessor), resultAggregator);
        when(resultAggregator.aggregate(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.routeToAgents(contentMessage);

        // Should complete quickly via CountDownLatch, not polling with Thread.sleep
        Optional<Map<AgentResult.AgentType, AgentResult>> result =
                service.awaitResults(CONTENT_HASH, 5000);
        assertTrue(result.isPresent());
        assertTrue(result.get().containsKey(AgentResult.AgentType.ANALYSIS));
    }

    @Test
    void cleanupStaleResults_shouldRemoveCompletedEntries() throws Exception {
        service = new ContentRoutingServiceImpl(properties, List.of(), resultAggregator);

        // Manually insert a completed entry
        Map<String, Map<AgentResult.AgentType, AgentResult>> processingResults = getMapViaReflection(service, "processingResults");
        Map<String, CompletableFuture<Void>> completionFutures = getMapViaReflection(service, "completionFutures");

        Map<AgentResult.AgentType, AgentResult> dummyResults = new ConcurrentHashMap<>();
        processingResults.put("stale-hash", dummyResults);
        CompletableFuture<Void> completedFuture = CompletableFuture.completedFuture(null);
        completionFutures.put("stale-hash", completedFuture);

        // Run cleanup
        service.cleanupStaleResults();

        // Stale entry should be removed
        assertFalse(processingResults.containsKey("stale-hash"));
        assertFalse(completionFutures.containsKey("stale-hash"));
    }

    private ExecutorService getExecutorViaReflection(ContentRoutingServiceImpl target) throws Exception {
        Field field = ContentRoutingServiceImpl.class.getDeclaredField("executor");
        field.setAccessible(true);
        return (ExecutorService) field.get(target);
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> getMapViaReflection(ContentRoutingServiceImpl target, String fieldName) throws Exception {
        Field field = ContentRoutingServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<K, V>) field.get(target);
    }
}
