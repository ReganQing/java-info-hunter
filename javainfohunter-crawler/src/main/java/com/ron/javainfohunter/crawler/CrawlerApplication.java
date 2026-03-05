package com.ron.javainfohunter.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JavaInfoHunter Crawler Application
 *
 * <p>Main Spring Boot application class for the crawler module.
 * This module is responsible for:</p>
 * <ul>
 *   <li>RSS feed crawling and content extraction</li>
 *   <li>Publishing raw content to RabbitMQ queues</li>
 *   <li>Managing RSS source configurations</li>
 *   <li>Tracking crawl statistics and failures</li>
 * </ul>
 *
 * <p><b>Architecture:</b></p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     Crawler Module                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │                                                               │
 * │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
 * │  │ RSS Feed    │───▶│ Content      │───▶│ RabbitMQ     │  │
 * │  │ Crawler     │    │ Processor    │    │ Publisher    │  │
 * │  └──────────────┘    └──────────────┘    └──────────────┘  │
 * │         │                                       │           │
 * │         ▼                                       ▼           │
 * │  ┌──────────────┐                      ┌──────────────┐   │
 * │  │ RSS Sources  │                      │ Raw Content  │   │
 * │  │ Repository   │                      │ Queue        │   │
 * │  └──────────────┘                      └──────────────┘   │
 * │                                                               │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 */
@SpringBootApplication
@EnableJpaAuditing
public class CrawlerApplication {

    /**
     * Main entry point for the Crawler application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CrawlerApplication.class, args);
    }

}
