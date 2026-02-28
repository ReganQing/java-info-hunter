package com.ron.javainfohunter.ai.agent.coordinator;

import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import com.ron.javainfohunter.ai.agent.coordinator.impl.AgentManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentManager 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class AgentManagerTest {

    private AgentManager agentManager;
    private BaseAgent testAgent1;
    private BaseAgent testAgent2;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManagerImpl();

        // 创建测试 Agent
        testAgent1 = new BaseAgent() {
            @Override
            public String step() {
                return "Agent1 step";
            }

            @Override
            public void cleanup() {
            }
        };
        testAgent1.setName("Agent1");

        testAgent2 = new BaseAgent() {
            @Override
            public String step() {
                return "Agent2 step";
            }

            @Override
            public void cleanup() {
            }
        };
        testAgent2.setName("Agent2");
    }

    @Test
    void testRegisterAgent() {
        agentManager.registerAgent("agent1", testAgent1);

        assertTrue(agentManager.getAgent("agent1").isPresent());
        assertEquals(testAgent1, agentManager.getAgent("agent1").get());
    }

    @Test
    void testRegisterMultipleAgents() {
        agentManager.registerAgent("agent1", testAgent1);
        agentManager.registerAgent("agent2", testAgent2);

        assertEquals(2, agentManager.getAgentNames().size());
        assertTrue(agentManager.getAgentNames().contains("agent1"));
        assertTrue(agentManager.getAgentNames().contains("agent2"));
    }

    @Test
    void testUnregisterAgent() {
        agentManager.registerAgent("agent1", testAgent1);
        agentManager.unregisterAgent("agent1");

        assertFalse(agentManager.getAgent("agent1").isPresent());
        assertEquals(0, agentManager.getAgentNames().size());
    }

    @Test
    void testGetNonExistentAgent() {
        assertFalse(agentManager.getAgent("nonexistent").isPresent());
    }

    @Test
    void testGetAllAgentNames() {
        agentManager.registerAgent("agent1", testAgent1);
        agentManager.registerAgent("agent2", testAgent2);

        var names = agentManager.getAgentNames();
        assertEquals(2, names.size());
    }

    @Test
    void testGetAgentCount() {
        assertEquals(0, agentManager.getAgentCount());

        agentManager.registerAgent("agent1", testAgent1);
        assertEquals(1, agentManager.getAgentCount());

        agentManager.registerAgent("agent2", testAgent2);
        assertEquals(2, agentManager.getAgentCount());
    }

    @Test
    void testRegisterDuplicateAgent() {
        agentManager.registerAgent("agent1", testAgent1);
        agentManager.registerAgent("agent1", testAgent2);

        // 应该覆盖旧的 Agent
        assertEquals(testAgent2, agentManager.getAgent("agent1").get());
        assertEquals(1, agentManager.getAgentCount());
    }
}
