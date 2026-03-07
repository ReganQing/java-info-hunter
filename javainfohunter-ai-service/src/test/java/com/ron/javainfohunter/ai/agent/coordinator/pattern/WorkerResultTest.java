package com.ron.javainfohunter.ai.agent.coordinator.pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkerResult 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class WorkerResultTest {

    @Test
    void testBuilderCreatesValidObject() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker1")
                .success(true)
                .output("Task completed successfully")
                .errorMessage(null)
                .executionTimeMs(100)
                .build();

        assertNotNull(result);
        assertEquals("worker1", result.getWorkerId());
        assertTrue(result.isSuccess());
        assertEquals("Task completed successfully", result.getOutput());
        assertNull(result.getErrorMessage());
        assertEquals(100, result.getExecutionTimeMs());
    }

    @Test
    void testBuilderForFailedResult() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker2")
                .success(false)
                .output(null)
                .errorMessage("Task failed: timeout")
                .executionTimeMs(5000)
                .build();

        assertNotNull(result);
        assertEquals("worker2", result.getWorkerId());
        assertFalse(result.isSuccess());
        assertNull(result.getOutput());
        assertEquals("Task failed: timeout", result.getErrorMessage());
        assertEquals(5000, result.getExecutionTimeMs());
    }

    @Test
    void testBuilderWithDefaults() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker3")
                .success(false)
                .executionTimeMs(0)
                .build();

        assertNotNull(result);
        assertEquals("worker3", result.getWorkerId());
        assertFalse(result.isSuccess()); // default should be false
        assertNull(result.getOutput());
        assertNull(result.getErrorMessage());
        assertEquals(0, result.getExecutionTimeMs());
    }

    @Test
    void testGettersAndSetters() {
        WorkerResult result = new WorkerResult();
        result.setWorkerId("worker4");
        result.setSuccess(true);
        result.setOutput("Output");
        result.setErrorMessage("Error");
        result.setExecutionTimeMs(200);

        assertEquals("worker4", result.getWorkerId());
        assertTrue(result.isSuccess());
        assertEquals("Output", result.getOutput());
        assertEquals("Error", result.getErrorMessage());
        assertEquals(200, result.getExecutionTimeMs());
    }

    @Test
    void testNullWorkerId() {
        WorkerResult result = WorkerResult.builder()
                .success(true)
                .output("Result")
                .build();

        assertNull(result.getWorkerId());
        assertTrue(result.isSuccess());
    }

    @Test
    void testZeroExecutionTime() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker5")
                .success(true)
                .executionTimeMs(0)
                .build();

        assertEquals(0, result.getExecutionTimeMs());
        assertTrue(result.isSuccess());
    }

    @Test
    void testNegativeExecutionTime() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker6")
                .executionTimeMs(-100)
                .build();

        assertEquals(-100, result.getExecutionTimeMs());
        // 负值在实际使用时可能需要验证
    }

    @Test
    void testSuccessWithErrorMessage() {
        // 不应该同时有 success=true 和 errorMessage
        WorkerResult result = WorkerResult.builder()
                .workerId("worker7")
                .success(true)
                .errorMessage("Warning message")
                .build();

        assertTrue(result.isSuccess());
        assertEquals("Warning message", result.getErrorMessage());
        // 这种情况可能需要业务逻辑验证
    }

    @Test
    void testFailureWithOutput() {
        // 失败时可能有部分输出
        WorkerResult result = WorkerResult.builder()
                .workerId("worker8")
                .success(false)
                .output("Partial output")
                .errorMessage("Failed to complete")
                .build();

        assertFalse(result.isSuccess());
        assertEquals("Partial output", result.getOutput());
        assertEquals("Failed to complete", result.getErrorMessage());
    }

    @Test
    void testLargeExecutionTime() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker9")
                .executionTimeMs(Long.MAX_VALUE)
                .build();

        assertEquals(Long.MAX_VALUE, result.getExecutionTimeMs());
    }

    @Test
    void testEmptyOutput() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker10")
                .success(true)
                .output("")
                .build();

        assertTrue(result.isSuccess());
        assertEquals("", result.getOutput());
        assertTrue(result.getOutput().isEmpty());
    }

    @Test
    void testEmptyErrorMessage() {
        WorkerResult result = WorkerResult.builder()
                .workerId("worker11")
                .success(false)
                .errorMessage("")
                .build();

        assertFalse(result.isSuccess());
        assertEquals("", result.getErrorMessage());
        assertTrue(result.getErrorMessage().isEmpty());
    }
}
