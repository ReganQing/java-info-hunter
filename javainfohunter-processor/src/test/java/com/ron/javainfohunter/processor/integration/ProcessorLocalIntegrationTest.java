package com.ron.javainfohunter.processor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.ai.service.EmbeddingService;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import com.ron.javainfohunter.processor.service.ContentRoutingService;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests using local services (PostgreSQL, RabbitMQ).
 *
 * <p>These tests use your local PostgreSQL instance and running Docker containers
 * for RabbitMQ. This allows faster testing without Testcontainers overhead.</p>
 *
 * <p><b>Prerequisites:</b></p>
 * <ul>
 *   <li>PostgreSQL running on localhost:5432 with database 'javainfohunter'</li>
 *   <li>RabbitMQ container accessible on localhost:25672</li>
 * </ul>
 *
 * <p>Run with: mvn test -Dtest=ProcessorLocalIntegrationTest -Dspring.profiles.active=local -Drun.local.tests=true</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "run.local.tests", matches = "true",
        disabledReason = "Local tests require PostgreSQL and RabbitMQ running locally. " +
                "Run with -Drun.local.tests=true")
class ProcessorLocalIntegrationTest {

    @Autowired
    private ContentRoutingService contentRoutingService;

    @Autowired
    private RawContentRepository rawContentRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private RssSourceRepository rssSourceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private EmbeddingService embeddingService;

