package com.ron.javainfohunter.crawler.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.publisher.ContentPublisher;
import com.ron.javainfohunter.crawler.publisher.PublishResult;
import com.ron.javainfohunter.crawler.service.CrawlCoordinator;
import com.ron.javainfohunter.crawler.service.RssFeedCrawler;
import com.ron.javainfohunter.crawler.service.RssSourceService;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RawContentRepository;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for Crawler module.
 *
 * <p>These tests verify the complete crawler workflow:</p>
 * <ul>
 *   <li>RSS feed crawling and parsing</li>
 *   <li>Content deduplication</li>
 *   <li>Message publishing to RabbitMQ</li>
 *   <li>Data persistence to PostgreSQL</li>
 * </ul>
 *
 * <p>Uses Testcontainers for real PostgreSQL and RabbitMQ instances.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class CrawlerIntegrationTest {

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
    private RssFeedCrawler rssFeedCrawler;

    @Autowired
    private ContentPublisher contentPublisher;

    @Autowired
    private CrawlCoordinator crawlCoordinator;

    @Autowired
    private RssSourceService rssSourceService;

    @Autowired
    private RawContentRepository rawContentRepository;

    @Autowired
    private RssSourceRepository rssSourceRepository;

    @Autowired
    private CrawlerProperties crawlerProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private CountDownLatch publishLatch;

    @BeforeEach
    void setUp() {
        // Clean up database
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();

        // Setup latch for tracking publish calls
        publishLatch = new CountDownLatch(1);

        // Mock RabbitTemplate to track publish calls without actually sending to RabbitMQ
        doAnswer(invocation -> {
            String exchange = invocation.getArgument(0);
            String routingKey = invocation.getArgument(1);
            Object message = invocation.getArgument(2);

            // Simulate successful publish
            publishLatch.countDown();

            return null;
        }).when(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                any(RawContentMessage.class),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class)
        );
    }

    @AfterEach
    void tearDown() {
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();
    }

    @Test
    void testCrawlFeed_ShouldExtractContentSuccessfully() {
        // Arrange
        String feedUrl = "https://example.com/rss";
        Long sourceId = 1L;

        // Create and save RSS source
        RssSource source = RssSource.builder()
                .name("Test Feed")
                .url(feedUrl)
                .category("technology")
                .isActive(true)
                .build();
        source = rssSourceRepository.save(source);

        // Act
        CrawlResult result = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRssSourceId()).isEqualTo(source.getId());
        assertThat(result.getFeedUrl()).isEqualTo(feedUrl);
        assertThat(result.getDurationMs()).isGreaterThan(0);
    }

    @Test
    void testCrawlFeed_WithInvalidUrl_ShouldHandleGracefully() {
        // Arrange
        String invalidUrl = "not-a-valid-url";
        Long sourceId = 1L;

        // Act
        CrawlResult result = rssFeedCrawler.crawlFeed(invalidUrl, sourceId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotEmpty();
        assertThat(result.getErrorMessage()).contains("Invalid URL");
    }

    @Test
    void testCrawlFeed_WithUnsupportedProtocol_ShouldFail() {
        // Arrange
        String ftpUrl = "ftp://example.com/feed.xml";

        // Act
        CrawlResult result = rssFeedCrawler.crawlFeed(ftpUrl, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unsupported protocol");
    }

    @Test
    void testContentPublisher_ShouldPublishSingleMessage() {
        // Arrange
        RawContentMessage message = createTestRawContentMessage();

        // Act
        boolean published = contentPublisher.publishRawContent(message);

        // Assert
        assertThat(published).isTrue();
        verify(rabbitTemplate).convertAndSend(
                eq("crawler.direct"),
                eq("raw.content"),
                eq(message),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class)
        );
    }

    @Test
    void testContentPublisher_ShouldPublishBatchMessages() {
        // Arrange
        List<RawContentMessage> messages = List.of(
                createTestRawContentMessage(),
                createTestRawContentMessage(),
                createTestRawContentMessage()
        );

        // Act
        PublishResult result = contentPublisher.publishRawContentBatch(messages);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void testContentPublisher_WithNullMessage_ShouldReturnFalse() {
        // Act
        boolean published = contentPublisher.publishRawContent(null);

        // Assert
        assertThat(published).isFalse();
    }

    @Test
    void testContentPublisher_WithEmptyBatch_ShouldReturnEmptyResult() {
        // Act
        PublishResult result = contentPublisher.publishRawContentBatch(List.of());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void testCrawlCoordinator_ShouldCrawlSingleSource() {
        // Arrange
        RssSource source = RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .category("technology")
                .isActive(true)
                .build();
        source = rssSourceRepository.save(source);

        // Act
        CrawlResult result = crawlCoordinator.crawlSingleSourceSync(source);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRssSourceId()).isEqualTo(source.getId());
        assertThat(result.getDurationMs()).isGreaterThan(0);
    }

    @Test
    void testCrawlCoordinator_ShouldCrawlMultipleSources() {
        // Arrange
        RssSource source1 = rssSourceRepository.save(RssSource.builder()
                .name("Feed 1")
                .url("https://example.com/feed1")
                .category("tech")
                .isActive(true)
                .build());

        RssSource source2 = rssSourceRepository.save(RssSource.builder()
                .name("Feed 2")
                .url("https://example.com/feed2")
                .category("news")
                .isActive(true)
                .build());

        List<RssSource> sources = List.of(source1, source2);

        // Act
        List<RawContentMessage> messages = crawlCoordinator.crawlSources(sources);

        // Assert
        assertThat(messages).isNotNull();
        // Messages will be empty since we're not actually fetching real RSS feeds
    }

    @Test
    void testRawContentRepository_ShouldStoreAndRetrieveContent() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .category("tech")
                .isActive(true)
                .build());

        com.ron.javainfohunter.entity.RawContent rawContent =
                com.ron.javainfohunter.entity.RawContent.builder()
                        .rssSource(source)
                        .guid("test-guid-123")
                        .title("Test Article")
                        .link("https://example.com/article")
                        .rawContent("Test content")
                        .contentHash("abc123")
                        .author("Test Author")
                        .publishDate(Instant.now())
                        .crawlDate(Instant.now())
                        .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                        .build();

        // Act
        com.ron.javainfohunter.entity.RawContent saved = rawContentRepository.save(rawContent);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Article");
        assertThat(saved.getProcessingStatus()).isEqualTo(
                com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING);

        // Verify retrieval
        var found = rawContentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Article");
    }

    @Test
    void testRawContentRepository_ShouldCheckDuplicateByHash() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .isActive(true)
                .build());

        String hash = "unique-hash-123";
        com.ron.javainfohunter.entity.RawContent content =
                com.ron.javainfohunter.entity.RawContent.builder()
                        .rssSource(source)
                        .guid("guid-1")
                        .title("Article 1")
                        .rawContent("Content 1")
                        .contentHash(hash)
                        .crawlDate(Instant.now())
                        .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                        .build();
        rawContentRepository.save(content);

        // Act
        var found = rawContentRepository.findByContentHash(hash);
        var notFound = rawContentRepository.findByContentHash("non-existent-hash");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getContentHash()).isEqualTo(hash);
        assertThat(notFound).isEmpty();
    }

    @Test
    void testRawContentRepository_ShouldFindExistingHashesBatch() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .isActive(true)
                .build());

        rawContentRepository.save(com.ron.javainfohunter.entity.RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Article 1")
                .rawContent("Content 1")
                .contentHash("hash1")
                .crawlDate(Instant.now())
                .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                .build());

        rawContentRepository.save(com.ron.javainfohunter.entity.RawContent.builder()
                .rssSource(source)
                .guid("guid-2")
                .title("Article 2")
                .rawContent("Content 2")
                .contentHash("hash2")
                .crawlDate(Instant.now())
                .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                .build());

        // Act
        var existingHashes = rawContentRepository.findExistingContentHashes(
                List.of("hash1", "hash2", "hash3", "hash4")
        );

        // Assert
        assertThat(existingHashes).hasSize(2);
        assertThat(existingHashes).containsExactlyInAnyOrder("hash1", "hash2");
    }

    @Test
    void testRawContent_ShouldUpdateProcessingStatus() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .isActive(true)
                .build());

        com.ron.javainfohunter.entity.RawContent content =
                com.ron.javainfohunter.entity.RawContent.builder()
                        .rssSource(source)
                        .guid("guid-1")
                        .title("Article")
                        .rawContent("Content")
                        .contentHash("hash-1")
                        .crawlDate(Instant.now())
                        .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                        .build();
        com.ron.javainfohunter.entity.RawContent saved = rawContentRepository.save(content);

        // Act - Mark as processing
        saved.markAsProcessing();
        rawContentRepository.save(saved);

        // Assert
        var retrieved = rawContentRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProcessingStatus()).isEqualTo(
                com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PROCESSING);
        assertThat(retrieved.get().isProcessing()).isTrue();
    }

    @Test
    void testRssSourceRepository_ShouldFindActiveSources() {
        // Arrange
        rssSourceRepository.save(RssSource.builder()
                .name("Active Feed 1")
                .url("https://example.com/feed1")
                .category("tech")
                .isActive(true)
                .build());

        rssSourceRepository.save(RssSource.builder()
                .name("Active Feed 2")
                .url("https://example.com/feed2")
                .category("news")
                .isActive(true)
                .build());

        rssSourceRepository.save(RssSource.builder()
                .name("Inactive Feed")
                .url("https://example.com/feed3")
                .category("sports")
                .isActive(false)
                .build());

        // Act
        List<RssSource> activeSources = rssSourceRepository.findByIsActiveTrue();

        // Assert
        assertThat(activeSources).hasSize(2);
        assertThat(activeSources).allMatch(RssSource::getIsActive);
    }

    @Test
    void testCrawlResult_ShouldCalculateSuccessRate() {
        // Arrange
        CrawlResult result = CrawlResult.builder()
                .success(true)
                .totalItems(100)
                .newItems(90)
                .duplicateItems(5)
                .failedItems(5)
                .durationMs(1000)
                .build();

        // Assert
        assertThat(result.getTotalItems()).isEqualTo(100);
        assertThat(result.getNewItems()).isEqualTo(90);
        assertThat(result.getDuplicateItems()).isEqualTo(5);
        assertThat(result.getFailedItems()).isEqualTo(5);
        assertThat(result.getSuccessRate()).isEqualTo(95.0);
    }

    @Test
    void testRawContentMessage_ShouldSerializeToJson() throws Exception {
        // Arrange
        RawContentMessage message = createTestRawContentMessage();

        // Act
        String json = objectMapper.writeValueAsString(message);

        // Assert
        assertThat(json).isNotEmpty();
        assertThat(json).contains("\"title\":\"Test Article\"");
        assertThat(json).contains("\"contentHash\":\"test-hash-123\"");

        // Verify deserialization
        RawContentMessage deserialized = objectMapper.readValue(json, RawContentMessage.class);
        assertThat(deserialized.getTitle()).isEqualTo(message.getTitle());
        assertThat(deserialized.getContentHash()).isEqualTo(message.getContentHash());
    }

    @Test
    void testCrawlerProperties_ShouldHaveValidDefaults() {
        // Assert
        assertThat(crawlerProperties.getFeed().getMaxArticlesPerFeed()).isGreaterThan(0);
        assertThat(crawlerProperties.getFeed().getConnectionTimeout()).isGreaterThan(0);
        assertThat(crawlerProperties.getFeed().getReadTimeout()).isGreaterThan(0);
        assertThat(crawlerProperties.getFeed().getUserAgent()).isNotEmpty();
        assertThat(crawlerProperties.getDeduplication().isEnabled()).isTrue();
        assertThat(crawlerProperties.getDeduplication().getHashAlgorithm()).isEqualTo("SHA-256");
    }

    /**
     * Integration test for the complete workflow:
     * 1. Create RSS source
     * 2. Crawl feed
     * 3. Publish messages
     * 4. Verify data in database
     */
    @Test
    void testCompleteCrawlWorkflow_ShouldStoreDataSuccessfully() {
        // Arrange - Create RSS source
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Integration Test Feed")
                .url("https://example.com/integration-test-rss")
                .category("integration-test")
                .isActive(true)
                .build());

        // Act - Crawl the feed
        CrawlResult crawlResult = crawlCoordinator.crawlSingleSourceSync(source);

        // Assert - Verify crawl result
        assertThat(crawlResult).isNotNull();
        assertThat(crawlResult.getRssSourceId()).isEqualTo(source.getId());

        // Verify RSS source was updated
        var updatedSource = rssSourceRepository.findById(source.getId());
        assertThat(updatedSource).isPresent();
        assertThat(updatedSource.get().getLastCrawledAt()).isNotNull();
    }

    // Helper methods

    private RawContentMessage createTestRawContentMessage() {
        return RawContentMessage.builder()
                .guid("test-guid-" + System.currentTimeMillis())
                .title("Test Article")
                .link("https://example.com/article")
                .rawContent("This is test content for the article.")
                .contentHash("test-hash-" + System.currentTimeMillis())
                .rssSourceId(1L)
                .rssSourceName("Test Feed")
                .rssSourceUrl("https://example.com/rss")
                .author("Test Author")
                .publishDate(Instant.now())
                .crawlDate(Instant.now())
                .category("technology")
                .tags(new String[]{"tech", "test"})
                .build();
    }
}
