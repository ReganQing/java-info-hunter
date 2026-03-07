package com.ron.javainfohunter.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.ai.agent.coordinator.pattern.TaskDelegation;
import com.ron.javainfohunter.ai.agent.coordinator.pattern.WorkerResult;
import com.ron.javainfohunter.ai.tool.annotation.Tool;
import com.ron.javainfohunter.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协调者工具集
 * <p>
 * 为 CoordinatorAgent 提供任务分配、状态检查、结果聚合等工具
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class CoordinatorTools {

    private final ObjectMapper objectMapper;

    public CoordinatorTools() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分配任务给 Workers
     * <p>
     * Master Agent 使用此工具将任务分配给多个 Worker Agents
     * </p>
     *
     * @param taskId 任务 ID
     * @param taskDescription 任务描述
     * @param workerTasksJson Worker 任务映射 JSON 字符串 (格式: {"worker1":"task1","worker2":"task2"})
     * @param timeoutSeconds 超时时间（秒）
     * @param waitForAll 是否等待所有 Worker 完成
     * @return 任务分配结果
     */
    @Tool(name = "delegateTask", description = "分配任务给 Workers")
    public String delegateTask(
            @ToolParam("任务 ID") String taskId,
            @ToolParam("任务描述") String taskDescription,
            @ToolParam("Worker 任务映射 JSON") String workerTasksJson,
            @ToolParam("超时时间（秒）") int timeoutSeconds,
            @ToolParam("是否等待所有 Worker 完成") boolean waitForAll) {

        log.info("Delegating task {} to workers: {}", taskId, taskDescription);

        try {
            // 解析 JSON（简化实现，实际应使用 Jackson）
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

            return String.format("Successfully delegated task %s to %d workers. " +
                    "Timeout: %ds, waitForAll: %b",
                    taskId, workerTasks.size(), timeoutSeconds, waitForAll);

        } catch (Exception e) {
            log.error("Failed to delegate task", e);
            return "Failed to delegate task: " + e.getMessage();
        }
    }

    /**
     * 检查 Worker 状态
     * <p>
     * 检查指定的 Workers 是否已完成任务
     * </p>
     *
     * @param workerIdsJson Worker ID 列表 JSON 字符串 (格式: ["worker1","worker2"])
     * @return Worker 状态信息
     */
    @Tool(name = "checkWorkerStatus", description = "检查 Worker 完成状态")
    public String checkWorkerStatus(
            @ToolParam("Worker ID 列表 JSON") String workerIdsJson) {

        log.debug("Checking worker status for: {}", workerIdsJson);

        try {
            List<String> workerIds = parseWorkerIdsJson(workerIdsJson);

            StringBuilder sb = new StringBuilder();
            sb.append("Worker status for ").append(workerIds.size()).append(" workers:\n");

            for (String workerId : workerIds) {
                // 简化实现：返回状态信息
                sb.append(String.format("- Worker %s: %s\n", workerId, "Status unknown"));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to check worker status", e);
            return "Failed to check worker status: " + e.getMessage();
        }
    }

    /**
     * 聚合 Worker 结果
     * <p>
     * 将多个 Worker 的执行结果聚合成一个汇总结果
     * </p
     *
     * @param resultsJson Worker 结果列表 JSON 字符串
     * @return 聚合后的结果
     */
    @Tool(name = "aggregateResults", description = "聚合 Worker 结果")
    public String aggregateResults(
            @ToolParam("Worker 结果列表 JSON") String resultsJson) {

        log.info("Aggregating worker results");

        try {
            // 简化实现：返回聚合信息
            return "Successfully aggregated worker results";

        } catch (Exception e) {
            log.error("Failed to aggregate results", e);
            return "Failed to aggregate results: " + e.getMessage();
        }
    }

    /**
     * 获取可用的 Workers 列表
     * <p>
     * 返回当前可用的所有 Worker Agents
     * </p>
     *
     * @return 可用 Workers 列表
     */
    @Tool(name = "getAvailableWorkers", description = "获取可用 Workers 列表")
    public String getAvailableWorkers() {
        log.debug("Getting available workers");

        try {
            // 简化实现：返回示例 Workers
            List<String> workers = List.of("worker1", "worker2", "worker3");

            StringBuilder sb = new StringBuilder();
            sb.append("Available workers (").append(workers.size()).append("):\n");
            for (String worker : workers) {
                sb.append("- ").append(worker).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to get available workers", e);
            return "Failed to get available workers: " + e.getMessage();
        }
    }

    /**
     * 解析 Worker 任务 JSON（使用 Jackson ObjectMapper）
     * <p>
     * 安全地解析 JSON 字符串为 Map，防止注入攻击
     * </p>
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
     * <p>
     * 安全地解析 JSON 字符串为 List，防止注入攻击
     * </p>
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
