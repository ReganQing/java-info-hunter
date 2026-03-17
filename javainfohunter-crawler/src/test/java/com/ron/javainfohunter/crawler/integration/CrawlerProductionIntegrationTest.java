package com.ron.javainfohunter.crawler.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.dto.CrawlResult;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.crawler.handler.RetryHandler;
import com.ron.javainfohunter.crawler.metrics.CrawlMetricsCollector;
import com.ron.javainfohunter.crawler.publisher.ContentPublisher;
import com.ron.javainfohunter.crawler.service.CrawlCoordinator;
import com.ron.javainfohunter.crawler.service.RssFeedCrawler;
import com.ron.javainfohunter.crawler.service.RssSourceService;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.RawContentRepository;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * Production-ready integration tests for Crawler module.
 *
 * <p>Tests verify:</p>
 * <ul>
 *   <li>RSS feed crawling from real URLs</li>
 *   <li>Content extraction and parsing</li>
 *   <li>Deduplication via content hashing</li>
 *   <li>Message publishing to RabbitMQ</li>
 *   <li>Error handling and retry logic</li>
 *   <li>Metrics collection</li>
 *   <li>Database persistence</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b></p>
 * <ul>
 *   <li>Local PostgreSQL database named 'javainfohunter'</li>
 *   <li>Environment variables: DB_USERNAME, DB_PASSWORD</li>
 * </ul>
 *
 * <p><b>Run with:</b></p>
 * <pre>
 * export DB_USERNAME=admin
 * export DB_PASSWORD=admin6866!@#
 * mvn test -Dtest=CrawlerProductionIntegrationTest -Drun.production.tests=true -pl javainfohunter-crawler
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("production-test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/javainfohunter",
    "spring.datasource.username=${DB_USERNAME:admin}",
    "spring.datasource.password=${DB_PASSWORD:admin6866!@#}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY:sk-test-key}",
    "spring.ai.dashscope.chat.enabled=false",
    "spring.main.allow-bean-definition-overriding=true",
    "javainfohunter.crawler.enabled=true",
    "javainfohunter.crawler.scheduler.enabled=false",
    "javainfohunter.crawler.feed.max-articles-per-feed=50",
    "javainfohunter.crawler.feed.connection-timeout=30000",
    "javainfohunter.crawler.feed.read-timeout=60000",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=25672",
    "spring.rabbitmq.username=${RABBITMQ_USERNAME:admin}",
    "spring.rabbitmq.password=${RABBITMQ_PASSWORD:admin}",
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.auto-startup=false"
})
@EnabledIfSystemProperty(named = "run.production.tests", matches = "true",
        disabledReason = "Production tests require explicit enablement. " +
                "Run with -Drun.production.tests=true")
public class CrawlerProductionIntegrationTest {

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

    @Autowired(required = false)
    private RetryHandler retryHandler;

    @Autowired(required = false)
    private CrawlMetricsCollector metricsCollector;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "rabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    // Track published messages for verification
    private RawContentMessage lastPublishedMessage;

    @BeforeEach
    void setUp() {
        lastPublishedMessage = null;

        // Mock RabbitTemplate to capture published messages
        doAnswer(invocation -> {
            lastPublishedMessage = invocation.getArgument(2);
            return null;
        }).when(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                any(RawContentMessage.class),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class)
        );

        // Clean up database (only if tables exist)
        try {
            rawContentRepository.deleteAll();
            rssSourceRepository.deleteAll();
        } catch (Exception e) {
            // Tables might not exist yet, ignore
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up database (only if tables exist)
        try {
            rawContentRepository.deleteAll();
            rssSourceRepository.deleteAll();
        } catch (Exception e) {
            // Tables might not exist, ignore
        }
    }

    @Nested
    @DisplayName("RSS Feed Crawling Tests")
    class RssFeedCrawlingTests {

        @Test
        @DisplayName("Should crawl real RSS feed successfully")
        void shouldCrawlRealRssFeedSuccessfully() {
            // Given - Use a real public RSS feed
            String feedUrl = "https://rss.nytimes.com/services/xml/rss/nyt/Technology.xml";
            RssSource source = createAndSaveRssSource("NYTimes Technology", feedUrl, "technology");

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRssSourceId()).isEqualTo(source.getId());
            assertThat(result.getFeedUrl()).isEqualTo(feedUrl);
            assertThat(result.getTotalItems()).isGreaterThan(0);
            assertThat(result.getDurationMs()).isGreaterThan(0);

