package com.ron.javainfohunter.crawler;

import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.repository.AgentExecutionRepository;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import com.ron.javainfohunter.repository.RefreshTokenRepository;
import com.ron.javainfohunter.repository.RssSourceRepository;
import com.ron.javainfohunter.repository.UserRepository;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
@SpringBootApplication(scanBasePackages = "com.ron.javainfohunter")
@EnableJpaRepositories(basePackageClasses = {
    RssSourceRepository.class,
    RawContentRepository.class
}, includeFilters = {
    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RssSourceRepository.class),
    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RawContentRepository.class)
}, excludeFilters = {
    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AgentExecutionRepository.class),
    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NewsRepository.class),
    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RefreshTokenRepository.class),
    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UserRepository.class)
})
@EntityScan(basePackageClasses = {
    RssSource.class,
    RawContent.class,
    News.class
})
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
