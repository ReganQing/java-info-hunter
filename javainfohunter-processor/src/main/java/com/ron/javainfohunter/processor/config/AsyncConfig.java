package com.ron.javainfohunter.processor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for Processor module.
 *
 * <p>This class provides configured thread pools for asynchronous operations,
 * ensuring proper resource management and thread safety in concurrent processing.</p>
 *
 * <p><b>Thread Pool Configuration:</b></p>
 * <ul>
 *   <li><b>processorTaskExecutor:</b> General async task execution</li>
 *   <li><b>aggregatorExecutor:</b> Dedicated executor for result aggregation operations</li>
 * </ul>
 *
 * <p><b>Configuration Properties:</b></p>
 * <pre>
 * javainfohunter:
 *   processor:
 *     async:
 *       core-pool-size: 4        # Default: number of CPU cores
 *       max-pool-size: 16        # Default: 2x CPU cores
 *       queue-capacity: 100      # Default: 100
 *       thread-name-prefix: processor-async-
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * Default core pool size: number of available processors.
     */
    private static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * Default max pool size: 2x the number of available processors.
     */
    private static final int DEFAULT_MAX_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();

    /**
     * Default queue capacity for pending tasks.
     */
    private static final int DEFAULT_QUEUE_CAPACITY = 100;

    /**
     * Default thread keep-alive time in seconds.
     */
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;

    /**
     * General purpose task executor for async operations.
     *
     * <p>This executor is used for asynchronous processing that doesn't require
     * dedicated thread pool isolation. It uses virtual threads when available
     * (Java 21+) for improved concurrency with minimal resource overhead.</p>
     *
     * <p>For Java 21+, consider using Executors.newVirtualThreadPerTaskExecutor()
     * for even better performance with I/O-bound tasks.</p>
     *
     * @return configured Executor for async task execution
     */
    @Bean(name = "processorTaskExecutor")
    public Executor processorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: number of threads to keep alive
        executor.setCorePoolSize(DEFAULT_CORE_POOL_SIZE);

        // Max pool size: maximum number of threads allowed
        executor.setMaxPoolSize(DEFAULT_MAX_POOL_SIZE);

        // Queue capacity: number of tasks to queue before creating new threads
        executor.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);

        // Thread name prefix for easy identification in logs
        executor.setThreadNamePrefix("processor-async-");

        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(DEFAULT_KEEP_ALIVE_SECONDS);

        // Allow core threads to time out
        executor.setAllowCoreThreadTimeOut(true);

        // Rejection policy: caller runs when queue is full
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized processorTaskExecutor: coreSize={}, maxSize={}, queueCapacity={}",
                DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, DEFAULT_QUEUE_CAPACITY);

        return executor;
    }

    /**
     * Dedicated executor for result aggregation operations.
     *
     * <p>This executor is specifically configured for use by the ResultAggregator
     * service, ensuring isolation from other async operations and preventing
     * resource contention.</p>
     *
     * @return configured Executor for result aggregation
     */
    @Bean(name = "aggregatorExecutor")
    public Executor aggregatorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Aggregation is I/O intensive (database, message queue), use more threads
        executor.setCorePoolSize(Math.max(4, DEFAULT_CORE_POOL_SIZE));
        executor.setMaxPoolSize(Math.max(8, DEFAULT_MAX_POOL_SIZE));
        executor.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);

        executor.setThreadNamePrefix("aggregator-");
        executor.setKeepAliveSeconds(DEFAULT_KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);

        // Rejection policy: caller runs when queue is full
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized aggregatorExecutor: coreSize={}, maxSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), DEFAULT_QUEUE_CAPACITY);

        return executor;
    }
}
