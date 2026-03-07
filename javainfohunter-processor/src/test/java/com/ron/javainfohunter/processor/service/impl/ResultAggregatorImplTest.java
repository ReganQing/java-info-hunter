package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.ai.service.EmbeddingService;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.processor.dto.RawContentMessage;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ResultAggregatorImpl}
 *
 * Tests the store() method which handles:
 * - Vector generation via EmbeddingService
 * - Database persistence via NewsRepository
 * - Message publishing via RabbitTemplate
 * - RawContent status updates
 * - Transaction synchronization
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class ResultAggregatorImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RawContentRepository rawContentRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Executor aggregatorExecutor;

    @InjectMocks
    private ResultAggregatorImpl resultAggregator;

    @InjectMocks
    private TransactionalStoreService transactionalStoreService;

    private RawContent testRawContent;
    private ProcessedContentMessage testMessage;
    private float[] testEmbedding;

    @BeforeEach
    void setUp() {
        // Inject mocks into TransactionalStoreService
        transactionalStoreService = new TransactionalStoreService(
                embeddingService,
                rawContentRepository,
                newsRepository
        );

        // Inject TransactionalStoreService and RabbitTemplate into ResultAggregatorImpl
        resultAggregator = new ResultAggregatorImpl(
                transactionalStoreService,
                rabbitTemplate,
                aggregatorExecutor
        );

        // Create test embedding vector (1536 dimensions as per OpenAI)
        testEmbedding = new float[1536];
        Arrays.fill(testEmbedding, 0.1f);

        // Create test RawContent
        testRawContent = RawContent.builder()
                .id(1L)
                .contentHash("abc123hash")
                .title("Test Article")
                .rawContent("This is the full content of the test article.")
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .crawlDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Create test ProcessedContentMessage
        testMessage = ProcessedContentMessage.builder()
                .contentHash("abc123hash")
                .rssSourceId(1L)
                .sourceUrl("https://example.com/article")
                .title("Test Article")
                .rawContent("This is the full content of the test article.")
                .summary("This is a summary of the test article.")
                .sentimentScore(0.75)
                .sentimentLabel("positive")
                .importanceScore(0.85)
                .category("technology")
                .tags(Arrays.asList("AI", "machine-learning", "test"))
                .keywords(Arrays.asList("artificial intelligence", "testing"))
                .processedAt(Instant.now())
                .build();

        // Configure executor to run tasks synchronously for testing
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(aggregatorExecutor).execute(any(Runnable.class));
    }

    @Test
    void store_ShouldGenerateEmbeddingAndSaveNews_WhenValidMessage() {
        // Given
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then - Verify embedding was generated
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingService).embed(textCaptor.capture());
        assertThat(textCaptor.getValue()).contains("Test Article");
        assertThat(textCaptor.getValue()).contains("This is a summary of the test article.");

        // Then - Verify News was saved
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        News savedNews = newsCaptor.getValue();
        assertThat(savedNews.getTitle()).isEqualTo("Test Article");
        assertThat(savedNews.getSummary()).isEqualTo("This is a summary of the test article.");
        assertThat(savedNews.getSentiment()).isEqualTo(News.Sentiment.POSITIVE);
        assertThat(savedNews.getSentimentScore()).isEqualTo(new BigDecimal("0.75"));
        assertThat(savedNews.getImportanceScore()).isEqualTo(new BigDecimal("0.85"));
        assertThat(savedNews.getCategory()).isEqualTo("technology");
        assertThat(savedNews.getTags()).containsExactly("AI", "machine-learning", "test");
        assertThat(savedNews.getKeywords()).containsExactly("artificial intelligence", "testing");
        assertThat(savedNews.getLanguage()).isEqualTo("zh");
        assertThat(savedNews.isPublished()).isFalse();

        // Then - Verify RawContent status was updated twice (PROCESSING -> COMPLETED)
        ArgumentCaptor<RawContent> rawContentCaptor = ArgumentCaptor.forClass(RawContent.class);
        verify(rawContentRepository, times(2)).save(rawContentCaptor.capture());
        List<RawContent> savedRawContents = rawContentCaptor.getAllValues();
        // In non-transactional test, the same object is modified in place
        // So both captures show the final state (COMPLETED)
        assertThat(savedRawContents.get(1).getProcessingStatus())
                .isEqualTo(RawContent.ProcessingStatus.COMPLETED);

        // Then - Verify message was published
        verify(rabbitTemplate).convertAndSend(
                eq("processor.direct"),
                eq("processed.content"),
                any(Object.class)
        );
    }

    @Test
    void store_ShouldHandleNullSentimentLabel_AsNeutral() {
        // Given
        testMessage.setSentimentLabel(null);
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getSentiment()).isEqualTo(News.Sentiment.NEUTRAL);
    }

    @Test
    void store_ShouldParseSentimentCaseInsensitively() {
        // Given
        testMessage.setSentimentLabel("NEGATIVE");
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getSentiment()).isEqualTo(News.Sentiment.NEGATIVE);
    }

    @Test
    void store_ShouldCalculateReadingTimeBasedOnContentLength() {
        // Given
        String longContent = "a".repeat(600); // Should result in 2 minutes
        testRawContent.setRawContent(longContent);
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getReadingTimeMinutes()).isEqualTo(2);
    }

    @Test
    void store_ShouldThrowException_WhenRawContentNotFound() {
        // Given
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                resultAggregator.store(testMessage).toCompletableFuture().join()
        ).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RawContent not found");

        verify(newsRepository, never()).save(any(News.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void store_ShouldUpdateStatusToFailed_WhenExceptionOccurs() {
        // Given
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString()))
                .thenThrow(new RuntimeException("Embedding service failed"));
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertThatThrownBy(() ->
                resultAggregator.store(testMessage).toCompletableFuture().join()
        ).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        // Verify status was updated to FAILED (after PROCESSING)
        ArgumentCaptor<RawContent> rawContentCaptor = ArgumentCaptor.forClass(RawContent.class);
        verify(rawContentRepository, times(2)).save(rawContentCaptor.capture());
        List<RawContent> savedRawContents = rawContentCaptor.getAllValues();
        // In non-transactional test, the same object is modified in place
        // The final state is FAILED
        assertThat(savedRawContents.get(1).getProcessingStatus())
                .isEqualTo(RawContent.ProcessingStatus.FAILED);

        verify(newsRepository, never()).save(any(News.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void store_ShouldHandleNullScoresGracefully() {
        // Given
        testMessage.setSentimentScore(null);
        testMessage.setImportanceScore(null);
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getSentimentScore()).isNull();
        assertThat(newsCaptor.getValue().getImportanceScore()).isNull();
    }

    @Test
    void store_ShouldHandleEmptyCollections() {
        // Given
        testMessage.setTags(Collections.emptyList());
        testMessage.setKeywords(Collections.emptyList());
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getTags()).isNullOrEmpty();
        assertThat(newsCaptor.getValue().getKeywords()).isNullOrEmpty();
    }

    @Test
    void store_ShouldUseRawContent_WhenProvided() {
        // Given
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getFullContent())
                .isEqualTo("This is the full content of the test article.");
    }

    @Test
    void store_ShouldPublishProcessedContentDTO_WithCorrectFields() {
        // Given
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq("processor.direct"),
                eq("processed.content"),
                messageCaptor.capture()
        );

        Object publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage).isInstanceOf(Map.class); // Jackson converts to Map
        Map<String, Object> messageMap = (Map<String, Object>) publishedMessage;
        assertThat(messageMap.get("newsId")).isEqualTo(100L);
        assertThat(messageMap.get("contentHash")).isEqualTo("abc123hash");
        assertThat(messageMap.get("title")).isEqualTo("Test Article");
        assertThat(messageMap.get("category")).isEqualTo("technology");
        assertThat(messageMap.get("importanceScore")).isEqualTo(BigDecimal.valueOf(0.85));
    }

    @Test
    void store_ShouldRoundScoresToTwoDecimalPlaces() {
        // Given
        testMessage.setSentimentScore(0.756789);
        testMessage.setImportanceScore(0.854321);
        when(rawContentRepository.findByContentHash("abc123hash"))
                .thenReturn(Optional.of(testRawContent));
        when(embeddingService.embed(anyString())).thenReturn(testEmbedding);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(100L);
            return news;
        });
        when(rawContentRepository.save(any(RawContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        resultAggregator.store(testMessage).toCompletableFuture().join();

        // Then
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());
        assertThat(newsCaptor.getValue().getSentimentScore())
                .isEqualTo(new BigDecimal("0.76"));
        assertThat(newsCaptor.getValue().getImportanceScore())
                .isEqualTo(new BigDecimal("0.85"));
    }

    @Test
    void aggregate_ShouldCreateProcessedContentMessage_WithAllFields() {
        // Given
        RawContentMessage rawContentMessage = RawContentMessage.builder()
                .contentHash("hash123")
                .rssSourceId(1L)
                .link("https://example.com")
                .title("Test Title")
                .rawContent("Test content")
                .build();

        Map<com.ron.javainfohunter.processor.dto.AgentResult.AgentType,
                com.ron.javainfohunter.processor.dto.AgentResult> agentResults = new HashMap<>();

        // Analysis result
        Map<String, Object> analysisData = new HashMap<>();
        analysisData.put("sentimentScore", 0.8);
        analysisData.put("sentimentLabel", "positive");
        analysisData.put("importanceScore", 0.9);
        analysisData.put("keywords", Arrays.asList("keyword1", "keyword2"));
        agentResults.put(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.ANALYSIS,
                com.ron.javainfohunter.processor.dto.AgentResult.builder()
                        .agentType(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.ANALYSIS)
                        .success(true)
                        .result(analysisData)
                        .build());

        // Summary result
        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("summary", "Test summary");
        agentResults.put(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.SUMMARY,
                com.ron.javainfohunter.processor.dto.AgentResult.builder()
                        .agentType(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.SUMMARY)
                        .success(true)
                        .result(summaryData)
                        .build());

        // Classification result
        Map<String, Object> classificationData = new HashMap<>();
        classificationData.put("category", "technology");
        classificationData.put("tags", Arrays.asList("AI", "ML"));
        agentResults.put(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.CLASSIFICATION,
                com.ron.javainfohunter.processor.dto.AgentResult.builder()
                        .agentType(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.CLASSIFICATION)
                        .success(true)
                        .result(classificationData)
                        .build());

        // When
        ProcessedContentMessage result = resultAggregator.aggregate(rawContentMessage, agentResults)
                .toCompletableFuture().join();

        // Then
        assertThat(result.getContentHash()).isEqualTo("hash123");
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getSummary()).isEqualTo("Test summary");
        assertThat(result.getSentimentScore()).isEqualTo(0.8);
        assertThat(result.getSentimentLabel()).isEqualTo("positive");
        assertThat(result.getImportanceScore()).isEqualTo(0.9);
        assertThat(result.getKeywords()).containsExactly("keyword1", "keyword2");
        assertThat(result.getCategory()).isEqualTo("technology");
        assertThat(result.getTags()).containsExactly("AI", "ML");
    }

    @Test
    void aggregate_ShouldHandleMissingAgentResults() {
        // Given
        RawContentMessage rawContentMessage = RawContentMessage.builder()
                .contentHash("hash123")
                .rssSourceId(1L)
                .link("https://example.com")
                .title("Test Title")
                .rawContent("Test content")
                .build();

        Map<com.ron.javainfohunter.processor.dto.AgentResult.AgentType,
                com.ron.javainfohunter.processor.dto.AgentResult> agentResults = new HashMap<>();

        // When
        ProcessedContentMessage result = resultAggregator.aggregate(rawContentMessage, agentResults)
                .toCompletableFuture().join();

        // Then
        assertThat(result.getContentHash()).isEqualTo("hash123");
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getSummary()).isNull();
        assertThat(result.getSentimentScore()).isNull();
        assertThat(result.getSentimentLabel()).isNull();
        assertThat(result.getImportanceScore()).isNull();
        assertThat(result.getKeywords()).isNull();
        assertThat(result.getCategory()).isNull();
        assertThat(result.getTags()).isNull();
    }

    @Test
    void aggregate_ShouldHandleFailedAgentResults() {
        // Given
        RawContentMessage rawContentMessage = RawContentMessage.builder()
                .contentHash("hash123")
                .rssSourceId(1L)
                .link("https://example.com")
                .title("Test Title")
                .rawContent("Test content")
                .build();

        Map<com.ron.javainfohunter.processor.dto.AgentResult.AgentType,
                com.ron.javainfohunter.processor.dto.AgentResult> agentResults = new HashMap<>();

        // Failed analysis result
        agentResults.put(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.ANALYSIS,
                com.ron.javainfohunter.processor.dto.AgentResult.builder()
                        .agentType(com.ron.javainfohunter.processor.dto.AgentResult.AgentType.ANALYSIS)
                        .success(false)
                        .errorMessage("Analysis failed")
                        .build());

        // When
        ProcessedContentMessage result = resultAggregator.aggregate(rawContentMessage, agentResults)
                .toCompletableFuture().join();

        // Then
        assertThat(result.getContentHash()).isEqualTo("hash123");
        assertThat(result.getSentimentScore()).isNull();
        assertThat(result.getSentimentLabel()).isNull();
        assertThat(result.getImportanceScore()).isNull();
        assertThat(result.getKeywords()).isNull();
    }
}
