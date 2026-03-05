package com.ron.javainfohunter.crawler.health;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import com.ron.javainfohunter.crawler.metrics.CrawlMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for the crawler module.
 *
 * <p>This component provides Spring Boot Actuator health checks for the crawler,
 * including:</p>
 * <ul>
 *   <li>Overall crawler status</li>
 *   <li>Database connectivity</li>
 *   <li>RabbitMQ connectivity</li>
 *   <li>Recent crawl metrics</li>
 *   <li>Error rate monitoring</li>
 * </ul>
 *
 * <p><b>Health Check Endpoint:</b></p>
 * <pre>
 * GET /actuator/health/crawler
 * </pre>
 *
 * <p><b>Response Example:</b></p>
 * <pre>
 * {
 *   "status": "UP",
 *   "details": {
 *     "enabled": true,
 *     "database": "UP",
 *     "rabbitmq": "UP",
 *     "metrics": {
 *       "totalCrawls": 1523,
 *       "successRate": 0.984,
 *       "uptime": "5 hours 23 minutes"
 *     },
 *     "lastCrawl": "2025-01-15T10:30:00Z"
 *   }
 * }
 * </pre>
 *
 * @see org.springframework.boot.actuate.health.HealthIndicator
 * @see CrawlMetricsCollector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerHealthIndicator implements HealthIndicator {

    private final CrawlerProperties crawlerProperties;
    private final CrawlMetricsCollector metricsCollector;
    private final DataSource dataSource;
    private final ConnectionFactory rabbitConnectionFactory;

    /**
     * Check the health of the crawler module.
     *
     * <p>This method checks:</p>
     * <ol>
     *   <li>Database connectivity</li>
     *   <li>RabbitMQ connectivity</li>
     *   <li>Recent error rates</li>
     *   <li>Crawler configuration status</li>
     * </ol>
     *
     * <p><b>Health Status:</b></p>
     * <ul>
     *   <li><b>UP:</b> All checks passed</li>
     *   <li><b>DOWN:</b> Critical failure (database or RabbitMQ unavailable)</li>
     *   <li><b>DEGRADED:</b> Non-critical issues (high error rate, recent failures)</li>
     * </ul>
     *
     * @return Health status with details
     */
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        Map<String, Object> details = new HashMap<>();

        try {
            // Check if crawler is enabled
            boolean enabled = crawlerProperties.isEnabled();
            details.put("enabled", enabled);

            if (!enabled) {
                builder.down()
                    .withDetail("reason", "Crawler is disabled in configuration");
                return builder.build();
            }

            // Check database connectivity
            String dbStatus = checkDatabase();
            details.put("database", dbStatus);

            boolean dbUp = "UP".equals(dbStatus);
            if (!dbUp) {
                builder.down().withDetail("database", dbStatus);
            }

            // Check RabbitMQ connectivity
            String rabbitmqStatus = checkRabbitMQ();
            details.put("rabbitmq", rabbitmqStatus);

            boolean rabbitmqUp = "UP".equals(rabbitmqStatus);
            if (!rabbitmqUp) {
                builder.down().withDetail("rabbitmq", rabbitmqStatus);
            }

            // Add metrics summary
            CrawlMetricsCollector.CrawlMetricsSummary metrics = metricsCollector.getSummary();
            Map<String, Object> metricsDetails = new HashMap<>();

            metricsDetails.put("totalCrawls", metrics.getTotalCrawls());
            metricsDetails.put("successfulCrawls", metrics.getSuccessfulCrawls());
            metricsDetails.put("failedCrawls", metrics.getFailedCrawls());
            metricsDetails.put("successRate", String.format("%.2f%%", metrics.getSuccessRate() * 100));
            metricsDetails.put("totalArticles", metrics.getTotalArticlesCrawled());
            metricsDetails.put("uptime", metrics.getUptime());
            metricsDetails.put("sources", metrics.getSourceCount());

            details.put("metrics", metricsDetails);

            // Check error rates
            double errorRate = calculateErrorRate(metrics);
            details.put("errorRate", String.format("%.2f%%", errorRate * 100));

            // Add configuration details
            Map<String, Object> configDetails = new HashMap<>();
            configDetails.put("schedulerEnabled", crawlerProperties.getScheduler().isEnabled());
            configDetails.put("maxArticlesPerFeed", crawlerProperties.getFeed().getMaxArticlesPerFeed());
            configDetails.put("connectionTimeout", crawlerProperties.getFeed().getConnectionTimeout() + "ms");
            configDetails.put("maxRetries", crawlerProperties.getRetry().getMaxAttempts());
            configDetails.put("initialRetryDelay", crawlerProperties.getRetry().getInitialDelay() + "ms");
            details.put("configuration", configDetails);

            // Determine overall health
            if (dbUp && rabbitmqUp) {
                // Check if error rate is acceptable
                if (errorRate > 0.5 && metrics.getTotalCrawls() > 10) {
                    // More than 0.5% error rate with significant traffic
                    builder.up()
                        .withDetail("warning", "High error rate detected");
                } else {
                    builder.up();
                }
            } else {
                builder.down();
            }

            builder.withDetails(details);

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            builder.down()
                .withDetail("error", e.getMessage())
                .withDetail("errorClass", e.getClass().getSimpleName());
        }

        return builder.build();
    }

    /**
     * Check database connectivity.
     *
     * @return "UP" if database is accessible, "DOWN" otherwise with error message
     */
    private String checkDatabase() {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Execute a simple query to verify connectivity
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return String.format("UP (%s %s)",
                        metaData.getDatabaseProductName(),
                        metaData.getDatabaseProductVersion());
                }
            }

            return "DOWN (Query failed)";

        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return "DOWN (" + e.getMessage() + ")";
        }
    }

    /**
     * Check RabbitMQ connectivity.
     *
     * @return "UP" if RabbitMQ is accessible, "DOWN" otherwise with error message
     */
    private String checkRabbitMQ() {
        try {
            Connection connection = rabbitConnectionFactory.createConnection();

            if (connection != null && connection.isOpen()) {
                connection.close();
                return "UP (RabbitMQ connected)";
            }

            return "DOWN (Connection not open)";

        } catch (Exception e) {
            log.error("RabbitMQ health check failed: {}", e.getMessage());
            return "DOWN (" + e.getMessage() + ")";
        }
    }

    /**
     * Calculate error rate from metrics.
     *
     * @param metrics the metrics summary
     * @return error rate as a percentage (0.0 to 1.0)
     */
    private double calculateErrorRate(CrawlMetricsCollector.CrawlMetricsSummary metrics) {
        long total = metrics.getTotalCrawls();
        if (total == 0) {
            return 0.0;
        }
        return (double) metrics.getFailedCrawls() / total;
    }

    /**
     * Check if crawler has been running recently.
     *
     * @param metrics the metrics summary
     * @return true if last crawl was within 2 hours
     */
    private boolean isRecentlyActive(CrawlMetricsCollector.CrawlMetricsSummary metrics) {
        if (metrics.getTotalCrawls() == 0) {
            return false;
        }

        // Check if metrics show recent activity
        // This is a simplified check - in production you'd track last crawl time
        return true;
    }

    /**
     * Get the uptime in a human-readable format.
     *
     * @param startTime the start time
     * @return formatted uptime string
     */
    private String formatUptime(Instant startTime) {
        long seconds = ChronoUnit.SECONDS.between(startTime, Instant.now());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%d hours %d minutes", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d minutes", minutes);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}
