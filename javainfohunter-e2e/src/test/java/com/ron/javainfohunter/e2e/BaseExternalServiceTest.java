package com.ron.javainfohunter.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for E2E tests using external services (PostgreSQL, RabbitMQ, Redis).
 *
 * <p>This configuration is used when Testcontainers is disabled.
 * Tests will use external services running on localhost or configured via environment variables.
 *
 * <p>To use this configuration, run tests with: -Dtestcontainers.enable=false
 */
@SpringBootTest(classes = TestApplication.class)
@DisabledIfSystemProperty(named = "testcontainers.enable", matches = "true",
    disabledReason = "Testcontainers is enabled. Use BaseE2ETest instead.")
public abstract class BaseExternalServiceTest {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL Configuration - use environment variables or defaults
        registry.add("spring.datasource.url",
            () -> System.getProperty("db.url", "jdbc:postgresql://localhost:5432/javainfohunter"));
        registry.add("spring.datasource.username",
            () -> System.getProperty("db.user", "postgres"));
        registry.add("spring.datasource.password",
            () -> System.getProperty("db.password", "postgres"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA Configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");

        // RabbitMQ Configuration
        registry.add("spring.rabbitmq.host",
            () -> System.getProperty("rabbitmq.host", "localhost"));
        registry.add("spring.rabbitmq.port",
            () -> Integer.parseInt(System.getProperty("rabbitmq.port", "5672")));
        registry.add("spring.rabbitmq.username",
            () -> System.getProperty("rabbitmq.user", "admin"));
        registry.add("spring.rabbitmq.password",
            () -> System.getProperty("rabbitmq.password", "admin"));

        // Redis Configuration
        registry.add("spring.data.redis.host",
            () -> System.getProperty("redis.host", "localhost"));
        registry.add("spring.data.redis.port",
            () -> Integer.parseInt(System.getProperty("redis.port", "6379")));
        registry.add("spring.data.redis.timeout", () -> "3000ms");

        // AI Service Configuration
        registry.add("spring.ai.dashscope.api-key",
            () -> System.getProperty("test.dashscope.api.key", "sk-test-key-for-testing-only"));
        registry.add("spring.ai.dashscope.chat.enabled", () -> "false");

        // Actuator Configuration
        registry.add("management.endpoints.web.exposure.include", () -> "health,info,metrics");
        registry.add("management.endpoint.health.show-details", () -> "always");
    }

    @BeforeAll
    static void verifyExternalServicesAvailable() {
        // External services should be available
        // Tests will fail if services are not running
    }

    protected String getPostgresJdbcUrl() {
        return System.getProperty("db.url", "jdbc:postgresql://localhost:5432/javainfohunter");
    }

    protected Integer getRabbitmqPort() {
        return Integer.parseInt(System.getProperty("rabbitmq.port", "5672"));
    }

    protected Integer getRedisPort() {
        return Integer.parseInt(System.getProperty("redis.port", "6379"));
    }
}
