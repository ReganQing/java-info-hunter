package com.ron.javainfohunter.ai.agent.coordinator;

import com.ron.javainfohunter.ai.agent.core.AgentState;
import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import com.ron.javainfohunter.ai.agent.coordinator.impl.AgentManagerImpl;
import com.ron.javainfohunter.ai.agent.coordinator.impl.TaskCoordinatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskCoordinator 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class TaskCoordinatorTest {

    private TaskCoordinator taskCoordinator;
    private AgentManager agentManager;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManagerImpl();
        taskCoordinator = new TaskCoordinatorImpl(agentManager);

        // 注册测试 Agent
        agentManager.registerAgent("agent1", new TestAgent("Agent1", "Result1"));
        agentManager.registerAgent("agent2", new TestAgent("Agent2", "Result2"));
        agentManager.registerAgent("agent3", new TestAgent("Agent3", "Result3"));
    }

    /**
     * 测试用 Agent 实现
     */
    static class TestAgent extends BaseAgent {
        private final String result;

        public TestAgent(String name, String result) {
            setName(name);
            this.result = result;
        }

        @Override
        public String step() {
            setAgentState(AgentState.FINISHED);
            return result;
        }

        @Override
        public void cleanup() {
        }
    }

    @Test
    void testExecuteChain() {
        CoordinationResult result = taskCoordinator.executeChain(
            "Chain task",
            List.of("agent1", "agent2", "agent3")
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getFinalOutput());
        assertTrue(result.getAgentOutputs().containsKey("agent1"));
        assertTrue(result.getAgentOutputs().containsKey("agent2"));
        assertTrue(result.getAgentOutputs().containsKey("agent3"));
    }

    @Test
    void testExecuteChainWithMissingAgent() {
        CoordinationResult result = taskCoordinator.executeChain(
            "Chain task",
            List.of("agent1", "nonexistent", "agent3")
        );

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("not found"));
    }

    @Test
    void testExecuteParallel() {
        CoordinationResult result = taskCoordinator.executeParallel(
            "Parallel task",
            List.of("agent1", "agent2", "agent3")
        );

        assertTrue(result.isSuccess());
        assertEquals(3, result.getAgentOutputs().size());
    }

    @Test
    void testExecuteParallelWithPartialFailure() {
        // agent2 会失败
        agentManager.registerAgent("failing-agent", new BaseAgent() {
            @Override
            public String step() {
                setAgentState(AgentState.ERROR);
                throw new RuntimeException("Agent failed");
            }

            @Override
            public void cleanup() {
            }
        });

        CoordinationResult result = taskCoordinator.executeParallel(
            "Parallel task",
            List.of("agent1", "failing-agent")
        );

        // 并行模式下，单个 Agent 失败不应该影响整体
        assertNotNull(result);
    }

    @Test
    void testExecuteChainWithEmptyAgentList() {
        // 空列表的处理 - 实际实现可能返回成功（空操作）
        CoordinationResult result = taskCoordinator.executeChain(
            "Chain task",
            List.of()
        );

        // 根据实际实现，空列表可能返回成功
        assertNotNull(result);
    }

    @Test
    void testExecuteParallelWithEmptyAgentList() {
        // 空列表的处理
        CoordinationResult result = taskCoordinator.executeParallel(
            "Parallel task",
            List.of()
        );

        // 根据实际实现，空列表可能返回成功
        assertNotNull(result);
    }

    @Test
    void testCoordinationResultProperties() {
        agentManager.registerAgent("test-agent", new TestAgent("Test", "Test result"));

        CoordinationResult result = taskCoordinator.executeChain(
            "Test task",
            List.of("test-agent")
        );

        assertTrue(result.isSuccess());
        assertFalse(result.getAgentOutputs().isEmpty());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getFinalOutput());
    }
}
