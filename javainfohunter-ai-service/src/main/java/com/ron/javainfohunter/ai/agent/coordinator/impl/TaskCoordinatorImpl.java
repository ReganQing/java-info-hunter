package com.ron.javainfohunter.ai.agent.coordinator.impl;

import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.coordinator.CollaborationPattern;
import com.ron.javainfohunter.ai.agent.coordinator.CoordinationResult;
import com.ron.javainfohunter.ai.agent.coordinator.TaskCoordinator;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Agent 任务协调器实现
 * <p>
 * 支持多种协作模式：
 * - Chain: 链式执行
 * - Parallel: 并行执行（使用虚拟线程）
 * - Master-Worker: 主从模式
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
public class TaskCoordinatorImpl implements TaskCoordinator {

    private final AgentManager agentManager;
    private final ExecutorService executor;

    /**
     * 构造函数
     *
     * @param agentManager Agent 管理器
     */
    public TaskCoordinatorImpl(AgentManager agentManager) {
        this.agentManager = agentManager;
        // 使用虚拟线程执行器（JDK 21+）
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public CoordinationResult executeMasterWorker(String taskDescription,
                                                   String masterAgentId,
                                                   List<String> workerAgentIds) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 验证 Master Agent
            if (!agentManager.isAgentRegistered(masterAgentId)) {
                return CoordinationResult.failure("Master agent not found: " + masterAgentId);
            }

            // 验证 Worker Agents
            for (String workerId : workerAgentIds) {
                if (!agentManager.isAgentRegistered(workerId)) {
                    return CoordinationResult.failure("Worker agent not found: " + workerId);
                }
            }

            // 获取 Master Agent
            BaseAgent masterAgent = agentManager.getAgent(masterAgentId).orElseThrow();

            // TODO: 实现 Master-Worker 逻辑
            // 1. Master 分配任务给 Workers
            // 2. Workers 并行执行
            // 3. Master 汇总结果

            return CoordinationResult.failure("Master-Worker pattern not implemented yet");

        } catch (Exception e) {
            log.error("Master-Worker execution failed", e);
            return CoordinationResult.failure("Master-Worker execution failed: " + e.getMessage());
        } finally {
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            log.debug("Master-Worker execution took {} ms", duration.toMillis());
        }
    }

    @Override
    public CoordinationResult executeChain(String taskDescription, List<String> agentIds) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 验证所有 Agents
            for (String agentId : agentIds) {
                if (!agentManager.isAgentRegistered(agentId)) {
                    return CoordinationResult.failure("Agent not found: " + agentId);
                }
            }

            Map<String, String> agentOutputs = new HashMap<>();
            String currentInput = taskDescription;

            // 链式执行
            for (String agentId : agentIds) {
                BaseAgent agent = agentManager.getAgent(agentId).orElseThrow();
                log.info("Chain: Executing agent {} with input: {}", agentId,
                        currentInput.substring(0, Math.min(100, currentInput.length())));

                String output = agent.run(currentInput);
                agentOutputs.put(agentId, output);
                currentInput = output; // 输出作为下一个的输入
            }

            Duration duration = Duration.between(startTime, LocalDateTime.now());
            return CoordinationResult.success(currentInput, agentOutputs, duration);

        } catch (Exception e) {
            log.error("Chain execution failed", e);
            return CoordinationResult.failure("Chain execution failed: " + e.getMessage());
        }
    }

    @Override
    public CoordinationResult executeParallel(String taskDescription, List<String> agentIds) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 验证所有 Agents
            for (String agentId : agentIds) {
                if (!agentManager.isAgentRegistered(agentId)) {
                    return CoordinationResult.failure("Agent not found: " + agentId);
                }
            }

            // 并行执行（使用虚拟线程）
            List<CompletableFuture<Map.Entry<String, String>>> futures = agentIds.stream()
                    .map(agentId -> CompletableFuture.supplyAsync(() -> {
                        BaseAgent agent = agentManager.getAgent(agentId).orElseThrow();
                        log.info("Parallel: Executing agent {}", agentId);

                        String output = agent.run(taskDescription);
                        return Map.entry(agentId, output);
                    }, executor))
                    .toList();

            // 等待所有完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 收集结果
            Map<String, String> agentOutputs = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // 汇总结果
            String finalOutput = "Parallel execution completed. Agents: " + String.join(", ", agentIds);

            Duration duration = Duration.between(startTime, LocalDateTime.now());
            return CoordinationResult.success(finalOutput, agentOutputs, duration);

        } catch (Exception e) {
            log.error("Parallel execution failed", e);
            return CoordinationResult.failure("Parallel execution failed: " + e.getMessage());
        }
    }

    @Override
    public CoordinationResult execute(String taskDescription,
                                       CollaborationPattern pattern,
                                       List<String> agentIds) {
        log.info("Executing task with pattern: {}, agents: {}", pattern, agentIds);

        return switch (pattern) {
            case CHAIN -> executeChain(taskDescription, agentIds);
            case PARALLEL -> executeParallel(taskDescription, agentIds);
            case MASTER_WORKER -> {
                // Master-Worker 需要 Master Agent ID，这里默认使用第一个
                if (agentIds.isEmpty()) {
                    yield CoordinationResult.failure("No agents provided for Master-Worker pattern");
                }
                String masterAgentId = agentIds.get(0);
                List<String> workerAgentIds = agentIds.subList(1, agentIds.size());
                yield executeMasterWorker(taskDescription, masterAgentId, workerAgentIds);
            }
        };
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        executor.shutdown();
    }
}
