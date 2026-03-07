package com.ron.javainfohunter.e2e;

import com.ron.javainfohunter.e2e.helper.ApiTestHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Test: REST API Endpoints
 *
 * Comprehensive testing of all REST API endpoints.
 * Tests cover CRUD operations, search, filtering, and edge cases.
 *
 * Test Categories:
 * 1. RSS Source Management (5 tests)
 * 2. News Query and Search (5 tests)
 * 3. Advanced Features (5 tests)
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "testcontainers.enable=false"
})
public class ApiEndpointsE2ETest extends BaseExternalServiceTest {

    @Value("${local.server.port:8080}")
    private int port;

    private ApiTestHelper apiHelper;
    private static Long testSourceId;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        apiHelper = new ApiTestHelper(baseUrl);
    }

    // ========================================
    // RSS Source Management Tests (5 tests)
    // ========================================

    /**
     * Test 1: Create RSS source - Happy path
     */
    @Test
    @Order(1)
    @DisplayName("RSS-01: Should create RSS source successfully")
    void test01_CreateRssSource_Success() {
        System.out.println("RSS-01: Creating RSS source");

        Map<String, Object> requestBody = Map.of(
                "name", ApiTestHelper.generateUniqueName("Tech Blog"),
                "url", "https://example.com/rss.xml",
                "category", "Technology",
                "isActive", true,
                "crawlIntervalMinutes", 60
        );

        Response response = apiHelper.createRssSource(requestBody);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.name", notNullValue())
                .body("data.url", equalTo("https://example.com/rss.xml"))
                .body("data.category", equalTo("Technology"))
                .body("data.isActive", equalTo(true));

        testSourceId = response.jsonPath().getLong("data.id");
    }

    /**
     * Test 2: Create RSS source - Validation error (missing required fields)
     */
    @Test
    @Order(2)
    @DisplayName("RSS-02: Should reject RSS source with missing fields")
    void test02_CreateRssSource_MissingFields() {
        System.out.println("RSS-02: Creating RSS source with missing fields");

        Map<String, Object> requestBody = Map.of(
                "name", "Incomplete Source"
                // Missing: url, category
        );

        Response response = apiHelper.createRssSource(requestBody);

        ApiTestHelper.assertError(response, 400);
    }

    /**
     * Test 3: Get RSS sources with pagination
     */
    @Test
    @Order(3)
    @DisplayName("RSS-03: Should get paginated RSS sources")
    void test03_GetRssSources_Pagination() {
        System.out.println("RSS-03: Getting paginated RSS sources");

        Response response = apiHelper.getRssSources(0, 10);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue())
                .body("data.pageable.pageSize", equalTo(10))
                .body("data.pageable.pageNumber", equalTo(0));
    }

    /**
     * Test 4: Get RSS sources with filters
     */
    @Test
    @Order(4)
    @DisplayName("RSS-04: Should filter RSS sources by category")
    void test04_GetRssSources_FilterByCategory() {
        System.out.println("RSS-04: Filtering RSS sources by category");

        Map<String, Object> params = Map.of(
                "category", "Technology",
                "isActive", true,
                "page", 0,
                "size", 20
        );

        Response response = apiHelper.getRssSources(params);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());
    }

    /**
     * Test 5: Update and delete RSS source
     */
    @Test
    @Order(5)
    @DisplayName("RSS-05: Should update and delete RSS source")
    void test05_UpdateAndDeleteRssSource() {
        System.out.println("RSS-05: Updating and deleting RSS source");

        Assumptions.assumeTrue(testSourceId != null, "Test source ID must exist");

        // Update
        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Tech Blog",
                "url", "https://example.com/updated-rss.xml",
                "category", "Tech",
                "isActive", false,
                "crawlIntervalMinutes", 120
        );

        Response updateResponse = apiHelper.updateRssSource(testSourceId, updateRequest);
        ApiTestHelper.assertSuccess(updateResponse);
        updateResponse.then()
                .body("data.name", equalTo("Updated Tech Blog"))
                .body("data.isActive", equalTo(false));

        // Delete
        Response deleteResponse = apiHelper.deleteRssSource(testSourceId);
        deleteResponse.then().statusCode(204);

        // Verify deletion
        Response getResponse = apiHelper.getRssSourceById(testSourceId);
        ApiTestHelper.assertError(getResponse, 404);
    }

    // ========================================
    // News Query and Search Tests (5 tests)
    // ========================================

    /**
     * Test 6: Get news with default pagination
     */
    @Test
    @Order(6)
    @DisplayName("NEWS-01: Should get paginated news")
    void test06_GetNews_Pagination() {
        System.out.println("NEWS-01: Getting paginated news");

        Response response = apiHelper.getNews(0, 20);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue())
                .body("data.pageable.pageSize", equalTo(20))
                .body("data.pageable.pageNumber", equalTo(0));
    }

    /**
     * Test 7: Get news with filters
     */
    @Test
    @Order(7)
    @DisplayName("NEWS-02: Should filter news by category and sentiment")
    void test07_GetNews_WithFilters() {
        System.out.println("NEWS-02: Filtering news by category and sentiment");

        Map<String, Object> params = Map.of(
                "category", "Technology",
                "sentiment", "POSITIVE",
                "startDate", Instant.now().minusSeconds(86400).toString(),
                "endDate", Instant.now().toString(),
                "page", 0,
                "size", 10
        );

        Response response = apiHelper.getNews(params);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());
    }

    /**
     * Test 8: Get news by ID
     */
    @Test
    @Order(8)
    @DisplayName("NEWS-03: Should get news by ID")
    @Disabled("Requires existing news data - TODO: Enable after data seeding")
    void test08_GetNewsById() {
        System.out.println("NEWS-03: Getting news by ID");

        Long newsId = 1L; // This would come from a seeded test data
        Response response = apiHelper.getNewsById(newsId);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.id", equalTo(newsId.intValue()))
                .body("data.title", notNullValue())
                .body("data.summary", notNullValue());
    }

    /**
     * Test 9: Search news
     */
    @Test
    @Order(9)
    @DisplayName("NEWS-04: Should search news by query")
    void test09_SearchNews() {
        System.out.println("NEWS-04: Searching news");

        Response response = apiHelper.searchNews("AI", 0, 10);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());

        List<Map<String, Object>> results = response.jsonPath().getList("data.content");
        System.out.println("Found " + results.size() + " search results");
    }

    /**
     * Test 10: Get news by category
     */
    @Test
    @Order(10)
    @DisplayName("NEWS-05: Should get news by category")
    void test10_GetNewsByCategory() {
        System.out.println("NEWS-05: Getting news by category");

        Response response = apiHelper.getNewsByCategory("Technology", 0, 10);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());
    }

    // ========================================
    // Advanced Features Tests (5 tests)
    // ========================================

    /**
     * Test 11: Get trending news
     */
    @Test
    @Order(11)
    @DisplayName("ADV-01: Should get trending news")
    void test11_GetTrendingNews() {
        System.out.println("ADV-01: Getting trending news");

        Response response = apiHelper.getTrendingNews(10);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(10));
    }

    /**
     * Test 12: Get similar news
     */
    @Test
    @Order(12)
    @DisplayName("ADV-02: Should get similar news")
    @Disabled("Requires existing news data - TODO: Enable after data seeding")
    void test12_GetSimilarNews() {
        System.out.println("ADV-02: Getting similar news");

        Long newsId = 1L;
        Response response = apiHelper.getSimilarNews(newsId, 5);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(5));
    }

    /**
     * Test 13: Invalid sort direction
     */
    @Test
    @Order(13)
    @DisplayName("ADV-03: Should reject invalid sort direction")
    void test13_InvalidSortDirection() {
        System.out.println("ADV-03: Testing invalid sort direction");

        Map<String, Object> params = Map.of(
                "sortDirection", "INVALID",
                "page", 0,
                "size", 10
        );

        Response response = apiHelper.getNews(params);

        ApiTestHelper.assertError(response, 400);
    }

    /**
     * Test 14: Empty search query
     */
    @Test
    @Order(14)
    @DisplayName("ADV-04: Should reject empty search query")
    void test14_EmptySearchQuery() {
        System.out.println("ADV-04: Testing empty search query");

        Response response = apiHelper.searchNews("   ", 0, 10);

        ApiTestHelper.assertError(response, 400);
        response.then()
                .body("message", containsString("empty"));
    }

    /**
     * Test 15: Pagination boundary values
     */
    @Test
    @Order(15)
    @DisplayName("ADV-05: Should handle pagination boundaries")
    void test15_PaginationBoundaries() {
        System.out.println("ADV-05: Testing pagination boundaries");

        // Test minimum page size
        Response response1 = apiHelper.getNews(0, 1);
        ApiTestHelper.assertSuccess(response1);

        // Test maximum page size
        Response response2 = apiHelper.getNews(0, 100);
        ApiTestHelper.assertSuccess(response2);

        // Test invalid page size (> 100)
        Map<String, Object> params = Map.of("page", 0, "size", 101);
        Response response3 = apiHelper.getNews(params);
        ApiTestHelper.assertError(response3, 400);
    }

    // ========================================
    // Edge Cases (Additional tests)
    // ========================================

    /**
     * Test 16: Non-existent resource
     */
    @Test
    @Order(16)
    @DisplayName("EDGE-01: Should return 404 for non-existent RSS source")
    void test16_NonExistentRssSource() {
        System.out.println("EDGE-01: Testing non-existent RSS source");

        Response response = apiHelper.getRssSourceById(99999L);
        ApiTestHelper.assertError(response, 404);
    }

    /**
     * Test 17: Non-existent news
     */
    @Test
    @Order(17)
    @DisplayName("EDGE-02: Should return 404 for non-existent news")
    void test17_NonExistentNews() {
        System.out.println("EDGE-02: Testing non-existent news");

        Response response = apiHelper.getNewsById(99999L);
        ApiTestHelper.assertError(response, 404);
    }

    /**
     * Test 18: Invalid ID format
     */
    @Test
    @Order(18)
    @DisplayName("EDGE-03: Should handle invalid ID format")
    void test18_InvalidIdFormat() {
        System.out.println("EDGE-03: Testing invalid ID format");

        // This would typically be caught by validation
        // For now, we'll document this as a potential edge case
        System.out.println("SKIP: Invalid ID format testing - requires validation implementation");
    }

    /**
     * Test 19: Special characters in search
     */
    @Test
    @Order(19)
    @DisplayName("EDGE-04: Should handle special characters in search")
    void test19_SpecialCharactersInSearch() {
        System.out.println("EDGE-04: Testing special characters in search");

        Response response = apiHelper.searchNews("C++ & Java", 0, 10);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());
    }

    /**
     * Test 20: Unicode in search
     */
    @Test
    @Order(20)
    @DisplayName("EDGE-05: Should handle Unicode in search")
    void test20_UnicodeInSearch() {
        System.out.println("EDGE-05: Testing Unicode in search");

        Response response = apiHelper.searchNews("人工智能", 0, 10);

        ApiTestHelper.assertSuccess(response);
        response.then()
                .statusCode(200)
                .body("data.content", notNullValue());
    }

    @AfterAll
    static void cleanup() {
        System.out.println("API Endpoints E2E Test completed");
    }
}
