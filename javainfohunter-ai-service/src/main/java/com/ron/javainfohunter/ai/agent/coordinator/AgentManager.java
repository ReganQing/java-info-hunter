package com.ron.javainfohunter.ai.agent.coordinator;

import com.ron.javainfohunter.ai.agent.core.BaseAgent;

import java.util.List;
import java.util.Optional;

/**
 * Agent 管理器接口
 * <p>
 * 负责 Agent 的注册、注销、查询等生命周期管理
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
public interface AgentManager {

    /**
     * 注册一个 Agent
     *
     * @param name  Agent 名称（唯一标识）
     * @param agent Agent 实例
     */
    void registerAgent(String name, BaseAgent agent);

    /**
     * 注销一个 Agent
     *
     * @param name Agent 名称
     */
    void unregisterAgent(String name);

    /**
     * 获取指定 Agent
     *
     * @param name Agent 名称
     * @return Agent 实例（如果存在）
     */
    Optional<BaseAgent> getAgent(String name);

    /**
     * 获取所有已注册的 Agent 名称
     *
     * @return Agent 名称列表
     */
    List<String> getAgentNames();

    /**
     * 获取所有已注册的 Agent
     *
     * @return Agent 列表
     */
    List<BaseAgent> getAllAgents();

    /**
     * 检查 Agent 是否已注册
     *
     * @param name Agent 名称
     * @return true 如果已注册
     */
    boolean isAgentRegistered(String name);

    /**
     * 获取已注册的 Agent 数量
     *
     * @return Agent 数量
     */
    int getAgentCount();
}
