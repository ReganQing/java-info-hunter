package com.ron.javainfohunter.crawler.handler;

import com.ron.javainfohunter.crawler.config.CrawlerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retry handler with exponential backoff for crawler operations.
 *
 * <p>This component provides retry logic with the following features:</p>
 * <ul>
 *   <li>Exponential backoff (1s, 2s, 4s, ...)</li>
 *   <li>Configurable max retry attempts</li>
 *   <li>Conditional retry based on exception type</li>
 *   <li>Retry attempt logging</li>
 *   <li>Support for different retry strategies per error type</li>
 * </ul>
 *
 * <p><b>Retry Configuration:</b></p>
 * <pre>
 * javainfohunter:
 *   crawler:
 *     retry:
 *       max-attempts: 3
 *       initial-delay: 1000  # 1 second
 *       backoff-multiplier: 2.0  # Exponential
 *       max-delay: 60000  # 1 minute
 * </pre>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Simple retry with default settings
 * String result = retryHandler.executeWithRetry(
 *     "Fetch RSS feed",
 *     () -> rssFetcher.fetch(url),
 *     e -> e instanceof IOException
 * );
 *
 * // Custom retry condition
 * Data data = retryHandler.executeWithRetry(
 *     "Process article",
 *     () -> processor.process(article),
 *     e -> e.isRetryable() && attempt &lt; maxRetries
 * );
 * </pre>
 *
 * @see CrawlerProperties.Retry
 * @see CrawlErrorHandler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryHandler {

    private final CrawlerProperties crawlerProperties;

    /**
     * Execute a task with retry logic using exponential backoff.
     *
     * <p>This method will:</p>
     * <ol>
     *   <li>Execute the task</li>
     *   <li>If task fails and retry condition is met, wait and retry</li>
     *   <li>Continue until success or max attempts reached</li>
     *   <li>Log each retry attempt</li>
     * </ol>
     *
     * <p><b>Backoff Strategy:</b></p>
     * <pre>
     * Attempt 0: Execute immediately
     * Attempt 1: Wait 1s (initialDelay)
     * Attempt 2: Wait 2s (initialDelay * multiplier^1)
     * Attempt 3: Wait 4s (initialDelay * multiplier^2)
     * ...
     * Max wait: maxDelay (capped)
     * </pre>
     *
     * @param taskName descriptive name for the task (for logging)
     * @param task the task to execute
     * @param retryCondition predicate to determine if exception should trigger retry
     * @param <T> return type of the task
     * @return task result
     * @throws RuntimeException if all retry attempts fail
     */
    public <T> T executeWithRetry(String taskName, Supplier<T> task, Predicate<Exception> retryCondition) {
        CrawlerProperties.Retry retryConfig = crawlerProperties.getRetry();
        int maxAttempts = retryConfig.getMaxAttempts();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 0) {
                    log.debug("Retry attempt {}/{} for task: {}", attempt, maxAttempts, taskName);
                }

                T result = task.get();

                if (attempt > 0) {
                    log.info("Task succeeded on retry attempt {}/{}: {}", attempt, maxAttempts, taskName);
                }

                return result;

            } catch (Exception e) {
                lastException = e;

                // Check if we should retry
                if (attempt >= maxAttempts || !retryCondition.test(e)) {
                    log.error("Task failed after {} attempts: {}", attempt + 1, taskName);
                    break;
                }

                // Calculate backoff delay
                long delay = calculateBackoff(attempt);

                log.warn("Task failed (attempt {}/{}): {}. Retrying in {}ms...",
                    attempt + 1, maxAttempts + 1, taskName, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        // All attempts failed
        String errorMsg = String.format("Task '%s' failed after %d attempts",
            taskName, maxAttempts + 1);
        log.error("{}: {}", errorMsg, lastException.getMessage());
        throw new RuntimeException(errorMsg, lastException);
    }

    /**
     * Execute a task with retry logic using default retry condition.
     *
     * <p>Default retry condition: retry all exceptions except:
     * <ul>
     *   <li>IllegalArgumentException</li>
     *   <li>IllegalStateException</li>
     *   <li>InterruptedException</li>
     * </ul>
     *
     * @param taskName descriptive name for the task
     * @param task the task to execute
     * @param <T> return type of the task
     * @return task result
     */
    public <T> T executeWithRetry(String taskName, Supplier<T> task) {
        return executeWithRetry(taskName, task, this::shouldRetry);
    }

    /**
     * Execute a task with retry logic, using ErrorType for retry decision.
     *
     * @param taskName descriptive name for the task
     * @param task the task to execute
     * @param errorHandler the error handler to classify exceptions
     * @param <T> return type of the task
     * @return task result
     */
    public <T> T executeWithRetry(
        String taskName,
        Supplier<T> task,
        CrawlErrorHandler errorHandler
    ) {
        return executeWithRetry(taskName, task, e -> {
            ErrorType errorType = errorHandler.classifyError(e);
            return errorHandler.isRetryable(errorType);
        });
    }

    /**
     * Calculate exponential backoff delay for a given retry attempt.
     *
     * <p>Formula: delay = initialDelay * (backoffMultiplier ^ attempt)</p>
     * <p>Capped at maxDelay</p>
     *
     * @param attempt the retry attempt number (0-based)
     * @return delay in milliseconds
     */
    protected long calculateBackoff(int attempt) {
        CrawlerProperties.Retry retryConfig = crawlerProperties.getRetry();

        long baseDelay = retryConfig.getInitialDelay();
        double multiplier = retryConfig.getBackoffMultiplier();

        // Calculate exponential backoff
        long delay = (long) (baseDelay * Math.pow(multiplier, attempt));

        // Cap at max delay
        return Math.min(delay, retryConfig.getMaxDelay());
    }

    /**
     * Determine if an exception should trigger a retry.
     *
     * <p>Default rules:</p>
     * <ul>
     *   <li><b>Retry:</b> IOException, SQLException, AMQP exceptions</li>
     *   <li><b>No Retry:</b> IllegalArgumentException, IllegalStateException, InterruptedException</li>
     *   <li><b>Retry:</b> Other RuntimeExceptions (conservative)</li>
     * </ul>
     *
     * @param e the exception to check
     * @return true if retry should be attempted
     */
    protected boolean shouldRetry(Exception e) {
        if (e == null) {
            return false;
        }

        // Don't retry for programming errors
        if (e instanceof IllegalArgumentException ||
            e instanceof IllegalStateException ||
            e instanceof InterruptedException) {
            return false;
        }

        // Retry for I/O and transient errors
        if (e instanceof java.io.IOException) {
            return true;
        }

        if (e instanceof SQLException) {
            return true;
        }

        // Retry for AMQP errors
        if (e instanceof org.springframework.amqp.AmqpException) {
            return true;
        }

        // Default to retry for other exceptions
        return true;
    }

    /**
     * Execute a void task with retry logic.
     *
     * @param taskName descriptive name for the task
     * @param task the runnable task to execute
     * @param retryCondition predicate to determine if exception should trigger retry
     */
    public void executeWithRetry(
        String taskName,
        Runnable task,
        Predicate<Exception> retryCondition
    ) {
        executeWithRetry(taskName, () -> {
            task.run();
            return null;
        }, retryCondition);
    }

    /**
     * Execute a void task with retry logic using default condition.
     *
     * @param taskName descriptive name for the task
     * @param task the runnable task to execute
     */
    public void executeWithRetry(String taskName, Runnable task) {
        executeWithRetry(taskName, task, this::shouldRetry);
    }

    /**
     * Get the configured maximum retry attempts.
     *
     * @return max retry attempts
     */
    public int getMaxRetryAttempts() {
        return crawlerProperties.getRetry().getMaxAttempts();
    }

    /**
     * Get the configured initial retry delay.
     *
     * @return initial delay in milliseconds
     */
    public long getInitialDelay() {
        return crawlerProperties.getRetry().getInitialDelay();
    }

    /**
     * Get the configured backoff multiplier.
     *
     * @return backoff multiplier
     */
    public double getBackoffMultiplier() {
        return crawlerProperties.getRetry().getBackoffMultiplier();
    }

    /**
     * Get the configured maximum retry delay.
     *
     * @return maximum delay in milliseconds
     */
    public long getMaxDelay() {
        return crawlerProperties.getRetry().getMaxDelay();
    }
}
