package com.ron.javainfohunter.ai.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * RabbitMQ 学习测试应用
 *
 * 用于运行 RabbitMQ 渐进式学习测试
 *
 * 注意：只启用 RabbitMQ 相关功能，排除所有 AI 服务和数据库配置
 */
@SpringBootApplication(
    exclude = {
        // 排除所有数据库相关自动配置
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
    }
)
@ComponentScan(basePackages = "com.ron.javainfohunter.ai.learning")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
