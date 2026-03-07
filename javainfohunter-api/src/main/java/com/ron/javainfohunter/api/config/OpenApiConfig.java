package com.ron.javainfohunter.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration
 *
 * Configures OpenAPI documentation for the REST API.
 * Integrates with SpringDoc and Knife4j for enhanced API documentation.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
public class OpenApiConfig {

    /**
     * Custom OpenAPI bean
     *
     * @return OpenAPI configuration with API metadata
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JavaInfoHunter REST API")
                        .version("0.0.1-SNAPSHOT")
                        .description(
                                "REST API for JavaInfoHunter - High-Performance Distributed Information Collection System. " +
                                "Provides endpoints for managing RSS sources, querying news, and monitoring agent executions."
                        )
                        .contact(new Contact()
                                .name("JavaInfoHunter Team")
                                .email("contact@javainfohunter.com")
                                .url("https://github.com/javainfohunter")
                        )
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Server"),
                        new Server()
                                .url("https://api.javainfohunter.com")
                                .description("Production Server")
                ));
    }
}
