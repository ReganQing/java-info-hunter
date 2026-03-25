package com.ron.javainfohunter.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration;

/**
 * JavaInfoHunter REST API Application
 *
 * REST API module for the JavaInfoHunter distributed information collection system.
 * Provides endpoints for managing RSS sources, querying news, and monitoring agent executions.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootApplication(
    scanBasePackages = "com.ron.javainfohunter",
    exclude = {
        DashScopeChatAutoConfiguration.class,
        DashScopeAgentAutoConfiguration.class
    }
)
@EnableJpaRepositories(basePackages = "com.ron.javainfohunter.repository")
@EntityScan(basePackages = "com.ron.javainfohunter.entity")
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
