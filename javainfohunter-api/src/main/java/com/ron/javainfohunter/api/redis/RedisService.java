package com.ron.javainfohunter.api.redis;

import java.time.Duration;

/**
 * Redis Service Interface
 *
 * Provides high-level Redis operations for:
 * - Caching (RSS sources, content processing status)
 * - Distributed locking (acquire, release, extend)
 * - Rate limiting (sliding window algorithm)
 * - Generic Redis operations
 *
 * Thread Safety: All implementations must be thread-safe
 */
public interface RedisService {

    // ==================== RSS Source Caching ====================

    /**
     * Cache RSS source metadata
     *
     * @param sourceId   RSS source ID
     * @param sourceName RSS source name
     * @param ttl        Time-to-live for cache
     * @throws IllegalArgumentException if sourceId is null or sourceName is blank
     */
    void cacheRssSource(Long sourceId, String sourceName, Duration ttl);

    /**
     * Get cached RSS source name
     *
     * @param sourceId RSS source ID
     * @return Cached source name, or null if not found
     */
    String getCachedRssSource(Long sourceId);

    /**
     * Evict RSS source from cache
     *
     * @param sourceId RSS source ID
     */
    void evictRssSource(Long sourceId);

    // ==================== Content Processing Status ====================

    /**
     * Check if content has been processed
     *
     * @param contentHash Content hash (e.g., SHA-256)
     * @return true if content is marked as processed
     * @throws IllegalArgumentException if contentHash is blank
     */
    boolean isContentProcessed(String contentHash);

    /**
     * Mark content as processed
     *
     * @param contentHash Content hash
     * @param ttl         Time-to-live for processed marker
     * @throws IllegalArgumentException if contentHash is blank
     */
    void markContentProcessed(String contentHash, Duration ttl);

    // ==================== Distributed Locking ====================

    /**
     * Acquire a distributed lock
     *
     * @param lockKey   Lock key (will be prefixed with "lock:")
     * @param lockValue Unique lock value (e.g., UUID)
     * @param ttl       Lock time-to-live (auto-release)
     * @return true if lock acquired, false otherwise
     * @throws IllegalArgumentException if lockKey, lockValue, or ttl is null
     */
    boolean acquireLock(String lockKey, String lockValue, Duration ttl);

    /**
     * Release a distributed lock
     *
     * @param lockKey   Lock key (will be prefixed with "lock:")
     * @param lockValue Unique lock value (must match acquire call)
     * @return true if lock was released, false otherwise
     * @throws IllegalArgumentException if lockKey or lockValue is blank
     */
    boolean releaseLock(String lockKey, String lockValue);

    /**
     * Extend lock TTL
     *
     * @param lockKey       Lock key (will be prefixed with "lock:")
     * @param lockValue     Unique lock value (must match acquire call)
     * @param additionalTtl Additional time to extend
     * @return true if lock was extended, false otherwise
     * @throws IllegalArgumentException if any parameter is null/blank
     */
    boolean extendLock(String lockKey, String lockValue, Duration additionalTtl);

    // ==================== Rate Limiting ====================

    /**
     * Check rate limit using sliding window algorithm
     *
     * @param key    Rate limit key (e.g., "api:endpoint:/api/news")
     * @param limit  Maximum requests allowed
     * @param window Time window for rate limiting
     * @return true if request is allowed, false if limit exceeded
     * @throws IllegalArgumentException if key is blank, limit <= 0, or window is null
     */
    boolean checkRateLimit(String key, int limit, Duration window);

    /**
     * Get current request count within rate limit window
     *
     * @param key Rate limit key
     * @return Current request count
     */
    long getRateLimitCount(String key);

    /**
     * Reset rate limit for a key
     *
     * @param key Rate limit key
     */
    void resetRateLimit(String key);

    // ==================== Generic Operations ====================

    /**
     * Set value with TTL
     *
     * @param key   Cache key
     * @param value Cache value
     * @param ttl   Time-to-live
     * @throws IllegalArgumentException if key is null
     */
    void set(String key, Object value, Duration ttl);

    /**
     * Get value by key
     *
     * @param key Cache key
     * @return Cached value, or null if not found
     */
    Object get(String key);

    /**
     * Delete key
     *
     * @param key Cache key
     */
    void delete(String key);

    /**
     * Check if key exists
     *
     * @param key Cache key
     * @return true if key exists
     */
    boolean exists(String key);

    /**
     * Get remaining TTL in seconds
     *
     * @param key Cache key
     * @return TTL in seconds, -1 if key exists but has no expiry, -2 if key does not exist
     */
    long getTtl(String key);
}
