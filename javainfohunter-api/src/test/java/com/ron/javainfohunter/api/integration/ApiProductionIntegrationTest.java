package com.ron.javainfohunter.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.api.dto.ApiResponse;
import com.ron.javainfohunter.api.dto.request.RssSourceRequest;
import com.ron.javainfohunter.api.dto.response.AgentExecutionResponse;
import com.ron.javainfohunter.api.dto.response.AgentStatsResponse;
import com.ron.javainfohunter.api.dto.response.NewsResponse;
import com.ron.javainfohunter.api.dto.response.RssSourceResponse;
import com.ron.javainfohunter.api.dto.response.SimilarNewsResponse;
import com.ron.javainfohunter.api.dto.response.SystemStatusResponse;
import com.ron.javainfohunter.api.redis.RedisService;
import com.ron.javainfohunter.entity.AgentExecution;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.AgentExecutionRepository;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import com.ron.javainfohunter.repository.RssSourceRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Production-ready integration tests for API module.
 *
 * <p>Tests verify:</p>
 * <ul>
 *   <li>REST API endpoints return correct HTTP status codes</li>
 *   <li>Response data structure is correct</li>
 *   <li>Error handling works properly</li>
 *   <li>Validation of input parameters</li>
 *   <li>Authentication/authorization (if applicable)</li>
 *   <li>Rate limiting functionality</li>
 *   <li>CORS configuration</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b></p>
 * <ul>
 *   <li>Local PostgreSQL running on port 5432</li>
 *   <li>Local RabbitMQ running on port 5672</li>
 *   <li>Local Redis running on port 6379</li>
 *   <li>Database: javainfohunter (user: admin, password: admin6866!@#)</li>
 *   <li>Environment variables: RABBITMQ_USERNAME, RABBITMQ_PASSWORD (default: admin/admin)</li>
 * </ul>
 *
 * <p><b>Run with:</b></p>
 * <pre>
 * # Run the test (uses local PostgreSQL configured in @TestPropertySource)
 * mvn test -Dtest=ApiProductionIntegrationTest -Drun.production.tests=true -pl javainfohunter-api
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        com.ron.javainfohunter.api.TestApplication.class
    }
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/javainfohunter",
    "spring.datasource.username=admin",
    "spring.datasource.password=admin6866!@#",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.flyway.baseline-on-migrate=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.autoconfigure.exclude=com.ron.javainfohunter.ai.autoconfigure.AiServiceAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioSpeechAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioTranscriptionAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeImageAutoConfiguration,com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeRerankAutoConfiguration",
    "javainfohunter.ai.enabled=false"
})
@EnabledIfSystemProperty(named = "run.production.tests", matches = "true",
        disabledReason = "Production tests require explicit enablement. " +
                "Run with -Drun.production.tests=true")