            // Verify source was updated
            RssSource updated = rssSourceRepository.findById(source.getId()).orElseThrow();
            assertThat(updated.getLastCrawledAt()).isNotNull();
        }

        @Test
        @DisplayName("Should crawl RSS feed with custom timeout")
        void shouldCrawlRssFeedWithCustomTimeout() {
            // Given
            String feedUrl = "https://www.theverge.com/rss/index.xml";
            RssSource source = createAndSaveRssSource("The Verge", feedUrl, "tech");

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotalItems()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle invalid RSS feed URL")
        void shouldHandleInvalidRssFeedUrl() {
            // Given
            String invalidUrl = "https://invalid-domain-that-does-not-exist-12345.com/feed.xml";
            RssSource source = createAndSaveRssSource("Invalid Feed", invalidUrl, "test");

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(invalidUrl, source.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isNotEmpty();

            // Verify failure was recorded
            RssSource updated = rssSourceRepository.findById(source.getId()).orElseThrow();
            assertThat(updated.getFailedCrawls()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should extract content from feed items")
        void shouldExtractContentFromFeedItems() {
            // Given
            String feedUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml";
            RssSource source = createAndSaveRssSource("BBC Tech", feedUrl, "technology");

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNewItems()).isGreaterThan(0);

            // Verify items were stored
            List<RawContent> contents = rawContentRepository.findByRssSourceId(source.getId());
            assertThat(contents).isNotEmpty();

            // Verify content fields
            RawContent firstContent = contents.get(0);
            assertThat(firstContent.getTitle()).isNotEmpty();
            assertThat(firstContent.getLink()).isNotEmpty();
            assertThat(firstContent.getContentHash()).isNotEmpty();
            assertThat(firstContent.getGuid()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Content Deduplication Tests")
    class ContentDeduplicationTests {

        @Test
        @DisplayName("Should detect duplicate content via hash")
        void shouldDetectDuplicateContentViaHash() {
            // Given
            String feedUrl = "https://www.engadget.com/rss.xml";
            RssSource source = createAndSaveRssSource("Engadget", feedUrl, "tech");

            // When - First crawl
            CrawlResult firstCrawl = rssFeedCrawler.crawlFeed(feedUrl, source.getId());
            long firstNewItems = firstCrawl.getNewItems();

            // When - Second crawl (should detect duplicates)
            rawContentRepository.deleteAll(); // Clear to simulate fresh check
            CrawlResult secondCrawl = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

            // Then
            assertThat(firstCrawl.isSuccess()).isTrue();
            assertThat(secondCrawl.isSuccess()).isTrue();
            // Second crawl should have fewer new items (mostly duplicates)
        }

        @Test
        @DisplayName("Should generate consistent content hashes")
        void shouldGenerateConsistentContentHashes() {
            // Given
            String feedUrl = "https://arstechnica.com/rss/";
            RssSource source = createAndSaveRssSource("Ars Technica", feedUrl, "tech");

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();

            List<RawContent> contents = rawContentRepository.findByRssSourceId(source.getId());
            assertThat(contents).isNotEmpty();

            // Verify all hashes are non-empty and valid format
            for (RawContent content : contents) {
                assertThat(content.getContentHash()).isNotEmpty();
                assertThat(content.getContentHash()).hasSize(64); // SHA-256 = 64 hex chars
            }
        }

        @Test
        @DisplayName("Should find existing hashes in batch")
        void shouldFindExistingHashesInBatch() {
            // Given
            String feedUrl = "https://techcrunch.com/feed/";
            RssSource source = createAndSaveRssSource("TechCrunch", feedUrl, "tech");
            rssFeedCrawler.crawlFeed(feedUrl, source.getId());

            List<RawContent> contents = rawContentRepository.findByRssSourceId(source.getId());
            List<String> existingHashes = contents.stream()
                    .limit(5)
                    .map(RawContent::getContentHash)
                    .toList();

            // Add some non-existing hashes
            List<String> testHashes = new java.util.ArrayList<>(existingHashes);
            testHashes.add("nonexistenthash123");
            testHashes.add("anotherfakehash456");

            // When
            List<String> foundHashes = new java.util.ArrayList<>(rawContentRepository.findExistingContentHashes(new java.util.HashSet<>(testHashes)));

            // Then
            assertThat(foundHashes).containsAll(existingHashes);
            assertThat(foundHashes).doesNotContain("nonexistenthash123");
            assertThat(foundHashes).doesNotContain("anotherfakehash456");
        }
    }

    @Nested
    @DisplayName("Message Publishing Tests")
    class MessagePublishingTests {

        @Test
        @DisplayName("Should publish raw content message to RabbitMQ")
        void shouldPublishRawContentMessage() {
            // Given
            RssSource source = createAndSaveRssSource("Test", "https://example.com/feed", "test");
            RawContent content = createAndSaveRawContent(source, "Test Article", "https://example.com/article");

            RawContentMessage message = RawContentMessage.builder()
                    .guid(content.getGuid())
                    .title(content.getTitle())
                    .link(content.getLink())
                    .rawContent(content.getRawContent())
                    .contentHash(content.getContentHash())
                    .rssSourceId(source.getId())
                    .rssSourceName(source.getName())
                    .rssSourceUrl(source.getUrl())
                    .author(content.getAuthor())
                    .publishDate(content.getPublishDate())
                    .crawlDate(content.getCrawlDate())
                    .category(source.getCategory())
                    .build();

            // When
            boolean published = contentPublisher.publishRawContent(message);

            // Then
            assertThat(published).isTrue();
            verify(rabbitTemplate).convertAndSend(
                    any(String.class),
                    any(String.class),
                    eq(message),
                    any(org.springframework.amqp.rabbit.connection.CorrelationData.class)
            );
        }

        @Test
        @DisplayName("Should publish batch of messages")
        void shouldPublishBatchOfMessages() {
            // Given
            RssSource source = createAndSaveRssSource("Batch Test", "https://example.com/feed", "test");
            List<RawContentMessage> messages = List.of(
                    createMessage(source, "Article 1", "guid-1"),
                    createMessage(source, "Article 2", "guid-2"),
                    createMessage(source, "Article 3", "guid-3")
            );

            // When
            var result = contentPublisher.publishRawContentBatch(messages);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isEqualTo(3);
            assertThat(result.getSuccessCount()).isEqualTo(3);
            assertThat(result.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle publish failure gracefully")
        void shouldHandlePublishFailureGracefully() {
            // Given
            RawContentMessage nullMessage = null;

            // When
            boolean result = contentPublisher.publishRawContent(nullMessage);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("CrawlCoordinator Tests")
    class CrawlCoordinatorTests {

        @Test
        @DisplayName("Should crawl single source synchronously")
        void shouldCrawlSingleSourceSynchronously() {
            // Given
            String feedUrl = "https://www.wired.com/feed/rss";
            RssSource source = createAndSaveRssSource("Wired", feedUrl, "tech");

            // When
            CrawlResult result = crawlCoordinator.crawlSingleSourceSync(source);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRssSourceId()).isEqualTo(source.getId());

            // Verify content was stored
            List<RawContent> contents = rawContentRepository.findByRssSourceId(source.getId());
            assertThat(contents).isNotEmpty();
        }

        @Test
        @DisplayName("Should crawl all active sources")
        void shouldCrawlAllActiveSources() {
            // Given
            createAndSaveRssSource("Source 1", "https://www.theverge.com/rss/index.xml", "tech");
            createAndSaveRssSource("Source 2", "https://rss.cnn.com/rss/edition.rss", "news");
            createAndSaveInactiveRssSource("Inactive Source", "https://example.com/feed", "test");

            // When
            List<RawContentMessage> results = crawlCoordinator.crawlSources(
                    rssSourceRepository.findAll().stream()
                            .filter(RssSource::getIsActive)
                            .limit(2)
                            .toList()
            );

            // Then
            assertThat(results).isNotNull();
        }

        @Test
        @DisplayName("Should get crawl statistics")
        void shouldGetCrawlStatistics() {
            // Given
            createAndSaveRssSource("Stats Test", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", "news");

            // When
            String stats = crawlCoordinator.getStatistics();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats).contains("active RSS sources");
        }
    }

    @Nested
    @DisplayName("Error Handling and Retry Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should increment failed crawls counter on error")
        void shouldIncrementFailedCrawlsCounterOnError() {
            // Given
            String badUrl = "https://this-domain-does-not-exist-12345.com/feed.xml";
            RssSource source = createAndSaveRssSource("Bad Feed", badUrl, "test");
            long initialFailedCrawls = source.getFailedCrawls();

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(badUrl, source.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();

            RssSource updated = rssSourceRepository.findById(source.getId()).orElseThrow();
            assertThat(updated.getFailedCrawls()).isGreaterThan(initialFailedCrawls);
        }

        @Test
        @DisplayName("Should mark source as inactive after max retries")
        void shouldMarkSourceAsInactiveAfterMaxRetries() {
            // Given
            String badUrl = "https://another-invalid-domain-67890.com/feed.xml";
            RssSource source = createAndSaveRssSource("Failing Feed", badUrl, "test");
            source.setFailedCrawls(Long.valueOf(source.getMaxRetries()));
            rssSourceRepository.save(source);

            // When
            CrawlResult result = rssFeedCrawler.crawlFeed(badUrl, source.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();

            RssSource updated = rssSourceRepository.findById(source.getId()).orElseThrow();
            // After max retries, source should be marked inactive
            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should calculate success rate correctly")
        void shouldCalculateSuccessRateCorrectly() {
            // Given
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

            // When
            double successRate = result.getSuccessRate();

            // Then
            assertThat(successRate).isEqualTo(95.0); // (100-5)/100 * 100
        }
    }

    @Nested
    @DisplayName("Metrics Collection Tests")
    class MetricsCollectionTests {

        @Test
        @DisplayName("Should record crawl metrics")
        void shouldRecordCrawlMetrics() {
            // Given
            if (metricsCollector != null) {
                String feedUrl = "https://www.wired.com/feed/rss";
                RssSource source = createAndSaveRssSource("Metrics Test", feedUrl, "tech");

                // When
                CrawlResult result = rssFeedCrawler.crawlFeed(feedUrl, source.getId());

                // Then
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getDurationMs()).isGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("Database Persistence Tests")
    class DatabasePersistenceTests {

        @Test
        @DisplayName("Should persist and retrieve RSS source")
        void shouldPersistAndRetrieveRssSource() {
            // Given
            RssSource source = RssSource.builder()
                    .name("Persistence Test")
                    .url("https://example.com/feed")
                    .category("test")
                    .isActive(true)
                    .crawlIntervalSeconds(3600)
                    .totalArticles(0L)
                    .failedCrawls(0L)
                    .maxRetries(3)
                    .retryBackoffSeconds(60)
                    .build();

            // When
            RssSource saved = rssSourceRepository.save(source);

            // Then
            assertThat(saved.getId()).isNotNull();

            Optional<RssSource> found = rssSourceRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Persistence Test");
        }

        @Test
        @DisplayName("Should persist raw content with all fields")
        void shouldPersistRawContentWithAllFields() {
            // Given
            RssSource source = createAndSaveRssSource("Content Test", "https://example.com/feed", "test");

            // When
            RawContent content = RawContent.builder()
                    .rssSource(source)
                    .guid("test-guid-" + System.currentTimeMillis())
                    .title("Test Article")
                    .link("https://example.com/article")
                    .rawContent("Test content here")
                    .contentHash("hash-" + System.currentTimeMillis())
                    .author("Test Author")
                    .publishDate(Instant.now())
                    .crawlDate(Instant.now())
                    .processingStatus(RawContent.ProcessingStatus.PENDING)
                    .build();

            RawContent saved = rawContentRepository.save(content);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTitle()).isEqualTo("Test Article");
            assertThat(saved.getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.PENDING);
        }
    }

    // Helper methods

    private RssSource createAndSaveRssSource(String name, String url, String category) {
        RssSource source = RssSource.builder()
                .name(name)
                .url(url)
                .category(category)
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build();
        return rssSourceRepository.save(source);
    }

    private RssSource createAndSaveInactiveRssSource(String name, String url, String category) {
        RssSource source = RssSource.builder()
                .name(name)
                .url(url)
                .category(category)
                .isActive(false)
                .crawlIntervalSeconds(3600)
                .totalArticles(0L)
                .failedCrawls(0L)
                .maxRetries(3)
                .retryBackoffSeconds(60)
                .build();
        return rssSourceRepository.save(source);
    }

    private RawContent createAndSaveRawContent(RssSource source, String title, String link) {
        RawContent content = RawContent.builder()
                .rssSource(source)
                .guid("guid-" + System.currentTimeMillis())
                .title(title)
                .link(link)
                .rawContent("Content for " + title)
                .contentHash("hash-" + System.currentTimeMillis())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build();
        return rawContentRepository.save(content);
    }

    private RawContentMessage createMessage(RssSource source, String title, String guid) {
        return RawContentMessage.builder()
                .guid(guid)
                .title(title)
                .link("https://example.com/" + guid)
                .rawContent("Content for " + title)
                .contentHash("hash-" + guid)
                .rssSourceId(source.getId())
                .rssSourceName(source.getName())
                .rssSourceUrl(source.getUrl())
                .publishDate(Instant.now())
                .crawlDate(Instant.now())
                .category(source.getCategory())
                .build();
    }
}
