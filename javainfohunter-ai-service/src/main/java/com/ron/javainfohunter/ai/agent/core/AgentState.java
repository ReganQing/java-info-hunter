package com.ron.javainfohunter.ai.agent.core;

import lombok.Getter;

/**
 * Agent 执行状态
 * <p>
 * 定义 Agent 生命周期中的各种状态
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Getter
public enum AgentState {

    /**
     * 空闲状态，可以接受新任务
     */
    IDLE("空闲"),

    /**
     * 运行中，正在执行任务
     */
    RUNNING("运行中"),

    /**
     * 已完成，任务执行成功
     */
    FINISHED("已完成"),

    /**
     * 错误状态，任务执行失败
     */
    ERROR("错误");

    private final String description;

    AgentState(String description) {
        this.description = description;
    }
}
