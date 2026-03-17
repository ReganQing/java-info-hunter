package com.ron.javainfohunter.api;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application for integration tests.
 * <p>
 * This is a minimal Spring Boot application that only loads the API module,
 * excluding the AI service module to avoid complex dependencies in tests.
 * </p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {
    "com.ron.javainfohunter.api.controller",
    "com.ron.javainfohunter.api.service",
    "com.ron.javainfohunter.api.dto",
    "com.ron.javainfohunter.api.aspect",
    "com.ron.javainfohunter.api.config",
    "com.ron.javainfohunter.api.redis"
})
@EnableJpaRepositories(basePackages = "com.ron.javainfohunter.repository")
@EntityScan(basePackages = "com.ron.javainfohunter.entity")
public class TestApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(TestApplication.class, args);
    }
}
