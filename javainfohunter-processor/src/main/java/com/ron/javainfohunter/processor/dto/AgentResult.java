package com.ron.javainfohunter.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Result of a single agent execution during content processing.
 *
 * <p>This DTO captures the outcome of an individual agent (analysis, summary,
 * classification) processing a piece of content, including the result data,
 * success status, and timing information.</p>
 *
 * <p><b>Agent Types:</b></p>
 * <ul>
 *   <li>{@link AgentType#ANALYSIS} - Deep content analysis (sentiment, topics)</li>
 *   <li>{@link AgentType#SUMMARY} - Text summarization</li>
 *   <li>{@link AgentType#CLASSIFICATION} - Content categorization and tagging</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /**
     * The type of agent that produced this result.
     */
    private AgentType agentType;

    /**
     * Content hash identifier (SHA-256) for deduplication and correlation.
     */
    private String contentHash;

    /**
     * The agent's output data.
     * Structure varies by agent type:
     * <ul>
     *   <li>ANALYSIS: sentimentScore, topics, entities</li>
     *   <li>SUMMARY: summaryText, keyPoints</li>
     *   <li>CLASSIFICATION: category, tags, confidence</li>
     * </ul>
     */
    private Map<String, Object> result;

    /**
     * Whether the agent execution completed successfully.
     */
    @Builder.Default
    private boolean success = false;

    /**
     * Error message if the agent execution failed.
     */
    private String errorMessage;

    /**
     * Execution duration in milliseconds.
     */
    @Builder.Default
    private long durationMs = 0;

    /**
     * Timestamp when the agent processing completed.
     */
    private Instant processedAt;

    /**
     * Agent type enumeration representing the different agent specializations.
     */
    public enum AgentType {
        /**
         * Content analysis agent - performs sentiment analysis, topic extraction,
         * and entity recognition.
         */
        ANALYSIS,

        /**
         * Summary agent - generates concise summaries and extracts key points.
         */
        SUMMARY,

        /**
         * Classification agent - categorizes content and assigns relevant tags.
         */
        CLASSIFICATION
    }

    /**
     * Create a successful agent result.
     *
     * @param agentType the agent type
     * @param contentHash the content hash
     * @param result the result data
     * @param durationMs execution duration in milliseconds
     * @return a successful AgentResult
     */
    public static AgentResult success(AgentType agentType, String contentHash,
                                     Map<String, Object> result, long durationMs) {
        return AgentResult.builder()
                .agentType(agentType)
                .contentHash(contentHash)
                .result(result)
                .success(true)
                .durationMs(durationMs)
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Create a failed agent result.
     *
     * @param agentType the agent type
     * @param contentHash the content hash
     * @param errorMessage the error message
     * @param durationMs execution duration in milliseconds
     * @return a failed AgentResult
     */
    public static AgentResult failure(AgentType agentType, String contentHash,
                                     String errorMessage, long durationMs) {
        return AgentResult.builder()
                .agentType(agentType)
                .contentHash(contentHash)
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .processedAt(Instant.now())
                .build();
    }
}
