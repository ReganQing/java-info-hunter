package com.ron.javainfohunter.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis cache operations using Testcontainers.
 * Tests caching, TTL, and data persistence.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Redis container connectivity</li>
 *   <li>Basic Redis operations (set, get, delete)</li>
 *   <li>TTL (Time To Live) functionality</li>
 *   <li>Connection lifecycle</li>
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Redis Integration Tests")
public class RedisIntegrationTest extends BaseExternalServiceTest {

    @Autowired(required = false)
    private RedisConnectionFactory connectionFactory;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String TEST_KEY_PREFIX = "test:e2e:";

    @AfterEach
    void tearDown() {
        // Clean up test keys
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory().getConnection().keyCommands()
                .del((TEST_KEY_PREFIX + "*").getBytes());
        }
    }

    @Test
    @DisplayName("Should connect to Redis container successfully")
    void shouldConnectToRedis() {
        // Assert
        assertThat(connectionFactory).isNotNull();
        assertThat(getRedisPort()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should perform basic set and get operations")
    void shouldPerformSetAndGet() {
        // Arrange
        String key = TEST_KEY_PREFIX + "string:key";
        String value = "test-value";

        // Act
        redisTemplate.opsForValue().set(key, value);
        String retrieved = redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).isEqualTo(value);

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    @DisplayName("Should handle TTL (Time To Live) correctly")
    void shouldHandleTTL() {
        // Arrange
        String key = TEST_KEY_PREFIX + "ttl:key";
        String value = "expiring-value";

        // Act - Set with 2 second TTL
        redisTemplate.opsForValue().set(key, value, 2, TimeUnit.SECONDS);

        // Assert - Value exists immediately
        assertThat(redisTemplate.hasKey(key)).isTrue();
        assertThat(redisTemplate.getExpire(key, TimeUnit.SECONDS)).isGreaterThan(0);

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    @DisplayName("Should handle delete operations")
    void shouldHandleDelete() {
        // Arrange
        String key = TEST_KEY_PREFIX + "delete:key";
        redisTemplate.opsForValue().set(key, "value");

        // Act
        Boolean deleted = redisTemplate.delete(key);

        // Assert
        assertThat(deleted).isTrue();
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("Should handle special characters and Unicode")
    void shouldHandleSpecialCharacters() {
        // Arrange
        String key = TEST_KEY_PREFIX + "unicode:key";
        String value = "Test with emoji 🚀 and 中文 and special chars \n\t\"'";

        // Act
        redisTemplate.opsForValue().set(key, value);
        String retrieved = redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).isEqualTo(value);

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    @DisplayName("Should handle increment operations")
    void shouldHandleIncrement() {
        // Arrange
        String key = TEST_KEY_PREFIX + "counter:key";

        // Act & Assert
        Long value1 = redisTemplate.opsForValue().increment(key);
        assertThat(value1).isEqualTo(1);

        Long value2 = redisTemplate.opsForValue().increment(key);
        assertThat(value2).isEqualTo(2);

        Long value5 = redisTemplate.opsForValue().increment(key, 5);
        assertThat(value5).isEqualTo(7);

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    @DisplayName("Should handle large values")
    void shouldHandleLargeValues() {
        // Arrange
        String key = TEST_KEY_PREFIX + "large:key";
        String largeValue = "A".repeat(100_000); // 100KB of data

        // Act
        redisTemplate.opsForValue().set(key, largeValue);
        String retrieved = redisTemplate.opsForValue().get(key);

        // Assert
        assertThat(retrieved).hasSize(100_000);
        assertThat(retrieved).startsWith("AAA...");

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    @DisplayName("Should check if key exists")
    void shouldCheckKeyExists() {
        // Arrange
        String existingKey = TEST_KEY_PREFIX + "existing:key";
        String nonExistingKey = TEST_KEY_PREFIX + "nonexisting:key";
        redisTemplate.opsForValue().set(existingKey, "value");

        // Act & Assert
        assertThat(redisTemplate.hasKey(existingKey)).isTrue();
        assertThat(redisTemplate.hasKey(nonExistingKey)).isFalse();

        // Cleanup
        redisTemplate.delete(existingKey);
    }

    @Test
    @DisplayName("Should get Redis port from container")
    void shouldGetRedisPort() {
        // Act & Assert
        Integer port = getRedisPort();
        assertThat(port).isGreaterThan(0);
        assertThat(port).isLessThan(65536);
    }
}
