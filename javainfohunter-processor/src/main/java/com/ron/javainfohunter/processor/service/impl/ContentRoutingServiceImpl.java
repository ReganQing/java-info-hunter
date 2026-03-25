package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.dto.RawContentMessage;
import com.ron.javainfohunter.processor.service.ContentRoutingService;
import com.ron.javainfohunter.processor.service.ResultAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of content routing service that distributes
 * processing to AI agents using parallel execution.
 *
 * <p>This service manages the complete workflow of content processing:</p>
 * <ol>
 *   <li>Receives raw content messages from the crawler</li>
 *   <li>Validates and prepares processing context</li>
 *   <li>Submits parallel tasks to enabled agents using virtual threads</li>
 *   <li>Aggregates results as they complete</li>
 *   <li>Provides result retrieval and cleanup methods</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Results stored in thread-safe ConcurrentHashMap</li>
 *   <li>Agent results use EnumMap for type-safe storage</li>
 *   <li>Virtual thread executor for high-concurrency parallel execution</li>
 * </ul>
 *
 * <p><b>Agent Filtering:</b></p>
 * Only agents that are enabled in {@link ProcessorProperties} will be invoked.
 * This allows runtime control over which processing steps are performed.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see com.ron.javainfohunter.processor.agent.AgentProcessor
 * @see com.ron.javainfohunter.processor.config.ProcessorProperties
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRoutingServiceImpl implements ContentRoutingService {

    private final ProcessorProperties properties;
    private final List<AgentProcessor> agentProcessors;
    private final ResultAggregator resultAggregator;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Store results for aggregation by content hash.
     *
     * Outer map: content hash → Inner map of agent results
     * Inner map: agent type → agent result
     *
     * Thread-safe: ConcurrentHashMap for concurrent access
     * Type-safe: EnumMap for agent type enumeration
     */
    private final Map<String, Map<AgentResult.AgentType, AgentResult>> processingResults =
            new ConcurrentHashMap<>();

    /**
     * Track completion futures for awaiting results.
     * Map: content hash → future that completes when all agents finish
     */
    private final Map<String, CompletableFuture<Void>> completionFutures =
            new ConcurrentHashMap<>();

    @Override
    public void routeToAgents(RawContentMessage contentMessage) {
        if (contentMessage == null) {
            throw new IllegalArgumentException("Content message cannot be null");
        }

        String contentHash = contentMessage.getContentHash();
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash cannot be null or blank");
        }

        log.debug("Routing content hash={} to agents", contentHash);

        // Check if processor module is enabled
        if (!properties.isEnabled()) {
            log.info("Processor module is disabled, skipping processing for hash={}", contentHash);
            return;
        }

        // Initialize result map for this content
        Map<AgentResult.AgentType, AgentResult> results = new EnumMap<>(AgentResult.AgentType.class);
        processingResults.put(contentHash, results);

        // Filter agents based on configuration
        List<AgentProcessor> enabledAgents = agentProcessors.stream()
                .filter(this::isAgentEnabled)
                .toList();

        if (enabledAgents.isEmpty()) {
            log.warn("No agents enabled for processing hash={}", contentHash);
            completeProcessing(contentHash);
            return;
        }

        log.debug("Processing hash={} with {} enabled agents", contentHash, enabledAgents.size());

        // Create completion future for this content
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        completionFutures.put(contentHash, completionFuture);

        // Submit parallel agent processing tasks
        for (AgentProcessor processor : enabledAgents) {
            executor.submit(() -> processWithAgent(processor, contentMessage, results));
        }

        // Complete the future when all agents finish
        // This is a simple implementation; a more robust version would track completion count
        CompletableFuture.runAsync(() -> {
            try {
                // Poll for completion - in production, use a countdown latch or similar
                while (results.size() < enabledAgents.size()) {
                    Thread.sleep(50);
                }
                completionFuture.complete(null);

                // Aggregate and store results when all agents complete
                log.debug("All agents completed for hash={}, starting aggregation", contentHash);
                resultAggregator.aggregate(contentMessage, results)
                    .thenCompose(resultAggregator::store)
                    .whenComplete((v, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to store processed content for hash={}", contentHash, throwable);
                        } else {
                            log.info("Successfully stored processed content for hash={}", contentHash);
                        }
                        // Clean up results after storing
                        removeResults(contentHash);
                    });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                completionFuture.completeExceptionally(e);
            }
        }, executor);

        log.info("Submitted {} agents for processing hash={}", enabledAgents.size(), contentHash);
    }

    @Override
    public Map<AgentResult.AgentType, AgentResult> getResults(String contentHash) {
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash cannot be null or blank");
        }

        Map<AgentResult.AgentType, AgentResult> results = processingResults.get(contentHash);
        if (results == null) {
            log.warn("No results found for content hash={}", contentHash);
            return Map.of();
        }

        // Return a copy to prevent concurrent modification
        return Map.copyOf(results);
    }

    @Override
    public Optional<Map<AgentResult.AgentType, AgentResult>> awaitResults(String contentHash, long timeoutMs) {
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash cannot be null or blank");
        }

        CompletableFuture<Void> future = completionFutures.get(contentHash);
        if (future == null) {
            log.warn("No completion future found for content hash={}", contentHash);
            return Optional.empty();
        }

        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return Optional.of(getResults(contentHash));
        } catch (TimeoutException e) {
            log.warn("Timeout waiting for results for content hash={}", contentHash);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error waiting for results for content hash={}", contentHash, e);
            return Optional.empty();
        }
    }

    @Override
    public Map<AgentResult.AgentType, AgentResult> removeResults(String contentHash) {
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash cannot be null or blank");
        }

        Map<AgentResult.AgentType, AgentResult> removed = processingResults.remove(contentHash);
        completionFutures.remove(contentHash);

        if (removed != null && !removed.isEmpty()) {
            log.info("Removed results for content hash={}: {} agent results",
                    contentHash, removed.size());
        }

        return removed != null ? removed : Map.of();
    }

    /**
     * Process content with a single agent.
     *
     * <p>This method runs in a virtual thread and handles the complete
     * lifecycle of a single agent processing task.</p>
     *
     * @param processor the agent processor to invoke
     * @param contentMessage the content to process
     * @param results the result map to store results in
     */
    private void processWithAgent(AgentProcessor processor, RawContentMessage contentMessage,
                                   Map<AgentResult.AgentType, AgentResult> results) {
        String contentHash = contentMessage.getContentHash();
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Starting agent {} for hash={}", processor.getAgentType(), contentHash);

            AgentResult result = processor.process(contentMessage);
            results.put(processor.getAgentType(), result);

            long duration = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                log.info("Agent {} completed for hash={} in {}ms",
                        processor.getAgentType(), contentHash, duration);
            } else {
                log.warn("Agent {} failed for hash={} in {}ms: {}",
                        processor.getAgentType(), contentHash, duration, result.getErrorMessage());
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Record failure
            AgentResult failureResult = AgentResult.builder()
                    .agentType(processor.getAgentType())
                    .contentHash(contentHash)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(duration)
                    .build();

            results.put(processor.getAgentType(), failureResult);

            log.error("Agent {} threw exception for hash={} in {}ms",
                    processor.getAgentType(), contentHash, duration, e);
        }
    }

    /**
     * Check if an agent is enabled based on configuration.
     *
     * @param processor the agent processor to check
     * @return true if the agent is enabled, false otherwise
     */
    private boolean isAgentEnabled(AgentProcessor processor) {
        return switch (processor.getAgentType()) {
            case ANALYSIS -> properties.getAgents().getAnalysis().isEnabled();
            case SUMMARY -> properties.getAgents().getSummary().isEnabled();
            case CLASSIFICATION -> properties.getAgents().getClassification().isEnabled();
        };
    }

    /**
     * Mark processing as complete for a content hash.
     *
     * <p>This is called when no agents are enabled or when processing
     * needs to be terminated early.</p>
     *
     * @param contentHash the content hash to complete
     */
    private void completeProcessing(String contentHash) {
        CompletableFuture<Void> future = completionFutures.remove(contentHash);
        if (future != null && !future.isDone()) {
            future.complete(null);
        }
    }

}
