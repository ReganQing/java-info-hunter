package com.ron.javainfohunter.ai.agent.core;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct (Reasoning and Acting) 模式 Agent
 * <p>
 * 实现思考-行动循环模式，让 Agent 能够：
 * 1. 思考（think）：分析当前情况，决定是否需要行动
 * 2. 行动（act）：执行具体的操作
 * </p>
 *
 * <p>ReAct 模式参考论文: https://arxiv.org/abs/2210.03629</p>
 *
 * @author Ron
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    /**
     * 思考阶段：决定是否需要行动
     *
     * @return true 表示需要行动，false 表示可以结束
     */
    public abstract boolean think();

    /**
     * 行动阶段：执行具体操作
     *
     * @return 行动结果
     */
    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                agentState = AgentState.FINISHED;
                return "思考完成，无需行动";
            }
            return act();
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            return "步骤执行发生错误：" + e.getMessage();
        }
    }
}
