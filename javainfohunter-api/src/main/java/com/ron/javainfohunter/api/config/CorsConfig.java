package com.ron.javainfohunter.api.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS Configuration for JavaInfoHunter API
 *
 * Configures Cross-Origin Resource Sharing (CORS) for the REST API.
 * Uses Spring Boot's ConfigurationProperties for flexible, environment-specific configuration.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>CORS is disabled by default for security</li>
 *   <li>Production environments must use strict origin whitelists</li>
 *   <li>Wildcard origins with credentials are automatically rejected by Spring</li>
 * </ul>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
@EnableConfigurationProperties(CorsConfig.CorsProperties.class)
public class CorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    public CorsConfig(CorsProperties corsProperties) {
        logger.info("CORS enabled: {}, origins: {}", corsProperties.isEnabled(), corsProperties.getAllowedOrigins());
    }

    /**
     * CORS configuration properties
     *
     * Maps to: javainfohunter.api.cors.* in application.yml
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "javainfohunter.api.cors")
    public static class CorsProperties {

        /**
         * Enable/disable CORS configuration
         * Default: false (disabled for security)
         */
        private boolean enabled = false;

        /**
         * Path pattern for CORS configuration
         * Default: /** (all paths)
         */
        private String pathPattern = "/**";

        /**
         * Allowed origins for CORS requests
         * Example: http://localhost:3000, https://example.com
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /**
         * Allowed HTTP methods
         * Example: GET, POST, PUT, DELETE, OPTIONS
         */
        private List<String> allowedMethods = new ArrayList<>();

        /**
         * Allowed headers in CORS requests
         * Use "*" for wildcard (all headers)
         */
        private List<String> allowedHeaders = new ArrayList<>();

        /**
         * Allow credentials (cookies, authorization headers)
         * Default: false
         *
         * SECURITY WARNING: Cannot use "*" in allowedOrigins when this is true
         */
        private boolean allowCredentials = false;

        /**
         * How long (in seconds) the response from a pre-flight request can be cached
         * Default: 3600 (1 hour)
         */
        private long maxAge = 3600L;
    }
}
