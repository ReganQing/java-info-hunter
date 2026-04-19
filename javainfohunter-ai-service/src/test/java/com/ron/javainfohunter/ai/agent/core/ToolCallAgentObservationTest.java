package com.ron.javainfohunter.ai.agent.core;

import com.ron.javainfohunter.ai.tool.observation.ErrorContext;
import com.ron.javainfohunter.ai.tool.observation.ErrorType;
import com.ron.javainfohunter.ai.tool.observation.ToolObservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolCallAgent - ToolObservation Formatting")
class ToolCallAgentObservationTest {

    @Test
    @DisplayName("formatToolResult - SUCCESS observation includes status and summary")
    void formatToolResult_successObservation() {
        String obsJson = toJson(ToolObservation.success("解析完成", "提取了 3 个标题"));

        String result = ToolCallAgent.formatToolResult("parseHtml", obsJson);

        assertTrue(result.contains("[SUCCESS]"));
        assertTrue(result.contains("解析完成"));
        assertTrue(result.contains("parseHtml"));
        assertTrue(result.contains("提取了 3 个标题"));
    }

    @Test
    @DisplayName("formatToolResult - FAILURE observation includes error type and retry info")
    void formatToolResult_failureObservation() {
        String obsJson = toJson(ToolObservation.failure("解析失败",
            new ErrorContext(ErrorType.VALIDATION, "empty input",
                "输入不能为空", false, "提供有效的 HTML 内容")));

        String result = ToolCallAgent.formatToolResult("parseHtml", obsJson);

        assertTrue(result.contains("[FAILURE]"));
        assertTrue(result.contains("VALIDATION"));
        assertTrue(result.contains("不可重试"));
        assertTrue(result.contains("重试建议"));
    }

    @Test
    @DisplayName("formatToolResult - FAILURE with retryable error shows 可重试")
    void formatToolResult_retryableFailure() {
        String obsJson = toJson(ToolObservation.failure("网络错误",
            new ErrorContext(ErrorType.NETWORK, "connection timeout",
                "连接超时", true, "等待后重试")));

        String result = ToolCallAgent.formatToolResult("fetchUrl", obsJson);

        assertTrue(result.contains("可重试"));
        assertTrue(result.contains("等待后重试"));
    }

    @Test
    @DisplayName("formatToolResult - PARTIAL observation includes status")
    void formatToolResult_partialObservation() {
        String obsJson = toJson(ToolObservation.partial("部分完成", "处理了 5/10 项"));

        String result = ToolCallAgent.formatToolResult("batchProcess", obsJson);

        assertTrue(result.contains("[PARTIAL]"));
        assertTrue(result.contains("部分完成"));
    }

    @Test
    @DisplayName("formatToolResult - non-structured string falls back to raw format")
    void formatToolResult_nonStructured_fallback() {
        String result = ToolCallAgent.formatToolResult("oldTool", "plain text result");

        assertTrue(result.contains("oldTool"));
        assertTrue(result.contains("plain text result"));
        assertFalse(result.contains("[SUCCESS]"));
    }

    @Test
    @DisplayName("formatToolResult - null responseData returns empty result message")
    void formatToolResult_nullResponse() {
        String result = ToolCallAgent.formatToolResult("someTool", null);

        assertTrue(result.contains("空结果"));
    }

    @Test
    @DisplayName("formatToolResult - empty responseData returns empty result message")
    void formatToolResult_emptyResponse() {
        String result = ToolCallAgent.formatToolResult("someTool", "");

        assertTrue(result.contains("空结果"));
    }

    @Test
    @DisplayName("formatToolResult - invalid JSON falls back to raw format")
    void formatToolResult_invalidJson_fallback() {
        String result = ToolCallAgent.formatToolResult("tool", "not json at all {broken");

        assertTrue(result.contains("tool"));
        assertTrue(result.contains("not json at all"));
    }

    @Test
    @DisplayName("formatToolResult - JSON without status field falls back to raw format")
    void formatToolResult_jsonWithoutStatus_fallback() {
        String result = ToolCallAgent.formatToolResult("tool", "{\"message\":\"hello\"}");

        assertTrue(result.contains("hello"));
        assertFalse(result.contains("[SUCCESS]"));
    }

    private static String toJson(ToolObservation obs) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