    private static boolean propertiesInitialized = false;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!propertiesInitialized) {
            // Configure for local services
            registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://localhost:5432/javainfohunter");
            registry.add("spring.rabbitmq.port", () -> 25672);
            propertiesInitialized = true;
        }
    }

    @BeforeEach
    void setUp() {
        // Clean up database
        newsRepository.deleteAll();
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();

        // Mock AI services
        mockChatService();
        mockEmbeddingService();
    }

    @AfterEach
    void tearDown() {
        newsRepository.deleteAll();
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
        RawContent content = RawContent.builder()
                .rssSource(source)
                .guid("test-guid-123")
                .title("Test Article")
                .link("https://example.com/article")
                .rawContent("Test content here")
                .contentHash("unique-hash-abc123")
                .author("Test Author")
                .publishDate(Instant.now())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build();

        RawContent saved = rawContentRepository.save(content);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Article");
        assertThat(saved.getContentHash()).isEqualTo("unique-hash-abc123");
        assertThat(saved.getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.PENDING);
    }

    @Test
    void testRawContentRepository_ShouldUpdateProcessingStatus() {
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

        RawContent content = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Article")
                .rawContent("Content")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build());

        // Act - Mark as processing
        content.markAsProcessing();
        rawContentRepository.save(content);

        // Assert
        var retrieved = rawContentRepository.findById(content.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.PROCESSING);

        // Mark as completed
        retrieved.get().markAsCompleted();
        rawContentRepository.save(retrieved.get());

        var completed = rawContentRepository.findById(content.getId());
        assertThat(completed).isPresent();
        assertThat(completed.get().getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.COMPLETED);
        assertThat(completed.get().isCompleted()).isTrue();
    }

    @Test
    void testRawContentRepository_ShouldMarkAsFailed() {
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

        RawContent content = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Article")
                .rawContent("Content")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PROCESSING)
                .build());

        // Act
        content.markAsFailed("Processing failed due to timeout");
        rawContentRepository.save(content);

        // Assert
        var retrieved = rawContentRepository.findById(content.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProcessingStatus()).isEqualTo(RawContent.ProcessingStatus.FAILED);
        assertThat(retrieved.get().getErrorMessage()).isEqualTo("Processing failed due to timeout");
        assertThat(retrieved.get().isFailed()).isTrue();
    }

    @Test
    void testRawContentRepository_ShouldFindByProcessingStatus() {
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

        rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Article 1")
                .rawContent("Content 1")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build());

        rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-2")
                .title("Article 2")
                .rawContent("Content 2")
                .contentHash("hash-2")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build());

        rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-3")
                .title("Article 3")
                .rawContent("Content 3")
                .contentHash("hash-3")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build());

        // Act
        List<RawContent> pendingContent = rawContentRepository.findByProcessingStatus(
                RawContent.ProcessingStatus.PENDING);

        // Assert
        assertThat(pendingContent).hasSize(2);
        assertThat(pendingContent).allMatch(c -> c.getProcessingStatus() == RawContent.ProcessingStatus.PENDING);
    }

    @Test
    void testNewsRepository_ShouldStoreProcessedContent() {
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

        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Original Title")
                .rawContent("Original content")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.PROCESSING)
                .build());

        News news = News.builder()
                .rawContent(rawContent)
                .title("Processed Title")
                .summary("This is a summary")
                .fullContent("Full content here")
                .sentiment(News.Sentiment.POSITIVE)
                .sentimentScore(new BigDecimal("0.85"))
                .importanceScore(new BigDecimal("0.75"))
                .category("technology")
                .tags(new String[]{"tech", "ai"})
                .keywords(new String[]{"machine learning", "test"})
                .language("zh")
                .readingTimeMinutes(5)
                .isPublished(false)
                .build();

        // Act
        News savedNews = newsRepository.save(news);

        // Assert
        assertThat(savedNews.getId()).isNotNull();
        assertThat(savedNews.getTitle()).isEqualTo("Processed Title");
        assertThat(savedNews.getSummary()).isEqualTo("This is a summary");
        assertThat(savedNews.getSentiment()).isEqualTo(News.Sentiment.POSITIVE);
        assertThat(savedNews.getSentimentScore()).isEqualByComparingTo("0.85");
        assertThat(savedNews.getImportanceScore()).isEqualByComparingTo("0.75");
        assertThat(savedNews.getCategory()).isEqualTo("technology");
        assertThat(savedNews.getTags()).containsExactly("tech", "ai");
        assertThat(savedNews.getKeywords()).containsExactly("machine learning", "test");
        assertThat(savedNews.getLanguage()).isEqualTo("zh");
        assertThat(savedNews.getReadingTimeMinutes()).isEqualTo(5);
        assertThat(savedNews.isPublished()).isFalse();
    }

    @Test
    void testNewsRepository_ShouldFindByRawContent() {
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

        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Original Title")
                .rawContent("Original content")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build());

        News news = newsRepository.save(News.builder()
                .rawContent(rawContent)
                .title("Processed Title")
                .summary("Summary")
                .fullContent("Full content")
                .sentiment(News.Sentiment.NEUTRAL)
                .language("zh")
                .isPublished(false)
                .build());

        // Act
        Optional<News> found = newsRepository.findByRawContentId(rawContent.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Processed Title");
        assertThat(found.get().getRawContent().getId()).isEqualTo(rawContent.getId());
    }

    @Test
    void testNewsRepository_ShouldFindBySentiment() {
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

        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Original Title")
                .rawContent("Original content")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build());

        newsRepository.save(News.builder()
                .rawContent(rawContent)
                .title("Positive News")
                .summary("Good news")
                .fullContent("Full content")
                .sentiment(News.Sentiment.POSITIVE)
                .sentimentScore(new BigDecimal("0.8"))
                .importanceScore(new BigDecimal("0.7"))
                .language("zh")
                .isPublished(true)
                .build());

        newsRepository.save(News.builder()
                .rawContent(rawContent)
                .title("Negative News")
                .summary("Bad news")
                .fullContent("Full content")
                .sentiment(News.Sentiment.NEGATIVE)
                .sentimentScore(new BigDecimal("-0.6"))
                .importanceScore(new BigDecimal("0.5"))
                .language("zh")
                .isPublished(true)
                .build());

        // Act
        var positiveNewsPage = newsRepository.findBySentiment(News.Sentiment.POSITIVE,
                org.springframework.data.domain.PageRequest.of(0, 10));
        var negativeNewsPage = newsRepository.findBySentiment(News.Sentiment.NEGATIVE,
                org.springframework.data.domain.PageRequest.of(0, 10));

        // Assert
        assertThat(positiveNewsPage.getContent()).hasSize(1);
        assertThat(positiveNewsPage.getContent().get(0).getTitle()).isEqualTo("Positive News");

        assertThat(negativeNewsPage.getContent()).hasSize(1);
        assertThat(negativeNewsPage.getContent().get(0).getTitle()).isEqualTo("Negative News");
    }

    @Test
    void testNewsRepository_ShouldCountBySentiment() {
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

        RawContent rawContent = rawContentRepository.save(RawContent.builder()
                .rssSource(source)
                .guid("guid-1")
                .title("Original Title")
                .rawContent("Original content")
                .contentHash("hash-1")
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build());

        newsRepository.save(News.builder()
                .rawContent(rawContent)
                .title("News 1")
                .summary("Summary 1")
                .fullContent("Content 1")
                .sentiment(News.Sentiment.POSITIVE)
                .language("zh")
                .isPublished(true)
                .build());

        newsRepository.save(News.builder()
                .rawContent(rawContent)
                .title("News 2")
                .summary("Summary 2")
                .fullContent("Content 2")
                .sentiment(News.Sentiment.POSITIVE)
                .language("zh")
                .isPublished(true)
                .build());

        // Act
        long positiveCount = newsRepository.countBySentiment(News.Sentiment.POSITIVE);
        long neutralCount = newsRepository.countBySentiment(News.Sentiment.NEUTRAL);

        // Assert
        assertThat(positiveCount).isEqualTo(2);
        assertThat(neutralCount).isEqualTo(0);
    }

    @Test
    void testNews_ShouldCalculateEngagementScore() {
        // Arrange
        News news = News.builder()
                .title("Test News")
                .summary("Summary")
                .fullContent("Content")
                .sentiment(News.Sentiment.NEUTRAL)
                .viewCount(1000L)
                .likeCount(50)
                .shareCount(10)
                .language("zh")
                .isPublished(true)
                .build();

        // Act & Assert
        // Engagement score = views + 10*likes + 20*shares = 1000 + 500 + 200 = 1700
        assertThat(news.getEngagementScore()).isEqualTo(1700L);
    }

    @Test
    void testNews_ShouldCheckHighImportance() {
        // Arrange
        News highImportance = News.builder()
                .title("Important News")
                .summary("Important summary")
                .fullContent("Content")
                .sentiment(News.Sentiment.POSITIVE)
                .importanceScore(new BigDecimal("0.8"))
                .language("zh")
                .isPublished(true)
                .build();

        News lowImportance = News.builder()
                .title("Normal News")
                .summary("Normal summary")
                .fullContent("Content")
                .sentiment(News.Sentiment.NEUTRAL)
                .importanceScore(new BigDecimal("0.5"))
                .language("zh")
                .isPublished(true)
                .build();

        // Act & Assert
        assertThat(highImportance.isHighImportance()).isTrue();
        assertThat(lowImportance.isHighImportance()).isFalse();
    }

    @Test
    void testRawContentMessage_ShouldDeserializeFromJson() throws Exception {
        // Arrange
        String json = """
                {
                    "guid": "test-guid-123",
                    "title": "Test Article",
                    "link": "https://example.com/article",
                    "rawContent": "Test content",
                    "contentHash": "abc123def456",
                    "rssSourceId": 1,
                    "rssSourceName": "Test Feed",
                    "rssSourceUrl": "https://example.com/rss",
                    "author": "Test Author",
                    "publishDate": "2024-01-01T00:00:00Z",
                    "crawlDate": "2024-01-01T01:00:00Z",
                    "category": "technology",
                    "tags": ["tech", "test"]
                }
                """;

        // Act
        RawContentMessage message = objectMapper.readValue(json, RawContentMessage.class);

        // Assert
        assertThat(message.getGuid()).isEqualTo("test-guid-123");
        assertThat(message.getTitle()).isEqualTo("Test Article");
        assertThat(message.getContentHash()).isEqualTo("abc123def456");
        assertThat(message.getCategory()).isEqualTo("technology");
        assertThat(message.getTags()).containsExactly("tech", "test");
    }

    // Helper methods

    private void mockChatService() {
        when(chatService.chat(anyString(), any())).thenReturn("""
                {
                    "summary": "This is a generated summary",
                    "topics": ["technology", "AI"],
                    "keywords": ["test", "automation"],
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
}
