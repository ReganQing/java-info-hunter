package com.ron.javainfohunter.ai.agent.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseAgent 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class BaseAgentTest {

    private TestAgent agent;

    /**
     * 测试用 Agent 实现
     */
    static class TestAgent extends BaseAgent {
        private String stepResult = "Step completed";
        private int stepCount = 0;

        @Override
        public String step() {
            stepCount++;
            if (stepCount >= 3) {
                setAgentState(AgentState.FINISHED);
                return "Task completed";
            }
            return stepResult;
        }

        @Override
        public void cleanup() {
            // 清理资源
        }

        public int getStepCount() {
            return stepCount;
        }
    }

    @BeforeEach
    void setUp() {
        agent = new TestAgent();
        agent.setName("TestAgent");
        agent.setMaxSteps(5);
    }

    @Test
    void testInitialState() {
        assertEquals(AgentState.IDLE, agent.getAgentState());
        assertEquals("TestAgent", agent.getName());
        assertEquals(5, agent.getMaxSteps());
        assertEquals(0, agent.getCurrentStep()); // AtomicInteger 返回 int
    }

    @Test
    void testSuccessfulExecution() {
        String result = agent.run("Test prompt");

        assertTrue(result.contains("Task completed"));
        assertEquals(AgentState.IDLE, agent.getAgentState());
        assertEquals(0, agent.getCurrentStep());
    }

    @Test
    void testExecutionWithMaxSteps() {
        agent.setMaxSteps(2);
        agent.run("Test prompt");

        // After run() completes, state resets to IDLE
        assertEquals(AgentState.IDLE, agent.getAgentState());
    }

    @Test
    void testEmptyPromptThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            agent.run("");
        });

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testCancelExecution() {
        // 在另一个线程中运行 Agent
        new Thread(() -> agent.run("Long running task")).start();

        // 等待一小段时间后取消
        try {
            Thread.sleep(100);
            agent.cancel();
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
    }

    @Test
    void testStatusInfo() {
        String status = agent.getStatusInfo();

        assertTrue(status.contains("TestAgent"));
        assertTrue(status.contains("IDLE"));
    }

    @Test
    void run_shouldBeReinvocableAfterCompletion() {
        String result1 = agent.run("first task");
        assertNotNull(result1);

        // Second call should not throw IllegalStateException
        String result2 = agent.run("second task");
        assertNotNull(result2);
    }

    @Test
    void run_shouldResetStateAfterError() {
        FailingTestAgent failingAgent = new FailingTestAgent();
        failingAgent.setName("failing");
        String result = failingAgent.run("task");

        assertTrue(result.startsWith("Error:"));
        assertEquals(AgentState.IDLE, failingAgent.getAgentState());
    }

    static class FailingTestAgent extends BaseAgent {
        @Override
        public String step() {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public void cleanup() {
        }
    }
}
