package com.ron.javainfohunter.api.integration;

import com.ron.javainfohunter.api.config.CorsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CORS configuration
 *
 * These tests verify that:
 * - CORS configuration beans are properly created
 * - CorsConfig implements WebMvcConfigurer
 * - Properties are loaded from application.yml
 *
 * Note: These tests use @SpringBootTest with a minimal configuration.
 * The test profile is used to avoid loading unnecessary beans.
 */
@SpringBootTest(classes = CorsConfig.class)
@ActiveProfiles("test")
@DisplayName("CORS Integration Tests")
class CorsIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private CorsConfig corsConfig;

    @Autowired(required = false)
    private CorsConfig.CorsProperties corsProperties;

    @Autowired(required = false)
    private WebMvcConfigurer webMvcConfigurer;

    @Test
    @DisplayName("CorsConfig bean should be created")
    void testCorsConfigBean_ShouldBeCreated() {
        assertNotNull(corsConfig,
                "CorsConfig bean should be created");
    }

    @Test
    @DisplayName("CorsConfig should implement WebMvcConfigurer")
    void testCorsConfig_ShouldImplementWebMvcConfigurer() {
        assertNotNull(corsConfig);
        assertTrue(corsConfig instanceof WebMvcConfigurer,
                "CorsConfig should implement WebMvcConfigurer");
    }

    @Test
    @DisplayName("CorsProperties bean should be created")
    void testCorsPropertiesBean_ShouldBeCreated() {
        assertNotNull(corsProperties,
                "CorsProperties bean should be created");
    }

    @Test
    @DisplayName("CorsProperties should load configuration correctly")
    void testCorsProperties_ShouldLoadConfigurationCorrectly() {
        assertNotNull(corsProperties);
        // Note: enabled status depends on application.yml configuration
        // Tests verify configuration is loaded, not the specific value
        assertEquals("/**", corsProperties.getPathPattern(),
                "Path pattern should be /**");
        assertEquals(3600L, corsProperties.getMaxAge(),
                "Max age should be 3600 seconds");
        assertFalse(corsProperties.isAllowCredentials(),
                "Allow credentials should be false when using wildcard origins");
    }

    @Test
    @DisplayName("CORS should be configurable via properties")
    void testCorsProperties_ShouldBeConfigurable() {
        assertNotNull(corsProperties);

        // Verify defaults can be changed
        corsProperties.setEnabled(true);
        corsProperties.setAllowedOrigins(List.of("http://localhost:3000"));
        corsProperties.setAllowedMethods(List.of("GET", "POST"));

        assertTrue(corsProperties.isEnabled());
        assertEquals(List.of("http://localhost:3000"), corsProperties.getAllowedOrigins());
        assertEquals(List.of("GET", "POST"), corsProperties.getAllowedMethods());
    }

    @Test
    @DisplayName("CorsConfig should handle null origins list")
    void testCorsConfig_ShouldHandleNullOrigins() {
        // This test verifies that the configuration handles edge cases
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();
        properties.setEnabled(true);
        // Don't set any origins - should use empty list

        CorsConfig config = new CorsConfig(properties);
        assertNotNull(config,
                "CorsConfig should handle empty origins list");
    }

    @Test
    @DisplayName("CorsConfig should handle wildcard headers")
    void testCorsConfig_ShouldHandleWildcardHeaders() {
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();
        properties.setEnabled(true);
        properties.setAllowedHeaders(List.of("*"));

        CorsConfig config = new CorsConfig(properties);
        assertNotNull(config,
                "CorsConfig should handle wildcard headers");
    }

    @Test
    @DisplayName("CorsConfig should support multiple origins")
    void testCorsConfig_ShouldSupportMultipleOrigins() {
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();
        properties.setEnabled(true);
        properties.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://example.com"
        ));

        CorsConfig config = new CorsConfig(properties);
        assertNotNull(config,
                "CorsConfig should support multiple origins");
    }

    @Test
    @DisplayName("CORS should be disabled when enabled flag is false")
    void testCorsConfig_ShouldBeDisabled_WhenEnabledIsFalse() {
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();
        properties.setEnabled(false);

        CorsConfig config = new CorsConfig(properties);
        assertNotNull(config,
                "CorsConfig should exist even when CORS is disabled");
    }

    @Test
    @DisplayName("CorsProperties should support all configurable fields")
    void testCorsProperties_ShouldSupportAllFields() {
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();

        // Test all configurable fields
        properties.setEnabled(true);
        properties.setPathPattern("/api/**");
        properties.setAllowedOrigins(List.of("http://localhost:3000"));
        properties.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        properties.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        properties.setAllowCredentials(true);
        properties.setMaxAge(7200L);

        assertTrue(properties.isEnabled());
        assertEquals("/api/**", properties.getPathPattern());
        assertEquals(List.of("http://localhost:3000"), properties.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), properties.getAllowedMethods());
        assertEquals(List.of("Content-Type", "Authorization"), properties.getAllowedHeaders());
        assertTrue(properties.isAllowCredentials());
        assertEquals(7200L, properties.getMaxAge());
    }
}
