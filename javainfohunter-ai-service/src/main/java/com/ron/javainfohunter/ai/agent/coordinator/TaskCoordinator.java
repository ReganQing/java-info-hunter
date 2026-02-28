package com.ron.javainfohunter.ai.agent.coordinator;

import java.util.List;

/**
 * Agent 任务协调器接口
 * <p>
 * 负责编排多个 Agent 的协作执行，支持多种协作模式
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
public interface TaskCoordinator {

    /**
     * Master-Worker 模式执行
     * <p>
     * 在此模式中，Master Agent 负责任务分配和结果汇总，
     * Worker Agents 并行执行子任务
     * </p>
     *
     * @param taskDescription 任务描述
     * @param masterAgentId  主 Agent ID
     * @param workerAgentIds  Worker Agent IDs
     * @return 执行结果
     * @throws IllegalArgumentException 如果 masterAgentId 未注册或 workerAgentIds 为空
     */
    CoordinationResult executeMasterWorker(
            String taskDescription,
            String masterAgentId,
            List<String> workerAgentIds
    );

    /**
     * Chain 模式执行
     * <p>
     * Agents 按顺序执行，每个 Agent 的输出作为下一个 Agent 的输入
     * </p>
     *
     * @param taskDescription 任务描述
     * @param agentIds       Agent IDs（按执行顺序）
     * @return 执行结果
     * @throws IllegalArgumentException 如果 agentIds 为空或包含未注册的 Agent
     */
    CoordinationResult executeChain(
            String taskDescription,
            List<String> agentIds
    );

    /**
     * Parallel 模式执行
     * <p>
     * 所有 Agent 同时执行同一个任务，结果聚合
     * </p>
     *
     * @param taskDescription 任务描述
     * @param agentIds       Agent IDs
     * @return 执行结果
     * @throws IllegalArgumentException 如果 agentIds 为空或包含未注册的 Agent
     */
    CoordinationResult executeParallel(
            String taskDescription,
            List<String> agentIds
    );

    /**
     * 通用执行方法
     * <p>
     * 支持任意协作模式的执行
     * </p>
     *
     * @param taskDescription 任务描述
     * @param pattern        协作模式
     * @param agentIds       Agent IDs
     * @return 执行结果
     * @throws IllegalArgumentException 如果 pattern 不支持或 agentIds 无效
     */
    CoordinationResult execute(
            String taskDescription,
            CollaborationPattern pattern,
            List<String> agentIds
    );
}
