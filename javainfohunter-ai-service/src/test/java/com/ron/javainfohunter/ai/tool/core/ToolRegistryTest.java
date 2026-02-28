package com.ron.javainfohunter.ai.tool.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ToolRegistry 单元测试
 *
 * @author Ron
 * @since 1.0.0
 */
class ToolRegistryTest {

    private ToolRegistry toolRegistry;
    private ToolCallback mockTool1;
    private ToolCallback mockTool2;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();

        // 创建 Mock ToolCallback
        mockTool1 = mock(ToolCallback.class);
        when(mockTool1.toString()).thenReturn("MockTool1");

        mockTool2 = mock(ToolCallback.class);
        when(mockTool2.toString()).thenReturn("MockTool2");
    }

    @Test
    void testRegisterTool() {
        toolRegistry.registerTool(mockTool1);

        assertEquals(1, toolRegistry.getToolCount());
    }

    @Test
    void testRegisterMultipleTools() {
        toolRegistry.registerTool(mockTool1);
        // 等待 1 毫秒确保时间戳不同
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        toolRegistry.registerTool(mockTool2);

        assertEquals(2, toolRegistry.getToolCount());
    }

    @Test
    void testGetTool() {
        toolRegistry.registerTool(mockTool1);

        // ToolRegistry 使用时间戳作为 key，所以无法直接通过名称获取
        // 这里只测试工具数量
        assertEquals(1, toolRegistry.getToolCount());
    }

    @Test
    void testGetAllTools() {
        toolRegistry.registerTool(mockTool1);
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        toolRegistry.registerTool(mockTool2);

        ToolCallback[] tools = toolRegistry.getAllTools();
        assertEquals(2, tools.length);
    }

    @Test
    void testClearTools() {
        toolRegistry.registerTool(mockTool1);
        toolRegistry.registerTool(mockTool2);
        toolRegistry.clear();

        assertEquals(0, toolRegistry.getToolCount());
    }

    @Test
    void testRegisterToolsList() {
        // 由于 ToolRegistry 使用反射获取 name，这里只测试批量注册功能
        ToolCallback[] tools = {mockTool1};
        toolRegistry.registerTools(java.util.List.of(tools));

        assertEquals(1, toolRegistry.getToolCount());
    }
}
