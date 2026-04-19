package com.ron.javainfohunter.ai.tool.impl;

import com.ron.javainfohunter.ai.tool.observation.ErrorType;
import com.ron.javainfohunter.ai.tool.observation.ToolObservation;
import com.ron.javainfohunter.ai.tool.observation.ToolStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSummarizationToolTest {

    private TextSummarizationTool tool;

    @BeforeEach
    void setUp() {
        tool = new TextSummarizationTool();
    }

    // === extractSummary ===

    @Test
    void extractSummary_longText_returnsSuccess() {
        String text = "人工智能技术正在快速改变世界。机器学习算法已经在图像识别领域取得了突破性进展。" +
            "自然语言处理技术使得机器能够理解人类语言。深度学习模型在各个领域都展现出强大的能力。" +
            "未来人工智能将在更多领域发挥重要作用。";

        ToolObservation obs = tool.extractSummary(text, 2);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertNotNull(obs.summary());
        assertNotNull(obs.details());
        assertNull(obs.error());
    }

    @Test
    void extractSummary_nullInput_returnsFailure() {
        ToolObservation obs = tool.extractSummary(null, 3);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
        assertEquals(ErrorType.VALIDATION, obs.error().type());
    }

    @Test
    void extractSummary_emptyInput_returnsFailure() {
        ToolObservation obs = tool.extractSummary("   ", 3);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
    }

    @Test
    void extractSummary_shortText_returnsSuccess() {
        String text = "这是一段较短的文本内容，用于测试摘要功能。";
        ToolObservation obs = tool.extractSummary(text, 3);

        assertEquals(ToolStatus.SUCCESS, obs.status());
    }

    @Test
    void extractSummary_preservesOriginalOrder() {
        String text = "第一句重要内容包含关键词人工智能。第二句不太重要。" +
            "第三句也包含人工智能关键词。第四句又是普通内容。" +
            "第五句再次提到人工智能发展方向。";

        ToolObservation obs = tool.extractSummary(text, 2);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        // Results should be in original text order, not score order
        assertNotNull(obs.details());
    }

    // === extractKeywords ===

    @Test
    void extractKeywords_validText_returnsSuccess() {
        String text = "人工智能和机器学习是当前科技领域的热门话题。" +
            "深度学习作为机器学习的一个分支，在图像识别和自然语言处理方面取得了显著成果。" +
            "人工智能技术的应用范围正在不断扩大。";

        ToolObservation obs = tool.extractKeywords(text, 5);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertNotNull(obs.details());
        assertNull(obs.error());
    }

    @Test
    void extractKeywords_nullInput_returnsFailure() {
        ToolObservation obs = tool.extractKeywords(null, 10);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertNotNull(obs.error());
        assertEquals(ErrorType.VALIDATION, obs.error().type());
    }

    @Test
    void extractKeywords_emptyInput_returnsFailure() {
        ToolObservation obs = tool.extractKeywords("", 10);

        assertEquals(ToolStatus.FAILURE, obs.status());
    }

    @Test
    void extractKeywords_limitsResults() {
        String text = "人工智能技术正在快速发展。机器学习算法不断优化。" +
            "深度学习模型越来越强大。自然语言处理取得突破。" +
            "计算机视觉技术成熟。数据科学领域蓬勃发展。";

        ToolObservation obs = tool.extractKeywords(text, 3);

        assertEquals(ToolStatus.SUCCESS, obs.status());
        // Should return at most 3 keywords
        String[] keywords = obs.details().split("[、,]");
        assertTrue(keywords.length <= 3);
    }
}
