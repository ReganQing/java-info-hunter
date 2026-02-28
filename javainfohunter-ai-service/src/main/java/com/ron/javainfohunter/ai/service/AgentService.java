package com.ron.javainfohunter.ai.service;

import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.coordinator.CollaborationPattern;
import com.ron.javainfohunter.ai.agent.coordinator.CoordinationResult;
import com.ron.javainfohunter.ai.agent.coordinator.TaskCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Agent 编排服务
 * <p>
 * 提供简化的 Agent 协作接口
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private final TaskCoordinator taskCoordinator;
    private final AgentManager agentManager;

    /**
     * 使用 Chain 模式执行任务
     *
     * @param taskDescription 任务描述
     * @param agentIds       Agent IDs（按顺序）
     * @return 执行结果
     */
    public CoordinationResult executeChain(String taskDescription, List<String> agentIds) {
        log.info("Executing chain task with agents: {}", agentIds);
        return taskCoordinator.executeChain(taskDescription, agentIds);
    }

    /**
     * 使用 Parallel 模式执行任务
     *
     * @param taskDescription 任务描述
     * @param agentIds       Agent IDs
     * @return 执行结果
     */
    public CoordinationResult executeParallel(String taskDescription, List<String> agentIds) {
        log.info("Executing parallel task with agents: {}", agentIds);
        return taskCoordinator.executeParallel(taskDescription, agentIds);
    }

    /**
     * 使用 Master-Worker 模式执行任务
     *
     * @param taskDescription 任务描述
     * @param masterAgentId  主 Agent ID
     * @param workerAgentIds  Worker Agent IDs
     * @return 执行结果
     */
    public CoordinationResult executeMasterWorker(String taskDescription,
                                                    String masterAgentId,
                                                    List<String> workerAgentIds) {
        log.info("Executing master-worker task - Master: {}, Workers: {}",
                masterAgentId, workerAgentIds);
        return taskCoordinator.executeMasterWorker(taskDescription, masterAgentId, workerAgentIds);
    }

    /**
     * 使用指定模式执行任务
     *
     * @param taskDescription 任务描述
     * @param pattern        协作模式
     * @param agentIds       Agent IDs
     * @return 执行结果
     */
    public CoordinationResult execute(String taskDescription,
                                       CollaborationPattern pattern,
                                       List<String> agentIds) {
        log.info("Executing task with pattern: {}, agents: {}", pattern, agentIds);
        return taskCoordinator.execute(taskDescription, pattern, agentIds);
    }

    /**
     * 获取 Agent 管理器
     *
     * @return AgentManager
     */
    public AgentManager getAgentManager() {
        return agentManager;
    }

    /**
     * 获取任务协调器
     *
     * @return TaskCoordinator
     */
    public TaskCoordinator getTaskCoordinator() {
        return taskCoordinator;
    }
}
