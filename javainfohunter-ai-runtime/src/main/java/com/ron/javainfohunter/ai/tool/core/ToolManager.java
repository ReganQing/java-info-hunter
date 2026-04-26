package com.ron.javainfohunter.ai.tool.core;

import com.ron.javainfohunter.ai.tool.annotation.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

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
        // Auto-discover and register all ToolCallback beans
        applicationContext.getBeansOfType(ToolCallback.class)
                .values()
                .forEach(tool -> {
                    toolRegistry.registerTool(tool);
                    log.info("Auto-registered ToolCallback: {}", tool.getClass().getSimpleName());
                });

        // Auto-discover @Tool-annotated methods on @Component beans
        applicationContext.getBeansWithAnnotation(Component.class)
                .values()
                .forEach(bean -> scanAndRegisterAnnotatedMethods(bean));
    }

    private void scanAndRegisterAnnotatedMethods(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                String toolName = toolAnnotation.name().isEmpty()
                        ? method.getName()
                        : toolAnnotation.name();
                log.info("Found @Tool method: {}#{} (name={})",
                        bean.getClass().getSimpleName(), method.getName(), toolName);
                // The @Tool methods are invoked directly by agents via reflection,
                // not through ToolCallback. Logging here for discovery visibility.
            }
        }
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
