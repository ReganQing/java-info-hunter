package com.ron.javainfohunter.api.integration;

import com.ron.javainfohunter.api.redis.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for infrastructure components
 *
 * These tests verify that:
 * - Redis connection works
 * - PostgreSQL connection works (via JPA)
 * - Rate limiting works end-to-end
 *
 * To run these tests:
 * 1. Start Docker Compose: docker-compose up -d
 * 2. Run tests: mvnw test -Dtest=InfrastructureIntegrationTest -Drun.integration.tests=true
 *
 * Note: These tests are disabled by default and only run when explicitly enabled
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "run.integration.tests", matches = "true")
class InfrastructureIntegrationTest {

    @Autowired
    private RedisService redisService;

    // ==================== Redis Integration Tests ====================

    @Test
    void redis_ShouldStoreAndRetrieveData() {
        // Given
        String key = "test:integration:key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        // When
        redisService.set(key, value, ttl);
        Object retrieved = redisService.get(key);

        // Then
        assertThat(retrieved).isEqualTo(value);

        // Cleanup
        redisService.delete(key);
    }

    @Test
    void redis_ShouldSupportExpiration() throws InterruptedException {
        // Given
        String key = "test:integration:expiry";
        String value = "test-value";
        Duration ttl = Duration.ofSeconds(2);

        // When
        redisService.set(key, value, ttl);

        // Then - immediately verify it exists
        assertThat(redisService.exists(key)).isTrue();
        assertThat(redisService.get(key)).isEqualTo(value);

        // Wait for expiration
        Thread.sleep(2500);

        // Verify it has expired
        assertThat(redisService.exists(key)).isFalse();
        assertThat(redisService.get(key)).isNull();
    }

    // ==================== Rate Limiting Integration Tests ====================

    @Test
    void rateLimit_ShouldAllowRequests_WhenUnderLimit() {
        // Given
        String key = "test:ratelimit:integration";
        int limit = 5;
        Duration window = Duration.ofSeconds(10);

        // When - make requests under the limit
        for (int i = 0; i < limit; i++) {
            boolean allowed = redisService.checkRateLimit(key, limit, window);
            assertThat(allowed).isTrue();
        }

        // Verify count
        long count = redisService.getRateLimitCount(key);
        assertThat(count).isEqualTo(limit);

        // Cleanup
        redisService.resetRateLimit(key);
    }

    @Test
    void rateLimit_ShouldDenyRequests_WhenOverLimit() {
        // Given
        String key = "test:ratelimit:exceeded";
        int limit = 3;
        Duration window = Duration.ofSeconds(10);

        // When - make requests up to the limit
        for (int i = 0; i < limit; i++) {
            redisService.checkRateLimit(key, limit, window);
        }

        // Next request should be denied
        boolean allowed = redisService.checkRateLimit(key, limit, window);

        // Then
        assertThat(allowed).isFalse();

        // Cleanup
        redisService.resetRateLimit(key);
    }

    @Test
    void rateLimit_ShouldReset_WhenWindowExpires() throws InterruptedException {
        // Given
        String key = "test:ratelimit:reset";
        int limit = 2;
        Duration window = Duration.ofSeconds(2);

        // When - make requests up to the limit
        for (int i = 0; i < limit; i++) {
            redisService.checkRateLimit(key, limit, window);
        }

        // Wait for window to expire
        Thread.sleep(2500);

        // Next request should be allowed
        boolean allowed = redisService.checkRateLimit(key, limit, window);

        // Then
        assertThat(allowed).isTrue();

        // Cleanup
        redisService.resetRateLimit(key);
    }

    // ==================== Distributed Lock Integration Tests ====================

    @Test
    void distributedLock_ShouldAcquireAndRelease() {
        // Given
        String lockKey = "test:lock:integration";
        String lockValue = "lock-holder-1";
        Duration ttl = Duration.ofSeconds(10);

        // When - acquire lock
        boolean acquired = redisService.acquireLock(lockKey, lockValue, ttl);

        // Then
        assertThat(acquired).isTrue();

        // Verify lock exists
        assertThat(redisService.exists("lock:" + lockKey)).isTrue();

        // Release lock
        boolean released = redisService.releaseLock(lockKey, lockValue);
        assertThat(released).isTrue();

        // Verify lock is gone
        assertThat(redisService.exists("lock:" + lockKey)).isFalse();
    }

    @Test
    void distributedLock_ShouldFail_WhenAlreadyLocked() {
        // Given
        String lockKey = "test:lock:contention";
        String lockValue1 = "holder-1";
        String lockValue2 = "holder-2";
        Duration ttl = Duration.ofSeconds(10);

        // When - first lock acquisition
        boolean acquired1 = redisService.acquireLock(lockKey, lockValue1, ttl);
        assertThat(acquired1).isTrue();

        // Second lock acquisition should fail
        boolean acquired2 = redisService.acquireLock(lockKey, lockValue2, ttl);

        // Then
        assertThat(acquired2).isFalse();

        // Cleanup
        redisService.releaseLock(lockKey, lockValue1);
    }

    @Test
    void distributedLock_ShouldExtend_WhenOwned() {
        // Given
        String lockKey = "test:lock:extend";
        String lockValue = "lock-holder";
        Duration ttl = Duration.ofSeconds(5);
        Duration extension = Duration.ofSeconds(10);

        // When - acquire lock
        boolean acquired = redisService.acquireLock(lockKey, lockValue, ttl);
        assertThat(acquired).isTrue();

        // Extend lock
        boolean extended = redisService.extendLock(lockKey, lockValue, extension);

        // Then
        assertThat(extended).isTrue();

        // Cleanup
        redisService.releaseLock(lockKey, lockValue);
    }

    @Test
    void distributedLock_ShouldNotExtend_WhenNotOwned() {
        // Given
        String lockKey = "test:lock:extend-fail";
        String lockValue1 = "holder-1";
        String lockValue2 = "holder-2";
        Duration ttl = Duration.ofSeconds(5);
        Duration extension = Duration.ofSeconds(10);

        // When - acquire lock
        boolean acquired = redisService.acquireLock(lockKey, lockValue1, ttl);
        assertThat(acquired).isTrue();

        // Try to extend with wrong owner
        boolean extended = redisService.extendLock(lockKey, lockValue2, extension);

        // Then
        assertThat(extended).isFalse();

        // Cleanup
        redisService.releaseLock(lockKey, lockValue1);
    }

    // ==================== Caching Integration Tests ====================

    @Test
    void caching_ShouldCacheRssSource() {
        // Given
        Long sourceId = 1L;
        String sourceName = "Test RSS Source";
        Duration ttl = Duration.ofHours(1);

        // When
        redisService.cacheRssSource(sourceId, sourceName, ttl);
        String retrieved = redisService.getCachedRssSource(sourceId);

        // Then
        assertThat(retrieved).isEqualTo(sourceName);

        // Cleanup
        redisService.evictRssSource(sourceId);
    }

    @Test
    void caching_ShouldMarkContentProcessed() {
        // Given
        String contentHash = "abc123def456";
        Duration ttl = Duration.ofDays(7);

        // When
        redisService.markContentProcessed(contentHash, ttl);

        // Then
        assertThat(redisService.isContentProcessed(contentHash)).isTrue();
        assertThat(redisService.isContentProcessed("different-hash")).isFalse();
    }
}
