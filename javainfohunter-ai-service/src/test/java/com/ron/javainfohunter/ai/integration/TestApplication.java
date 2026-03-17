package com.ron.javainfohunter.ai.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test application for integration tests.
 *
 * <p>This class provides the @SpringBootConfiguration needed for @SpringBootTest tests.
 * The javainfohunter-ai-service module is a library/starter module without a main
 * application class, so we need this test configuration.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@TestConfiguration
@SpringBootApplication
public class TestApplication {
    // No additional configuration needed - auto-configuration will handle everything
}
