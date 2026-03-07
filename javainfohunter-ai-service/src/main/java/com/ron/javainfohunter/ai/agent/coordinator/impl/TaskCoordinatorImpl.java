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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
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

    // Security constants for DoS prevention
    private static final int MAX_CONCURRENT_WORKERS = 100;
    private static final int MAX_WORKER_QUEUE_SIZE = 500;
    private static final int WORKER_TIMEOUT_SECONDS = 30;
    private static final int MASTER_TIMEOUT_SECONDS = 300;

    // Agent ID validation pattern (alphanumeric, hyphens, underscores, 1-64 chars)
    private static final Pattern AGENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]{1,64}$");

    // Sanitized error messages (prevent information leakage)
    private static final String WORKER_ERROR_MSG = "Worker execution failed";
    private static final String COORDINATION_ERROR_MSG = "Task coordination failed";
    private static final String WORKER_TIMEOUT_MSG = "Worker execution timed out";

    private final AgentManager agentManager;
    private final ExecutorService executor;
    private final Semaphore workerLimiter;

    /**
     * 构造函数
     *
     * @param agentManager Agent 管理器
     */
    public TaskCoordinatorImpl(AgentManager agentManager) {
        this.agentManager = agentManager;
        // 使用虚拟线程执行器（JDK 21+）
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        // Semaphore to limit concurrent workers (DoS prevention)
        this.workerLimiter = new Semaphore(MAX_CONCURRENT_WORKERS);
    }

    /**
     * Validate agent ID format
     *
     * @param agentId the agent ID to validate
     * @throws IllegalArgumentException if agent ID is invalid
     */
    private void validateAgentId(String agentId) {
        if (agentId == null || agentId.isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        if (!AGENT_ID_PATTERN.matcher(agentId).matches()) {
            throw new IllegalArgumentException(
                "Invalid agent ID format: must be 1-64 alphanumeric characters, hyphens, or underscores"
            );
        }
    }

    /**
     * Validate a list of agent IDs
     *
     * @param agentIds the list of agent IDs to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAgentIds(List<String> agentIds) {
        if (agentIds == null) {
            throw new IllegalArgumentException("Agent IDs list cannot be null");
        }
        // Check for duplicates
        Set<String> uniqueIds = new HashSet<>(agentIds);
        if (uniqueIds.size() != agentIds.size()) {
            throw new IllegalArgumentException("Duplicate agent IDs detected");
        }
        // Validate each ID
        agentIds.forEach(this::validateAgentId);
    }

    @Override
    public CoordinationResult executeMasterWorker(String taskDescription,
                                                   String masterAgentId,
                                                   List<String> workerAgentIds) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Validate agent IDs (security check)
            try {
                validateAgentId(masterAgentId);
                validateAgentIds(workerAgentIds);
            } catch (IllegalArgumentException e) {
                return CoordinationResult.failure("Invalid agent ID: " + e.getMessage());
            }

            // Check worker queue size limit (DoS prevention)
            if (workerAgentIds.size() > MAX_WORKER_QUEUE_SIZE) {
                return CoordinationResult.failure(
                    "Too many workers. Maximum: " + MAX_WORKER_QUEUE_SIZE + ", provided: " + workerAgentIds.size()
                );
            }

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

            // 如果 Master Agent 是 CoordinatorAgent，设置 Workers
            if (masterAgent instanceof com.ron.javainfohunter.ai.agent.specialized.CoordinatorAgent coordinator) {
                coordinator.setWorkers(workerAgentIds);
            }

            // 并行执行 Workers (with semaphore limiting and timeout)
            Map<String, String> agentOutputs = new HashMap<>();
            List<CompletableFuture<WorkerResultEntry>> workerFutures = workerAgentIds.stream()
                    .map(workerId -> CompletableFuture.supplyAsync(() -> {
                        long workerStartTime = System.currentTimeMillis();
                        try {
                            // Acquire semaphore (DoS prevention)
                            workerLimiter.acquire();
                            try {
                                BaseAgent worker = agentManager.getAgent(workerId).orElseThrow();
                                log.info("Master-Worker: Executing worker {}", workerId);

                                String output = worker.run(taskDescription);
                                long executionTime = System.currentTimeMillis() - workerStartTime;

                                return new WorkerResultEntry(workerId, true, output, null, executionTime);
                            } finally {
                                workerLimiter.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            long executionTime = System.currentTimeMillis() - workerStartTime;
                            log.error("Worker {} interrupted", workerId, e);
                            return new WorkerResultEntry(workerId, false, null, WORKER_TIMEOUT_MSG, executionTime);
                        } catch (Exception e) {
                            long executionTime = System.currentTimeMillis() - workerStartTime;
                            log.error("Worker {} failed", workerId, e); // Full details in log only
                            return new WorkerResultEntry(workerId, false, null, WORKER_ERROR_MSG, executionTime);
                        }
                    }, executor))
                    .toList();

            // 等待所有 Workers 完成
            try {
                CompletableFuture.allOf(workerFutures.toArray(new CompletableFuture[0]))
                    .get(MASTER_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.error("Master-Worker execution timed out after {}s", MASTER_TIMEOUT_SECONDS);
                return CoordinationResult.failure(COORDINATION_ERROR_MSG);
            } catch (Exception e) {
                log.error("Master-Worker execution failed", e);
                return CoordinationResult.failure(COORDINATION_ERROR_MSG);
            }

            // 收集 Worker 结果
            List<WorkerResultEntry> workerResults = workerFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            // 将结果添加到 Master Agent（如果是 CoordinatorAgent）
            if (masterAgent instanceof com.ron.javainfohunter.ai.agent.specialized.CoordinatorAgent coordinator) {
                for (WorkerResultEntry entry : workerResults) {
                    com.ron.javainfohunter.ai.agent.coordinator.pattern.WorkerResult result =
                            com.ron.javainfohunter.ai.agent.coordinator.pattern.WorkerResult.builder()
                                    .workerId(entry.workerId())
                                    .success(entry.success())
                                    .output(entry.output())
                                    .errorMessage(entry.errorMessage())
                                    .executionTimeMs(entry.executionTimeMs())
                                    .build();
                    coordinator.addWorkerResult(result);
                }
            }

            // 收集所有 Agent 输出
            for (WorkerResultEntry entry : workerResults) {
                String output = entry.success() ? entry.output() : "Error: " + entry.errorMessage();
                agentOutputs.put(entry.workerId(), output);
            }

            // 执行 Master Agent 进行结果汇总
            String masterOutput = masterAgent.run(taskDescription);
            agentOutputs.put(masterAgentId, masterOutput);

            Duration duration = Duration.between(startTime, LocalDateTime.now());
            return CoordinationResult.success(masterOutput, agentOutputs, duration);

        } catch (Exception e) {
            log.error("Master-Worker execution failed for task: {}", taskDescription, e);
            return CoordinationResult.failure(COORDINATION_ERROR_MSG);
        } finally {
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            log.debug("Master-Worker execution took {} ms", duration.toMillis());
        }
    }

    /**
     * Worker 结果记录（内部使用）
     */
    private record WorkerResultEntry(
            String workerId,
            boolean success,
            String output,
            String errorMessage,
            long executionTimeMs
    ) {}

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
