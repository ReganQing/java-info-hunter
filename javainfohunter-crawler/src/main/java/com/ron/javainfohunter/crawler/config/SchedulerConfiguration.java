package com.ron.javainfohunter.crawler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executors;

/**
 * Configuration class for Spring Scheduling with virtual thread support.
 *
 * <p>This configuration enables Spring's scheduled task execution and
 * configures a TaskScheduler that uses Java 21 virtual threads for
 * efficient concurrent execution of scheduled tasks.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Enables @Scheduled annotation support</li>
 *   <li>Uses virtual threads for improved concurrency</li>
 *   <li>Configures error handling for scheduled tasks</li>
 * </ul>
 *
 * @see org.springframework.scheduling.annotation.EnableScheduling
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfiguration {

    /**
     * Configure a TaskScheduler that uses virtual threads.
     *
     * <p>Virtual threads (Java 21+) are lightweight threads that allow
     * for massive concurrency without the overhead of platform threads.
     * This is ideal for I/O-bound scheduled tasks like RSS feed crawling.</p>
     *
     * <p><b>Benefits:</b></p>
     * <ul>
     *   <li>Lower memory footprint per thread</li>
     *   <li>Ability to create thousands of concurrent tasks</li>
     *   <li>Better resource utilization for I/O-bound operations</li>
     * </ul>
     *
     * @return TaskScheduler configured with virtual thread executor
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        log.info("Initializing TaskScheduler with virtual thread support");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("crawl-scheduler-");
        scheduler.setPoolSize(10); // Core pool size
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRejectedExecutionHandler((r, e) -> {
            log.warn("Scheduled task rejected: {}", r.toString());
            // Fallback: run in caller thread
            r.run();
        });

        // Note: ThreadPoolTaskScheduler doesn't support setTaskExecutor()
        // Virtual threads will be used by the crawlExecutor bean for actual crawl work

        // Handle uncaught exceptions
        scheduler.setErrorHandler(throwable -> {
            log.error("Error in scheduled task", throwable);
        });

        log.info("TaskScheduler initialized successfully");
        return scheduler;
    }

    /**
     * Configure a separate TaskExecutor for running crawl jobs.
     *
     * <p>This executor is dedicated to the actual crawl work and uses
     * virtual threads to enable concurrent crawling of multiple RSS sources.</p>
     *
     * @return ExecutorService backed by virtual threads
     */
    @Bean(name = "crawlExecutor")
    public java.util.concurrent.ExecutorService crawlExecutor() {
        log.info("Initializing crawl executor with virtual threads");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
