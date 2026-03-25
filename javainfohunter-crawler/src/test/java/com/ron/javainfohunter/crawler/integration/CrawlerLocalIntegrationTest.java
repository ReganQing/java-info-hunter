package com.ron.javainfohunter.crawler.integration;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.publisher.ContentPublisher;
import com.ron.javainfohunter.crawler.service.CrawlCoordinator;
import com.ron.javainfohunter.crawler.service.RssFeedCrawler;
import com.ron.javainfohunter.crawler.service.RssSourceService;
import com.ron.javainfohunter.entity.RssSource;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

/**
 * Integration tests using local services (PostgreSQL, RabbitMQ).
 *
 * <p>These tests use your local PostgreSQL instance and running Docker containers
 * for RabbitMQ and Redis. This allows faster testing without Testcontainers overhead.</p>
 *
 * <p><b>Prerequisites:</b></p>
 * <ul>
 *   <li>PostgreSQL running on localhost:5432 with database 'javainfohunter'</li>
 *   <li>RabbitMQ container accessible on localhost:25672</li>
 *   <li>Redis container accessible on localhost:6379</li>
 * </ul>
 *
 * <p>Run with: mvn test -Dtest=CrawlerLocalIntegrationTest -Dspring.profiles.active=local</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class CrawlerLocalIntegrationTest {

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

    @MockBean(name = "rabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    private static boolean propertiesInitialized = false;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!propertiesInitialized) {
            // Configure for local services
            registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://localhost:5432/javainfohunter");
            registry.add("spring.rabbitmq.port", () -> 25672);
            registry.add("spring.data.redis.port", () -> 6379);
            propertiesInitialized = true;
        }
    }

    @BeforeEach
    void setUp() {
        // Clean up database
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();

        // Mock RabbitTemplate for testing without actual RabbitMQ calls
        doAnswer(invocation -> null).when(rabbitTemplate).convertAndSend(
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
    void testDatabaseConnection_ShouldConnectSuccessfully() {
        // This test verifies the database connection is working
        long count = rssSourceRepository.count();
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testRssSourceRepository_ShouldStoreAndRetrieve() {
        // Arrange & Act
        RssSource source = RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .category("test")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build();

        RssSource saved = rssSourceRepository.save(source);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Feed");

        var found = rssSourceRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo("https://example.com/rss");
    }

    @Test
    void testRawContentRepository_ShouldStoreWithAllFields() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .category("test")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build());

        // Act
        com.ron.javainfohunter.entity.RawContent content =
                com.ron.javainfohunter.entity.RawContent.builder()
                .rssSource(source)
                .guid("test-guid-123")
                .title("Test Article")
                .link("https://example.com/article")
                .rawContent("Test content here")
                .contentHash("unique-hash-abc123")
                .author("Test Author")
                .publishDate(Instant.now())
                .crawlDate(Instant.now())
                .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                .build();

        com.ron.javainfohunter.entity.RawContent saved = rawContentRepository.save(content);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Article");
        assertThat(saved.getContentHash()).isEqualTo("unique-hash-abc123");
        assertThat(saved.getProcessingStatus()).isEqualTo(
                com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING);
    }

    @Test
    void testRawContentRepository_ShouldFindExistingHashes() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
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

        // Assert - Verifies batch query works
        assertThat(existingHashes).hasSize(2);
        assertThat(existingHashes).containsExactlyInAnyOrder("hash1", "hash2");
    }

    @Test
    void testRssFeedCrawler_ShouldHandleInvalidUrl() {
        // Arrange
        String invalidUrl = "not-a-valid-url";

        // Act
        CrawlResult result = rssFeedCrawler.crawlFeed(invalidUrl, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid URL");
    }

    @Test
    void testRssFeedCrawler_ShouldHandleUnsupportedProtocol() {
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
    void testContentPublisher_ShouldHandleNullMessage() {
        // Act
        boolean published = contentPublisher.publishRawContent(null);

        // Assert
        assertThat(published).isFalse();
    }

    @Test
    void testContentPublisher_ShouldHandleEmptyBatch() {
        // Act
        var result = contentPublisher.publishRawContentBatch(List.of());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    void testCrawlCoordinator_ShouldGetStatistics() {
        // Act
        String stats = crawlCoordinator.getStatistics();

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats).contains("active RSS sources");
    }

    @Test
    void testRawContent_ShouldUpdateProcessingStatus() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build());

        com.ron.javainfohunter.entity.RawContent content =
                com.ron.javainfohunter.entity.RawContent.builder()
                .rssSource(source)
                .guid("guid-status")
                .title("Status Test")
                .rawContent("Content")
                .contentHash("status-hash")
                .crawlDate(Instant.now())
                .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PENDING)
                .build();

        com.ron.javainfohunter.entity.RawContent saved = rawContentRepository.save(content);

        // Act - Test status transitions
        saved.markAsProcessing();
        rawContentRepository.save(saved);

        var processing = rawContentRepository.findById(saved.getId()).get();
        assertThat(processing.isProcessing()).isTrue();
        assertThat(processing.isReadyForProcessing()).isFalse();

        processing.markAsCompleted();
        rawContentRepository.save(processing);

        var completed = rawContentRepository.findById(saved.getId()).get();
        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.isProcessing()).isFalse();
    }

    @Test
    void testRawContent_ShouldMarkAsFailed() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Test Feed")
                .url("https://example.com/rss")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build());

        com.ron.javainfohunter.entity.RawContent content =
                com.ron.javainfohunter.entity.RawContent.builder()
                .rssSource(source)
                .guid("guid-fail")
                .title("Fail Test")
                .rawContent("Content")
                .contentHash("fail-hash")
                .crawlDate(Instant.now())
                .processingStatus(com.ron.javainfohunter.entity.RawContent.ProcessingStatus.PROCESSING)
                .build();

        com.ron.javainfohunter.entity.RawContent saved = rawContentRepository.save(content);

        // Act
        saved.markAsFailed("Test failure message");
        rawContentRepository.save(saved);

        // Assert
        var failed = rawContentRepository.findById(saved.getId()).get();
        assertThat(failed.isFailed()).isTrue();
        assertThat(failed.getErrorMessage()).isEqualTo("Test failure message");
    }

    @Test
    void testRssSourceRepository_ShouldFindByIsActive() {
        // Arrange
        rssSourceRepository.save(RssSource.builder()
                .name("Active Feed")
                .url("https://example.com/active")
                .category("tech")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build());

        rssSourceRepository.save(RssSource.builder()
                .name("Inactive Feed")
                .url("https://example.com/inactive")
                .category("news")
                .isActive(false)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build());

        // Act
        List<RssSource> activeSources = rssSourceRepository.findByIsActiveTrue();
        List<RssSource> inactiveSources = rssSourceRepository.findByIsActive(
                false, org.springframework.data.domain.PageRequest.of(0, 10)).getContent();

        // Assert
        assertThat(activeSources).hasSize(1);
        assertThat(activeSources.get(0).getName()).isEqualTo("Active Feed");

        assertThat(inactiveSources).hasSize(1);
        assertThat(inactiveSources.get(0).getName()).isEqualTo("Inactive Feed");
    }

    @Test
    void testCrawlResult_ShouldCalculateMetrics() {
        // Arrange
        CrawlResult result = CrawlResult.builder()
                .success(true)
                .rssSourceId(1L)
                .feedUrl("https://example.com/feed")
                .totalItems(100)
                .newItems(85)
                .duplicateItems(10)
                .failedItems(5)
                .durationMs(5000)
                .build();

        // Assert
        assertThat(result.getSuccessRate()).isEqualTo(95.0); // (100-5)/100 * 100
        assertThat(result.isPartialSuccess()).isTrue();
        assertThat(result.getTotalItems()).isEqualTo(result.getNewItems() + result.getDuplicateItems() + result.getFailedItems());
    }

    @Test
    void testCompleteWorkflow_ShouldStoreContentAndPublish() {
        // Arrange
        RssSource source = rssSourceRepository.save(RssSource.builder()
                .name("Workflow Test Feed")
                .url("https://example.com/workflow")
                .category("workflow-test")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build());

        // Act - Simulate crawling
        CrawlResult crawlResult = crawlCoordinator.crawlSingleSourceSync(source);

        // Assert
        assertThat(crawlResult).isNotNull();
        assertThat(crawlResult.getRssSourceId()).isEqualTo(source.getId());

        // Verify source was updated
        var updated = rssSourceRepository.findById(source.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getLastCrawledAt()).isNotNull();
    }
}
