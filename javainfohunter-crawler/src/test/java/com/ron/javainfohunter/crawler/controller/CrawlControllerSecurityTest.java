package com.ron.javainfohunter.crawler.controller;

import com.ron.javainfohunter.crawler.scheduler.CrawlOrchestrator;
import com.ron.javainfohunter.repository.RssSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CrawlControllerSecurityTest {

    @Mock
    private CrawlOrchestrator crawlOrchestrator;

    @Mock
    private RssSourceRepository rssSourceRepository;

    private CrawlController controller;

    @BeforeEach
    void setUp() {
        controller = new CrawlController(crawlOrchestrator, rssSourceRepository);
    }

    @Test
    void trigger_shouldRequireApiKeyWhenConfigured() {
        ReflectionTestUtils.setField(controller, "apiKey", "secret-key");

        var response = controller.triggerCrawl("wrong-key");
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void trigger_shouldAcceptCorrectApiKey() {
        ReflectionTestUtils.setField(controller, "apiKey", "secret-key");

        // Even with correct key, no sources means we get a different response
        var response = controller.triggerCrawl("secret-key");
        assertNotEquals(401, response.getStatusCode().value());
    }

    @Test
    void trigger_shouldAllowWhenNoKeyConfigured() {
        ReflectionTestUtils.setField(controller, "apiKey", "");

        var response = controller.triggerCrawl(null);
        assertNotEquals(401, response.getStatusCode().value());
    }

    @Test
    void triggerSource_shouldRequireApiKeyWhenConfigured() {
        ReflectionTestUtils.setField(controller, "apiKey", "secret-key");

        var response = controller.triggerCrawlForSource(1L, "wrong-key");
        assertEquals(401, response.getStatusCode().value());
    }
}
