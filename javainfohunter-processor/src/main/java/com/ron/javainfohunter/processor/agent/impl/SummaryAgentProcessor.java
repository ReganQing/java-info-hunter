package com.ron.javainfohunter.processor.agent.impl;

import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.util.ContentPreprocessor;
import com.ron.javainfohunter.dto.RawContentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Summary Agent Processor implementation with chunked map-reduce processing.
 *
 * <p>This processor generates concise summaries of RSS content using AI.
 * For long content that exceeds the token limit, it uses a map-reduce strategy:</p>
 * <ol>
 *   <li><b>Map</b>: Split content into chunks and summarize each independently</li>
 *   <li><b>Reduce</b>: Merge chunk summaries into a final coherent summary</li>
 * </ol>
 *
 * <p><b>Processing Flow:</b></p>
 * <ol>
 *   <li>Estimate token count for the content</li>
 *   <li>If within limit: process in a single API call</li>
 *   <li>If over limit: split into chunks, summarize each, merge results</li>
 * </ol>
 *
 * <p><b>Configuration:</b></p>
 * <pre>
 * javainfohunter:
 *   processor:
 *     agents:
 *       summary:
 *         enabled: true
 *         timeout: 120000
 *         max-summary-length: 500
 *         max-content-tokens: 8000
 *         chunk-token-limit: 6000
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
     * System prompt for merging chunk summaries (reduce step).
     */
    private static final String MERGE_SYSTEM_PROMPT = """
            You are a professional text summarization expert.
            You will receive multiple summaries of different parts of the same article.
            Your task is to merge them into a single coherent, non-redundant summary.

            Rules:
            - Eliminate duplicate information across chunk summaries
            - Preserve all unique key points from each chunk
            - Maintain logical flow and coherence
            - Keep the same output format as individual summaries

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

            String rawContent = content.getRawContent();
            ProcessorProperties.SummaryConfig summaryConfig = properties.getAgents().getSummary();
            int maxContentTokens = summaryConfig.getMaxContentTokens();
            int estimatedTokens = ContentPreprocessor.estimateTokenCount(rawContent != null ? rawContent : "");

            String summary;
            if (estimatedTokens <= maxContentTokens) {
                log.debug("Content within token limit ({} tokens), processing directly", estimatedTokens);
                summary = summarizeDirectly(content);
            } else {
                log.info("Content exceeds token limit ({} > {}), using chunked map-reduce processing for guid={}",
                    estimatedTokens, maxContentTokens, content.getGuid());
                summary = summarizeWithChunking(content, summaryConfig);
            }

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
     * Summarize content in a single API call (content within token limit).
     */
    private String summarizeDirectly(RawContentMessage content) {
        String prompt = buildSummaryPrompt(content, null);
        return chatService.chat(SYSTEM_PROMPT, prompt);
    }

    /**
     * Summarize content using map-reduce chunked processing.
     *
     * <p>Step 1 (Map): Split content into chunks and summarize each.
     * Step 2 (Reduce): Merge all chunk summaries into a final summary.</p>
     */
    private String summarizeWithChunking(RawContentMessage content,
                                          ProcessorProperties.SummaryConfig summaryConfig) {
        String rawContent = content.getRawContent();
        int chunkTokenLimit = summaryConfig.getChunkTokenLimit();

        // Step 1: Split into chunks
        List<String> chunks = ContentPreprocessor.splitIntoChunks(rawContent, chunkTokenLimit);
        log.info("Split content into {} chunks for guid={}", chunks.size(), content.getGuid());

        // Step 2: Summarize each chunk (Map phase)
        List<String> chunkSummaries = chunks.stream()
            .map(chunk -> {
                String chunkPrompt = buildChunkSummaryPrompt(content.getTitle(), chunk);
                return chatService.chat(SYSTEM_PROMPT, chunkPrompt);
            })
            .collect(Collectors.toList());

        log.debug("Completed map phase: {} chunk summaries generated for guid={}",
            chunkSummaries.size(), content.getGuid());

        // Step 3: Merge chunk summaries (Reduce phase)
        if (chunkSummaries.size() == 1) {
            return chunkSummaries.get(0);
        }

        return mergeChunkSummaries(chunkSummaries);
    }

    /**
     * Merge multiple chunk summaries into a single coherent summary (Reduce step).
     */
    private String mergeChunkSummaries(List<String> chunkSummaries) {
        StringBuilder mergePrompt = new StringBuilder();
        mergePrompt.append("The following are summaries of different parts of the same article. ");
        mergePrompt.append("Please merge them into a single coherent summary.\n\n");

        for (int i = 0; i < chunkSummaries.size(); i++) {
            mergePrompt.append("--- Part ").append(i + 1).append(" ---\n");
            mergePrompt.append(chunkSummaries.get(i)).append("\n\n");
        }

        return chatService.chat(MERGE_SYSTEM_PROMPT, mergePrompt.toString());
    }

    /**
     * Build the summary prompt with content and configuration.
     *
     * @param content       the raw content message
     * @param maxContentTokens if not null, truncate content to this token limit
     * @return formatted prompt for the AI service
     */
    private String buildSummaryPrompt(RawContentMessage content, Integer maxContentTokens) {
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
            int tokenLimit = maxContentTokens != null
                ? maxContentTokens
                : properties.getAgents().getSummary().getMaxContentTokens();
            String contentToSummarize = ContentPreprocessor.truncateToTokenLimit(rawContent, tokenLimit);
            prompt.append("Content:\n").append(contentToSummarize);
        } else {
            prompt.append("Content: [No content available]");
        }

        return prompt.toString();
    }

    /**
     * Build a summary prompt for a single content chunk.
     */
    private String buildChunkSummaryPrompt(String title, String chunk) {
        int maxLength = properties.getAgents().getSummary().getMaxSummaryLength();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please summarize this section of an article.\n");
        prompt.append("Target length: approximately ").append(maxLength).append(" characters.\n");
        prompt.append("This is part of a longer article. Focus on the key points in this section.\n\n");

        if (title != null && !title.isBlank()) {
            prompt.append("Article Title: ").append(title).append("\n\n");
        }

        prompt.append("Content Section:\n").append(chunk);

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
