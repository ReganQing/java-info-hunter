package com.ron.javainfohunter.ai.agent.coordinator;

/**
 * Agent 协作模式
 * <p>
 * 定义多种 Agent 协作模式，用于处理复杂的多步骤任务
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
public enum CollaborationPattern {

    /**
     * 主从模式
     * <p>
     * 一个协调者 Agent 负责任务分配和结果汇总
     * 多个 Worker Agent 并行执行子任务
     * </p>
     */
    MASTER_WORKER,

    /**
     * 链式模式
     * <p>
     * Agent 按顺序执行，每个 Agent 的输出作为下一个 Agent 的输入
     * 适用于需要依次处理的流水线任务
     * </p>
     */
    CHAIN,

    /**
     * 并行模式
     * <p>
     * 多个 Agent 同时执行同一个任务，结果聚合
     * 适用于需要从多个角度分析的任务
     * </p>
     */
    PARALLEL
}
