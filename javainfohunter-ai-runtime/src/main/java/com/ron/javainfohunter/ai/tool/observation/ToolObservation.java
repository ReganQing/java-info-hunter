package com.ron.javainfohunter.ai.tool.observation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 标准工具观测结果
 * <p>
 * 所有工具应返回此结构，帮助 Agent 理解执行结果并做出后续决策。
 * </p>
 *
 * @param status      执行状态：SUCCESS | PARTIAL | FAILURE
 * @param summary     一行结果描述（Agent 快速理解结果）
 * @param details     完整输出内容
 * @param nextActions 建议的后续操作
 * @param artifacts   产生的文件路径 / ID
 * @param error       错误上下文（成功时为 null）
 */
public record ToolObservation(
    ToolStatus status,
    String summary,
    String details,
    List<String> nextActions,
    Map<String, String> artifacts,
    ErrorContext error
) {

    /**
     * 创建成功观测
     */
    public static ToolObservation success(String summary, String details) {
        return new ToolObservation(
            ToolStatus.SUCCESS,
            summary,
            details,
            List.of("continue"),
            Collections.emptyMap(),
            null
        );
    }

    /**
     * 创建失败观测
     */
    public static ToolObservation failure(String summary, ErrorContext error) {
        List<String> actions = error != null && error.retryHint() != null
            ? List.of(error.retryHint(), "skip", "abort")
            : List.of("abort");

        return new ToolObservation(
            ToolStatus.FAILURE,
            summary,
            null,
            actions,
            Collections.emptyMap(),
            error
        );
    }

    /**
     * 创建部分成功观测
     */
    public static ToolObservation partial(String summary, String details) {
        return new ToolObservation(
            ToolStatus.PARTIAL,
            summary,
            details,
            List.of("review_partial_results", "retry_failed"),
            Collections.emptyMap(),
            null
        );
    }

    /**
     * 返回附带 artifacts 的新实例（不可变）
     */
    public ToolObservation withArtifacts(Map<String, String> artifacts) {
        return new ToolObservation(status, summary, details, nextActions, artifacts, error);
    }

    /**
     * 返回附带 nextActions 的新实例（不可变）
     */
    public ToolObservation withNextActions(List<String> nextActions) {
        return new ToolObservation(status, summary, details, nextActions, artifacts, error);
    }
}
