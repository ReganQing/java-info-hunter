package com.ron.javainfohunter.e2e;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application configuration for E2E tests.
 *
 * <p>This configuration scans the necessary packages for components,
 * repositories, and entities from all modules.
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.ron.javainfohunter",
        "com.ron.javainfohunter.e2e"
    }
)
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = "com.ron.javainfohunter.repository")
public class TestApplication {
    // Test configuration class
}
