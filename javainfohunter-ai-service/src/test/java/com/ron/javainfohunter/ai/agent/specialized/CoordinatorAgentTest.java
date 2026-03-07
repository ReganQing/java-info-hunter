package com.ron.javainfohunter.ai.agent.specialized;

import com.ron.javainfohunter.ai.agent.coordinator.pattern.TaskDelegation;
import com.ron.javainfohunter.ai.agent.coordinator.pattern.WorkerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CoordinatorAgent 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class CoordinatorAgentTest {

    private CoordinatorAgent coordinatorAgent;

    @BeforeEach
    void setUp() {
        coordinatorAgent = new CoordinatorAgent("TestCoordinator");
    }

    @Test
    void testConstructorWithName() {
        CoordinatorAgent agent = new CoordinatorAgent("MyCoordinator");
        assertEquals("MyCoordinator", agent.getName());
    }

    @Test
    void testSetWorkers() {
        List<String> workers = List.of("worker1", "worker2", "worker3");
        coordinatorAgent.setWorkers(workers);

        assertNotNull(coordinatorAgent.getWorkers());
        assertEquals(3, coordinatorAgent.getWorkers().size());
        assertTrue(coordinatorAgent.getWorkers().contains("worker1"));
        assertTrue(coordinatorAgent.getWorkers().contains("worker2"));
        assertTrue(coordinatorAgent.getWorkers().contains("worker3"));
    }

    @Test
    void testSetWorkersWithEmptyList() {
        coordinatorAgent.setWorkers(List.of());
        assertNotNull(coordinatorAgent.getWorkers());
        assertTrue(coordinatorAgent.getWorkers().isEmpty());
    }

    @Test
    void testSetWorkersWithNull() {
        coordinatorAgent.setWorkers(null);
        assertNull(coordinatorAgent.getWorkers());
    }

    @Test
    void testAddWorkerResult() {
        WorkerResult result1 = WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .output("Result 1")
                .executionTimeMs(100)
                .build();

        WorkerResult result2 = WorkerResult.builder()
                .workerId("worker2")
                .success(true)
                .output("Result 2")
                .executionTimeMs(150)
                .build();

        coordinatorAgent.addWorkerResult(result1);
        coordinatorAgent.addWorkerResult(result2);

        assertEquals(2, coordinatorAgent.getWorkerResults().size());
    }

    @Test
    void testAddWorkerResultWithNull() {
        coordinatorAgent.addWorkerResult(null);
        // 应该优雅处理 null，不抛出异常
        assertNotNull(coordinatorAgent.getWorkerResults());
        // 可能 size 为 0 或 1，取决于实现
    }

    @Test
    void testGetWorkerResults() {
        List<WorkerResult> results = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            WorkerResult result = WorkerResult.builder()
                    .workerId("worker" + i)
                    .success(true)
                    .output("Result " + i)
                    .executionTimeMs(100 * i)
                    .build();
            results.add(result);
            coordinatorAgent.addWorkerResult(result);
        }

        List<WorkerResult> retrievedResults = coordinatorAgent.getWorkerResults();
        assertEquals(5, retrievedResults.size());

        // 验证顺序保持
        assertEquals("worker1", retrievedResults.get(0).getWorkerId());
        assertEquals("worker5", retrievedResults.get(4).getWorkerId());
    }

    @Test
    void testAreAllWorkersCompleteWithNoWorkers() {
        coordinatorAgent.setWorkers(List.of());
        assertTrue(coordinatorAgent.areAllWorkersComplete());
    }

    @Test
    void testAreAllWorkersCompleteWithNoResults() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2", "worker3"));
        assertFalse(coordinatorAgent.areAllWorkersComplete());
    }

    @Test
    void testAreAllWorkersCompletePartial() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2", "worker3"));

        // 只添加部分结果
        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        assertFalse(coordinatorAgent.areAllWorkersComplete());
    }

    @Test
    void testAreAllWorkersCompleteAllSuccess() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2", "worker3"));

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(true)
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker3")
                .success(true)
                .build());

        assertTrue(coordinatorAgent.areAllWorkersComplete());
    }

    @Test
    void testAreAllWorkersCompleteWithFailures() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2", "worker3"));

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(false)
                .errorMessage("Failed")
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker3")
                .success(true)
                .build());

        // 即使有失败，也应该算作完成（有结果）
        assertTrue(coordinatorAgent.areAllWorkersComplete());
    }

    @Test
    void testAreAllWorkersCompleteWithExtraResults() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2"));

        // 添加超过 workers 数量的结果
        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(true)
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker3")
                .success(true)
                .build());

        assertTrue(coordinatorAgent.areAllWorkersComplete());
    }

    @Test
    void testClearWorkerResults() {
        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(true)
                .build());

        assertEquals(2, coordinatorAgent.getWorkerResults().size());

        // 清空结果
        coordinatorAgent.clearWorkerResults();

        assertTrue(coordinatorAgent.getWorkerResults().isEmpty());
    }

    @Test
    void testThreadSafetyOfWorkerResults() throws InterruptedException {
        // 测试并发添加结果的线程安全性
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                WorkerResult result = WorkerResult.builder()
                        .workerId("worker" + index)
                        .success(true)
                        .output("Result " + index)
                        .build();
                coordinatorAgent.addWorkerResult(result);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(10, coordinatorAgent.getWorkerResults().size());
    }

    @Test
    void testGetSuccessfulWorkerResults() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2", "worker3"));

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .output("Success 1")
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(false)
                .errorMessage("Failed")
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker3")
                .success(true)
                .output("Success 3")
                .build());

        List<WorkerResult> successfulResults = coordinatorAgent.getSuccessfulWorkerResults();
        assertEquals(2, successfulResults.size());
        assertTrue(successfulResults.stream().allMatch(WorkerResult::isSuccess));
    }

    @Test
    void testGetFailedWorkerResults() {
        coordinatorAgent.setWorkers(List.of("worker1", "worker2", "worker3"));

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .output("Success 1")
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(false)
                .errorMessage("Failed 2")
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker3")
                .success(false)
                .errorMessage("Failed 3")
                .build());

        List<WorkerResult> failedResults = coordinatorAgent.getFailedWorkerResults();
        assertEquals(2, failedResults.size());
        assertTrue(failedResults.stream().allMatch(r -> !r.isSuccess()));
    }

    @Test
    void testGetWorkerResultById() {
        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .output("Result 1")
                .build());

        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker2")
                .success(true)
                .output("Result 2")
                .build());

        WorkerResult result = coordinatorAgent.getWorkerResultById("worker1");
        assertNotNull(result);
        assertEquals("worker1", result.getWorkerId());
        assertEquals("Result 1", result.getOutput());
    }

    @Test
    void testGetWorkerResultByIdNotFound() {
        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        WorkerResult result = coordinatorAgent.getWorkerResultById("worker999");
        assertNull(result);
    }

    @Test
    void testGetWorkerResultByIdWithNull() {
        coordinatorAgent.addWorkerResult(WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .build());

        WorkerResult result = coordinatorAgent.getWorkerResultById(null);
        assertNull(result);
    }
}
