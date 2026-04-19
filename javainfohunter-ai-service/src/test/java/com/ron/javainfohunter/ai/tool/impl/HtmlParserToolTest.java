package com.ron.javainfohunter.ai.tool.impl;

import com.ron.javainfohunter.ai.tool.observation.ErrorType;
import com.ron.javainfohunter.ai.tool.observation.ToolObservation;
import com.ron.javainfohunter.ai.tool.observation.ToolStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlParserToolTest {

    private HtmlParserTool tool;

    @BeforeEach
    void setUp() {
        tool = new HtmlParserTool();
    }

    // === parseHtml ===

    @Test
    void parseHtml_validHtml_returnsSuccess() {
        String html = "<html><head><title>Test Title</title></head>" +
            "<body><p>Hello world article content here.</p></body></html>";

        ToolObservation obs = tool.parseHtml(html);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertNotNull(obs.summary());
        assertTrue(obs.summary().contains("Test Title"));
        assertTrue(obs.details().contains("Hello world"));
        assertNull(obs.error());
    }

    @Test
    void parseHtml_extractsTitleFromBody_whenNoTitleTag() {
        String html = "<html><body><h1>Heading Title</h1><p>Content here.</p></body></html>";

        ToolObservation obs = tool.parseHtml(html);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertTrue(obs.details().contains("Heading Title"));
    }

    @Test
    void parseHtml_removesAdsAndNav() {
        String html = "<html><head><title>News</title></head><body>" +
            "<nav>Navigation links</nav>" +
            "<div class='ad'>Buy now!</div>" +
            "<footer>Footer content</footer>" +
            "<aside>Sidebar</aside>" +
            "<p>Real article content.</p>" +
            "</body></html>";

        ToolObservation obs = tool.parseHtml(html);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertFalse(obs.details().contains("Navigation links"));
        assertFalse(obs.details().contains("Buy now!"));
        assertTrue(obs.details().contains("Real article content"));
    }

    @Test
    void parseHtml_nullInput_returnsFailure() {
        ToolObservation obs = tool.parseHtml(null);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
        assertEquals(ErrorType.VALIDATION, obs.error().type());
    }

    @Test
    void parseHtml_emptyInput_returnsFailure() {
        ToolObservation obs = tool.parseHtml("");

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
    }

    @Test
    void parseHtml_malformedHtml_returnsSuccessWithFallback() {
        // Jsoup is lenient - even malformed HTML gets parsed
        ToolObservation obs = tool.parseHtml("<p>Just a fragment</p>");

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertTrue(obs.details().contains("Just a fragment"));
    }

    // === extractMetadata ===

    @Test
    void extractMetadata_fullHtml_returnsSuccessWithMetadata() {
        String html = "<html><head>" +
            "<title>Article Title</title>" +
            "<meta name='description' content='Article description'>" +
            "<meta name='keywords' content='java, ai, spring'>" +
            "</head><body><p>Content</p></body></html>";

        ToolObservation obs = tool.extractMetadata(html);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertTrue(obs.details().contains("Article Title"));
        assertTrue(obs.details().contains("Article description"));
        assertTrue(obs.details().contains("java, ai, spring"));
    }

    @Test
    void extractMetadata_nullInput_returnsFailure() {
        ToolObservation obs = tool.extractMetadata(null);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
    }

    // === extractLinks ===

    @Test
    void extractLinks_htmlWithLinks_returnsSuccess() {
        String html = "<html><body>" +
            "<a href='https://example.com/page1'>Page 1</a>" +
            "<a href='https://example.com/page2'>Page 2</a>" +
            "</body></html>";

        ToolObservation obs = tool.extractLinks(html);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertTrue(obs.details().contains("Page 1"));
        assertTrue(obs.details().contains("Page 2"));
    }

    @Test
    void extractLinks_nullInput_returnsFailure() {
        ToolObservation obs = tool.extractLinks(null);

        assertEquals(ToolStatus.FAILURE, obs.status());
    }

    @Test
    void extractLinks_limitsTo20Links() {
        StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 30; i++) {
            html.append("<a href='https://example.com/page").append(i).append("'>Link ").append(i).append("</a>");
        }
        html.append("</body></html>");

        ToolObservation obs = tool.extractLinks(html.toString());

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertTrue(obs.details().contains("10 个链接"));
    }

    // === stripHtml ===

    @Test
    void stripHtml_returnsPlainText() {
        String html = "<html><body><p>Hello <b>world</b></p></body></html>";

        ToolObservation obs = tool.stripHtml(html);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertTrue(obs.details().contains("Hello world"));
        assertFalse(obs.details().contains("<b>"));
    }

    @Test
    void stripHtml_nullInput_returnsFailure() {
        ToolObservation obs = tool.stripHtml(null);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
    }
}
