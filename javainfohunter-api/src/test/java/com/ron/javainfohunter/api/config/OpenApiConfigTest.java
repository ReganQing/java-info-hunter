package com.ron.javainfohunter.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenApiConfig
 */
class OpenApiConfigTest {

    @Test
    void testOpenApiBeanCreation() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
    }

    @Test
    void testOpenApiInfo() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();
        Info info = openAPI.getInfo();

        assertEquals("JavaInfoHunter REST API", info.getTitle());
        assertEquals("0.0.1-SNAPSHOT", info.getVersion());
        assertNotNull(info.getDescription());
    }

    @Test
    void testOpenApiContact() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();
        Info info = openAPI.getInfo();
        Contact contact = info.getContact();

        assertNotNull(contact);
        assertEquals("JavaInfoHunter Team", contact.getName());
    }

    @Test
    void testOpenApiLicense() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();
        Info info = openAPI.getInfo();
        License license = info.getLicense();

        assertNotNull(license);
        assertEquals("MIT License", license.getName());
        assertEquals("https://opensource.org/licenses/MIT", license.getUrl());
    }

    @Test
    void testOpenApiServers() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI.getServers());
        assertFalse(openAPI.getServers().isEmpty());
        assertEquals("http://localhost:8080", openAPI.getServers().get(0).getUrl());
        assertEquals("Local Server", openAPI.getServers().get(0).getDescription());
    }
}
