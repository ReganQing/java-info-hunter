package com.ron.javainfohunter.ai.tool.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for CoordinatorTools JSON parsing
 * <p>
 * Tests to ensure JSON parsing is safe from injection attacks
 * and handles edge cases properly.
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@DisplayName("CoordinatorTools Security Tests")
class CoordinatorToolsSecurityTest {

    private CoordinatorTools tools;

    @BeforeEach
    void setUp() {
        tools = new CoordinatorTools();
    }

    // ==================== parseWorkerTasksJson Tests ====================

    @Test
    @DisplayName("parseWorkerTasksJson - Valid JSON should parse correctly")
    void parseWorkerTasksJson_ValidJson_ReturnsMap() {
        String json = "{\"worker1\":\"task1\",\"worker2\":\"task2\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(json);

        assertEquals(2, result.size());
        assertEquals("task1", result.get("worker1"));
        assertEquals("task2", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Empty JSON should return empty map")
    void parseWorkerTasksJson_EmptyJson_ReturnsEmptyMap() {
        String json = "{}";
        Map<String, String> result = tools.parseWorkerTasksJson(json);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Null input should return empty map")
    void parseWorkerTasksJson_NullInput_ReturnsEmptyMap() {
        Map<String, String> result = tools.parseWorkerTasksJson(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Empty string should return empty map")
    void parseWorkerTasksJson_EmptyString_ReturnsEmptyMap() {
        Map<String, String> result = tools.parseWorkerTasksJson("");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Invalid JSON should throw exception")
    void parseWorkerTasksJson_InvalidJson_ThrowsException() {
        String invalidJson = "{invalid json}";

        assertThrows(IllegalArgumentException.class, () -> tools.parseWorkerTasksJson(invalidJson));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Injection attempt with colons should be safe")
    void parseWorkerTasksJson_InjectionAttemptWithColons_SafelyHandled() {
        // Task descriptions containing colons should parse correctly
        String malicious = "{\"worker1\":\"task:with:colons\",\"worker2\":\"another:task\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(malicious);

        assertEquals(2, result.size());
        assertEquals("task:with:colons", result.get("worker1"));
        assertEquals("another:task", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Injection attempt with commas should be safe")
    void parseWorkerTasksJson_InjectionAttemptWithCommas_SafelyHandled() {
        // Task descriptions containing commas should parse correctly
        String malicious = "{\"worker1\":\"task,with,commas\",\"worker2\":\"task2\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(malicious);

        assertEquals(2, result.size());
        assertEquals("task,with,commas", result.get("worker1"));
        assertEquals("task2", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Injection attempt with braces should be safe")
    void parseWorkerTasksJson_InjectionAttemptWithBraces_SafelyHandled() {
        // Task descriptions containing braces should parse correctly
        String malicious = "{\"worker1\":\"task{with}braces\",\"worker2\":\"task2\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(malicious);

        assertEquals(2, result.size());
        assertEquals("task{with}braces", result.get("worker1"));
        assertEquals("task2", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Injection attempt with quotes should be safe")
    void parseWorkerTasksJson_InjectionAttemptWithQuotes_SafelyHandled() {
        // Task descriptions containing quotes should parse correctly
        String malicious = "{\"worker1\":\"task\\\"with\\\"quotes\",\"worker2\":\"task2\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(malicious);

        assertEquals(2, result.size());
        assertEquals("task\"with\"quotes", result.get("worker1"));
        assertEquals("task2", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Complex nested values should be safe")
    void parseWorkerTasksJson_ComplexNestedValues_SafelyHandled() {
        // Complex values with multiple special characters
        String complex = "{\"worker1\":\"Task: A, B, C {status}\",\"worker2\":\"http://example.com?param=value\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(complex);

        assertEquals(2, result.size());
        assertEquals("Task: A, B, C {status}", result.get("worker1"));
        assertEquals("http://example.com?param=value", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - JSON injection attempt should fail safely")
    void parseWorkerTasksJson_JsonInjectionAttempt_FailsSafely() {
        // Attempt to inject additional JSON key-value pairs
        String injection = "{\"worker1\":\"task1\",\"worker2\":\"task2\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(injection);

        assertEquals(2, result.size());
        assertFalse(result.containsKey("injected"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Unicode characters should be preserved")
    void parseWorkerTasksJson_UnicodeCharacters_Preserved() {
        String unicode = "{\"worker1\":\"任务描述\",\"worker2\":\"태스크\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(unicode);

        assertEquals(2, result.size());
        assertEquals("任务描述", result.get("worker1"));
        assertEquals("태스크", result.get("worker2"));
    }

    @Test
    @DisplayName("parseWorkerTasksJson - Special JSON characters should be escaped")
    void parseWorkerTasksJson_SpecialJsonCharacters_Escaped() {
        String specialChars = "{\"worker1\":\"line1\\nline2\",\"worker2\":\"tab\\there\"}";
        Map<String, String> result = tools.parseWorkerTasksJson(specialChars);

        assertEquals(2, result.size());
        assertEquals("line1\nline2", result.get("worker1"));
        assertEquals("tab\there", result.get("worker2"));
    }

    // ==================== parseWorkerIdsJson Tests ====================

    @Test
    @DisplayName("parseWorkerIdsJson - Valid JSON should parse correctly")
    void parseWorkerIdsJson_ValidJson_ReturnsList() {
        String json = "[\"worker1\",\"worker2\",\"worker3\"]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertEquals(3, result.size());
        assertTrue(result.contains("worker1"));
        assertTrue(result.contains("worker2"));
        assertTrue(result.contains("worker3"));
    }

    @Test
    @DisplayName("parseWorkerIdsJson - Empty array should return empty list")
    void parseWorkerIdsJson_EmptyArray_ReturnsEmptyList() {
        String json = "[]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseWorkerIdsJson - Null input should return empty list")
    void parseWorkerIdsJson_NullInput_ReturnsEmptyList() {
        List<String> result = tools.parseWorkerIdsJson(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseWorkerIdsJson - Empty string should return empty list")
    void parseWorkerIdsJson_EmptyString_ReturnsEmptyList() {
        List<String> result = tools.parseWorkerIdsJson("");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseWorkerIdsJson - Invalid JSON should throw exception")
    void parseWorkerIdsJson_InvalidJson_ThrowsException() {
        String invalidJson = "[invalid json]";

        assertThrows(IllegalArgumentException.class, () -> tools.parseWorkerIdsJson(invalidJson));
    }

    @Test
    @DisplayName("parseWorkerIdsJson - IDs with commas should be safe")
    void parseWorkerIdsJson_IdsWithCommas_SafelyHandled() {
        // IDs containing commas should parse correctly
        String json = "[\"worker,1\",\"worker,2\",\"worker,3\"]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertEquals(3, result.size());
        assertTrue(result.contains("worker,1"));
        assertTrue(result.contains("worker,2"));
        assertTrue(result.contains("worker,3"));
    }

    @Test
    @DisplayName("parseWorkerIdsJson - IDs with brackets should be safe")
    void parseWorkerIdsJson_IdsWithBrackets_SafelyHandled() {
        // IDs containing brackets should parse correctly
        String json = "[\"worker[1]\",\"worker[2]\",\"worker[3]\"]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertEquals(3, result.size());
        assertTrue(result.contains("worker[1]"));
        assertTrue(result.contains("worker[2]"));
        assertTrue(result.contains("worker[3]"));
    }

    @Test
    @DisplayName("parseWorkerIdsJson - IDs with quotes should be safe")
    void parseWorkerIdsJson_IdsWithQuotes_SafelyHandled() {
        // IDs containing escaped quotes should parse correctly
        String json = "[\"worker\\\"1\\\"\",\"worker\\\"2\\\"\"]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertEquals(2, result.size());
        assertTrue(result.contains("worker\"1\""));
        assertTrue(result.contains("worker\"2\""));
    }

    @Test
    @DisplayName("parseWorkerIdsJson - Unicode characters should be preserved")
    void parseWorkerIdsJson_UnicodeCharacters_Preserved() {
        String json = "[\"작업자1\",\"작업자2\",\"worker3\"]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertEquals(3, result.size());
        assertTrue(result.contains("작업자1"));
        assertTrue(result.contains("작업자2"));
        assertTrue(result.contains("worker3"));
    }

    @Test
    @DisplayName("parseWorkerIdsJson - Special characters in IDs should be preserved")
    void parseWorkerIdsJson_SpecialCharacters_Preserved() {
        String json = "[\"worker-1\",\"worker_2\",\"worker.3\"]";
        List<String> result = tools.parseWorkerIdsJson(json);

        assertEquals(3, result.size());
        assertTrue(result.contains("worker-1"));
        assertTrue(result.contains("worker_2"));
        assertTrue(result.contains("worker.3"));
    }
}
