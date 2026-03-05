package com.ron.javainfohunter.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregated message containing fully processed content with all agent results.
 *
 * <p>This DTO represents the final output of the content processor module,
 * combining the original raw content with results from all agents
 * (analysis, summary, classification).</p>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * Raw Content Queue → Content Processor → Processed Content Queue → Database/Publisher
 * </pre>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * Raw Content Queue → Content Processor → Processed Content Queue → Database/Publisher
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see AgentResult
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedContentMessage {

    /**
     * Content hash identifier (SHA-256) for deduplication.
     */
    private String contentHash;

    /**
     * RSS source ID (database primary key).
     */
    private Long rssSourceId;

    /**
     * Original source URL of the content.
     */
    private String sourceUrl;

    /**
     * Content title.
     */
    private String title;

    /**
     * Raw content text (HTML stripped).
     */
    private String rawContent;

    /**
     * Generated summary of the content.
     */
    private String summary;

    /**
     * Sentiment score (-1.0 to 1.0, where negative = negative sentiment,
     * positive = positive sentiment).
     */
    private Double sentimentScore;

    /**
     * Human-readable sentiment label (e.g., "POSITIVE", "NEGATIVE", "NEUTRAL").
     */
    private String sentimentLabel;

    /**
     * Extracted keywords from the content.
     */
    private List<String> keywords;

    /**
     * Primary category classification.
     */
    private String category;

    /**
     * Tags assigned to the content.
     */
    private List<String> tags;

    /**
     * Importance score (0.0 to 1.0) indicating content significance.
     */
    private Double importanceScore;

    /**
     * Vector embedding of the content for similarity search.
     * Array length depends on the embedding model used.
     */
    private float[] embedding;

    /**
     * Individual results from each agent that processed this content.
     * Maps agent type to its result for detailed inspection.
     */
    private Map<AgentResult.AgentType, AgentResult> agentResults;

    /**
     * Timestamp when processing was completed.
     */
    private Instant processedAt;

    /**
     * Create a minimal processed content message with required fields.
     *
     * @param contentHash the content hash
     * @param rssSourceId the RSS source ID
     * @param sourceUrl the source URL
     * @param title the content title
     * @param rawContent the raw content
     * @return a ProcessedContentMessage with minimal fields set
     */
    public static ProcessedContentMessage create(String contentHash, Long rssSourceId,
                                                String sourceUrl, String title, String rawContent) {
        return ProcessedContentMessage.builder()
                .contentHash(contentHash)
                .rssSourceId(rssSourceId)
                .sourceUrl(sourceUrl)
                .title(title)
                .rawContent(rawContent)
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Add an agent result to this message.
     *
     * @param agentResult the agent result to add
     */
    public void addAgentResult(AgentResult agentResult) {
        if (agentResults == null) {
            agentResults = new java.util.HashMap<>();
        }
        agentResults.put(agentResult.getAgentType(), agentResult);

        // Extract common fields from agent results
        if (agentResult.isSuccess() && agentResult.getResult() != null) {
            Map<String, Object> result = agentResult.getResult();

            switch (agentResult.getAgentType()) {
                case ANALYSIS:
                    if (result.containsKey("sentimentScore")) {
                        this.sentimentScore = ((Number) result.get("sentimentScore")).doubleValue();
                    }
                    if (result.containsKey("sentimentLabel")) {
                        this.sentimentLabel = (String) result.get("sentimentLabel");
                    }
                    if (result.containsKey("keywords")) {
                        this.keywords = (List<String>) result.get("keywords");
                    }
                    break;

                case SUMMARY:
                    if (result.containsKey("summary")) {
                        this.summary = (String) result.get("summary");
                    }
                    break;

                case CLASSIFICATION:
                    if (result.containsKey("category")) {
                        this.category = (String) result.get("category");
                    }
                    if (result.containsKey("tags")) {
                        this.tags = (List<String>) result.get("tags");
                    }
                    if (result.containsKey("importanceScore")) {
                        this.importanceScore = ((Number) result.get("importanceScore")).doubleValue();
                    }
                    break;
            }
        }
    }

    /**
     * Check if all agents executed successfully.
     *
     * @return true if all agent results indicate success
     */
    public boolean isAllAgentsSuccessful() {
        if (agentResults == null || agentResults.isEmpty()) {
            return false;
        }
        return agentResults.values().stream().allMatch(AgentResult::isSuccess);
    }

    /**
     * Get the total processing duration across all agents.
     *
     * @return total duration in milliseconds
     */
    public long getTotalProcessingDuration() {
        if (agentResults == null) {
            return 0;
        }
        return agentResults.values().stream()
                .mapToLong(AgentResult::getDurationMs)
                .sum();
    }
}
