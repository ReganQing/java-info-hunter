package com.ron.javainfohunter.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private CorsConfig.CorsProperties corsProperties;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsConfig.CorsProperties();
    }

    @Test
    void testCorsPropertiesDefaults() {
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
    void testCorsPropertiesSettersAndGetters() {
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
    void testCorsConfigConstructionDoesNotThrow() {
        assertDoesNotThrow(() -> new CorsConfig(corsProperties));
    }

    @Test
    void testCorsConfigConstructionWhenEnabled() {
        corsProperties.setEnabled(true);
        corsProperties.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        assertDoesNotThrow(() -> new CorsConfig(corsProperties));
    }

    @Test
    void testCorsPropertiesWithMultipleOrigins() {
        List<String> origins = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://staging.example.com"
        );

        corsProperties.setAllowedOrigins(origins);
        assertEquals(3, corsProperties.getAllowedOrigins().size());
        assertTrue(corsProperties.getAllowedOrigins().containsAll(origins));
    }

    @Test
    void testCorsPropertiesWithAllCommonMethods() {
        List<String> methods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        corsProperties.setAllowedMethods(methods);
        assertEquals(6, corsProperties.getAllowedMethods().size());
    }

    @Test
    void testCorsPropertiesWithEmptyCollections() {
        corsProperties.setAllowedOrigins(List.of());
        corsProperties.setAllowedMethods(List.of());
        corsProperties.setAllowedHeaders(List.of());

        assertTrue(corsProperties.getAllowedOrigins().isEmpty());
        assertTrue(corsProperties.getAllowedMethods().isEmpty());
        assertTrue(corsProperties.getAllowedHeaders().isEmpty());
    }
}
