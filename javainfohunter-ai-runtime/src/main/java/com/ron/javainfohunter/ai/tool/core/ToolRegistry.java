package com.ron.javainfohunter.ai.tool.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 * <p>
 * 管理所有可用的工具，提供注册、查询功能
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ToolRegistry {

    /**
     * 工具存储（线程安全）
     */
    private final Map<String, ToolCallback> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     *
     * @param tool 工具
     */
    public void registerTool(ToolCallback tool) {
        // ToolCallback 接口可能有 name() 方法（函数式接口）
        String toolName = "tool-" + System.currentTimeMillis();
        try {
            // 尝试通过反射获取 name
            java.lang.reflect.Method getNameMethod = tool.getClass().getMethod("getName");
            if (getNameMethod != null) {
                Object name = getNameMethod.invoke(tool);
                if (name != null) {
                    toolName = name.toString();
                }
            }
        } catch (Exception e) {
            // 如果无法获取 name，使用默认名称
            log.warn("Cannot get tool name, using default: {}", toolName);
        }
        tools.put(toolName, tool);
    }

    /**
     * 批量注册工具
     *
     * @param tools 工具列表
     */
    public void registerTools(List<ToolCallback> tools) {
        tools.forEach(tool -> registerTool(tool));
    }

    /**
     * 获取指定工具
     *
     * @param name 工具名称
     * @return 工具（如果存在）
     */
    public ToolCallback getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有工具
     *
     * @return 工具数组
     */
    public ToolCallback[] getAllTools() {
        return tools.values().toArray(new ToolCallback[0]);
    }

    /**
     * 获取工具数量
     *
     * @return 工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
    }
}
