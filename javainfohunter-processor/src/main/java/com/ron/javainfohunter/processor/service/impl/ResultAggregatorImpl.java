package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.processor.dto.RawContentMessage;
import com.ron.javainfohunter.processor.service.ResultAggregator;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link ResultAggregator} that aggregates results from multiple AI agents.
 *
 * <p>This service extracts and combines data from analysis, summary, and classification agents
 * to create a unified {@link ProcessedContentMessage}. All operations are performed asynchronously
 * using {@link CompletableFuture} to support high-throughput message processing.</p>
 *
 * <p><b>Agent Result Extraction:</b></p>
 * <ul>
 *   <li><b>ANALYSIS:</b> sentimentScore, sentimentLabel, importanceScore, keywords</li>
 *   <li><b>SUMMARY:</b> summary text</li>
 *   <li><b>CLASSIFICATION:</b> category, tags</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultAggregatorImpl implements ResultAggregator {

    @Override
    public CompletionStage<ProcessedContentMessage> aggregate(
            RawContentMessage content,
            Map<AgentResult.AgentType, AgentResult> agentResults) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Aggregating results for content: hash={}, agents={}",
                    content.getContentHash(), agentResults.keySet());

            // Extract analysis results
            AgentResult analysisResult = agentResults.get(AgentResult.AgentType.ANALYSIS);
            Double sentimentScore = null;
            String sentimentLabel = null;
            Double importanceScore = null;
            List<String> keywords = null;

            if (analysisResult != null && analysisResult.isSuccess() && analysisResult.getResult() != null) {
                Map<String, Object> result = analysisResult.getResult();
                sentimentScore = extractDouble(result, "sentimentScore");
                sentimentLabel = extractString(result, "sentimentLabel");
                importanceScore = extractDouble(result, "importanceScore");
                keywords = extractList(result, "keywords");
            }

            // Extract summary results
            AgentResult summaryResult = agentResults.get(AgentResult.AgentType.SUMMARY);
            String summary = null;

            if (summaryResult != null && summaryResult.isSuccess() && summaryResult.getResult() != null) {
                summary = extractString(summaryResult.getResult(), "summary");
            }

            // Extract classification results
            AgentResult classificationResult = agentResults.get(AgentResult.AgentType.CLASSIFICATION);
            String category = null;
            List<String> tags = null;

            if (classificationResult != null && classificationResult.isSuccess()
                    && classificationResult.getResult() != null) {
                Map<String, Object> result = classificationResult.getResult();
                category = extractString(result, "category");
                tags = extractList(result, "tags");
            }

            // Build the processed content message
            ProcessedContentMessage processedMessage = ProcessedContentMessage.builder()
                    .contentHash(content.getContentHash())
                    .rssSourceId(content.getRssSourceId())
                    .sourceUrl(content.getLink())
                    .title(content.getTitle())
                    .rawContent(content.getRawContent())
                    .summary(summary)
                    .sentimentScore(sentimentScore)
                    .sentimentLabel(sentimentLabel)
                    .keywords(keywords)
                    .category(category)
                    .tags(tags)
                    .importanceScore(importanceScore)
                    .agentResults(agentResults)
                    .processedAt(Instant.now())
                    .build();

            // Log summary info
            log.info("Aggregated processed content: hash={}, summary={}, category={}, sentiment={}",
                    content.getContentHash(),
                    summarizeString(summary, 50),
                    category,
                    sentimentLabel);

            return processedMessage;
        });
    }

    @Override
    public CompletionStage<Void> store(ProcessedContentMessage message) {
        return CompletableFuture.runAsync(() -> {
            log.info("Storing processed content: hash={}, title={}",
                    message.getContentHash(),
                    message.getTitle());

            // TODO: Generate embedding for similarity search
            // TODO: Store to database (news table)
            // TODO: Publish to downstream queue (processed.content.queue)

            log.debug("Processed content stored successfully: hash={}", message.getContentHash());
        });
    }

    /**
     * Extract a Double value from a result map.
     *
     * @param result the result map
     * @param key the key to extract
     * @return the Double value, or null if not present or invalid
     */
    @Nullable
    protected Double extractDouble(Map<String, Object> result, String key) {
        Object value = result.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Extract a String value from a result map.
     *
     * @param result the result map
     * @param key the key to extract
     * @return the String value, or null if not present
     */
    @Nullable
    protected String extractString(Map<String, Object> result, String key) {
        Object value = result.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : null;
    }

    /**
     * Extract a List of Strings from a result map.
     *
     * @param result the result map
     * @param key the key to extract
     * @return the List value, or null if not present or invalid
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected List<String> extractList(Map<String, Object> result, String key) {
        Object value = result.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }

    /**
     * Create a summarized version of a string for logging.
     *
     * @param text the text to summarize
     * @param maxLength the maximum length
     * @return the summarized string
     */
    private String summarizeString(String text, int maxLength) {
        if (text == null) {
            return "N/A";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
