package com.ron.javainfohunter.api.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis Service Implementation
 *
 * Features:
 * - Thread-safe operations using Redis atomic commands
 * - Distributed locking with Lua scripts for atomicity
 * - Sliding window rate limiting using sorted sets
 * - Comprehensive validation and logging
 *
 * Thread Safety: All operations are thread-safe using Redis single-threaded model
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // Lua scripts for distributed locking (atomic operations)
    private final DefaultRedisScript<Long> acquireLockScript;
    private final DefaultRedisScript<Long> releaseLockScript;
    private final DefaultRedisScript<Long> extendLockScript;

    // Prefix constants
    private static final String RSS_SOURCE_PREFIX = "rss:source:";
    private static final String CONTENT_PROCESSED_PREFIX = "content:processed:";
    private static final String LOCK_PREFIX = "lock:";
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    /**
     * Constructor with Lua script initialization
     */
    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate,
                           StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;

        // Initialize Lua scripts
        this.acquireLockScript = new DefaultRedisScript<>();
        this.acquireLockScript.setScriptText(
            "if redis.call('exists', KEYS[1]) == 0 then " +
                "redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
                "return 1 " +
            "else " +
                "return 0 " +
            "end"
        );
        this.acquireLockScript.setResultType(Long.class);

        this.releaseLockScript = new DefaultRedisScript<>();
        this.releaseLockScript.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "redis.call('del', KEYS[1]) " +
                "return 1 " +
            "else " +
                "return 0 " +
            "end"
        );
        this.releaseLockScript.setResultType(Long.class);

        this.extendLockScript = new DefaultRedisScript<>();
        this.extendLockScript.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "redis.call('pexpire', KEYS[1], ARGV[2]) " +
                "return 1 " +
            "else " +
                "return 0 " +
            "end"
        );
        this.extendLockScript.setResultType(Long.class);
    }

    // ==================== RSS Source Caching ====================

    @Override
    public void cacheRssSource(Long sourceId, String sourceName, Duration ttl) {
        validateNotNull(sourceId, "sourceId");
        validateNotBlank(sourceName, "sourceName");
        validateNotNull(ttl, "ttl");

        String key = RSS_SOURCE_PREFIX + sourceId;
        log.debug("Caching RSS source: {} = {}", key, sourceName);

        redisTemplate.opsForValue().set(key, sourceName, ttl);
    }

    @Override
    public String getCachedRssSource(Long sourceId) {
        validateNotNull(sourceId, "sourceId");

        String key = RSS_SOURCE_PREFIX + sourceId;
        Object value = redisTemplate.opsForValue().get(key);

        log.debug("Retrieved cached RSS source: {} = {}", key, value);
        return value != null ? value.toString() : null;
    }

    @Override
    public void evictRssSource(Long sourceId) {
        validateNotNull(sourceId, "sourceId");

        String key = RSS_SOURCE_PREFIX + sourceId;
        log.debug("Evicting RSS source from cache: {}", key);

        redisTemplate.delete(key);
    }

    // ==================== Content Processing Status ====================

    @Override
    public boolean isContentProcessed(String contentHash) {
        validateNotBlank(contentHash, "contentHash");

        String key = CONTENT_PROCESSED_PREFIX + contentHash;
        Boolean exists = redisTemplate.hasKey(key);

        log.debug("Checking if content processed: {} = {}", key, exists);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void markContentProcessed(String contentHash, Duration ttl) {
        validateNotBlank(contentHash, "contentHash");
        validateNotNull(ttl, "ttl");

        String key = CONTENT_PROCESSED_PREFIX + contentHash;
        log.debug("Marking content as processed: {}", key);

        redisTemplate.opsForValue().set(key, "PROCESSED", ttl);
    }

    // ==================== Distributed Locking ====================

    @Override
    public boolean acquireLock(String lockKey, String lockValue, Duration ttl) {
        validateNotBlank(lockKey, "lockKey");
        validateNotBlank(lockValue, "lockValue");
        validateNotNull(ttl, "ttl");

        String key = LOCK_PREFIX + lockKey;
        long ttlMs = ttl.toMillis();

        log.debug("Acquiring lock: {} with value: {}", key, lockValue);

        Long result = redisTemplate.execute(
            acquireLockScript,
            Collections.singletonList(key),
            lockValue,
            String.valueOf(ttlMs)
        );

        boolean acquired = Long.valueOf(1L).equals(result);
        log.debug("Lock acquire result: {} for key: {}", acquired, key);
        return acquired;
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        validateNotBlank(lockKey, "lockKey");
        validateNotBlank(lockValue, "lockValue");

        String key = LOCK_PREFIX + lockKey;

        log.debug("Releasing lock: {} with value: {}", key, lockValue);

        Long result = redisTemplate.execute(
            releaseLockScript,
            Collections.singletonList(key),
            lockValue
        );

        boolean released = Long.valueOf(1L).equals(result);
        log.debug("Lock release result: {} for key: {}", released, key);
        return released;
    }

    @Override
    public boolean extendLock(String lockKey, String lockValue, Duration additionalTtl) {
        validateNotBlank(lockKey, "lockKey");
        validateNotBlank(lockValue, "lockValue");
        validateNotNull(additionalTtl, "additionalTtl");

        String key = LOCK_PREFIX + lockKey;
        long ttlMs = additionalTtl.toMillis();

        log.debug("Extending lock: {} with value: {} by {}ms", key, lockValue, ttlMs);

        Long result = redisTemplate.execute(
            extendLockScript,
            Collections.singletonList(key),
            lockValue,
            String.valueOf(ttlMs)
        );

        boolean extended = Long.valueOf(1L).equals(result);
        log.debug("Lock extend result: {} for key: {}", extended, key);
        return extended;
    }

    // ==================== Rate Limiting ====================

    @Override
    public boolean checkRateLimit(String key, int limit, Duration window) {
        validateNotBlank(key, "key");
        validatePositive(limit, "limit");
        validateNotNull(window, "window");

        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - window.toMillis();

        log.debug("Checking rate limit: {} (limit: {}, window: {}ms)", rateLimitKey, limit, window.toMillis());

        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();

        // Remove entries outside the window
        zSetOps.removeRangeByScore(rateLimitKey, 0, windowStart);

        // Count current requests
        Long currentCount = zSetOps.count(rateLimitKey, windowStart, now);

        if (currentCount != null && currentCount < limit) {
            // Add current request
            zSetOps.add(rateLimitKey, String.valueOf(now), now);
            // Set expiry
            stringRedisTemplate.expire(rateLimitKey, window.plusSeconds(10));
            log.debug("Rate limit check passed: {}/{}", currentCount + 1, limit);
            return true;
        } else {
            log.warn("Rate limit exceeded: {}/{} for key: {}", currentCount, limit, rateLimitKey);
            return false;
        }
    }

    @Override
    public long getRateLimitCount(String key) {
        validateNotBlank(key, "key");

        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        long now = Instant.now().toEpochMilli();

        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        Long count = zSetOps.count(rateLimitKey, 0, now);

        log.debug("Current rate limit count for {}: {}", rateLimitKey, count);
        return count != null ? count : 0L;
    }

    @Override
    public void resetRateLimit(String key) {
        validateNotBlank(key, "key");

        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        log.debug("Resetting rate limit for key: {}", rateLimitKey);

        stringRedisTemplate.delete(rateLimitKey);
    }

    // ==================== Generic Operations ====================

    @Override
    public void set(String key, Object value, Duration ttl) {
        validateNotNull(key, "key");
        validateNotNull(ttl, "ttl");

        log.debug("Setting key: {} with TTL: {}s", key, ttl.getSeconds());
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Object get(String key) {
        validateNotNull(key, "key");

        Object value = redisTemplate.opsForValue().get(key);
        log.debug("Getting key: {} = {}", key, value);
        return value;
    }

    @Override
    public void delete(String key) {
        validateNotNull(key, "key");

        log.debug("Deleting key: {}", key);
        redisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        validateNotNull(key, "key");

        Boolean exists = redisTemplate.hasKey(key);
        log.debug("Key exists: {} = {}", key, exists);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public long getTtl(String key) {
        validateNotNull(key, "key");

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        log.debug("TTL for key: {} = {}s", key, ttl);
        return ttl != null ? ttl : -2L;
    }

    // ==================== Validation Helpers ====================

    private void validateNotNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
    }

    private void validateNotBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + " cannot be blank");
        }
    }

    private void validatePositive(int value, String paramName) {
        if (value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive");
        }
    }
}
