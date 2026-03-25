package com.ron.javainfohunter.processor.agent.impl;

import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Summary Agent Processor implementation.
 *
 * <p>This processor generates concise summaries of RSS content using AI.
 * It extracts the key points and main ideas from the article content
 * while maintaining accuracy and readability.</p>
 *
 * <p><b>Processing Flow:</b></p>
 * <ol>
 *   <li>Extract title and content from the raw message</li>
 *   <li>Build a summary prompt with configured max length</li>
 *   <li>Call AI service to generate the summary</li>
 *   <li>Return the result with timing information</li>
 * </ol>
 *
 * <p><b>Configuration:</b></p>
 * <pre>
 * javainfohunter:
 *   processor:
 *     agents:
 *       summary:
 *         enabled: true
 *         timeout: 30000
 *         max-summary-length: 500
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see com.ron.javainfohunter.processor.agent.AgentProcessor
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "javainfohunter.processor.agents.summary",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SummaryAgentProcessor implements AgentProcessor {

    private final ChatService chatService;
    private final ProcessorProperties properties;

    /**
     * Default maximum summary length if not configured.
     */
    private static final int DEFAULT_MAX_SUMMARY_LENGTH = 500;

    /**
     * System prompt for the summary agent.
     */
    private static final String SYSTEM_PROMPT = """
            You are a professional text summarization expert.

            Your responsibilities:
            - Read and understand the article content
            - Extract core viewpoints and key information
            - Generate a concise, accurate, and readable summary
            - Maintain the logical structure and importance of the original text

            Summary principles:
            1. Accuracy: The summary must be faithful to the original text
            2. Conciseness: Use the fewest words to convey the most information
            3. Completeness: Cover the main points of the article
            4. Readability: Language should be fluent with clear logic

            Output format:
            [One-line summary] The most core content (within 20 words)

            [Key points]
            - Point 1
            - Point 2
            - Point 3

            [Summary paragraph] Complete paragraph summary
            """;

    /**
     * Process raw content to generate a summary.
     *
     * @param content the raw content message containing title and body
     * @return AgentResult with summary in the result map
     */
    @Override
    public AgentResult process(RawContentMessage content) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Generating summary for content: {}", content.getGuid());

            // Build the summary prompt
            String prompt = buildSummaryPrompt(content);

            // Call the AI service to generate summary
            String summary = chatService.chat(SYSTEM_PROMPT, prompt);

            long duration = System.currentTimeMillis() - startTime;

            log.debug("Summary generated for content {} in {}ms", content.getGuid(), duration);

            return AgentResult.success(
                AgentResult.AgentType.SUMMARY,
                content.getContentHash(),
                Map.of(
                    "summary", summary,
                    "title", content.getTitle() != null ? content.getTitle() : ""
                ),
                duration
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error generating summary for content: {}", content.getGuid(), e);

            return AgentResult.failure(
                AgentResult.AgentType.SUMMARY,
                content.getContentHash(),
                "Failed to generate summary: " + e.getMessage(),
                duration
            );
        }
    }

    /**
     * Build the summary prompt with content and configuration.
     *
     * @param content the raw content message
     * @return formatted prompt for the AI service
     */
    private String buildSummaryPrompt(RawContentMessage content) {
        int maxLength = properties.getAgents().getSummary().getMaxSummaryLength();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please generate a summary of the following article content.\n");
        prompt.append("Target length: approximately ").append(maxLength).append(" characters.\n\n");

        // Add title if available
        if (content.getTitle() != null && !content.getTitle().isBlank()) {
            prompt.append("Title: ").append(content.getTitle()).append("\n\n");
        }

        // Add content
        String rawContent = content.getRawContent();
        if (rawContent != null && !rawContent.isBlank()) {
            // Truncate content if too long to avoid token limits
            int maxContentLength = 10000;
            String contentToSummarize = rawContent.length() > maxContentLength
                ? rawContent.substring(0, maxContentLength) + "..."
                : rawContent;

            prompt.append("Content:\n").append(contentToSummarize);
        } else {
            prompt.append("Content: [No content available]");
        }

        return prompt.toString();
    }

    /**
     * Get the agent type identifier.
     *
     * @return AgentType.SUMMARY
     */
    @Override
    public AgentResult.AgentType getAgentType() {
        return AgentResult.AgentType.SUMMARY;
    }

}
