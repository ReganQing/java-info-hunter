package com.ron.javainfohunter.ai.agent.coordinator;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 协作执行结果
 * <p>
 * 封装多 Agent 协作执行的最终结果和中间状态
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Data
@Builder
public class CoordinationResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 最终输出
     */
    private String finalOutput;

    /**
     * 各 Agent 的输出（key: Agent ID, value: Agent 输出）
     */
    @Builder.Default
    private Map<String, String> agentOutputs = new HashMap<>();

    /**
     * 执行时长
     */
    private Duration duration;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 创建成功结果
     *
     * @param finalOutput  最终输出
     * @param agentOutputs 各 Agent 输出
     * @param duration     执行时长
     * @return 成功结果
     */
    public static CoordinationResult success(String finalOutput,
                                              Map<String, String> agentOutputs,
                                              Duration duration) {
        return CoordinationResult.builder()
                .success(true)
                .finalOutput(finalOutput)
                .agentOutputs(agentOutputs)
                .duration(duration)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static CoordinationResult failure(String errorMessage) {
        return CoordinationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
