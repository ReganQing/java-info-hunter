package com.ron.javainfohunter.e2e.performance;

import com.ron.javainfohunter.e2e.BaseExternalServiceTest;
import com.ron.javainfohunter.e2e.helper.ApiTestHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Virtual Thread Stress Test
 *
 * Performance test using Java 21 virtual threads to simulate high concurrency.
 * Tests the system's ability to handle 10,000 concurrent requests efficiently.
 *
 * Test Scenarios:
 * 1. Concurrent RSS source creation (1,000 requests)
 * 2. Concurrent news queries (10,000 requests)
 * 3. Concurrent search operations (5,000 requests)
 *
 * Performance Metrics:
 * - Total execution time
 * - Throughput (requests/second)
 * - Error rate
 * - P50, P95, P99 latency
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "testcontainers.enable=false"
})
public class VirtualThreadStressTest extends BaseExternalServiceTest {

    @Value("${local.server.port:8080}")
    private int port;

    private ApiTestHelper apiHelper;

    // Performance thresholds (adjust based on requirements)
    private static final int MAX_TOTAL_TIME_MS = 30000; // 30 seconds for 10K requests
    private static final double MAX_ERROR_RATE = 0.01; // 1% max error rate
    private static final int P95_LATENCY_MS = 1000; // P95 latency under 1 second

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        apiHelper = new ApiTestHelper(baseUrl);
    }

    /**
     * Test 1: 1,000 concurrent RSS source creation requests
     *
     * This test creates 1,000 RSS sources concurrently using virtual threads.
     * It tests the system's ability to handle write operations under high concurrency.
     */
    @Test
    @Order(1)
    @DisplayName("Should handle 1,000 concurrent RSS source creations")
    @Disabled("Requires API module completion - TODO: Enable when API is ready")
    void test01_ConcurrentRssSourceCreation() {
        System.out.println("=== Test 1: 1,000 Concurrent RSS Source Creations ===");

        int numberOfRequests = 1000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();

        Instant startTime = Instant.now();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfRequests; i++) {
                final int requestId = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        Instant requestStart = Instant.now();

                        Map<String, Object> requestBody = Map.of(
                                "name", ApiTestHelper.generateUniqueName("Stress Test Source"),
                                "url", "https://example.com/stress-test-" + requestId + ".xml",
                                "category", "Stress Test",
                                "isActive", true,
                                "crawlIntervalMinutes", 60
                        );

                        Response response = apiHelper.createRssSource(requestBody);

                        Instant requestEnd = Instant.now();
                        long latency = Duration.between(requestStart, requestEnd).toMillis();
                        latencies.add(latency);

                        if (response.getStatusCode() == 201) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Request " + requestId + " failed: " + e.getMessage());
                    }
                });
                futures.add(future);
            }

            // Wait for all requests to complete
            for (Future<?> future : futures) {
                try {
                    future.get(60, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request timed out");
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request failed: " + e.getMessage());
                }
            }
        }

        Instant endTime = Instant.now();
        long totalTime = Duration.between(startTime, endTime).toMillis();

        // Print performance metrics
        printPerformanceMetrics(numberOfRequests, successCount.get(), errorCount.get(),
                totalTime, latencies);

        // Assertions
        double errorRate = (double) errorCount.get() / numberOfRequests;
        assertTrue(errorRate <= MAX_ERROR_RATE,
                "Error rate should be <= " + (MAX_ERROR_RATE * 100) + "%, but was " + (errorRate * 100) + "%");
    }

    /**
     * Test 2: 10,000 concurrent news query requests
     *
     * This test performs 10,000 news queries concurrently using virtual threads.
     * It tests the system's ability to handle read operations under high concurrency.
     */
    @Test
    @Order(2)
    @DisplayName("Should handle 10,000 concurrent news queries")
    @Disabled("Requires API module completion - TODO: Enable when API is ready")
    void test02_ConcurrentNewsQueries() {
        System.out.println("=== Test 2: 10,000 Concurrent News Queries ===");

        int numberOfRequests = 10000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();

        Instant startTime = Instant.now();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfRequests; i++) {
                final int requestId = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        Instant requestStart = Instant.now();

                        Response response = apiHelper.getNews(0, 20);

                        Instant requestEnd = Instant.now();
                        long latency = Duration.between(requestStart, requestEnd).toMillis();
                        latencies.add(latency);

                        if (response.getStatusCode() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Request " + requestId + " failed: " + e.getMessage());
                    }
                });
                futures.add(future);
            }

            // Wait for all requests to complete
            for (Future<?> future : futures) {
                try {
                    future.get(60, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request timed out");
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request failed: " + e.getMessage());
                }
            }
        }

        Instant endTime = Instant.now();
        long totalTime = Duration.between(startTime, endTime).toMillis();

        // Print performance metrics
        printPerformanceMetrics(numberOfRequests, successCount.get(), errorCount.get(),
                totalTime, latencies);

        // Assertions
        assertTrue(totalTime <= MAX_TOTAL_TIME_MS,
                "Total time should be <= " + MAX_TOTAL_TIME_MS + "ms, but was " + totalTime + "ms");

        double throughput = (double) successCount.get() / (totalTime / 1000.0);
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");

        double errorRate = (double) errorCount.get() / numberOfRequests;
        assertTrue(errorRate <= MAX_ERROR_RATE,
                "Error rate should be <= " + (MAX_ERROR_RATE * 100) + "%, but was " + (errorRate * 100) + "%");
    }

    /**
     * Test 3: 5,000 concurrent search operations
     *
     * This test performs 5,000 search queries concurrently using virtual threads.
     * It tests the system's ability to handle complex search operations under high concurrency.
     */
    @Test
    @Order(3)
    @DisplayName("Should handle 5,000 concurrent search operations")
    @Disabled("Requires API module completion - TODO: Enable when API is ready")
    void test03_ConcurrentSearchOperations() {
        System.out.println("=== Test 3: 5,000 Concurrent Search Operations ===");

        int numberOfRequests = 5000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();

        Instant startTime = Instant.now();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfRequests; i++) {
                final int requestId = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        Instant requestStart = Instant.now();

                        Response response = apiHelper.searchNews("AI", 0, 10);

                        Instant requestEnd = Instant.now();
                        long latency = Duration.between(requestStart, requestEnd).toMillis();
                        latencies.add(latency);

                        if (response.getStatusCode() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Request " + requestId + " failed: " + e.getMessage());
                    }
                });
                futures.add(future);
            }

            // Wait for all requests to complete
            for (Future<?> future : futures) {
                try {
                    future.get(60, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request timed out");
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request failed: " + e.getMessage());
                }
            }
        }

        Instant endTime = Instant.now();
        long totalTime = Duration.between(startTime, endTime).toMillis();

        // Print performance metrics
        printPerformanceMetrics(numberOfRequests, successCount.get(), errorCount.get(),
                totalTime, latencies);

        // Assertions
        double throughput = (double) successCount.get() / (totalTime / 1000.0);
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");

        double errorRate = (double) errorCount.get() / numberOfRequests;
        assertTrue(errorRate <= MAX_ERROR_RATE,
                "Error rate should be <= " + (MAX_ERROR_RATE * 100) + "%, but was " + (errorRate * 100) + "%");
    }

    /**
     * Helper method to print performance metrics
     */
    private void printPerformanceMetrics(int totalRequests, int successCount, int errorCount,
                                         long totalTimeMs, List<Long> latencies) {
        System.out.println("\n=== Performance Metrics ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + errorCount);
        System.out.println("Total Time: " + totalTimeMs + "ms");

        double throughput = (double) successCount / (totalTimeMs / 1000.0);
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");

        double errorRate = (double) errorCount / totalRequests;
        System.out.println("Error Rate: " + String.format("%.4f", errorRate * 100) + "%");

        if (!latencies.isEmpty()) {
            latencies.sort(Long::compareTo);
            long p50 = latencies.get((int) (latencies.size() * 0.5));
            long p95 = latencies.get((int) (latencies.size() * 0.95));
            long p99 = latencies.get((int) (latencies.size() * 0.99));
            long avg = latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();

            System.out.println("\nLatency (ms):");
            System.out.println("  Average: " + avg);
            System.out.println("  P50: " + p50);
            System.out.println("  P95: " + p95);
            System.out.println("  P99: " + p99);

            // Assert P95 latency
            assertTrue(p95 <= P95_LATENCY_MS,
                    "P95 latency should be <= " + P95_LATENCY_MS + "ms, but was " + p95 + "ms");
        }

        System.out.println("========================\n");
    }
}
