package com.ron.javainfohunter.ai.agent.specialized;

import com.ron.javainfohunter.ai.agent.coordinator.pattern.WorkerResult;
import com.ron.javainfohunter.ai.agent.core.AgentState;
import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 协调者 Agent
 * <p>
 * 用于 Master-Worker 模式的 Master Agent，负责：
 * 1. 任务分配给 Workers
 * 2. 收集 Worker 结果
 * 3. 结果聚合和汇总
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoordinatorAgent extends BaseAgent {

    /**
     * Worker Agent IDs 列表
     */
    private List<String> workers;

    /**
     * Worker 执行结果列表（线程安全）
     */
    private final List<WorkerResult> workerResults;

    /**
     * Worker 结果映射（线程安全，用于快速查找）
     */
    private final Map<String, WorkerResult> workerResultsMap;

    /**
     * 默认构造函数（用于 Spring 依赖注入）
     */
    public CoordinatorAgent() {
        this("coordinator-agent");
    }

    /**
     * 构造函数
     *
     * @param name Agent 名称
     */
    public CoordinatorAgent(String name) {
        super();
        setName(name);
        this.workerResults = new CopyOnWriteArrayList<>();
        this.workerResultsMap = new ConcurrentHashMap<>();
    }

    @Override
    public String step() {
        // 默认实现：检查所有 Worker 是否完成
        if (areAllWorkersComplete()) {
            setAgentState(AgentState.FINISHED);
            return aggregateResults();
        } else {
            return "Waiting for workers to complete. Progress: " +
                   workerResults.size() + "/" + (workers != null ? workers.size() : 0);
        }
    }

    @Override
    public void cleanup() {
        workerResults.clear();
        workerResultsMap.clear();
    }

    /**
     * 设置 Workers 列表
     *
     * @param workers Worker Agent IDs
     */
    public void setWorkers(List<String> workers) {
        this.workers = workers != null ? new ArrayList<>(workers) : null;
    }

    /**
     * 获取 Workers 列表
     *
     * @return Worker Agent IDs
     */
    public List<String> getWorkers() {
        return workers != null ? new ArrayList<>(workers) : null;
    }

    /**
     * 添加 Worker 执行结果
     *
     * @param result Worker 执行结果
     */
    public void addWorkerResult(WorkerResult result) {
        if (result == null) {
            log.warn("Attempted to add null WorkerResult");
            return;
        }

        workerResults.add(result);
        if (result.getWorkerId() != null) {
            workerResultsMap.put(result.getWorkerId(), result);
        }

        log.debug("Added result from worker {}: success={}, output length={}",
                result.getWorkerId(), result.isSuccess(),
                result.getOutput() != null ? result.getOutput().length() : 0);
    }

    /**
     * 获取所有 Worker 结果
     *
     * @return Worker 结果列表
     */
    public List<WorkerResult> getWorkerResults() {
        return new ArrayList<>(workerResults);
    }

    /**
     * 检查所有 Workers 是否完成
     *
     * @return true 如果所有 Workers 都有结果
     */
    public boolean areAllWorkersComplete() {
        if (workers == null || workers.isEmpty()) {
            return true; // 没有 Workers 视为完成
        }

        // 检查每个 Worker 是否都有结果
        return workers.stream().allMatch(workerId ->
            workerResultsMap.containsKey(workerId)
        );
    }

    /**
     * 清空 Worker 结果
     */
    public void clearWorkerResults() {
        workerResults.clear();
        workerResultsMap.clear();
    }

    /**
     * 获取成功的 Worker 结果
     *
     * @return 成功的 Worker 结果列表
     */
    public List<WorkerResult> getSuccessfulWorkerResults() {
        return workerResults.stream()
                .filter(WorkerResult::isSuccess)
                .collect(Collectors.toList());
    }

    /**
     * 获取失败的 Worker 结果
     *
     * @return 失败的 Worker 结果列表
     */
    public List<WorkerResult> getFailedWorkerResults() {
        return workerResults.stream()
                .filter(result -> !result.isSuccess())
                .collect(Collectors.toList());
    }

    /**
     * 根据 Worker ID 获取结果
     *
     * @param workerId Worker ID
     * @return Worker 结果，如果不存在返回 null
     */
    public WorkerResult getWorkerResultById(String workerId) {
        if (workerId == null) {
            return null;
        }
        return workerResultsMap.get(workerId);
    }

    /**
     * 聚合所有 Worker 结果
     *
     * @return 聚合后的结果字符串
     */
    private String aggregateResults() {
        if (workerResults.isEmpty()) {
            return "No worker results to aggregate";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Aggregated results from ").append(workerResults.size()).append(" workers:\n");

        for (WorkerResult result : workerResults) {
            sb.append(String.format("- Worker %s: %s (success=%b, time=%dms)\n",
                    result.getWorkerId(),
                    result.getOutput() != null ? result.getOutput() : result.getErrorMessage(),
                    result.isSuccess(),
                    result.getExecutionTimeMs()));
        }

        return sb.toString();
    }
}
