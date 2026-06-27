package com.ron.javainfohunter.e2e;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application configuration for E2E tests.
 *
 * <p>This configuration intentionally limits component scanning to the API
 * module. The E2E test classpath also includes crawler and processor modules;
 * scanning the entire root package pulls in their application classes and
 * duplicates repository registration.
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.ron.javainfohunter.api",
        "com.ron.javainfohunter.e2e"
    }
)
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = "com.ron.javainfohunter.repository")
@EntityScan(basePackages = "com.ron.javainfohunter.entity")
public class TestApplication {
    // Test configuration class
}
