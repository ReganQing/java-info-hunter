package com.ron.javainfohunter.ai.agent.coordinator.pattern;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskDelegation 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class TaskDelegationTest {

    @Test
    void testBuilderCreatesValidObject() {
        Map<String, String> workerTasks = new HashMap<>();
        workerTasks.put("worker1", "Task 1");
        workerTasks.put("worker2", "Task 2");

        TaskDelegation delegation = TaskDelegation.builder()
                .taskId("task-123")
                .taskDescription("Test task")
                .workerTasks(workerTasks)
                .timeoutSeconds(30)
                .waitForAll(true)
                .build();

        assertNotNull(delegation);
        assertEquals("task-123", delegation.getTaskId());
        assertEquals("Test task", delegation.getTaskDescription());
        assertEquals(2, delegation.getWorkerTasks().size());
        assertEquals(30, delegation.getTimeoutSeconds());
        assertTrue(delegation.isWaitForAll());
    }

    @Test
    void testBuilderWithDefaults() {
        TaskDelegation delegation = TaskDelegation.builder()
                .taskId("task-456")
                .taskDescription("Simple task")
                .timeoutSeconds(0)
                .waitForAll(false)
                .build();

        assertNotNull(delegation);
        assertEquals("task-456", delegation.getTaskId());
        assertEquals("Simple task", delegation.getTaskDescription());
        assertNull(delegation.getWorkerTasks());
        assertEquals(0, delegation.getTimeoutSeconds());
        assertFalse(delegation.isWaitForAll());
    }

    @Test
    void testEmptyWorkerTasks() {
        TaskDelegation delegation = TaskDelegation.builder()
                .taskId("task-789")
                .taskDescription("No workers")
                .workerTasks(new HashMap<>())
                .build();

        assertNotNull(delegation);
        assertTrue(delegation.getWorkerTasks().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        TaskDelegation delegation = new TaskDelegation();
        delegation.setTaskId("task-999");
        delegation.setTaskDescription("Updated task");
        delegation.setTimeoutSeconds(60);
        delegation.setWaitForAll(false);

        assertEquals("task-999", delegation.getTaskId());
        assertEquals("Updated task", delegation.getTaskDescription());
        assertEquals(60, delegation.getTimeoutSeconds());
        assertFalse(delegation.isWaitForAll());
    }

    @Test
    void testNullTaskId() {
        TaskDelegation delegation = TaskDelegation.builder()
                .taskDescription("Task without ID")
                .build();

        assertNull(delegation.getTaskId());
        assertNotNull(delegation.getTaskDescription());
    }

    @Test
    void testNullTaskDescription() {
        TaskDelegation delegation = TaskDelegation.builder()
                .taskId("task-111")
                .build();

        assertNotNull(delegation.getTaskId());
        assertNull(delegation.getTaskDescription());
    }

    @Test
    void testNegativeTimeout() {
        TaskDelegation delegation = TaskDelegation.builder()
                .taskId("task-222")
                .timeoutSeconds(-10)
                .build();

        assertEquals(-10, delegation.getTimeoutSeconds());
        // 负值可能在实际使用时需要验证
    }

    @Test
    void testLargeNumberOfWorkers() {
        Map<String, String> workerTasks = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            workerTasks.put("worker" + i, "Task " + i);
        }

        TaskDelegation delegation = TaskDelegation.builder()
                .taskId("task-large")
                .workerTasks(workerTasks)
                .build();

        assertEquals(1000, delegation.getWorkerTasks().size());
    }
}
