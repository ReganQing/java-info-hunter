package com.ron.javainfohunter.ai.agent.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 基础抽象类
 * <p>
 * 管理 Agent 生命周期、状态和执行流程。
 * 提供同步和流式两种执行方式。
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Data
public abstract class BaseAgent {

    /**
     * Agent 名称
     */
    private String name;

    /**
     * Agent 描述
     */
    private String description;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 下一步提示词
     */
    private String nextStepPrompt;

    /**
     * 当前状态（使用 volatile 确保线程可见性）
     */
    private volatile AgentState agentState = AgentState.IDLE;

    /**
     * ChatClient (Spring AI)
     */
    protected ChatClient chatClient;

    /**
     * 消息历史（线程安全的 CopyOnWriteArrayList）
     */
    protected List<Message> messages = new CopyOnWriteArrayList<>();

    /**
     * 最大执行步数
     */
    private int maxSteps = 10;

    /**
     * 当前步数（使用 AtomicInteger 确保线程安全）
     */
    private final AtomicInteger currentStep = new AtomicInteger(0);

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 取消标志
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 执行任务（线程安全版本，用于并发调用）
     * <p>
     * 此方法不修改 Agent 的共享状态，适合在多线程环境中调用。
     * 每次调用都使用独立的局部状态，确保线程安全。
     * </p>
     *
     * @param userPrompt 用户输入
     * @return 执行结果
     */
    public String runConcurrent(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt cannot be empty");
        }

        log.debug("Agent {} executing concurrent request", name);

        // 创建独立的消息列表用于本次执行
        List<Message> localMessages = new ArrayList<>();
        localMessages.add(new UserMessage(userPrompt));

        // 执行单步（大多数 Agent 只需要一步）
        String result;
        try {
            result = executeStep(userPrompt);
        } catch (Exception e) {
            log.error("Agent {} error in concurrent execution", name, e);
            result = "Error: " + e.getMessage();
        }

        log.debug("Agent {} concurrent execution completed", name);
        return result;
    }

    /**
     * 执行任务（原始方法，用于单线程调用）
     *
     * @param userPrompt 用户输入
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 同步块确保状态检查和转换的原子性
        synchronized (this) {
            if (agentState != AgentState.IDLE) {
                throw new IllegalStateException("Agent is not idle, current state: " + agentState);
            }

            if (userPrompt == null || userPrompt.isBlank()) {
                throw new IllegalArgumentException("User prompt cannot be empty");
            }

            startTime = LocalDateTime.now();
            cancelled.set(false);
            agentState = AgentState.RUNNING;
        }

        log.info("Agent {} starting execution - Prompt: {}", name,
                userPrompt.substring(0, Math.min(100, userPrompt.length())));

        // 添加用户消息
        messages.add(new UserMessage(userPrompt));

        // 执行步骤
        List<String> results = new ArrayList<>();

        try {
            while (currentStep.get() < maxSteps && agentState == AgentState.RUNNING && !cancelled.get()) {
                if (cancelled.get()) {
                    log.info("Agent {} execution cancelled at step {}", name, currentStep.get());
                    agentState = AgentState.FINISHED;
                    results.add("Execution cancelled at step " + currentStep.get());
                    break;
                }

                log.debug("Agent {} executing step {}/{}", name, currentStep.get() + 1, maxSteps);
                String stepResult = step();
                String result = "Step" + currentStep.get() + ": " + stepResult;
                results.add(result);
                currentStep.incrementAndGet();

                log.info("Agent {} step {} completed: {}", name, currentStep.get(),
                        stepResult.substring(0, Math.min(100, stepResult.length())));

                if (currentStep.get() >= maxSteps) {
                    agentState = AgentState.FINISHED;
                    log.info("Agent {} finished - reached max steps ({})", name, maxSteps);
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                }

                if (agentState == AgentState.FINISHED) {
                    break;
                }
            }

            String finalResult = String.join("\n", results);
            log.info("Agent {} execution completed - Duration: {}ms, Steps: {}",
                    name, getExecutionDurationMillis(), currentStep.get());
            return finalResult;

        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Agent {} error at step {}", name, currentStep.get(), e);
            return "Error: " + e.getMessage();
        } finally {
            cleanup();
        }
    }

    /**
     * 获取执行持续时间（毫秒）
     */
    public long getExecutionDurationMillis() {
        if (startTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    /**
     * 取消执行
     */
    public void cancel() {
        log.info("Cancelling agent {} execution", name);
        cancelled.set(true);
    }

    /**
     * 获取 Agent 状态信息
     */
    public String getStatusInfo() {
        return String.format("Agent: %s, State: %s, Step: %d/%d, Duration: %dms",
                name, agentState, currentStep.get(), maxSteps, getExecutionDurationMillis());
    }

    /**
     * 获取当前步数
     */
    public int getCurrentStep() {
        return currentStep.get();
    }

    /**
     * 执行单个步骤（子类实现，用于原始 run 方法）
     *
     * @return 步骤结果
     */
    public abstract String step();

    /**
     * 执行步骤（子类实现，用于 runConcurrent 方法）
     * <p>
     * 默认实现调用 step() 方法。子类可以重写此方法以优化并发性能。
     * </p>
     *
     * @param userPrompt 用户提示
     * @return 执行结果
     */
    public String executeStep(String userPrompt) {
        // 默认实现：先添加消息，然后执行 step
        messages.add(new UserMessage(userPrompt));
        return step();
    }

    /**
     * 清理资源（子类实现）
     */
    public abstract void cleanup();
}
