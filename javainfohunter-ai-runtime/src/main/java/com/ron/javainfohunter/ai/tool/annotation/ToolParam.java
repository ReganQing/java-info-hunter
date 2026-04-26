package com.ron.javainfohunter.ai.tool.annotation;

import java.lang.annotation.*;

/**
 * 标记工具方法的参数
 * <p>
 * 用于描述工具参数的用途和格式
 * </p>
 *
 * <p>示例：</p>
 * <pre>
 * &#64;Tool(description = "发送 HTTP 请求")
 * public String httpRequest(
 *     &#64;ToolParam("URL 地址") String url,
 *     &#64;ToolParam("请求方法 (GET/POST)") String method
 * ) {
 *     // 工具实现
 * }
 * </pre>
 *
 * @author Ron
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

    /**
     * 参数描述
     * <p>
     * 将被发送给 LLM，帮助它理解如何填写此参数
     * </p>
     */
    String value() default "";
}