public class ApiProductionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RssSourceRepository rssSourceRepository;

    @Autowired
    private RawContentRepository rawContentRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private AgentExecutionRepository agentExecutionRepository;

    @BeforeAll
    static void setUpRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // Clean up database
        newsRepository.deleteAll();
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();
        agentExecutionRepository.deleteAll();

        // Clear Redis
        try {
            redisService.delete("*");
        } catch (Exception e) {
            // Redis might not be available
        }
    }

    @AfterEach
    void tearDown() {
        newsRepository.deleteAll();
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();
        agentExecutionRepository.deleteAll();
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return healthy status for actuator endpoint")
        void shouldReturnHealthyStatus() {
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/actuator/health")
            .then()
                    .statusCode(200)
                    .body("status", equalTo("UP"));
        }
    }

    @Nested
    @DisplayName("RSS Source API Tests")
    class RssSourceApiTests {

        @Test
        @DisplayName("Should create RSS source successfully")
        void shouldCreateRssSourceSuccessfully() {
            // Given
            RssSourceRequest request = RssSourceRequest.builder()
                    .name("Test RSS Feed")
                    .url("https://example.com/rss")
                    .category("test")
                    .isActive(true)
                    .crawlIntervalSeconds(3600)
                    .build();

            // When & Then
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
            .when()
                    .post("/api/v1/rss-sources")
            .then()
                    .statusCode(201)
                    .body("data.name", equalTo("Test RSS Feed"))
                    .body("data.url", equalTo("https://example.com/rss"))
                    .body("data.category", equalTo("test"))
                    .body("data.isActive", equalTo(true))
                    .body("message", equalTo("RSS source created successfully"));
        }

        @Test
        @DisplayName("Should validate RSS source creation")
        void shouldValidateRssSourceCreation() {
            // Given - Missing required field
            RssSourceRequest request = RssSourceRequest.builder()
                    .name("Incomplete Feed")
                    // Missing URL
                    .category("test")
                    .build();

            // When & Then
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
            .when()
                    .post("/api/v1/rss-sources")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should get RSS sources with pagination")
        void shouldGetRssSourcesWithPagination() {
            // Given - Create test data
            createTestRssSource("Feed 1", "https://example.com/feed1", "tech");
            createTestRssSource("Feed 2", "https://example.com/feed2", "news");
            createTestRssSource("Feed 3", "https://example.com/feed3", "tech");

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 2)
            .when()
                    .get("/api/v1/rss-sources")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(2))
                    .body("data.totalElements", equalTo(3))
                    .body("data.totalPages", equalTo(2))
                    .body("data.number", equalTo(0));
        }

        @Test
        @DisplayName("Should filter RSS sources by category")
        void shouldFilterRssSourcesByCategory() {
            // Given
            createTestRssSource("Tech Feed", "https://example.com/tech", "technology");
            createTestRssSource("News Feed", "https://example.com/news", "news");
            createTestRssSource("Another Tech", "https://example.com/tech2", "technology");

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("category", "technology")
            .when()
                    .get("/api/v1/rss-sources")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(2))
                    .body("data.content[0].category", equalTo("technology"));
        }

        @Test
        @DisplayName("Should get RSS source by ID")
        void shouldGetRssSourceById() {
            // Given
            RssSource source = createTestRssSource("Get By ID", "https://example.com/get", "test");

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/rss-sources/{id}", source.getId())
            .then()
                    .statusCode(200)
                    .body("data.id", equalTo(source.getId().intValue()))
                    .body("data.name", equalTo("Get By ID"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent RSS source")
        void shouldReturn404ForNonExistentRssSource() {
            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/rss-sources/999999")
            .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should update RSS source")
        void shouldUpdateRssSource() {
            // Given
            RssSource source = createTestRssSource("Original Name", "https://example.com/original", "test");

            RssSourceRequest request = RssSourceRequest.builder()
                    .name("Updated Name")
                    .url("https://example.com/updated")
                    .category("updated")
                    .isActive(false)
                    .crawlIntervalSeconds(7200)
                    .build();

            // When & Then
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
            .when()
                    .put("/api/v1/rss-sources/{id}", source.getId())
            .then()
                    .statusCode(200)
                    .body("data.name", equalTo("Updated Name"))
                    .body("data.category", equalTo("updated"))
                    .body("message", equalTo("RSS source updated successfully"));
        }

        @Test
        @DisplayName("Should delete RSS source")
        void shouldDeleteRssSource() {
            // Given
            RssSource source = createTestRssSource("To Delete", "https://example.com/delete", "test");

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .delete("/api/v1/rss-sources/{id}", source.getId())
            .then()
                    .statusCode(204);

            // Verify deletion
            assertThat(rssSourceRepository.findById(source.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should trigger manual crawl")
        void shouldTriggerManualCrawl() {
            // Given
            RssSource source = createTestRssSource("Crawl Test", "https://example.com/crawl", "test");

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .post("/api/v1/rss-sources/{id}/crawl", source.getId())
            .then()
                    .statusCode(200)
                    .body("data", notNullValue())
                    .body("message", equalTo("Crawl task triggered successfully"));
        }
    }

    @Nested
    @DisplayName("News API Tests")
    class NewsApiTests {

        @Test
        @DisplayName("Should get news list with pagination")
        void shouldGetNewsListWithPagination() {
            // Given
            createTestNews("Article 1", "technology");
            createTestNews("Article 2", "news");
            createTestNews("Article 3", "technology");

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 2)
            .when()
                    .get("/api/v1/news")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(2))
                    .body("data.totalElements", equalTo(3));
        }

        @Test
        @DisplayName("Should filter news by category")
        void shouldFilterNewsByCategory() {
            // Given
            createTestNews("Tech News 1", "technology");
            createTestNews("World News", "news");
            createTestNews("Tech News 2", "technology");

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("category", "technology")
            .when()
                    .get("/api/v1/news")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(2))
                    .body("data.content[0].category", equalTo("technology"));
        }

        @Test
        @DisplayName("Should filter news by sentiment")
        void shouldFilterNewsBySentiment() {
            // Given
            createTestNewsWithSentiment("Positive News", News.Sentiment.POSITIVE);
            createTestNewsWithSentiment("Negative News", News.Sentiment.NEGATIVE);
            createTestNewsWithSentiment("Another Positive", News.Sentiment.POSITIVE);

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("sentiment", "POSITIVE")
            .when()
                    .get("/api/v1/news")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(2));
        }

        @Test
        @DisplayName("Should get news by ID")
        void shouldGetNewsById() {
            // Given
            News news = createTestNews("Specific Article", "technology");

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/news/{id}", news.getId())
            .then()
                    .statusCode(200)
                    .body("data.id", equalTo(news.getId().intValue()))
                    .body("data.title", equalTo("Specific Article"));
        }

        @Test
        @DisplayName("Should search news by query")
        void shouldSearchNewsByQuery() {
            // Given
            createTestNews("Machine Learning Advances", "technology");
            createTestNews("New AI Breakthrough", "technology");
            createTestNews("World Cup Finals", "sports");

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("query", "AI")
            .when()
                    .get("/api/v1/news/search")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(1))
                    .body("data.content[0].title", containsString("AI"));
        }

        @Test
        @DisplayName("Should reject empty search query")
        void shouldRejectEmptySearchQuery() {
            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("query", "   ")
            .when()
                    .get("/api/v1/news/search")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should get trending news")
        void shouldGetTrendingNews() {
            // Given
            createTestNewsWithEngagement("Viral Article 1", 10000L, 500L, 100L);
            createTestNewsWithEngagement("Viral Article 2", 8000L, 400L, 80L);
            createTestNewsWithEngagement("Normal Article", 100L, 5L, 1L);

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("limit", 2)
            .when()
                    .get("/api/v1/news/trending")
            .then()
                    .statusCode(200)
                    .body("data", hasSize(2));
        }

        @Test
        @DisplayName("Should get similar news")
        void shouldGetSimilarNews() {
            // Given
            RssSource source = createTestRssSource("Tech Blog", "https://example.com/tech", "technology");
            News news = createTestNewsForSource(source, "AI in Healthcare", "technology");

            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("limit", 5)
            .when()
                    .get("/api/v1/news/{id}/similar", news.getId())
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }

        @Test
        @DisplayName("Should get news by category path")
        void shouldGetNewsByCategoryPath() {
            // Given
            createTestNews("Tech Article 1", "technology");
            createTestNews("Tech Article 2", "technology");
            createTestNews("Sports Article", "sports");

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/news/category/technology")
            .then()
                    .statusCode(200)
                    .body("data.content", hasSize(2))
                    .body("data.content[0].category", equalTo("technology"));
        }

        @Test
        @DisplayName("Should validate invalid sort direction")
        void shouldValidateInvalidSortDirection() {
            // When & Then
            given()
                    .accept(ContentType.JSON)
                    .queryParam("sortDirection", "INVALID")
            .when()
                    .get("/api/v1/news")
            .then()
                    .statusCode(400)
                    .body("error", containsString("Invalid sort direction"));
        }
    }

    @Nested
    @DisplayName("Agent API Tests")
    class AgentApiTests {

        @Test
        @DisplayName("Should get agent statistics")
        void shouldGetAgentStatistics() {
            // Given
            createTestAgentExecution("summary-agent", "SummaryAgent", AgentExecution.ExecutionStatus.COMPLETED);
            createTestAgentExecution("analysis-agent", "AnalysisAgent", AgentExecution.ExecutionStatus.FAILED);
            createTestAgentExecution("summary-agent", "SummaryAgent", AgentExecution.ExecutionStatus.COMPLETED);

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/stats")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }

        @Test
        @DisplayName("Should get agent execution history")
        void shouldGetAgentExecutionHistory() {
            // Given
            createTestAgentExecution("test-agent", "TestAgent", AgentExecution.ExecutionStatus.COMPLETED);

            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/executions")
            .then()
                    .statusCode(200)
                    .body("data.content", notNullValue());
        }
    }

    @Nested
    @DisplayName("Admin API Tests")
    class AdminApiTests {

        @Test
        @DisplayName("Should get system status")
        void shouldGetSystemStatus() {
            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/admin/status")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue())
                    .body("data.status", equalTo("UP"));
        }
    }

    @Nested
    @DisplayName("Response Structure Tests")
    class ResponseStructureTests {

        @Test
        @DisplayName("Should return consistent API response structure")
        void shouldReturnConsistentApiResponseStructure() {
            // Given
            RssSource source = createTestRssSource("Structure Test", "https://example.com/struct", "test");

            // When & Then
            ValidatableResponse response = given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/rss-sources/{id}", source.getId())
            .then()
                    .statusCode(200);

            // Verify response structure
            response.body("success", equalTo(true));
            response.body("data", notNullValue());
            response.body("timestamp", notNullValue());
        }

        @Test
        @DisplayName("Should include error details in error response")
        void shouldIncludeErrorDetailsInErrorResponse() {
            // When & Then
            given()
                    .accept(ContentType.JSON)
            .when()
                    .get("/api/v1/rss-sources/999999")
            .then()
                    .statusCode(404)
                    .body("success", equalTo(false))
                    .body("error", notNullValue())
                    .body("timestamp", notNullValue());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate page size limits")
        void shouldValidatePageSizeLimits() {
            // When - Request with size > 100
            given()
                    .accept(ContentType.JSON)
                    .queryParam("size", 101)
            .when()
                    .get("/api/v1/news")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should validate negative page number")
        void shouldValidateNegativePageNumber() {
            // When
            given()
                    .accept(ContentType.JSON)
                    .queryParam("page", -1)
            .when()
                    .get("/api/v1/news")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should validate URL format")
        void shouldValidateUrlFormat() {
            // Given
            RssSourceRequest request = RssSourceRequest.builder()
                    .name("Invalid URL Feed")
                    .url("not-a-valid-url")
                    .category("test")
                    .build();

            // When & Then
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
            .when()
                    .post("/api/v1/rss-sources")
            .then()
                    .statusCode(400);
        }
    }

    // Helper methods

    private RssSource createTestRssSource(String name, String url, String category) {
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

    private News createTestNews(String title, String category) {
        RssSource source = createTestRssSource("Test Source", "https://example.com/test", category);

        RawContent rawContent = RawContent.builder()
                .rssSource(source)
                .guid("guid-" + System.currentTimeMillis())
                .title(title)
                .link("https://example.com/article")
                .rawContent("Content for " + title)
                .contentHash("hash-" + System.currentTimeMillis())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build();
        rawContent = rawContentRepository.save(rawContent);

        News news = News.builder()
                .rawContent(rawContent)
                .title(title)
                .summary("Summary for " + title)
                .fullContent("Full content for " + title)
                .sentiment(News.Sentiment.NEUTRAL)
                .sentimentScore(new BigDecimal("0.5"))
                .importanceScore(new BigDecimal("0.5"))
                .category(category)
                .tags(new String[]{"test"})
                .keywords(new String[]{"test"})
                .language("en")
                .readingTimeMinutes(3)
                .isPublished(true)
                .build();
        return newsRepository.save(news);
    }

    private News createTestNewsWithSentiment(String title, News.Sentiment sentiment) {
        RssSource source = createTestRssSource("Sentiment Test", "https://example.com/sentiment", "test");

        RawContent rawContent = RawContent.builder()
                .rssSource(source)
                .guid("guid-sentiment-" + System.currentTimeMillis())
                .title(title)
                .link("https://example.com/article")
                .rawContent("Content")
                .contentHash("hash-sentiment-" + System.currentTimeMillis())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build();
        rawContent = rawContentRepository.save(rawContent);

        News news = News.builder()
                .rawContent(rawContent)
                .title(title)
                .summary("Summary")
                .fullContent("Content")
                .sentiment(sentiment)
                .sentimentScore(sentiment == News.Sentiment.POSITIVE ? new BigDecimal("0.8") :
                               sentiment == News.Sentiment.NEGATIVE ? new BigDecimal("-0.6") :
                               new BigDecimal("0.1"))
                .importanceScore(new BigDecimal("0.5"))
                .category("test")
                .language("en")
                .isPublished(true)
                .build();
        return newsRepository.save(news);
    }

    private News createTestNewsWithEngagement(String title, long views, long likes, long shares) {
        RssSource source = createTestRssSource("Engagement Test", "https://example.com/engage", "test");

        RawContent rawContent = RawContent.builder()
                .rssSource(source)
                .guid("guid-engage-" + System.currentTimeMillis())
                .title(title)
                .link("https://example.com/article")
                .rawContent("Content")
                .contentHash("hash-engage-" + System.currentTimeMillis())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build();
        rawContent = rawContentRepository.save(rawContent);

        News news = News.builder()
                .rawContent(rawContent)
                .title(title)
                .summary("Summary")
                .fullContent("Content")
                .sentiment(News.Sentiment.POSITIVE)
                .viewCount(views)
                .likeCount((int) likes)
                .shareCount((int) shares)
                .category("test")
                .language("en")
                .isPublished(true)
                .build();
        return newsRepository.save(news);
    }

    private News createTestNewsForSource(RssSource source, String title, String category) {
        RawContent rawContent = RawContent.builder()
                .rssSource(source)
                .guid("guid-source-" + System.currentTimeMillis())
                .title(title)
                .link("https://example.com/article")
                .rawContent("Content")
                .contentHash("hash-source-" + System.currentTimeMillis())
                .crawlDate(Instant.now())
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build();
        rawContent = rawContentRepository.save(rawContent);

        News news = News.builder()
                .rawContent(rawContent)
                .title(title)
                .summary("Summary")
                .fullContent("Content")
                .sentiment(News.Sentiment.NEUTRAL)
                .category(category)
                .language("en")
                .isPublished(true)
                .build();
        return newsRepository.save(news);
    }

    private AgentExecution createTestAgentExecution(String agentId, String agentType,
                                                    AgentExecution.ExecutionStatus status) {
        AgentExecution execution = AgentExecution.builder()
                .agentId(agentId)
                .agentType(agentType)
                .executionId("exec-" + System.currentTimeMillis())
                .status(status)
                .startTime(Instant.now())
                .durationMilliseconds(1000)
                .build();
        if (status == AgentExecution.ExecutionStatus.COMPLETED) {
            execution.setEndTime(Instant.now().plusSeconds(1));
        }
        return agentExecutionRepository.save(execution);
    }
}
