package com.ron.javainfohunter.api.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
        logCorsConfiguration();
    }

    /**
     * Configure CORS mappings
     *
     * This method registers CORS configuration with Spring MVC.
     *
     * @param registry CorsRegistry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsProperties.isEnabled()) {
            logger.info("CORS is disabled");
            return;
        }

        registry.addMapping(corsProperties.getPathPattern())
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
                .allowCredentials(corsProperties.isAllowCredentials())
                .maxAge(corsProperties.getMaxAge());

        logger.info("CORS mappings registered successfully");
    }

    /**
     * Log CORS configuration at startup
     */
    private void logCorsConfiguration() {
        logger.info("==========================================");
        logger.info("CORS Configuration");
        logger.info("==========================================");
        logger.info("Enabled: {}", corsProperties.isEnabled());
        logger.info("Path Pattern: {}", corsProperties.getPathPattern());
        logger.info("Allowed Origins: {}", corsProperties.getAllowedOrigins());
        logger.info("Allowed Methods: {}", corsProperties.getAllowedMethods());
        logger.info("Allowed Headers: {}", corsProperties.getAllowedHeaders());
        logger.info("Allow Credentials: {}", corsProperties.isAllowCredentials());
        logger.info("Max Age: {} seconds", corsProperties.getMaxAge());
        logger.info("==========================================");
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
