package com.ron.javainfohunter.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityEqualityTest {

    @Test
    void rssSource_equalsAndHashCode_shouldUseIdOnly() {
        RssSource source1 = RssSource.builder().id(1L).name("Test").url("http://test.com").build();
        RssSource source2 = RssSource.builder().id(1L).name("Different").url("http://other.com").build();

        assertEquals(source1, source2);
        assertEquals(source1.hashCode(), source2.hashCode());
    }

    @Test
    void rssSource_equals_shouldHandleNullId() {
        RssSource source1 = RssSource.builder().name("Test").url("http://test.com").build();
        RssSource source2 = RssSource.builder().name("Test").url("http://test.com").build();

        assertEquals(source1, source1);
        assertNotEquals(source1, source2);
    }

    @Test
    void news_equalsAndHashCode_shouldUseIdOnly() {
        News news1 = News.builder().id(1L).title("Title A").summary("Summary A").build();
        News news2 = News.builder().id(1L).title("Title B").summary("Summary B").build();

        assertEquals(news1, news2);
        assertEquals(news1.hashCode(), news2.hashCode());
    }

    @Test
    void rawContent_equalsAndHashCode_shouldUseIdOnly() {
        RawContent rc1 = RawContent.builder().id(1L).contentHash("hash1").build();
        RawContent rc2 = RawContent.builder().id(1L).contentHash("hash2").build();

        assertEquals(rc1, rc2);
        assertEquals(rc1.hashCode(), rc2.hashCode());
    }

    @Test
    void agentExecution_equalsAndHashCode_shouldUseIdOnly() {
        AgentExecution ae1 = AgentExecution.builder().id(1L).agentId("agent-1").build();
        AgentExecution ae2 = AgentExecution.builder().id(1L).agentId("agent-2").build();

        assertEquals(ae1, ae2);
        assertEquals(ae1.hashCode(), ae2.hashCode());
    }

    @Test
    void differentIds_shouldNotBeEqual() {
        RssSource source1 = RssSource.builder().id(1L).name("Test").url("http://test.com").build();
        RssSource source2 = RssSource.builder().id(2L).name("Test").url("http://test.com").build();

        assertNotEquals(source1, source2);
    }
}
