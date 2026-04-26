package com.ron.javainfohunter.ai.tool.annotation;

import java.lang.annotation.*;

/**
 * 工具注解
 * <p>
 * 标记方法为 AI 可调用的工具
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * 工具描述
     */
    String description();

    /**
     * 工具名称（可选）
     * 默认使用方法名
     */
    String name() default "";
}
