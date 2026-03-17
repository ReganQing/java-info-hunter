package com.ron.javainfohunter.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CorsConfig
 *
 * Tests CORS configuration loading and mapping to Spring's CorsRegistry.
 * Follows TDD methodology: tests written before implementation.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
class CorsConfigTest {

    private CorsConfig.CorsProperties corsProperties;
    private CorsRegistry corsRegistry;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsConfig.CorsProperties();
        corsRegistry = mock(CorsRegistry.class);
        when(corsRegistry.addMapping(any(String.class))).thenReturn(mock(org.springframework.web.servlet.config.annotation.CorsRegistration.class));
    }

    @Test
    void testCorsPropertiesDefaults() {
        // Test default values
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();

        assertFalse(properties.isEnabled(), "CORS should be disabled by default for security");
        assertEquals("/**", properties.getPathPattern(), "Default path pattern should be /**");
        assertNotNull(properties.getAllowedOrigins(), "Allowed origins should not be null");
        assertNotNull(properties.getAllowedMethods(), "Allowed methods should not be null");
        assertNotNull(properties.getAllowedHeaders(), "Allowed headers should not be null");
        assertFalse(properties.isAllowCredentials(), "Allow credentials should be false by default");
        assertEquals(3600L, properties.getMaxAge(), "Default max age should be 3600 seconds");
    }

    @Test
    void testCorsPropertiesSetters() {
        // Test setters
        corsProperties.setEnabled(true);
        corsProperties.setPathPattern("/api/**");
        corsProperties.setAllowedOrigins(List.of("http://localhost:3000"));
        corsProperties.setAllowedMethods(List.of("GET", "POST"));
        corsProperties.setAllowedHeaders(List.of("Content-Type"));
        corsProperties.setAllowCredentials(true);
        corsProperties.setMaxAge(1800L);

        assertTrue(corsProperties.isEnabled());
        assertEquals("/api/**", corsProperties.getPathPattern());
        assertEquals(List.of("http://localhost:3000"), corsProperties.getAllowedOrigins());
        assertEquals(List.of("GET", "POST"), corsProperties.getAllowedMethods());
        assertEquals(List.of("Content-Type"), corsProperties.getAllowedHeaders());
        assertTrue(corsProperties.isAllowCredentials());
        assertEquals(1800L, corsProperties.getMaxAge());
    }

    @Test
    void testCorsConfigWhenDisabled() {
        // When CORS is disabled, should not call registry.addMapping
        corsProperties.setEnabled(false);
        CorsConfig config = new CorsConfig(corsProperties);

        // This should not throw exception and should not call registry
        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));

        // Verify registry was not called
        verify(corsRegistry, never()).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigWhenEnabled() {
        // Test basic CORS configuration
        corsProperties.setEnabled(true);
        corsProperties.setPathPattern("/**");
        corsProperties.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        corsProperties.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsProperties.setAllowedHeaders(List.of("*"));
        corsProperties.setAllowCredentials(true);
        corsProperties.setMaxAge(3600L);

        CorsConfig config = new CorsConfig(corsProperties);

        // Should not throw exception
        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));

        // Verify registry was called once
        verify(corsRegistry, times(1)).addMapping("/**");
    }

    @Test
    void testCorsConfigurationWithWildcardHeaders() {
        // Test wildcard headers configuration
        corsProperties.setEnabled(true);
        corsProperties.setAllowedHeaders(List.of("*"));

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationWithMultipleOrigins() {
        // Test multiple allowed origins
        List<String> origins = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://staging.example.com"
        );

        corsProperties.setEnabled(true);
        corsProperties.setAllowedOrigins(origins);

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationWithAllCommonMethods() {
        // Test all common HTTP methods
        List<String> methods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

        corsProperties.setEnabled(true);
        corsProperties.setAllowedMethods(methods);

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationCredentialsDisabled() {
        // Test when credentials are not allowed
        corsProperties.setEnabled(true);
        corsProperties.setAllowCredentials(false);

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationCustomMaxAge() {
        // Test custom max age
        corsProperties.setEnabled(true);
        corsProperties.setMaxAge(7200L);

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationCustomPathPattern() {
        // Test custom path pattern
        corsProperties.setEnabled(true);
        corsProperties.setPathPattern("/api/**");

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping("/api/**");
    }

    @Test
    void testCorsConfigurationEmptyOrigins() {
        // Test with empty allowed origins list
        corsProperties.setEnabled(true);
        corsProperties.setAllowedOrigins(List.of());

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationEmptyMethods() {
        // Test with empty allowed methods list
        corsProperties.setEnabled(true);
        corsProperties.setAllowedMethods(List.of());

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsConfigurationEmptyHeaders() {
        // Test with empty allowed headers list
        corsProperties.setEnabled(true);
        corsProperties.setAllowedHeaders(List.of());

        CorsConfig config = new CorsConfig(corsProperties);

        assertDoesNotThrow(() -> config.addCorsMappings(corsRegistry));
        verify(corsRegistry, times(1)).addMapping(any(String.class));
    }

    @Test
    void testCorsPropertiesGetters() {
        // Test all getters
        corsProperties.setEnabled(true);
        corsProperties.setPathPattern("/test/**");
        corsProperties.setAllowedOrigins(List.of("http://test.com"));
        corsProperties.setAllowedMethods(List.of("GET"));
        corsProperties.setAllowedHeaders(List.of("X-Custom-Header"));
        corsProperties.setAllowCredentials(true);
        corsProperties.setMaxAge(1000L);

        assertTrue(corsProperties.isEnabled());
        assertEquals("/test/**", corsProperties.getPathPattern());
        assertEquals(List.of("http://test.com"), corsProperties.getAllowedOrigins());
        assertEquals(List.of("GET"), corsProperties.getAllowedMethods());
        assertEquals(List.of("X-Custom-Header"), corsProperties.getAllowedHeaders());
        assertTrue(corsProperties.isAllowCredentials());
        assertEquals(1000L, corsProperties.getMaxAge());
    }
}
