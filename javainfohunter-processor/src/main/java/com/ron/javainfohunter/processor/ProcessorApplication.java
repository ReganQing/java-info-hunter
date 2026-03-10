package com.ron.javainfohunter.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Content Processor Application
 *
 * <p>Consumes raw RSS content from the crawler module and processes it
 * using AI agent orchestration for analysis, summarization, and classification.</p>
 *
 * @author JavaInfoHunter
 * @since 0.0.1-SNAPSHOT
 */
@SpringBootApplication(scanBasePackages = {
    "com.ron.javainfohunter.processor",
    "com.ron.javainfohunter.ai",
    "com.ron.javainfohunter.repository",
    "com.ron.javainfohunter.entity"
})
@EnableJpaRepositories(basePackages = "com.ron.javainfohunter.repository")
@EntityScan(basePackages = "com.ron.javainfohunter.entity")
@EnableJpaAuditing
public class ProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }
}
