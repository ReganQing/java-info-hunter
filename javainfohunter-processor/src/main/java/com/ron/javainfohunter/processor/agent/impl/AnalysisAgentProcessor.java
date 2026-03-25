package com.ron.javainfohunter.processor.agent.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Agent processor for content analysis using the analysis-agent.
 *
 * <p>This processor performs deep content analysis including:</p>
 * <ul>
 *   <li>Sentiment analysis (score and label)</li>
 *   <li>Topic extraction</li>
 *   <li>Keyword identification</li>
 *   <li>Importance scoring</li>
 * </ul>
 *
 * <p>The processor communicates with the AI service's AnalysisAgent
 * via the AgentManager, sending a structured prompt and parsing
 * the JSON response into structured results.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "javainfohunter.processor.agents.analysis.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AnalysisAgentProcessor implements AgentProcessor {

    private final AgentManager agentManager;
    private final ProcessorProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Process raw content with the analysis agent.
     *
     * @param content the raw content message to process
     * @return agent processing result with sentiment, topics, keywords, and importance
     */
    @Override
    public AgentResult process(RawContentMessage content) {
        long startTime = System.currentTimeMillis();

        try {
            // Build analysis prompt
            String prompt = buildAnalysisPrompt(content);

            // Get the analysis agent and execute
            Optional<BaseAgent> agentOpt = agentManager.getAgent("analysis-agent");
            if (agentOpt.isEmpty()) {
                log.error("Analysis agent not registered in AgentManager");
                return AgentResult.builder()
                        .agentType(getAgentType())
                        .contentHash(content.getContentHash())
                        .success(false)
                        .errorMessage("Analysis agent not available")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .processedAt(Instant.now())
                        .build();
            }

            BaseAgent agent = agentOpt.get();
            log.debug("Executing analysis agent for content hash={}", content.getContentHash());

            // Execute the agent using thread-safe concurrent method
            String response = agent.runConcurrent(prompt);

            // Parse response
            Map<String, Object> result = parseAnalysisResponse(response);

            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .result(result)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Analysis failed for hash={}", content.getContentHash(), e);
            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();
        }
    }

    @Override
    public AgentResult.AgentType getAgentType() {
        return AgentResult.AgentType.ANALYSIS;
    }

    /**
     * Build the analysis prompt for the agent.
     *
     * @param content the raw content to analyze
     * @return formatted prompt string
     */
    private String buildAnalysisPrompt(RawContentMessage content) {
        // Truncate content to 5000 characters to avoid token limits
        String truncatedContent = content.getRawContent() != null && content.getRawContent().length() > 5000
                ? content.getRawContent().substring(0, 5000) + "..."
                : (content.getRawContent() != null ? content.getRawContent() : "");

        return String.format("""
                Please analyze the following news article and provide:

                1. Sentiment analysis (score from -1.0 to 1.0, and label: positive/negative/neutral)
                2. Key topics/themes (extract 3-5 main topics)
                3. Important keywords (extract 5-10 keywords)
                4. Importance score (0.0 to 1.0 based on news significance)

                Title: %s

                Content: %s

                Respond in JSON format:
                {
                  "sentiment": {"score": 0.5, "label": "positive"},
                  "topics": ["AI", "Technology", "Innovation"],
                  "keywords": ["artificial intelligence", "breakthrough", "research"],
                  "importance": 0.8
                }
                """,
                content.getTitle() != null ? content.getTitle() : "Untitled",
                truncatedContent
        );
    }

    /**
     * Parse the analysis response from the agent.
     *
     * @param response the raw response string from the agent
     * @return parsed result map with sentiment, topics, keywords, and importance
     */
    private Map<String, Object> parseAnalysisResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Try to extract JSON from the response (in case there's extra text)
            String jsonPart = extractJson(response);

            if (jsonPart != null) {
                // Parse JSON response
                Map<String, Object> parsed = objectMapper.readValue(jsonPart, new TypeReference<>() {});

                // Extract sentiment
                if (parsed.containsKey("sentiment") && parsed.get("sentiment") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sentiment = (Map<String, Object>) parsed.get("sentiment");
                    result.put("sentimentScore", sentiment.getOrDefault("score", 0.0));
                    result.put("sentimentLabel", sentiment.getOrDefault("label", "neutral"));
                } else {
                    result.put("sentimentScore", 0.0);
                    result.put("sentimentLabel", "neutral");
                }

                // Extract topics
                if (parsed.containsKey("topics") && parsed.get("topics") instanceof List) {
                    result.put("topics", parsed.get("topics"));
                } else {
                    result.put("topics", List.of());
                }

                // Extract keywords
                if (parsed.containsKey("keywords") && parsed.get("keywords") instanceof List) {
                    result.put("keywords", parsed.get("keywords"));
                } else {
                    result.put("keywords", List.of());
                }

                // Extract importance
                if (parsed.containsKey("importance")) {
                    result.put("importance", parsed.get("importance"));
                } else {
                    result.put("importance", 0.5);
                }
            } else {
                // Fallback if JSON parsing fails
                log.warn("Could not extract JSON from analysis response, using fallback");
                result = createFallbackResult(response);
            }

        } catch (Exception e) {
            log.warn("Failed to parse analysis response as JSON, using fallback: {}", e.getMessage());
            result = createFallbackResult(response);
        }

        // Always include raw response for debugging
        result.put("rawResponse", response);

        return result;
    }

    /**
     * Extract JSON from a potentially mixed response.
     *
     * @param response the response string that may contain JSON
     * @return extracted JSON string, or null if not found
     */
    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        // Look for JSON object boundaries
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return null;
    }

    /**
     * Create a fallback result when JSON parsing fails.
     *
     * @param response the raw response string
     * @return fallback result map
     */
    private Map<String, Object> createFallbackResult(String response) {
        Map<String, Object> result = new HashMap<>();
        result.put("sentimentScore", 0.0);
        result.put("sentimentLabel", "neutral");
        result.put("topics", List.of());
        result.put("keywords", List.of());
        result.put("importance", 0.5);
        return result;
    }
}
