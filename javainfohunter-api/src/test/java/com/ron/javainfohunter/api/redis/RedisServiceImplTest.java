package com.ron.javainfohunter.api.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisServiceImpl
 *
 * Test Coverage:
 * - Caching operations (RSS sources, content processing status)
 * - Distributed locking (acquire, release, extend)
 * - Rate limiting (sliding window algorithm)
 * - Generic Redis operations
 * - Edge cases (null values, empty strings, TTL operations)
 */
@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        // Common mock setup - use lenient() for all
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Mock execute method for Lua scripts - return 1L by default for successful operations
        // Handle both 3-argument and 4-argument calls (varargs)
        lenient().when(redisTemplate.execute(
            any(),
            anyList(),
            any(Object.class),
            any(Object.class)
        )).thenReturn(1L);

        lenient().when(redisTemplate.execute(
            any(),
            anyList(),
            any(Object.class)
        )).thenReturn(1L);

        // Create service instance directly
        redisService = new RedisServiceImpl(redisTemplate, stringRedisTemplate);
    }

    /**
     * Helper method to reset RedisTemplate mock and set up for failure scenarios
     */
    private void setupForFailure() {
        reset(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Setup failure for both 3-argument and 4-argument calls
        lenient().when(redisTemplate.execute(
            any(),
            anyList(),
            any(Object.class),
            any(Object.class)
        )).thenReturn(0L);

        lenient().when(redisTemplate.execute(
            any(),
            anyList(),
            any(Object.class)
        )).thenReturn(0L);
    }

    // ==================== RSS Source Caching Tests ====================

    @Test
    void cacheRssSource_ShouldCacheSourceWithTtl() {
        // Given
        Long sourceId = 1L;
        String sourceName = "Tech Blog";
        Duration ttl = Duration.ofHours(1);

        // When
        redisService.cacheRssSource(sourceId, sourceName, ttl);

        // Then
        verify(valueOperations).set(
            startsWith("rss:source:"),
            eq(sourceName),
            eq(ttl)
        );
    }

    @Test
    void cacheRssSource_ShouldHandleNullSourceName() {
        // Given
        Long sourceId = 1L;
        String sourceName = null;
        Duration ttl = Duration.ofHours(1);

        // When & Then
        assertThatThrownBy(() -> redisService.cacheRssSource(sourceId, sourceName, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCachedRssSource_ShouldReturnCachedSource() {
        // Given
        Long sourceId = 1L;
        String expectedSource = "Tech Blog";
        when(valueOperations.get(anyString())).thenReturn(expectedSource);

        // When
        String actualSource = redisService.getCachedRssSource(sourceId);

        // Then
        assertThat(actualSource).isEqualTo(expectedSource);
        verify(valueOperations).get(endsWith("1"));
    }

    @Test
    void getCachedRssSource_ShouldReturnNullWhenNotCached() {
        // Given
        Long sourceId = 999L;
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        String result = redisService.getCachedRssSource(sourceId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void evictRssSource_ShouldRemoveCachedSource() {
        // When
        redisService.evictRssSource(1L);

        // Then
        verify(redisTemplate).delete(endsWith("1"));
    }

    // ==================== Content Processing Status Tests ====================

    @Test
    void isContentProcessed_ShouldReturnTrue_WhenContentExists() {
        // Given
        String contentHash = "abc123";
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        boolean result = redisService.isContentProcessed(contentHash);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(endsWith(contentHash));
    }

    @Test
    void isContentProcessed_ShouldReturnFalse_WhenContentNotExists() {
        // Given
        String contentHash = "xyz789";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        boolean result = redisService.isContentProcessed(contentHash);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void markContentProcessed_ShouldSetKeyWithTtl() {
        // Given
        String contentHash = "abc123";
        Duration ttl = Duration.ofDays(7);

        // When
        redisService.markContentProcessed(contentHash, ttl);

        // Then
        verify(valueOperations).set(
            endsWith(contentHash),
            eq("PROCESSED"),
            eq(ttl)
        );
    }

    // ==================== Distributed Lock Tests ====================

    @Test
    void acquireLock_ShouldReturnTrue_WhenLockAcquired() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "lock-token-123";
        Duration ttl = Duration.ofSeconds(30);

        // Debug: Verify mock is set up correctly
        System.out.println("Mock setup test - calling acquireLock");

        // When
        boolean result = redisService.acquireLock(lockKey, lockValue, ttl);

        // Debug: Print result
        System.out.println("AcquireLock result: " + result);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void acquireLock_ShouldReturnFalse_WhenLockNotAcquired() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "lock-token-123";
        Duration ttl = Duration.ofSeconds(30);
        setupForFailure();

        // When
        boolean result = redisService.acquireLock(lockKey, lockValue, ttl);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void acquireLock_ShouldThrowException_WhenLockKeyIsNull() {
        // Given
        String lockKey = null;
        String lockValue = "lock-token-123";
        Duration ttl = Duration.ofSeconds(30);

        // When & Then
        assertThatThrownBy(() -> redisService.acquireLock(lockKey, lockValue, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releaseLock_ShouldReturnTrue_WhenLockReleased() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "lock-token-123";

        // When
        boolean result = redisService.releaseLock(lockKey, lockValue);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void releaseLock_ShouldReturnFalse_WhenLockNotOwned() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "wrong-token";
        setupForFailure();

        // When
        boolean result = redisService.releaseLock(lockKey, lockValue);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void extendLock_ShouldReturnTrue_WhenLockExtended() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "lock-token-123";
        Duration additionalTtl = Duration.ofSeconds(30);

        // When
        boolean result = redisService.extendLock(lockKey, lockValue, additionalTtl);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void extendLock_ShouldReturnFalse_WhenLockNotOwned() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "wrong-token";
        Duration additionalTtl = Duration.ofSeconds(30);
        setupForFailure();

        // When
        boolean result = redisService.extendLock(lockKey, lockValue, additionalTtl);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Rate Limiting Tests ====================

    @Test
    void checkRateLimit_ShouldAllowRequest_WhenUnderLimit() {
        // Given
        String key = "api:user:123";
        int limit = 100;
        Duration window = Duration.ofMinutes(1);
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(50L);

        // When
        boolean result = redisService.checkRateLimit(key, limit, window);

        // Then
        assertThat(result).isTrue();
        verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        verify(zSetOperations).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void checkRateLimit_ShouldDenyRequest_WhenOverLimit() {
        // Given
        String key = "api:user:123";
        int limit = 100;
        Duration window = Duration.ofMinutes(1);
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(100L);

        // When
        boolean result = redisService.checkRateLimit(key, limit, window);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getRateLimitCount_ShouldReturnCurrentCount() {
        // Given
        String key = "api:user:123";
        Long expectedCount = 42L;
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(expectedCount);

        // When
        long actualCount = redisService.getRateLimitCount(key);

        // Then
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Test
    void resetRateLimit_ShouldClearWindow() {
        // Given
        String key = "api:user:123";

        // When
        redisService.resetRateLimit(key);

        // Then
        verify(stringRedisTemplate).delete(endsWith(key));
    }

    // ==================== Generic Operations Tests ====================

    @Test
    void set_ShouldSetValueWithTtl() {
        // Given
        String key = "test:key";
        Object value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        // When
        redisService.set(key, value, ttl);

        // Then
        verify(valueOperations).set(key, value, ttl);
    }

    @Test
    void set_ShouldThrowException_WhenKeyIsNull() {
        // Given
        String key = null;
        Object value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        // When & Then
        assertThatThrownBy(() -> redisService.set(key, value, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void get_ShouldReturnValue() {
        // Given
        String key = "test:key";
        Object expectedValue = "test-value";
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // When
        Object actualValue = redisService.get(key);

        // Then
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    void get_ShouldReturnNull_WhenKeyNotExists() {
        // Given
        String key = "nonexistent:key";
        when(valueOperations.get(key)).thenReturn(null);

        // When
        Object result = redisService.get(key);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void delete_ShouldRemoveKey() {
        // Given
        String key = "test:key";

        // When
        redisService.delete(key);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    void exists_ShouldReturnTrue_WhenKeyExists() {
        // Given
        String key = "test:key";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean result = redisService.exists(key);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void exists_ShouldReturnFalse_WhenKeyNotExists() {
        // Given
        String key = "nonexistent:key";
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // When
        boolean result = redisService.exists(key);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getTtl_ShouldReturnTtlInSeconds() {
        // Given
        String key = "test:key";
        Long expectedTtl = 300L;
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(expectedTtl);

        // When
        long actualTtl = redisService.getTtl(key);

        // Then
        assertThat(actualTtl).isEqualTo(expectedTtl);
    }

    @Test
    void getTtl_ShouldReturnNegativeOne_WhenKeyHasNoTtl() {
        // Given
        String key = "test:key";
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-1L);

        // When
        long ttl = redisService.getTtl(key);

        // Then
        assertThat(ttl).isEqualTo(-1L);
    }

    @Test
    void getTtl_ShouldReturnNegativeTwo_WhenKeyNotExists() {
        // Given
        String key = "nonexistent:key";
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

        // When
        long ttl = redisService.getTtl(key);

        // Then
        assertThat(ttl).isEqualTo(-2L);
    }

    // ==================== Edge Cases Tests ====================

    @Test
    void cacheRssSource_ShouldHandleEmptySourceName() {
        // Given
        Long sourceId = 1L;
        String sourceName = "";
        Duration ttl = Duration.ofHours(1);

        // When & Then
        assertThatThrownBy(() -> redisService.cacheRssSource(sourceId, sourceName, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markContentProcessed_ShouldHandleNullHash() {
        // Given
        String contentHash = null;
        Duration ttl = Duration.ofDays(7);

        // When & Then
        assertThatThrownBy(() -> redisService.markContentProcessed(contentHash, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkRateLimit_ShouldHandleNullKey() {
        // Given
        String key = null;
        int limit = 100;
        Duration window = Duration.ofMinutes(1);

        // When & Then
        assertThatThrownBy(() -> redisService.checkRateLimit(key, limit, window))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkRateLimit_ShouldHandleZeroLimit() {
        // Given
        String key = "api:user:123";
        int limit = 0;
        Duration window = Duration.ofMinutes(1);

        // When & Then
        assertThatThrownBy(() -> redisService.checkRateLimit(key, limit, window))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkRateLimit_ShouldHandleNegativeLimit() {
        // Given
        String key = "api:user:123";
        int limit = -10;
        Duration window = Duration.ofMinutes(1);

        // When & Then
        assertThatThrownBy(() -> redisService.checkRateLimit(key, limit, window))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkRateLimit_ShouldHandleNullWindow() {
        // Given
        String key = "api:user:123";
        int limit = 100;
        Duration window = null;

        // When & Then
        assertThatThrownBy(() -> redisService.checkRateLimit(key, limit, window))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acquireLock_ShouldHandleNullLockValue() {
        // Given
        String lockKey = "test:lock";
        String lockValue = null;
        Duration ttl = Duration.ofSeconds(30);

        // When & Then
        assertThatThrownBy(() -> redisService.acquireLock(lockKey, lockValue, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acquireLock_ShouldHandleNullTtl() {
        // Given
        String lockKey = "test:lock";
        String lockValue = "lock-token-123";
        Duration ttl = null;

        // When & Then
        assertThatThrownBy(() -> redisService.acquireLock(lockKey, lockValue, ttl))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
