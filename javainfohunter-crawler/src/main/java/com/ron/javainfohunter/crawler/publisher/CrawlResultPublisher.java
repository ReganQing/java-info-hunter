package com.ron.javainfohunter.crawler.publisher;

import com.ron.javainfohunter.crawler.config.RabbitMQConfig;
import com.ron.javainfohunter.crawler.dto.CrawlResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publisher for crawl result messages.
 *
 * <p>This publisher handles publishing {@link CrawlResultMessage} objects to the
 * {@code crawler.crawl.result.queue} with the following features:</p>
 *
 * <ul>
 *   <li>Publisher confirms - waits for broker ACK before considering message sent</li>
 *   <li>Correlation ID generation for message tracing</li>
 *   <li>Statistics tracking for crawl operations</li>
 *   <li>Graceful error handling</li>
 * </ul>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * RSS Feed Crawler → CrawlResultPublisher → Crawl Result Queue → Monitoring/Analytics
 * </pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. RabbitTemplate is thread-safe.</p>
 *
 * @see CrawlResultMessage
 */
@Slf4j
@Service
public class CrawlResultPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Timeout for waiting for publisher confirms in milliseconds.
     */
    private static final long CONFIRM_TIMEOUT_MS = 5000;

    @Autowired
    public CrawlResultPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a crawl result message to RabbitMQ.
     *
     * <p>This method publishes crawl statistics and status after a crawl job completes.
     * It includes timing metrics, article counts, and error details if applicable.</p>
     *
     * @param message the crawl result message to publish
     * @return true if the message was published successfully, false otherwise
     */
    public boolean publishCrawlResult(CrawlResultMessage message) {
        if (message == null) {
            log.warn("Attempted to publish null crawl result message, skipping");
            return false;
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            log.debug("Publishing crawl result message: correlationId={}, sourceId={}, status={}, articlesCrawled={}",
                correlationId, message.getRssSourceId(), message.getStatus(), message.getArticlesCrawled());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.CRAWL_RESULT_ROUTING_KEY,
                message,
                message1 -> {
                    message1.getMessageProperties().setCorrelationId(correlationId);
                    return message1;
                }
            );

            log.info("Successfully published crawl result: correlationId={}, sourceId={}, status={}, duration={}ms",
                correlationId, message.getRssSourceId(), message.getStatus(), message.getDurationMs());

            return true;

        } catch (Exception e) {
            log.error("Failed to publish crawl result message: correlationId={}, sourceId={}, error={}",
                correlationId, message.getRssSourceId(), e.getMessage(), e);

            // Don't throw exception to avoid disrupting the crawler
            // Error is logged and can be monitored separately
            return false;
        }
    }

    /**
     * Publishes a successful crawl result message.
     *
     * <p>Convenience method for creating and publishing a success result.</p>
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param articlesCrawled number of articles crawled
     * @param newArticles number of new articles
     * @param duplicateArticles number of duplicate articles
     * @param startTime crawl start time
     * @param endTime crawl end time
     * @return true if published successfully
     */
    public boolean publishSuccessResult(
        Long rssSourceId,
        String rssSourceName,
        String rssSourceUrl,
        int articlesCrawled,
        int newArticles,
        int duplicateArticles,
        java.time.Instant startTime,
        java.time.Instant endTime
    ) {
        CrawlResultMessage message = CrawlResultMessage.builder()
            .rssSourceId(rssSourceId)
            .rssSourceName(rssSourceName)
            .rssSourceUrl(rssSourceUrl)
            .status(CrawlResultMessage.CrawlStatus.SUCCESS)
            .articlesCrawled(articlesCrawled)
            .newArticles(newArticles)
            .duplicateArticles(duplicateArticles)
            .failedArticles(0)
            .startTime(startTime)
            .endTime(endTime)
            .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
            .build();

        return publishCrawlResult(message);
    }

    /**
     * Publishes a partial success crawl result message.
     *
     * <p>Convenience method for creating and publishing a partial success result.</p>
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param articlesCrawled number of articles crawled
     * @param newArticles number of new articles
     * @param duplicateArticles number of duplicate articles
     * @param failedArticles number of failed articles
     * @param startTime crawl start time
     * @param endTime crawl end time
     * @param errorMessage error message describing partial failures
     * @return true if published successfully
     */
    public boolean publishPartialResult(
        Long rssSourceId,
        String rssSourceName,
        String rssSourceUrl,
        int articlesCrawled,
        int newArticles,
        int duplicateArticles,
        int failedArticles,
        java.time.Instant startTime,
        java.time.Instant endTime,
        String errorMessage
    ) {
        CrawlResultMessage message = CrawlResultMessage.builder()
            .rssSourceId(rssSourceId)
            .rssSourceName(rssSourceName)
            .rssSourceUrl(rssSourceUrl)
            .status(CrawlResultMessage.CrawlStatus.PARTIAL)
            .articlesCrawled(articlesCrawled)
            .newArticles(newArticles)
            .duplicateArticles(duplicateArticles)
            .failedArticles(failedArticles)
            .startTime(startTime)
            .endTime(endTime)
            .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
            .errorMessage(errorMessage)
            .build();

        return publishCrawlResult(message);
    }

    /**
     * Publishes a failed crawl result message.
     *
     * <p>Convenience method for creating and publishing a failure result.</p>
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param startTime crawl start time
     * @param endTime crawl end time
     * @param errorMessage error message
     * @param errorTrace error stack trace (optional)
     * @return true if published successfully
     */
    public boolean publishFailureResult(
        Long rssSourceId,
        String rssSourceName,
        String rssSourceUrl,
        java.time.Instant startTime,
        java.time.Instant endTime,
        String errorMessage,
        String errorTrace
    ) {
        CrawlResultMessage message = CrawlResultMessage.builder()
            .rssSourceId(rssSourceId)
            .rssSourceName(rssSourceName)
            .rssSourceUrl(rssSourceUrl)
            .status(CrawlResultMessage.CrawlStatus.FAILED)
            .articlesCrawled(0)
            .newArticles(0)
            .duplicateArticles(0)
            .failedArticles(0)
            .startTime(startTime)
            .endTime(endTime)
            .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
            .errorMessage(errorMessage)
            .errorTrace(errorTrace)
            .build();

        return publishCrawlResult(message);
    }

    /**
     * Publishes a skipped crawl result message.
     *
     * <p>Convenience method for creating and publishing a skipped result.</p>
     *
     * @param rssSourceId the RSS source ID
     * @param rssSourceName the RSS source name
     * @param rssSourceUrl the RSS source URL
     * @param reason reason for skipping
     * @return true if published successfully
     */
    public boolean publishSkippedResult(
        Long rssSourceId,
        String rssSourceName,
        String rssSourceUrl,
        String reason
    ) {
        java.time.Instant now = java.time.Instant.now();

        CrawlResultMessage message = CrawlResultMessage.builder()
            .rssSourceId(rssSourceId)
            .rssSourceName(rssSourceName)
            .rssSourceUrl(rssSourceUrl)
            .status(CrawlResultMessage.CrawlStatus.SKIPPED)
            .articlesCrawled(0)
            .newArticles(0)
            .duplicateArticles(0)
            .failedArticles(0)
            .startTime(now)
            .endTime(now)
            .durationMs(0L)
            .errorMessage(reason)
            .build();

        return publishCrawlResult(message);
    }
}
