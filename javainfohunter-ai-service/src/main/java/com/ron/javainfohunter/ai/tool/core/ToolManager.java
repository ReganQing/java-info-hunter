package com.ron.javainfohunter.ai.tool.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 工具管理器
 * <p>
 * 负责工具的自动发现和注册
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ToolManager implements ApplicationContextAware {

    private final ToolRegistry toolRegistry;

    public ToolManager(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 自动发现并注册所有 ToolCallback Bean
        applicationContext.getBeansOfType(ToolCallback.class)
                .values()
                .forEach(tool -> {
                    toolRegistry.registerTool(tool);
                    log.info("Auto-registered tool: {}", tool.getClass().getSimpleName());
                });
    }

    /**
     * 获取所有已注册的工具
     *
     * @return 工具数组
     */
    public ToolCallback[] getAllTools() {
        return toolRegistry.getAllTools();
    }
}
