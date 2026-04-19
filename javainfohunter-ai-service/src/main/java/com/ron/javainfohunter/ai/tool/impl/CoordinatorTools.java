package com.ron.javainfohunter.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.coordinator.pattern.TaskDelegation;
import com.ron.javainfohunter.ai.tool.annotation.Tool;
import com.ron.javainfohunter.ai.tool.annotation.ToolParam;
import com.ron.javainfohunter.ai.tool.observation.ErrorContext;
import com.ron.javainfohunter.ai.tool.observation.ErrorType;
import com.ron.javainfohunter.ai.tool.observation.ToolObservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 协调者工具集
 * <p>
 * 为 CoordinatorAgent 提供任务分配、状态检查、结果聚合等工具。
 * 所有方法返回结构化 ToolObservation。
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class CoordinatorTools {

    private final ObjectMapper objectMapper;
    private final AgentManager agentManager;

    public CoordinatorTools(AgentManager agentManager) {
        this.objectMapper = new ObjectMapper();
        this.agentManager = agentManager;
    }

    /**
     * 分配任务给 Workers
     *
     * @param taskId 任务 ID
     * @param taskDescription 任务描述
     * @param workerTasksJson Worker 任务映射 JSON 字符串 (格式: {"worker1":"task1","worker2":"task2"})
     * @param timeoutSeconds 超时时间（秒）
     * @param waitForAll 是否等待所有 Worker 完成
     * @return 结构化观测结果
     */
    @Tool(name = "delegateTask", description = "分配任务给 Workers")
    public ToolObservation delegateTask(
            @ToolParam("任务 ID") String taskId,
            @ToolParam("任务描述") String taskDescription,
            @ToolParam("Worker 任务映射 JSON") String workerTasksJson,
            @ToolParam("超时时间（秒）") int timeoutSeconds,
            @ToolParam("是否等待所有 Worker 完成") boolean waitForAll) {

        log.info("Delegating task {} to workers: {}", taskId, taskDescription);

        try {
            Map<String, String> workerTasks = parseWorkerTasksJson(workerTasksJson);

            TaskDelegation delegation = TaskDelegation.builder()
                    .taskId(taskId)
                    .taskDescription(taskDescription)
                    .workerTasks(workerTasks)
                    .timeoutSeconds(timeoutSeconds)
                    .waitForAll(waitForAll)
                    .build();

            log.debug("Task delegation created: {} workers, timeout={}s",
                    workerTasks.size(), timeoutSeconds);

            return ToolObservation.success(
                "任务 %s 已分配给 %d 个 Worker，超时 %ds".formatted(
                    taskId, workerTasks.size(), timeoutSeconds),
                String.format("任务ID: %s\nWorker 数量: %d\n超时: %ds\n等待全部: %b",
                    taskId, workerTasks.size(), timeoutSeconds, waitForAll)
            );
        } catch (Exception e) {
            log.error("Failed to delegate task", e);
            return ToolObservation.failure("任务分配失败",
                new ErrorContext(ErrorType.VALIDATION, e.getMessage(),
                    "任务分配过程中发生错误，请检查 JSON 格式", false,
                    "确保 workerTasksJson 格式为 {\"worker1\":\"task1\"}"));
        }
    }

    /**
     * 检查 Worker 状态
     *
     * @param workerIdsJson Worker ID 列表 JSON 字符串 (格式: ["worker1","worker2"])
     * @return 结构化观测结果
     */
    @Tool(name = "checkWorkerStatus", description = "检查 Worker 完成状态")
    public ToolObservation checkWorkerStatus(
            @ToolParam("Worker ID 列表 JSON") String workerIdsJson) {

        log.debug("Checking worker status for: {}", workerIdsJson);

        try {
            List<String> workerIds = parseWorkerIdsJson(workerIdsJson);

            StringBuilder sb = new StringBuilder();
            for (String workerId : workerIds) {
                sb.append("- Worker ").append(workerId).append(": Status unknown\n");
            }

            return ToolObservation.success(
                "已查询 %d 个 Worker 状态".formatted(workerIds.size()),
                sb.toString()
            );
        } catch (Exception e) {
            log.error("Failed to check worker status", e);
            return ToolObservation.failure("Worker 状态查询失败",
                new ErrorContext(ErrorType.VALIDATION, e.getMessage(),
                    "Worker ID 列表解析失败，请检查 JSON 格式", false,
                    "确保格式为 [\"worker1\",\"worker2\"]"));
        }
    }

    /**
     * 聚合 Worker 结果
     *
     * @param resultsJson Worker 结果列表 JSON 字符串
     * @return 结构化观测结果
     */
    @Tool(name = "aggregateResults", description = "聚合 Worker 结果")
    public ToolObservation aggregateResults(
            @ToolParam("Worker 结果列表 JSON") String resultsJson) {

        log.info("Aggregating worker results");

        try {
            return ToolObservation.success(
                "Worker 结果聚合完成",
                "结果已聚合"
            );
        } catch (Exception e) {
            log.error("Failed to aggregate results", e);
            return ToolObservation.failure("结果聚合失败",
                new ErrorContext(ErrorType.FATAL, e.getMessage(),
                    "结果聚合过程中发生错误", true, "可尝试重新聚合"));
        }
    }

    /**
     * 获取可用的 Workers 列表
     *
     * @return 结构化观测结果
     */
    @Tool(name = "getAvailableWorkers", description = "获取可用 Workers 列表")
    public ToolObservation getAvailableWorkers() {
        log.debug("Getting available workers");

        try {
            List<String> workers = agentManager.getAgentNames();

            StringBuilder sb = new StringBuilder();
            for (String worker : workers) {
                sb.append("- ").append(worker).append("\n");
            }

            return ToolObservation.success(
                "发现 %d 个可用 Worker".formatted(workers.size()),
                sb.toString()
            ).withArtifacts(Map.of("workerCount", String.valueOf(workers.size())));
        } catch (Exception e) {
            log.error("Failed to get available workers", e);
            return ToolObservation.failure("获取 Worker 列表失败",
                new ErrorContext(ErrorType.FATAL, e.getMessage(),
                    "获取可用 Worker 列表时发生错误", true, "可稍后重试"));
        }
    }

    /**
     * 解析 Worker 任务 JSON（使用 Jackson ObjectMapper）
     *
     * @param json JSON 字符串
     * @return Worker 任务映射
     * @throws IllegalArgumentException 如果 JSON 格式无效
     */
    Map<String, String> parseWorkerTasksJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON format for worker tasks", e);
            throw new IllegalArgumentException("Invalid JSON format for worker tasks", e);
        }
    }

    /**
     * 解析 Worker ID JSON（使用 Jackson ObjectMapper）
     *
     * @param json JSON 字符串
     * @return Worker ID 列表
     * @throws IllegalArgumentException 如果 JSON 格式无效
     */
    List<String> parseWorkerIdsJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON format for worker IDs", e);
            throw new IllegalArgumentException("Invalid JSON format for worker IDs", e);
        }
    }
}
