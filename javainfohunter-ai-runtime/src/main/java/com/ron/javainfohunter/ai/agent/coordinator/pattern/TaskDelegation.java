package com.ron.javainfohunter.ai.agent.coordinator.pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 任务分配对象
 * <p>
 * 用于 Master-Worker 模式中，Master Agent 向 Worker Agents 分配任务
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDelegation {

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 任务描述
     */
    private String taskDescription;

    /**
     * Worker 任务映射（key: Worker ID, value: 任务描述）
     */
    private Map<String, String> workerTasks;

    /**
     * 超时时间（秒）
     */
    @Builder.Default
    private int timeoutSeconds = 0;

    /**
     * 是否等待所有 Worker 完成
     */
    @Builder.Default
    private boolean waitForAll = false;
}
