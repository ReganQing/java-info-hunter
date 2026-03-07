package com.ron.javainfohunter.e2e;

import com.ron.javainfohunter.e2e.helper.ApiTestHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Test: RSS Crawl to API Flow
 *
 * Tests the complete data flow from RSS crawling to API availability:
 * 1. Create RSS source
 * 2. Trigger crawl
 * 3. Verify raw content stored
 * 4. Verify processing complete
 * 5. Verify news available via API
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "testcontainers.enable=false"
})
public class CrawlProcessApiE2ETest extends BaseExternalServiceTest {

    @Value("${local.server.port:8080}")
    private int port;

    private ApiTestHelper apiHelper;
    private static Long testSourceId;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        apiHelper = new ApiTestHelper(baseUrl);
    }

    /**
     * Test 1: Create RSS source via API
     *
     * Given: Valid RSS source data
     * When: POST to /api/v1/rss-sources
     * Then: Source is created and ID is returned
     */
    @Test
    @Order(1)
    @DisplayName("Should create RSS source via API")
    void test01_CreateRssSource() {
        System.out.println("TEST 1: Creating RSS source via API");

        // Arrange
        String testFeedUrl = ApiTestHelper.getTestRssFeedUrl();
        Map<String, Object> requestBody = Map.of(
                "name", ApiTestHelper.generateUniqueName("Test RSS Source"),
                "url", testFeedUrl,
                "category", "Technology",
                "isActive", true,
                "crawlIntervalMinutes", 60
        );

        // Act
        Response response = apiHelper.createRssSource(requestBody);

        // Assert
        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.name", notNullValue())
                .body("data.url", equalTo(testFeedUrl))
                .body("data.category", equalTo("Technology"))
                .body("data.isActive", equalTo(true));

        // Store source ID for subsequent tests
        testSourceId = response.jsonPath().getLong("data.id");
        assertNotNull(testSourceId, "Source ID should not be null");
        System.out.println("Created RSS source with ID: " + testSourceId);
    }

    /**
     * Test 2: Trigger manual crawl via API
     *
     * Given: Valid RSS source ID
     * When: POST to /api/v1/rss-sources/{id}/crawl
     * Then: Crawl task is triggered successfully
     */
    @Test
    @Order(2)
    @DisplayName("Should trigger manual crawl via API")
    void test02_TriggerManualCrawl() {
        System.out.println("TEST 2: Triggering manual crawl");

        // Skip if testSourceId is null (previous test failed)
        Assumptions.assumeTrue(testSourceId != null, "Test source ID must exist from Test 1");

        // Act
        Response response = apiHelper.triggerCrawl(testSourceId);

        // Assert
        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.status", notNullValue())
                .body("data.message", notNullValue());

        String status = response.jsonPath().getString("data.status");
        System.out.println("Crawl triggered with status: " + status);
    }

    /**
     * Test 3: Verify raw content stored in database
     *
     * Given: Crawl has been triggered
     * When: Query raw_content table
     * Then: Raw content entries exist for the source
     *
     * NOTE: This test may fail if crawler module is not fully implemented.
     * Mark with @Disabled if needed.
     */
    @Test
    @Order(3)
    @DisplayName("Should verify raw content stored in database")
    @Disabled("Requires crawler module completion - TODO: Enable when crawler is ready")
    void test03_VerifyRawContentStored() {
        System.out.println("TEST 3: Verifying raw content storage");

        // Skip if testSourceId is null
        Assumptions.assumeTrue(testSourceId != null, "Test source ID must exist from Test 1");

        // Wait for crawl to complete (up to 30 seconds)
        boolean crawlCompleted = ApiTestHelper.waitForCondition(
                () -> checkRawContentExists(testSourceId),
                30,
                1000
        );

        assertTrue(crawlCompleted, "Raw content should be stored within 30 seconds");
    }

    /**
     * Test 4: Verify processing complete and news stored
     *
     * Given: Raw content exists
     * When: Query news table
     * Then: Processed news entries exist
     *
     * NOTE: This test may fail if processor module is not fully implemented.
     * Mark with @Disabled if needed.
     */
    @Test
    @Order(4)
    @DisplayName("Should verify processed news in database")
    @Disabled("Requires processor module completion - TODO: Enable when processor is ready")
    void test04_VerifyProcessedNews() {
        System.out.println("TEST 4: Verifying processed news");

        // Skip if testSourceId is null
        Assumptions.assumeTrue(testSourceId != null, "Test source ID must exist from Test 1");

        // Wait for processing to complete (up to 60 seconds)
        boolean processingCompleted = ApiTestHelper.waitForCondition(
                () -> checkProcessedNewsExists(testSourceId),
                60,
                2000
        );

        assertTrue(processingCompleted, "Processing should complete within 60 seconds");
    }

    /**
     * Test 5: Verify news available via API
     *
     * Given: News has been processed
     * When: GET /api/v1/news
     * Then: News articles are returned
     *
     * NOTE: This test may fail if API module is not fully implemented.
     * Mark with @Disabled if needed.
     */
    @Test
    @Order(5)
    @DisplayName("Should retrieve news via API")
    @Disabled("Requires API module completion - TODO: Enable when API is ready")
    void test05_VerifyNewsViaApi() {
        System.out.println("TEST 5: Verifying news via API");

        // Act - Get all news
        Response response = apiHelper.getNews(0, 20);

        // Assert
        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue())
                .body("data.totalElements", greaterThan(0));

        List<Map<String, Object>> newsList = response.jsonPath().getList("data.content");
        assertNotNull(newsList, "News list should not be null");
        assertTrue(newsList.size() > 0, "Should have at least one news article");

        System.out.println("Found " + newsList.size() + " news articles via API");

        // Verify news has required fields
        Map<String, Object> firstNews = newsList.get(0);
        assertNotNull(firstNews.get("id"), "News ID should not be null");
        assertNotNull(firstNews.get("title"), "Title should not be null");
        assertNotNull(firstNews.get("summary"), "Summary should not be null");
        assertNotNull(firstNews.get("publishedAt"), "Published date should not be null");
    }

    /**
     * Test 6: Verify full-text search works
     *
     * Given: News articles exist
     * When: GET /api/v1/news/search
     * Then: Relevant articles are returned
     *
     * NOTE: This test may fail if search functionality is not implemented.
     * Mark with @Disabled if needed.
     */
    @Test
    @Order(6)
    @DisplayName("Should search news via API")
    @Disabled("Requires search functionality - TODO: Enable when search is ready")
    void test06_VerifyNewsSearch() {
        System.out.println("TEST 6: Verifying news search");

        // Act - Search for "AI" or "Spring Boot"
        Response response = apiHelper.searchNews("AI", 0, 10);

        // Assert
        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());

        List<Map<String, Object>> searchResults = response.jsonPath().getList("data.content");
        System.out.println("Found " + searchResults.size() + " search results for 'AI'");
    }

    /**
     * Test 7: Verify trending news endpoint
     *
     * Given: News articles exist
     * When: GET /api/v1/news/trending
     * Then: Trending articles are returned
     *
     * NOTE: This test may fail if trending functionality is not implemented.
     * Mark with @Disabled if needed.
     */
    @Test
    @Order(7)
    @DisplayName("Should retrieve trending news via API")
    @Disabled("Requires trending functionality - TODO: Enable when trending is ready")
    void test07_VerifyTrendingNews() {
        System.out.println("TEST 7: Verifying trending news");

        // Act - Get trending news
        Response response = apiHelper.getTrendingNews(10);

        // Assert
        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data", notNullValue());

        List<Map<String, Object>> trendingNews = response.jsonPath().getList("data");
        System.out.println("Found " + trendingNews.size() + " trending news articles");
    }

    /**
     * Cleanup: Delete test RSS source
     */
    @AfterAll
    static void cleanup() {
        System.out.println("Cleaning up test data (source ID: " + testSourceId + ")");
        // Note: Cleanup is optional; keeping test data can be useful for debugging
    }

    // Helper methods for database checks
    // These would typically use repository classes or direct database queries

    private boolean checkRawContentExists(Long sourceId) {
        // TODO: Implement using RawContentRepository or direct JDBC query
        throw new UnsupportedOperationException(
                "checkRawContentExists not implemented. Tests using this method should be disabled.");
    }

    private boolean checkProcessedNewsExists(Long sourceId) {
        // TODO: Implement using NewsRepository or direct JDBC query
        throw new UnsupportedOperationException(
                "checkProcessedNewsExists not implemented. Tests using this method should be disabled.");
    }
}
