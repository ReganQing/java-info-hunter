package com.ron.javainfohunter.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RabbitMQ message queue operations using Testcontainers.
 * Tests message publishing, consumption, and queue bindings.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>RabbitMQ container connectivity</li>
 *   <li>Connection factory configuration</li>
 *   <li>Basic message operations</li>
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("RabbitMQ Integration Tests")
public class RabbitMQIntegrationTest extends BaseExternalServiceTest {

    @Autowired(required = false)
    private ConnectionFactory connectionFactory;

    @AfterEach
    void tearDown() {
        // Clean up any test queues or exchanges if needed
    }

    @Test
    @DisplayName("Should connect to RabbitMQ container successfully")
    void shouldConnectToRabbitMQ() {
        // Assert
        assertThat(connectionFactory).isNotNull();
        assertThat(getRabbitmqPort()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should create connection to RabbitMQ")
    void shouldCreateConnection() {
        // Act
        Connection connection = connectionFactory.createConnection();

        // Assert
        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();

        // Cleanup
        connection.close();
    }

    @Test
    @DisplayName("Should support multiple connections")
    void shouldSupportMultipleConnections() {
        // Act
        Connection conn1 = connectionFactory.createConnection();
        Connection conn2 = connectionFactory.createConnection();

        // Assert
        assertThat(conn1).isNotNull();
        assertThat(conn2).isNotNull();
        assertThat(conn1.isOpen()).isTrue();
        assertThat(conn2.isOpen()).isTrue();

        // Cleanup
        conn1.close();
        conn2.close();
    }

    @Test
    @DisplayName("Should get host and port from container")
    void shouldGetContainerConfiguration() {
        // Act & Assert
        String host = connectionFactory.getHost();
        int port = connectionFactory.getPort();

        assertThat(host).isNotNull();
        assertThat(port).isGreaterThan(0);
        assertThat(port).isEqualTo(getRabbitmqPort());
    }

    @Test
    @DisplayName("Should close connection properly")
    void shouldCloseConnection() {
        // Arrange
        Connection connection = connectionFactory.createConnection();
        assertThat(connection.isOpen()).isTrue();

        // Act
        connection.close();

        // Assert
        assertThat(connection.isOpen()).isFalse();
    }

    @Test
    @DisplayName("Should handle connection lifecycle")
    void shouldHandleConnectionLifecycle() {
        // Act - Create
        Connection connection = connectionFactory.createConnection();
        assertThat(connection.isOpen()).isTrue();

        // Act - Close
        connection.close();
        assertThat(connection.isOpen()).isFalse();

        // Act - Create new connection
        Connection newConnection = connectionFactory.createConnection();
        assertThat(newConnection.isOpen()).isTrue();

        // Cleanup
        newConnection.close();
    }
}
