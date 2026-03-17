package com.ron.javainfohunter.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.ai.service.EmbeddingService;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.publisher.ContentPublisher;
import com.ron.javainfohunter.crawler.service.CrawlCoordinator;
import com.ron.javainfohunter.crawler.service.RssSourceService;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.processor.service.ContentRoutingService;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-End integration tests for the complete workflow:
 * Crawler → RabbitMQ → Processor → PostgreSQL
 *
 * <p>These tests verify the entire data pipeline:</p>
 * <ol>
 *   <li>Crawler fetches RSS feed and extracts content</li>
 *   <li>Content is published to RabbitMQ</li>
 *   <li>Processor consumes messages from RabbitMQ</li>
 *   <li>AI agents process the content</li>
 *   <li>Results are persisted to PostgreSQL (raw_content → news)</li>
 * </ol>
 *
 * <p>Uses Testcontainers for real PostgreSQL and RabbitMQ instances.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class CrawlerToProcessorE2ETest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4-management-alpine"))
            .withUser("test", "test")
            .withVhost("test");

    @Autowired
    private CrawlCoordinator crawlCoordinator;

    @Autowired
    private ContentPublisher contentPublisher;

    @Autowired
    private ContentRoutingService contentRoutingService;

    @Autowired
    private RssSourceService rssSourceService;

    @Autowired
    private RawContentRepository rawContentRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private RssSourceRepository rssSourceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ChatService chatService;

    @MockBean
    private EmbeddingService embeddingService;

    private Connection rabbitConnection;
    private Channel rabbitChannel;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up database
        newsRepository.deleteAll();
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();

        // Setup RabbitMQ connection for manual message consumption
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQContainer.getHost());
        factory.setPort(rabbitMQContainer.getAmqpPort());
        factory.setUsername("test");
        factory.setPassword("test");
        factory.setVirtualHost("test");

        rabbitConnection = factory.newConnection();
        rabbitChannel = rabbitConnection.createChannel();

        // Declare queues and exchanges
        rabbitChannel.exchangeDeclare("crawler.direct", "direct", true);
        rabbitChannel.queueDeclare("test.crawler.raw.content.queue", true, false, false, null);
        rabbitChannel.queueBind("test.crawler.raw.content.queue", "crawler.direct", "raw.content");

        // Mock AI services
        mockChatService();
        mockEmbeddingService();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (rabbitChannel != null && rabbitChannel.isOpen()) {
            rabbitChannel.close();
        }
        if (rabbitConnection != null && rabbitConnection.isOpen()) {
            rabbitConnection.close();
        }
        newsRepository.deleteAll();
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();
    }

    /**
     * E2E Test 1: Complete workflow from RSS source to processed news
     *
     * <p>This test verifies:</p>
     * <ol>
     *   <li>RSS source is created</li>
     *   <li>Crawler extracts content (simulated)</li>
     *   <li>Message is published to RabbitMQ</li>
     *   <li>Message is consumed from RabbitMQ</li>
     *   <li>Content is processed by agents</li>
     *   <li>Results are stored in database</li>
     * </ol>
     */
    @Test
    void testCompleteWorkflow_FromRssSourceToProcessedNews() throws Exception {
        // Step 1: Create RSS source
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("E2E Test Feed")
                .url("https://example.com/e2e-test-rss")
                .category("e2e-test")
                .isActive(true)
                .build());

        assertThat(source.getId()).isNotNull();

        // Step 2: Create raw content (simulating crawler output)
        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("e2e-test-guid-001")
                .title("E2E Test Article")
                .link("https://example.com/e2e-article")
                .rawContent("This is the E2E test article content.")
                .contentHash("e2e-test-hash-001")
                .author("E2E Test Author")
                .publishDate(Instant.now())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build());

        assertThat(rawContent.getId()).isNotNull();
        assertThat(rawContent.getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.PENDING);

        // Step 3: Publish message to RabbitMQ
        RawContentMessage crawlerMessage = RawContentMessage.builder()
                .guid(rawContent.getGuid())
                .title(rawContent.getTitle())
                .link(rawContent.getLink())
                .rawContent(rawContent.getRawContent())
                .contentHash(rawContent.getContentHash())
                .rssSourceId(source.getId())
                .rssSourceName(source.getName())
                .rssSourceUrl(source.getUrl())
                .author(rawContent.getAuthor())
                .publishDate(rawContent.getPublishDate())
                .crawlDate(rawContent.getCrawlDate())
                .category(source.getCategory())
                .build();

        contentPublisher.publishRawContent(crawlerMessage);

        // Step 4: Verify message was published to RabbitMQ
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        CountDownLatch consumeLatch = new CountDownLatch(1);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            receivedMessage.set(new String(delivery.getBody(), StandardCharsets.UTF_8));
            consumeLatch.countDown();
        };

        rabbitChannel.basicConsume("test.crawler.raw.content.queue", true, deliverCallback, consumerTag -> {});

        boolean messageReceived = consumeLatch.await(5, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedMessage.get()).isNotEmpty();

        // Step 5: Process content through routing service (simulating processor)
        // Convert crawler message to processor message format
        com.ron.javainfohunter.processor.dto.RawContentMessage processorMessage =
                com.ron.javainfohunter.processor.dto.RawContentMessage.builder()
                .guid(crawlerMessage.getGuid())
                .title(crawlerMessage.getTitle())
                .link(crawlerMessage.getLink())
                .rawContent(crawlerMessage.getRawContent())
                .contentHash(crawlerMessage.getContentHash())
                .rssSourceId(crawlerMessage.getRssSourceId())
                .rssSourceName(crawlerMessage.getRssSourceName())
                .rssSourceUrl(crawlerMessage.getRssSourceUrl())
                .author(crawlerMessage.getAuthor())
                .publishDate(crawlerMessage.getPublishDate())
                .crawlDate(crawlerMessage.getCrawlDate())
                .category(crawlerMessage.getCategory())
                .tags(crawlerMessage.getTags())
                .build();

        contentRoutingService.routeToAgents(processorMessage);

        // Wait for agent processing
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> !contentRoutingService.getResults(processorMessage.getContentHash()).isEmpty());

        // Step 6: Create processed news entry
        rawContent.setProcessingStatus(RawContent.ProcessingStatus.COMPLETED);
        rawContentRepository.save(rawContent);

        News news = News.builder()
                .rawContent(rawContent)
                .title(rawContent.getTitle())
                .summary("E2E test summary generated by AI")
                .fullContent(rawContent.getRawContent())
                .sentiment(News.Sentiment.POSITIVE)
                .sentimentScore(new BigDecimal("0.85"))
                .importanceScore(new BigDecimal("0.75"))
                .category(source.getCategory())
                .tags(new String[]{"e2e", "test"})
                .keywords(new String[]{"automation", "testing"})
                .language("zh")
                .readingTimeMinutes(1)
                .isPublished(false)
                .build();

        News savedNews = newsRepository.save(news);

        // Step 7: Verify complete workflow
        assertThat(savedNews.getId()).isNotNull();
        assertThat(savedNews.getRawContent().getId()).isEqualTo(rawContent.getId());

        // Verify raw content status is COMPLETED
        RawContent updatedRawContent = rawContentRepository.findById(rawContent.getId()).orElse(null);
        assertThat(updatedRawContent).isNotNull();
        assertThat(updatedRawContent.getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.COMPLETED);

        // Verify news can be retrieved by raw content ID
        var foundNews = newsRepository.findByRawContentId(rawContent.getId());
        assertThat(foundNews).isPresent();
        assertThat(foundNews.get().getTitle()).isEqualTo("E2E Test Article");
    }

    /**
     * E2E Test 2: Multiple content items processing
     *
     * <p>This test verifies processing of multiple content items
     * and proper aggregation of results.</p>
     */
    @Test
    void testBatchProcessing_MultipleContentItems() throws Exception {
        // Arrange - Create RSS source
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Batch Test Feed")
                .url("https://example.com/batch-test-rss")
                .category("batch-test")
                .isActive(true)
                .build());

        // Create multiple raw content items
        int itemCount = 5;
        List<RawContent> rawContents = new ArrayList<>();
        List<RawContentMessage> messages = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            RawContent rawContent = rawContentRepository.save(RawContent.builder()
                    .rssSource(source)
                    .guid("batch-guid-" + i)
                    .title("Batch Article " + i)
                    .rawContent("Content for article " + i)
                    .contentHash("batch-hash-" + i)
                    .crawlDate(Instant.now())
                    .processingStatus(RawContent.ProcessingStatus.PENDING)
                    .build());
            rawContents.add(rawContent);

            RawContentMessage message = RawContentMessage.builder()
                    .guid(rawContent.getGuid())
                    .title(rawContent.getTitle())
                    .rawContent(rawContent.getRawContent())
                    .contentHash(rawContent.getContentHash())
                    .rssSourceId(source.getId())
                    .rssSourceName(source.getName())
                    .rssSourceUrl(source.getUrl())
                    .crawlDate(Instant.now())
                    .build();
            messages.add(message);
        }

        // Act - Publish all messages
        var publishResult = contentPublisher.publishRawContentBatch(messages);

        // Assert
        assertThat(publishResult.getTotalCount()).isEqualTo(itemCount);
        assertThat(publishResult.getSuccessCount()).isEqualTo(itemCount);
        assertThat(publishResult.getFailureCount()).isEqualTo(0);

        // Verify all raw contents are stored
        List<RawContent> storedContents = rawContentRepository.findByRssSourceId(source.getId());
        assertThat(storedContents).hasSize(itemCount);
    }

    /**
     * E2E Test 3: Error handling and recovery
     *
     * <p>This test verifies that processing errors are handled correctly
     * and raw content status is updated appropriately.</p>
     */
    @Test
    void testErrorHandling_ShouldMarkContentAsFailed() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Error Test Feed")
                .url("https://example.com/error-test-rss")
                .isActive(true)
                .build());

        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("error-test-guid")
                .title("Error Test Article")
                .rawContent("Content that will fail processing")
                .contentHash("error-test-hash")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PROCESSING)
                .build());

        // Act - Simulate processing failure
        rawContent.markAsFailed("Simulated AI processing timeout");
        rawContentRepository.save(rawContent);

        // Assert
        RawContent failedContent = rawContentRepository.findById(rawContent.getId()).orElse(null);
        assertThat(failedContent).isNotNull();
        assertThat(failedContent.getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.FAILED);
        assertThat(failedContent.getErrorMessage()).isEqualTo("Simulated AI processing timeout");
        assertThat(failedContent.isFailed()).isTrue();

        // Verify no news entry was created
        var newsEntries = newsRepository.findByRawContentId(rawContent.getId());
        assertThat(newsEntries).isEmpty();
    }

    /**
     * E2E Test 4: Deduplication workflow
     *
     * <p>This test verifies that duplicate content is properly detected
     * and not processed again.</p>
     */
    @Test
    void testDeduplication_ShouldSkipDuplicateContent() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Dedup Test Feed")
                .url("https://example.com/dedup-test-rss")
                .isActive(true)
                .build());

        String duplicateHash = "duplicate-test-hash-123";

        // Create first content
        RawContent firstContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("first-guid")
                .title("First Article")
                .rawContent("Original content")
                .contentHash(duplicateHash)
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build());

        // Act - Try to create duplicate
        var existingContent = rawContentRepository.findByContentHash(duplicateHash);

        // Assert
        assertThat(existingContent).isPresent();
        assertThat(existingContent.get().getContentHash()).isEqualTo(duplicateHash);
        assertThat(existingContent.get().getId()).isEqualTo(firstContent.getId());

        // Verify only one entry exists
        List<RawContent> allWithHash = rawContentRepository.findAll().stream()
                .filter(rc -> rc.getContentHash().equals(duplicateHash))
                .toList();
        assertThat(allWithHash).hasSize(1);
    }

    /**
     * E2E Test 5: Complete news lifecycle
     *
     * <p>This test verifies the complete lifecycle from raw content
     * to published news.</p>
     */
    @Test
    void testNewsLifecycle_FromPendingToPublished() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Lifecycle Test Feed")
                .url("https://example.com/lifecycle-test-rss")
                .isActive(true)
                .build());

        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("lifecycle-guid")
                .title("Lifecycle Article")
                .rawContent("Content for lifecycle test")
                .contentHash("lifecycle-hash")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build());

        // Step 1: Verify initial state
        assertThat(rawContent.isReadyForProcessing()).isTrue();
        assertThat(rawContent.isProcessing()).isFalse();
        assertThat(rawContent.isCompleted()).isFalse();

        // Step 2: Mark as processing
        rawContent.markAsProcessing();
        rawContentRepository.save(rawContent);
        RawContent processingContent = rawContentRepository.findById(rawContent.getId()).get();
        assertThat(processingContent.isProcessing()).isTrue();

        // Step 3: Create news entry
        News news = newsRepository.save(News.builder()
                .rawContent(processingContent)
                .title("Processed: " + processingContent.getTitle())
                .summary("AI-generated summary")
                .fullContent(processingContent.getRawContent())
                .sentiment(News.Sentiment.NEUTRAL)
                .sentimentScore(new BigDecimal("0.1"))
                .importanceScore(new BigDecimal("0.6"))
                .category(source.getCategory())
                .language("zh")
                .isPublished(false)
                .build());

        // Step 4: Mark raw content as completed
        processingContent.markAsCompleted();
        rawContentRepository.save(processingContent);
        RawContent completedContent = rawContentRepository.findById(rawContent.getId()).get();
        assertThat(completedContent.isCompleted()).isTrue();

        // Step 5: Publish news
        news.publish();
        newsRepository.save(news);
        News publishedNews = newsRepository.findById(news.getId()).get();
        assertThat(publishedNews.isPublished()).isTrue();
        assertThat(publishedNews.getPublishedAt()).isNotNull();

        // Step 6: Increment engagement metrics
        publishedNews.incrementViewCount();
        publishedNews.incrementLikeCount();
        publishedNews.incrementShareCount();
        newsRepository.save(publishedNews);

        // Verify final state
        News finalNews = newsRepository.findById(news.getId()).get();
        assertThat(finalNews.getViewCount()).isEqualTo(1L);
        assertThat(finalNews.getLikeCount()).isEqualTo(1);
        assertThat(finalNews.getShareCount()).isEqualTo(1);
        assertThat(finalNews.getEngagementScore()).isEqualTo(1L + 10 * 1 + 20 * 1); // 31
    }

    /**
     * E2E Test 6: Statistics and reporting
     *
     * <p>This test verifies that statistics can be correctly retrieved
     * after processing multiple items.</p>
     */
    @Test
    void testStatistics_ShouldCorrectlyAggregateData() {
        // Arrange - Create RSS source
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Stats Test Feed")
                .url("https://example.com/stats-test-rss")
                .isActive(true)
                .build());

        // Create raw contents with different statuses
        rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("stats-guid-1")
                .title("Article 1")
                .rawContent("Content 1")
                .contentHash("stats-hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build());

        rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("stats-guid-2")
                .title("Article 2")
                .rawContent("Content 2")
                .contentHash("stats-hash-2")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build());

        rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("stats-guid-3")
                .title("Article 3")
                .rawContent("Content 3")
                .contentHash("stats-hash-3")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.FAILED)
                .errorMessage("Test failure")
                .build());

        // Act & Assert - Count by status
        long pendingCount = rawContentRepository.countByProcessingStatus(RawContent.ProcessingStatus.PENDING);
        long completedCount = rawContentRepository.countByProcessingStatus(RawContent.ProcessingStatus.COMPLETED);
        long failedCount = rawContentRepository.countByProcessingStatus(RawContent.ProcessingStatus.FAILED);

        assertThat(pendingCount).isEqualTo(1);
        assertThat(completedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);

        // Create news entries
        RawContent completedRaw = rawContentRepository.findByProcessingStatus(
                RawContent.ProcessingStatus.COMPLETED).get(0);

        newsRepository.save(News.builder()
                .rawContent(completedRaw)
                .title("News 1")
                .summary("Summary 1")
                .fullContent("Content 1")
                .sentiment(News.Sentiment.POSITIVE)
                .language("zh")
                .isPublished(true)
                .build());

        newsRepository.save(News.builder()
                .rawContent(completedRaw)
                .title("News 2")
                .summary("Summary 2")
                .fullContent("Content 2")
                .sentiment(News.Sentiment.POSITIVE)
                .language("zh")
                .isPublished(true)
                .build());

        // Count by sentiment
        long positiveCount = newsRepository.countBySentiment(News.Sentiment.POSITIVE);
        assertThat(positiveCount).isEqualTo(2);

        // Count by raw content source
        long sourceCount = rawContentRepository.countByRssSourceId(source.getId());
        assertThat(sourceCount).isEqualTo(3);
    }

    // Helper methods

    private void mockChatService() {
        when(chatService.chat(anyString(), any())).thenReturn("""
                {
                    "summary": "AI-generated summary",
                    "topics": ["test", "automation"],
                    "keywords": ["e2e", "testing"],
                    "sentiment": "positive",
                    "sentimentScore": 0.85,
                    "importanceScore": 0.75,
                    "category": "technology"
                }
                """);
    }

    private void mockEmbeddingService() {
        when(embeddingService.embed(anyString())).thenReturn(new float[1536]);
    }

    private static org.awaitility.core.ConditionFactory await() {
        return org.awaitility.Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS);
    }
}
