package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.ai.service.EmbeddingService;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service that handles all transactional database operations for result aggregation.
 *
 * <p>This service is separated from {@link ResultAggregatorImpl} to ensure proper
 * @Transactional semantics. The transactional method is called synchronously from
 * within the async task, ensuring the transaction context is properly bound to the
 * executing thread.</p>
 *
 * <p><b>Transaction Strategy:</b></p>
 * <ul>
 *   <li>Update RawContent status to PROCESSING first (prevents race conditions)</li>
 *   <li>Generate embedding vector</li>
 *   <li>Create and save News entity</li>
 *   <li>Update RawContent status to COMPLETED</li>
 *   <li>Register after-commit callback for message publishing</li>
 *   <li>On rollback: status changes are automatically reverted</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
@Slf4j
class TransactionalStoreService {

    private final EmbeddingService embeddingService;
    private final RawContentRepository rawContentRepository;
    private final NewsRepository newsRepository;

    /**
     * Store processed content in a single transaction.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Finds the RawContent by contentHash</li>
     *   <li>Updates RawContent status to PROCESSING (prevents race conditions)</li>
     *   <li>Generates embedding vector</li>
     *   <li>Creates and saves News entity</li>
     *   <li>Updates RawContent status to COMPLETED</li>
     *   <li>Registers after-commit callback for message publishing</li>
     * </ol>
     *
     * @param message the processed content message
     * @param messagePublisher callback to publish message after commit
     * @throws IllegalStateException if RawContent is not found
     */
    @org.springframework.transaction.annotation.Transactional
    public void storeProcessedContent(ProcessedContentMessage message,
                                      MessagePublisher messagePublisher) {
        try {
            log.info("Storing processed content: hash={}, title={}",
                    message.getContentHash(), message.getTitle());

            // 1. Find RawContent by contentHash
            RawContent rawContent = rawContentRepository
                    .findByContentHash(message.getContentHash())
                    .orElseThrow(() -> new IllegalStateException(
                            "RawContent not found for hash: " + message.getContentHash()));

            // 2. Update status to PROCESSING first (prevents race condition)
            rawContent.setProcessingStatus(RawContent.ProcessingStatus.PROCESSING);
            rawContentRepository.save(rawContent);

            // 3. Generate embedding vector
            String textForEmbedding = buildTextForEmbedding(message);
            float[] embedding = embeddingService.embed(textForEmbedding);
            log.debug("Generated embedding with {} dimensions", embedding.length);

            // 4. Create News entity
            News news = createNewsEntity(message, rawContent);

            // 5. Save to database
            News savedNews = newsRepository.save(news);
            log.info("Saved news with ID: {}", savedNews.getId());

            // 6. Update RawContent status to COMPLETED
            rawContent.setProcessingStatus(RawContent.ProcessingStatus.COMPLETED);
            rawContentRepository.save(rawContent);

            // 7. Register transaction synchronization for message publishing
            // This ensures message is only published after transaction commits
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                messagePublisher.publish(savedNews, message);
                            }
                        }
                );
            } else {
                // Fallback for testing or non-transactional contexts
                messagePublisher.publish(savedNews, message);
            }

            log.debug("Processed content stored successfully: hash={}", message.getContentHash());

        } catch (Exception e) {
            log.error("Failed to store processed content: hash={}",
                    message.getContentHash(), e);
            // Update status to FAILED
            updateRawContentStatus(message.getContentHash(), RawContent.ProcessingStatus.FAILED);
            throw e;
        }
    }

    /**
     * Build text for embedding generation.
     */
    private String buildTextForEmbedding(ProcessedContentMessage message) {
        String summary = message.getSummary() != null ? message.getSummary() : "";
        return message.getTitle() + " " + summary;
    }

    /**
     * Create News entity from ProcessedContentMessage.
     */
    private News createNewsEntity(ProcessedContentMessage message, RawContent rawContent) {
        return News.builder()
                .rawContent(rawContent)
                .title(message.getTitle())
                .summary(message.getSummary())
                .fullContent(rawContent.getRawContent())
                .sentiment(parseSentiment(message.getSentimentLabel()))
                .sentimentScore(convertToBigDecimal(message.getSentimentScore()))
                .importanceScore(convertToBigDecimal(message.getImportanceScore()))
                .category(message.getCategory())
                .tags(message.getTags() != null ? message.getTags().toArray(new String[0]) : null)
                .keywords(message.getKeywords() != null ? message.getKeywords().toArray(new String[0]) : null)
                .language("zh")
                .readingTimeMinutes(calculateReadingTime(rawContent.getRawContent()))
                .isPublished(false)
                .build();
    }

    /**
     * Parse sentiment label to News.Sentiment enum.
     */
    private News.Sentiment parseSentiment(String sentimentLabel) {
        if (sentimentLabel == null) {
            return News.Sentiment.NEUTRAL;
        }
        return switch (sentimentLabel.toLowerCase()) {
            case "positive" -> News.Sentiment.POSITIVE;
            case "negative" -> News.Sentiment.NEGATIVE;
            default -> News.Sentiment.NEUTRAL;
        };
    }

    /**
     * Convert Double to BigDecimal with 2 decimal places.
     */
    private BigDecimal convertToBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate reading time based on content length.
     * Average reading speed: 300 characters/minute for Chinese.
     */
    private Integer calculateReadingTime(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        int characterCount = content.length();
        return Math.max(1, characterCount / 300);
    }

    /**
     * Update RawContent processing status.
     */
    @Transactional
    private void updateRawContentStatus(String contentHash, RawContent.ProcessingStatus status) {
        rawContentRepository.findByContentHash(contentHash).ifPresent(rawContent -> {
            rawContent.setProcessingStatus(status);
            rawContentRepository.save(rawContent);
        });
    }
}
