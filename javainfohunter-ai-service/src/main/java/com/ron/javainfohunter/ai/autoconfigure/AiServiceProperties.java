package com.ron.javainfohunter.ai.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务配置属性
 * <p>
 * 通过 application.yml 或环境变量配置 AI 服务行为
 * </p>
 *
 * <p>配置示例：</p>
 * <pre>
 * javainfohunter:
 *   ai:
 *     enabled: true
 *     agent:
 *       max-steps: 10
 *       timeout: 300
 *     tool:
 *       auto-discovery: true
 * </pre>
 *
 * @author Ron
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "javainfohunter.ai")
public class AiServiceProperties {

    /**
     * 是否启用 AI 服务
     */
    private boolean enabled = true;

    /**
     * Agent 配置
     */
    private Agent agent = new Agent();

    /**
     * 工具配置
     */
    private Tool tool = new Tool();

    /**
     * Agent 配置
     */
    @Data
    public static class Agent {
        /**
         * 最大执行步数
         * <p>
         * 防止 Agent 无限循环执行
         * </p>
         */
        private int maxSteps = 10;

        /**
         * 执行超时时间（秒）
         */
        private int timeout = 300;
    }

    /**
     * 工具配置
     */
    @Data
    public static class Tool {
        /**
         * 是否启用工具自动发现
         * <p>
         * 自动扫描并注册所有 @Tool 注解的方法
         * </p>
         */
        private boolean autoDiscovery = true;
    }
}
