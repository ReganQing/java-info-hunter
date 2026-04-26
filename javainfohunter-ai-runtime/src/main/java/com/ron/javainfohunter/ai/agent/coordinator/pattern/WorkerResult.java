package com.ron.javainfohunter.ai.agent.coordinator.pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Worker 执行结果
 * <p>
 * 用于 Master-Worker 模式中，Worker Agent 返回执行结果
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResult {

    /**
     * Worker ID
     */
    private String workerId;

    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = false;

    /**
     * 输出结果
     */
    private String output;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间（毫秒）
     */
    @Builder.Default
    private long executionTimeMs = 0;
}
