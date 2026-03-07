package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.processor.dto.RawContentMessage;
import com.ron.javainfohunter.processor.service.ResultAggregator;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link ResultAggregator} that aggregates results from multiple AI agents.
 *
 * <p>This service extracts and combines data from analysis, summary, and classification agents
 * to create a unified {@link ProcessedContentMessage}. All operations are performed asynchronously
 * using {@link CompletableFuture} with a dedicated executor to support high-throughput message processing.</p>
 *
 * <p><b>Thread Safety & Transaction Management:</b></p>
 * <ul>
 *   <li>Async operations use a dedicated {@link Executor} instead of default ForkJoinPool</li>
 *   <li>Transactional operations are isolated in {@link TransactionalStoreService}</li>
 *   <li>Message publishing uses {@link TransactionSynchronization} to ensure after-commit execution</li>
 *   <li>Status updates are atomic within the same transaction</li>
 * </ul>
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

    private final TransactionalStoreService transactionalStoreService;
    private final RabbitTemplate rabbitTemplate;
    private final Executor aggregatorExecutor;

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
        }, aggregatorExecutor);
    }

    @Override
    public CompletionStage<Void> store(ProcessedContentMessage message) {
        return CompletableFuture.runAsync(
                () -> transactionalStoreService.storeProcessedContent(message, this::publishProcessedMessage),
                aggregatorExecutor
        );
    }

    /**
     * Publish processed message to downstream queue.
     *
     * <p>This method is called via transaction synchronization after commit,
     * ensuring that messages are only published for successfully committed data.</p>
     *
     * @param news the saved news entity
     * @param message the processed content message
     */
    private void publishProcessedMessage(News news, ProcessedContentMessage message) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("newsId", news.getId());
        dto.put("contentHash", message.getContentHash());
        dto.put("title", news.getTitle());
        dto.put("summary", news.getSummary());
        dto.put("category", news.getCategory());
        dto.put("sentiment", news.getSentiment().toString());
        dto.put("importanceScore", news.getImportanceScore());
        dto.put("processedAt", Instant.now());

        rabbitTemplate.convertAndSend(
                "processor.direct",
                "processed.content",
                dto
        );
        log.debug("Published processed message for news ID: {}", news.getId());
    }

    /**
     * Extract a Double value from a result map.
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
