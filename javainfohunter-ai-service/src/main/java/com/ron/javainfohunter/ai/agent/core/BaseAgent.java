package com.ron.javainfohunter.ai.agent.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * 当前状态
     */
    private AgentState agentState = AgentState.IDLE;

    /**
     * ChatClient (Spring AI)
     */
    protected ChatClient chatClient;

    /**
     * 消息历史
     */
    protected List<Message> messages = new ArrayList<>();

    /**
     * 最大执行步数
     */
    private int maxSteps = 10;

    /**
     * 当前步数
     */
    private int currentStep = 0;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 取消标志
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 执行任务
     *
     * @param userPrompt 用户输入
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent is not idle, current state: " + agentState);
        }

        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt cannot be empty");
        }

        startTime = LocalDateTime.now();
        cancelled.set(false);
        agentState = AgentState.RUNNING;

        log.info("Agent {} starting execution - Prompt: {}", name,
                userPrompt.substring(0, Math.min(100, userPrompt.length())));

        // 添加用户消息
        messages.add(new UserMessage(userPrompt));

        // 执行步骤
        List<String> results = new ArrayList<>();

        try {
            while (currentStep < maxSteps && agentState == AgentState.RUNNING && !cancelled.get()) {
                if (cancelled.get()) {
                    log.info("Agent {} execution cancelled at step {}", name, currentStep);
                    agentState = AgentState.FINISHED;
                    results.add("Execution cancelled at step " + currentStep);
                    break;
                }

                log.debug("Agent {} executing step {}/{}", name, currentStep + 1, maxSteps);
                String stepResult = step();
                String result = "Step" + currentStep + ": " + stepResult;
                results.add(result);
                currentStep++;

                log.info("Agent {} step {} completed: {}", name, currentStep,
                        stepResult.substring(0, Math.min(100, stepResult.length())));

                if (currentStep >= maxSteps) {
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
                    name, getExecutionDurationMillis(), currentStep);
            return finalResult;

        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Agent {} error at step {}", name, currentStep, e);
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
                name, agentState, currentStep, maxSteps, getExecutionDurationMillis());
    }

    /**
     * 执行单个步骤（子类实现）
     *
     * @return 步骤结果
     */
    public abstract String step();

    /**
     * 清理资源（子类实现）
     */
    public abstract void cleanup();
}
