package com.ron.javainfohunter.crawler.health;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.metrics.CrawlMetricsCollector;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrawlerHealthIndicatorTest {

    @Test
    void healthShouldBeUpWhenDatabaseAndRabbitChecksReturnUpWithDetails() throws Exception {
        CrawlerProperties properties = new CrawlerProperties();
        CrawlMetricsCollector metricsCollector = new CrawlMetricsCollector();

        DataSource dataSource = mock(DataSource.class);
        Connection sqlConnection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("18.3");
        when(sqlConnection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        ConnectionFactory rabbitConnectionFactory = mock(ConnectionFactory.class);
        org.springframework.amqp.rabbit.connection.Connection rabbitConnection =
            mock(org.springframework.amqp.rabbit.connection.Connection.class);
        when(rabbitConnectionFactory.createConnection()).thenReturn(rabbitConnection);
        when(rabbitConnection.isOpen()).thenReturn(true);

        CrawlerHealthIndicator indicator = new CrawlerHealthIndicator(
            properties,
            metricsCollector,
            dataSource,
            rabbitConnectionFactory
        );

        assertEquals(Status.UP, indicator.health().getStatus());
    }
}
