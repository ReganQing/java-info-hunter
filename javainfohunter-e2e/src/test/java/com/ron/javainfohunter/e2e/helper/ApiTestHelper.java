package com.ron.javainfohunter.e2e.helper;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API Test Helper Utility
 *
 * Provides common REST API testing operations for E2E tests.
 * Encapsulates RestAssured configuration and common request patterns.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public class ApiTestHelper {

    private static final Logger log = Logger.getLogger(ApiTestHelper.class.getName());

    private final String baseUrl;
    private String authToken;

    /**
     * Constructor with base URL
     *
     * @param baseUrl Base URL for API (e.g., http://localhost:8080)
     */
    public ApiTestHelper(String baseUrl) {
        this.baseUrl = baseUrl;
        RestAssured.baseURI = baseUrl;
        log.info(String.format("API Test Helper initialized with base URL: %s", baseUrl));
    }

    /**
     * Get base request specification with common headers
     *
     * @return RequestSpecification
     */
    public RequestSpecification getBaseRequest() {
        return given()
                .log().ifValidationFails()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    /**
     * Set authentication token for requests
     *
     * @param token JWT or Bearer token
     * @return this for fluent API
     */
    public ApiTestHelper withAuthToken(String token) {
        this.authToken = token;
        return this;
    }

    /**
     * Create RSS source via API
     *
     * @param requestBody Request body as Map
     * @return Response
     * @throws IllegalArgumentException if requestBody is null
     */
    public Response createRssSource(Map<String, Object> requestBody) {
        if (requestBody == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        log.info(String.format("Creating RSS source: %s", requestBody.get("name")));
        return getBaseRequest()
                .body(requestBody)
                .post("/api/v1/rss-sources");
    }

    /**
     * Get RSS sources with pagination
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @return Response
     * @throws IllegalArgumentException if page or size is negative
     */
    public Response getRssSources(int page, int size) {
        if (page < 0 || size < 0) {
            throw new IllegalArgumentException("Page and size must be non-negative");
        }
        return getBaseRequest()
                .queryParam("page", page)
                .queryParam("size", size)
                .get("/api/v1/rss-sources");
    }

    /**
     * Get RSS sources with filters
     *
     * @param params Query parameters as Map
     * @return Response
     * @throws IllegalArgumentException if params is null
     */
    public Response getRssSources(Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        return getBaseRequest()
                .queryParams(params)
                .get("/api/v1/rss-sources");
    }

    /**
     * Get RSS source by ID
     *
     * @param id RSS source ID
     * @return Response
     */
    public Response getRssSourceById(Long id) {
        return getBaseRequest()
                .get("/api/v1/rss-sources/{id}", id);
    }

    /**
     * Update RSS source
     *
     * @param id RSS source ID
     * @param requestBody Request body as Map
     * @return Response
     */
    public Response updateRssSource(Long id, Map<String, Object> requestBody) {
        log.info(String.format("Updating RSS source: %d", id));
        return getBaseRequest()
                .body(requestBody)
                .put("/api/v1/rss-sources/{id}", id);
    }

    /**
     * Delete RSS source
     *
     * @param id RSS source ID
     * @return Response
     */
    public Response deleteRssSource(Long id) {
        log.info(String.format("Deleting RSS source: %d", id));
        return getBaseRequest()
                .delete("/api/v1/rss-sources/{id}", id);
    }

    /**
     * Trigger crawl for RSS source
     *
     * @param id RSS source ID
     * @return Response
     */
    public Response triggerCrawl(Long id) {
        log.info(String.format("Triggering crawl for RSS source: %d", id));
        return getBaseRequest()
                .post("/api/v1/rss-sources/{id}/crawl", id);
    }

    /**
     * Get news with pagination
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @return Response
     */
    public Response getNews(int page, int size) {
        return getBaseRequest()
                .queryParam("page", page)
                .queryParam("size", size)
                .get("/api/v1/news");
    }

    /**
     * Get news with filters
     *
     * @param params Query parameters as Map
     * @return Response
     */
    public Response getNews(Map<String, Object> params) {
        return getBaseRequest()
                .queryParams(params)
                .get("/api/v1/news");
    }

    /**
     * Get news by ID
     *
     * @param id News ID
     * @return Response
     */
    public Response getNewsById(Long id) {
        return getBaseRequest()
                .get("/api/v1/news/{id}", id);
    }

    /**
     * Search news
     *
     * @param query Search query
     * @param page Page number (0-based)
     * @param size Page size
     * @return Response
     */
    public Response searchNews(String query, int page, int size) {
        return getBaseRequest()
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("size", size)
                .get("/api/v1/news/search");
    }

    /**
     * Get similar news
     *
     * @param id News ID
     * @param limit Maximum number of similar news
     * @return Response
     */
    public Response getSimilarNews(Long id, int limit) {
        return getBaseRequest()
                .queryParam("limit", limit)
                .get("/api/v1/news/{id}/similar", id);
    }

    /**
     * Get trending news
     *
     * @param limit Maximum number of trending news
     * @return Response
     */
    public Response getTrendingNews(int limit) {
        return getBaseRequest()
                .queryParam("limit", limit)
                .get("/api/v1/news/trending");
    }

    /**
     * Get news by category
     *
     * @param category Category name
     * @param page Page number (0-based)
     * @param size Page size
     * @return Response
     */
    public Response getNewsByCategory(String category, int page, int size) {
        return getBaseRequest()
                .queryParam("page", page)
                .queryParam("size", size)
                .get("/api/v1/news/category/{category}", category);
    }

    /**
     * Assert response has success status
     *
     * @param response Response to check
     */
    public static void assertSuccess(Response response) {
        response.then()
                .statusCode(anyOf(is(200), is(201), is(202)))
                .body("success", equalTo(true));
    }

    /**
     * Assert response has error status
     *
     * @param response Response to check
     * @param expectedStatusCode Expected HTTP status code
     */
    public static void assertError(Response response, int expectedStatusCode) {
        response.then()
                .statusCode(expectedStatusCode)
                .body("success", equalTo(false));
    }

    /**
     * Wait for condition with polling
     *
     * @param condition Condition to check
     * @param timeoutSeconds Timeout in seconds
     * @param pollIntervalMs Poll interval in milliseconds
     * @return true if condition met, false otherwise
     */
    public static boolean waitForCondition(CheckCondition condition,
                                           int timeoutSeconds,
                                           int pollIntervalMs) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.check()) {
                return true;
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Functional interface for condition checking
     */
    @FunctionalInterface
    public interface CheckCondition {
        boolean check();
    }

    /**
     * Generate test RSS feed URL
     * Configurable via system property: test.rss.feed.url
     *
     * @return Test RSS feed URL
     */
    public static String getTestRssFeedUrl() {
        return System.getProperty("test.rss.feed.url", "http://localhost:8089/test-rss-feed.xml");
    }

    /**
     * Generate unique test name
     *
     * @param prefix Name prefix
     * @return Unique name
     */
    public static String generateUniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
