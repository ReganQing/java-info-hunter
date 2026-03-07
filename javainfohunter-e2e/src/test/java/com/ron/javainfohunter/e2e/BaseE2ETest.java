package com.ron.javainfohunter.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.opentest4j.TestAbortedException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Base class for E2E tests using Testcontainers.
 *
 * <p>Tests will skip if Docker is not available.
 * To force Testcontainers usage, set system property: -Dtestcontainers.enable=true
 * To use external services instead, set: -Dtestcontainers.enable=false
 */
@Testcontainers
@SpringBootTest(classes = TestApplication.class)
@EnabledIfSystemProperty(named = "testcontainers.enable", matches = "true", disabledReason =
    "Testcontainers disabled. Set -Dtestcontainers.enable=true to use Testcontainers. " +
    "Without this flag, tests will attempt to use external services.")
public abstract class BaseE2ETest {

    // PostgreSQL Container
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("javainfohunter_test")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(Duration.ofSeconds(60));

    // RabbitMQ Container
    @Container
    static RabbitMQContainer rabbitmqContainer = new RabbitMQContainer("rabbitmq:3.12-alpine")
            .withStartupTimeout(Duration.ofSeconds(60));

    // Redis Container
    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(30));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL Configuration
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA Configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");

        // RabbitMQ Configuration
        registry.add("spring.rabbitmq.host", rabbitmqContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitmqContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmqContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmqContainer::getAdminPassword);

        // Redis Configuration
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
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
    static void verifyDockerAvailable() {
        try {
            org.testcontainers.DockerClientFactory.instance();
        } catch (Exception e) {
            throw new TestAbortedException(
                "Docker not available. To run tests with external services, " +
                "set -Dtestcontainers.enable=false and ensure PostgreSQL, RabbitMQ, and Redis are running.", e);
        }
    }

    protected String getPostgresJdbcUrl() {
        return postgresContainer.getJdbcUrl();
    }

    protected Integer getRabbitmqPort() {
        return rabbitmqContainer.getAmqpPort();
    }

    protected Integer getRedisPort() {
        return redisContainer.getMappedPort(6379);
    }
}
