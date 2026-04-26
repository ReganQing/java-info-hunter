package com.ron.javainfohunter.ai.agent.coordinator.impl;

import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 管理器实现
 * <p>
 * 使用 ConcurrentHashMap 存储 Agent，保证线程安全
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
public class AgentManagerImpl implements AgentManager {

    /**
     * Agent 存储（线程安全）
     */
    private final Map<String, BaseAgent> agents = new ConcurrentHashMap<>();

    @Override
    public void registerAgent(String name, BaseAgent agent) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Agent name cannot be null or empty");
        }
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }

        agents.put(name, agent);
        log.info("Registered agent: {} ({})", name, agent.getClass().getSimpleName());
    }

    @Override
    public void unregisterAgent(String name) {
        if (agents.remove(name) != null) {
            log.info("Unregistered agent: {}", name);
        }
    }

    @Override
    public Optional<BaseAgent> getAgent(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    @Override
    public List<String> getAgentNames() {
        return List.copyOf(agents.keySet());
    }

    @Override
    public List<BaseAgent> getAllAgents() {
        return List.copyOf(agents.values());
    }

    @Override
    public boolean isAgentRegistered(String name) {
        return agents.containsKey(name);
    }

    @Override
    public int getAgentCount() {
        return agents.size();
    }
}
