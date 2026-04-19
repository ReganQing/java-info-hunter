package com.ron.javainfohunter.ai.tool.observation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolObservationTest {

    @Test
    void success_factory_createsCorrectObservation() {
        ToolObservation obs = ToolObservation.success(
            "解析完成：标题 'Test'，正文 1000 字",
            "【标题】Test\n\n【正文】content..."
        );

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertEquals("解析完成：标题 'Test'，正文 1000 字", obs.summary());
        assertEquals("【标题】Test\n\n【正文】content...", obs.details());
        assertTrue(obs.nextActions().contains("continue"));
        assertNull(obs.error());
        assertTrue(obs.artifacts().isEmpty());
    }

    @Test
    void success_withArtifacts_createsCorrectObservation() {
        ToolObservation obs = ToolObservation.success(
            "链接提取完成",
            "10 个链接"
        ).withArtifacts(Map.of("linkCount", "10"));

        assertEquals(ToolStatus.SUCCESS, obs.status());
        assertEquals(Map.of("linkCount", "10"), obs.artifacts());
    }

    @Test
    void failure_factory_createsCorrectObservation() {
        ErrorContext error = new ErrorContext(
            ErrorType.PARSE,
            "HTML 格式无效",
            "提供的 HTML 内容格式异常",
            false,
            "尝试使用 stripHtml 去除标签后重试"
        );

        ToolObservation obs = ToolObservation.failure("HTML 解析失败", error);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertEquals("HTML 解析失败", obs.summary());
        assertNull(obs.details());
        assertNotNull(obs.error());
        assertEquals(ErrorType.PARSE, obs.error().type());
        assertFalse(obs.error().retryable());
        assertEquals("尝试使用 stripHtml 去除标签后重试", obs.error().retryHint());
        assertTrue(obs.nextActions().contains("abort"));
    }

    @Test
    void failure_retryable_hasCorrectNextActions() {
        ErrorContext error = new ErrorContext(
            ErrorType.NETWORK,
            "连接超时",
            "远程服务器响应超时",
            true,
            "等待 5 秒后重试"
        );

        ToolObservation obs = ToolObservation.failure("网络请求失败", error);

        assertEquals(ToolStatus.FAILURE, obs.status());
        assertTrue(obs.error().retryable());
        assertEquals("等待 5 秒后重试", obs.error().retryHint());
        assertTrue(obs.nextActions().contains("等待 5 秒后重试"));
    }

    @Test
    void partial_success_createsCorrectObservation() {
        ToolObservation obs = ToolObservation.partial(
            "部分完成：5/8 Worker 成功",
            "成功: worker1, worker2\n失败: worker3"
        );

        assertEquals(ToolStatus.PARTIAL, obs.status());
        assertEquals("部分完成：5/8 Worker 成功", obs.summary());
    }

    @Test
    void withNextActions_returnsNewInstance() {
        ToolObservation obs = ToolObservation.success("ok", "details");
        ToolObservation modified = obs.withNextActions(List.of("step1", "step2"));

        // Original unchanged (immutability)
        assertEquals(List.of("continue"), obs.nextActions());
        // New instance has updated value
        assertEquals(List.of("step1", "step2"), modified.nextActions());
    }

    @Test
    void record_equality_works() {
        ToolObservation obs1 = ToolObservation.success("same", "same details");
        ToolObservation obs2 = ToolObservation.success("same", "same details");
        assertEquals(obs1, obs2);
        assertEquals(obs1.hashCode(), obs2.hashCode());
    }

    @Test
    void toString_containsStatus() {
        ToolObservation obs = ToolObservation.success("ok", "detail");
        String str = obs.toString();
        assertTrue(str.contains("SUCCESS"));
        assertTrue(str.contains("ok"));
    }
}
