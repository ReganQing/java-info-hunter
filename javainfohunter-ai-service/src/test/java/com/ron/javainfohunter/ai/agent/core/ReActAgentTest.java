package com.ron.javainfohunter.ai.agent.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReActAgent 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class ReActAgentTest {

    private TestReActAgent agent;

    /**
     * 测试用 ReActAgent 实现
     */
    static class TestReActAgent extends ReActAgent {
        private boolean shouldAct = true;
        private String actResult = "Action completed";
        private int thinkCount = 0;
        private int actCount = 0;

        @Override
        public boolean think() {
            thinkCount++;
            if (thinkCount >= 3) {
                shouldAct = false;
            }
            return shouldAct;
        }

        @Override
        public String act() {
            actCount++;
            return actResult;
        }

        @Override
        public void cleanup() {
            // 清理资源
        }

        public int getThinkCount() {
            return thinkCount;
        }

        public int getActCount() {
            return actCount;
        }
    }

    @BeforeEach
    void setUp() {
        agent = new TestReActAgent();
        agent.setName("TestReActAgent");
        agent.setMaxSteps(10);
    }

    @Test
    void testThinkActCycle() {
        // 执行一步
        agent.setAgentState(AgentState.RUNNING); // 确保初始状态为 RUNNING
        String result = agent.step();

        assertEquals("Action completed", result);
        assertEquals(1, agent.getThinkCount());
        assertEquals(1, agent.getActCount());
        // 第一步后应该仍在运行或完成
        assertTrue(agent.getAgentState() == AgentState.RUNNING ||
                   agent.getAgentState() == AgentState.FINISHED);
    }

    @Test
    void testThinkReturnsFalse() {
        // 执行多步直到 think() 返回 false
        for (int i = 0; i < 5; i++) {
            agent.step();
        }

        // 应该在第 3 次思考后返回 false
        assertTrue(agent.getThinkCount() >= 3);
        assertEquals(AgentState.FINISHED, agent.getAgentState());
    }

    @Test
    void testActOnlyWhenThinkReturnsTrue() {
        agent.step(); // 第1步: think=true, act
        agent.step(); // 第2步: think=true, act
        agent.step(); // 第3步: think=false, no act

        assertEquals(3, agent.getThinkCount());
        assertEquals(2, agent.getActCount()); // act() 被调用 2 次
    }

    @Test
    void testExecutionWithException() {
        TestReActAgent errorAgent = new TestReActAgent() {
            @Override
            public boolean think() {
                throw new RuntimeException("Think error");
            }

            @Override
            public String act() {
                return "Should not reach here";
            }

            @Override
            public void cleanup() {
            }
        };

        errorAgent.setName("ErrorAgent");
        String result = errorAgent.step();

        assertTrue(result.contains("错误"));
        assertEquals(AgentState.ERROR, errorAgent.getAgentState());
    }
}
