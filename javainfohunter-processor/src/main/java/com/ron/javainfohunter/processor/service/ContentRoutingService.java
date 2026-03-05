package com.ron.javainfohunter.processor.service;

import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.RawContentMessage;

import java.util.Map;
import java.util.Optional;

/**
 * Service for routing raw content to AI agent processors.
 *
 * <p>This service manages the distribution of incoming content messages
 * to registered agent processors (analysis, summary, classification).
 * Agents execute in parallel using Java 21 virtual threads for optimal
 * throughput.</p>
 *
 * <p><b>Workflow:</b></p>
 * <pre>
 * RawContentMessage → routeToAgents() → Parallel Agent Execution → Results Storage
 *                                                        ↓
 *                                                getResults() / awaitResults()
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * This service uses thread-safe collections for result storage, allowing
 * concurrent access from multiple agent processors and result consumers.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see com.ron.javainfohunter.processor.agent.AgentProcessor
 * @see com.ron.javainfohunter.processor.dto.AgentResult
 */
public interface ContentRoutingService {

    /**
     * Route a raw content message to all registered agent processors.
     *
     * <p>This method submits the content for parallel processing by all
     * enabled agents. Processing happens asynchronously; results are stored
     * and can be retrieved using {@link #getResults(String)} or
     * {@link #awaitResults(String, long)}.</p>
     *
     * <p><b>Agent Execution:</b></p>
     * <ul>
     *   <li>Each agent processes independently in a virtual thread</li>
     *   <li>Results are stored as they complete</li>
     *   <li>Failures in one agent do not affect others</li>
     * </ul>
     *
     * @param contentMessage the raw content message to route to agents
     * @throws IllegalArgumentException if contentMessage is null or has invalid content hash
     */
    void routeToAgents(RawContentMessage contentMessage);

    /**
     * Get current results for a content hash.
     *
     * <p>This returns a snapshot of results at the time of calling.
     * Some agents may still be processing. Use {@link #awaitResults(String, long)}
     * to wait for completion.</p>
     *
     * @param contentHash the content hash identifier
     * @return map of agent type to result, or empty if no results exist yet
     */
    Map<AgentResult.AgentType, AgentResult> getResults(String contentHash);

    /**
     * Wait for all agents to complete processing.
     *
     * <p>This method blocks until all agents have completed or the timeout
     * expires. It returns the complete set of results.</p>
     *
     * @param contentHash the content hash identifier
     * @param timeoutMs maximum time to wait in milliseconds
     * @return optional containing the complete results, or empty if timeout occurred
     */
    Optional<Map<AgentResult.AgentType, AgentResult>> awaitResults(String contentHash, long timeoutMs);

    /**
     * Remove results from storage after processing is complete.
     *
     * <p>This should be called after results have been consumed and persisted
     * to prevent memory leaks. It is safe to call this before all agents
     * have completed; incomplete results will be discarded.</p>
     *
     * @param contentHash the content hash identifier
     * @return the removed results, or empty if none existed
     */
    Map<AgentResult.AgentType, AgentResult> removeResults(String contentHash);

}
