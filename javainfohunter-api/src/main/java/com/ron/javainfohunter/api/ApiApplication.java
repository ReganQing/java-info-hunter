package com.ron.javainfohunter.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * JavaInfoHunter REST API Application
 *
 * REST API module for the JavaInfoHunter distributed information collection system.
 * Provides endpoints for managing RSS sources, querying news, and monitoring agent executions.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootApplication(scanBasePackages = "com.ron.javainfohunter")
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
