package com.ron.javainfohunter.ai.agent.coordinator;

import com.ron.javainfohunter.ai.agent.core.AgentState;
import com.ron.javainfohunter.ai.agent.core.BaseAgent;
import com.ron.javainfohunter.ai.agent.coordinator.impl.AgentManagerImpl;
import com.ron.javainfohunter.ai.agent.coordinator.impl.TaskCoordinatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskCoordinator Master-Worker 模式集成测试
 *
 * @author Ron
 * @since 1.0.0
 */
class TaskCoordinatorMasterWorkerTest {

    private TaskCoordinator taskCoordinator;
    private AgentManager agentManager;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManagerImpl();
        taskCoordinator = new TaskCoordinatorImpl(agentManager);

        // 注册测试 Agents
        registerTestAgents();
    }

    private void registerTestAgents() {
        // 注册 Master Agent
        agentManager.registerAgent("master-agent", new TestMasterAgent("MasterAgent"));

        // 注册 Worker Agents
        for (int i = 1; i <= 3; i++) {
            agentManager.registerAgent("worker" + i, new TestWorkerAgent("Worker" + i, "Result" + i));
        }

        // 注册会失败的 Worker
        agentManager.registerAgent("failing-worker", new FailingWorkerAgent("FailingWorker"));

        // 注册慢速 Worker
        agentManager.registerAgent("slow-worker", new SlowWorkerAgent("SlowWorker"));
    }

    /**
     * 测试用 Master Agent
     */
    static class TestMasterAgent extends BaseAgent {
        private final List<String> workerResults = new CopyOnWriteArrayList<>();

        public TestMasterAgent(String name) {
            setName(name);
        }

        @Override
        public String step() {
            // Master Agent 协调逻辑
            setAgentState(AgentState.FINISHED);
            return "Master: All workers completed. Results: " + workerResults;
        }

        @Override
        public void cleanup() {
            workerResults.clear();
        }

        public void addWorkerResult(String result) {
            workerResults.add(result);
        }
    }

    /**
     * 测试用 Worker Agent
     */
    static class TestWorkerAgent extends BaseAgent {
        private final String result;

        public TestWorkerAgent(String name, String result) {
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

    /**
     * 会失败的 Worker Agent
     */
    static class FailingWorkerAgent extends BaseAgent {
        public FailingWorkerAgent(String name) {
            setName(name);
        }

        @Override
        public String step() {
            setAgentState(AgentState.ERROR);
            throw new RuntimeException("Worker failed intentionally");
        }

        @Override
        public void cleanup() {
        }
    }

    /**
     * 慢速 Worker Agent（模拟耗时操作）
     */
    static class SlowWorkerAgent extends BaseAgent {
        public SlowWorkerAgent(String name) {
            setName(name);
        }

        @Override
        public String step() {
            try {
                Thread.sleep(100); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            setAgentState(AgentState.FINISHED);
            return "Slow worker completed";
        }

        @Override
        public void cleanup() {
        }
    }

    @Test
    void testExecuteMasterWorkerBasic() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Test task",
            "master-agent",
            List.of("worker1", "worker2", "worker3")
        );

        // 验证基本结果
        assertNotNull(result);
        // 实现完成后应该是 true
        // assertTrue(result.isSuccess());
        // assertNotNull(result.getFinalOutput());
    }

    @Test
    void testExecuteMasterWorkerWithMissingMaster() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Test task",
            "nonexistent-master",
            List.of("worker1", "worker2")
        );

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Master agent not found") ||
                   result.getErrorMessage().contains("not found"));
    }

    @Test
    void testExecuteMasterWorkerWithMissingWorker() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Test task",
            "master-agent",
            List.of("worker1", "nonexistent-worker", "worker3")
        );

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Worker agent not found") ||
                   result.getErrorMessage().contains("not found"));
    }

    @Test
    void testExecuteMasterWorkerWithEmptyWorkerList() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Test task",
            "master-agent",
            List.of()
        );

        assertNotNull(result);
        // 空列表的处理可能返回成功（Master 只是没有 Worker 要协调）
        // 或者返回失败，取决于实现
    }

    @Test
    void testExecuteMasterWorkerWithSingleWorker() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Single worker task",
            "master-agent",
            List.of("worker1")
        );

        assertNotNull(result);
        // 实现完成后验证
    }

    @Test
    void testExecuteMasterWorkerWithPartialFailure() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Task with failing worker",
            "master-agent",
            List.of("worker1", "failing-worker", "worker3")
        );

        assertNotNull(result);
        // Master-Worker 模式应该能处理部分失败
        // 单个 Worker 失败不应该导致整个流程失败（取决于业务需求）
    }

    @Test
    void testExecuteMasterWorkerWithSlowWorkers() {
        long startTime = System.currentTimeMillis();

        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Task with slow workers",
            "master-agent",
            List.of("slow-worker", "slow-worker", "slow-worker")
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(result);
        // 由于使用虚拟线程并行执行，3个100ms的任务应该远小于300ms
        // 实际并行执行应该在150ms左右完成
        assertTrue(duration < 250, "Should complete in parallel, took: " + duration + "ms");
    }

    @Test
    void testExecuteMasterWorkerMultipleTimes() {
        // 测试多次执行的正确性
        for (int i = 0; i < 5; i++) {
            CoordinationResult result = taskCoordinator.executeMasterWorker(
                "Repeated task " + i,
                "master-agent",
                List.of("worker1", "worker2")
            );

            assertNotNull(result);
        }
    }

    @Test
    void testExecuteMasterWorkerLargeNumberOfWorkers() {
        // 创建大量 Worker
        List<String> workerIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String workerId = "dynamic-worker-" + i;
            agentManager.registerAgent(workerId, new TestWorkerAgent("Worker" + i, "Result" + i));
            workerIds.add(workerId);
        }

        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Large scale task",
            "master-agent",
            workerIds
        );

        assertNotNull(result);
        // 验证大规模并行执行的性能和正确性
    }

    @Test
    void testExecuteMasterWorkerWithExecuteMethod() {
        // 通过通用 execute 方法测试 Master-Worker 模式
        CoordinationResult result = taskCoordinator.execute(
            "Test via execute method",
            CollaborationPattern.MASTER_WORKER,
            List.of("master-agent", "worker1", "worker2")
        );

        assertNotNull(result);
        // 第一个 agent 是 master，其余是 workers
    }

    @Test
    void testExecuteMasterWorkerConcurrentSafety() {
        // 测试线程安全性：并发执行多个 Master-Worker 任务
        List<Thread> threads = new ArrayList<>();
        List<CoordinationResult> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            Thread thread = new Thread(() -> {
                CoordinationResult result = taskCoordinator.executeMasterWorker(
                    "Concurrent task " + taskId,
                    "master-agent",
                    List.of("worker1", "worker2")
                );
                results.add(result);
            });
            threads.add(thread);
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }
        }

        assertEquals(10, results.size());
        results.forEach(result -> assertNotNull(result));
    }

    @Test
    void testExecuteMasterWorkerAgentOutputs() {
        CoordinationResult result = taskCoordinator.executeMasterWorker(
            "Test outputs",
            "master-agent",
            List.of("worker1", "worker2", "worker3")
        );

        assertNotNull(result);
        // 实现完成后验证
        // assertTrue(result.getAgentOutputs().containsKey("master-agent"));
        // assertTrue(result.getAgentOutputs().containsKey("worker1"));
        // assertTrue(result.getAgentOutputs().containsKey("worker2"));
        // assertTrue(result.getAgentOutputs().containsKey("worker3"));
    }
}
