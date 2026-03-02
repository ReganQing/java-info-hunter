package com.ron.javainfohunter.ai.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段4: Topic Exchange 配置
 *
 * 学习目标:
 * 1. 理解路由模式（Routing）
 * 2. 学习 Topic Exchange 的路由键匹配规则
 * 3. 掌握按类别分发消息的实现方式
 *
 * 应用场景:
 * - 不同类别的新闻需要不同的处理流程
 * - 日志按级别分发（ERROR 到报警系统，INFO 到归档系统）
 * - 多租户系统的消息路由
 *
 * 关键概念:
 * - Topic Exchange: 主题交换机，根据路由键模式匹配队列
 * - Routing Key: 路由键，由点号分隔的单词（如: tech.ai, finance.stock）
 * - 通配符:
 *   - *: 匹配一个单词
 *   - #: 匹配零个或多个单词
 */
@Configuration
public class TopicConfig {

    /**
     * 新闻交换机（Topic 类型）
     *
     * Topic: 根据路由键模式匹配队列
     */
    @Bean
    public TopicExchange newsExchange() {
        return new TopicExchange("news.exchange");
    }

    /**
     * 科技新闻队列
     * 路由键模式: tech.*
     * 匹配示例: tech.ai, tech.blockchain, tech.mobile
     */
    @Bean
    public Queue techQueue() {
        return QueueBuilder.durable("tech.queue")
                .build();
    }

    /**
     * 财经新闻队列
     * 路由键模式: finance.*
     * 匹配示例: finance.stock, finance.fund, finance.bond
     */
    @Bean
    public Queue financeQueue() {
        return QueueBuilder.durable("finance.queue")
                .build();
    }

    /**
     * 所有新闻归档队列
     * 路由键模式: *.*
     * 匹配所有两级路由键（用于归档所有新闻）
     */
    @Bean
    public Queue allNewsQueue() {
        return QueueBuilder.durable("all.news.queue")
                .build();
    }

    /**
     * 绑定科技新闻队列
     * 路由键模式: tech.*
     */
    @Bean
    public Binding techBinding() {
        return BindingBuilder.bind(techQueue())
                .to(newsExchange())
                .with("tech.*");
    }

    /**
     * 绑定财经新闻队列
     * 路由键模式: finance.*
     */
    @Bean
    public Binding financeBinding() {
        return BindingBuilder.bind(financeQueue())
                .to(newsExchange())
                .with("finance.*");
    }

    /**
     * 绑定所有新闻归档队列
     * 路由键模式: *.*
     */
    @Bean
    public Binding allNewsBinding() {
        return BindingBuilder.bind(allNewsQueue())
                .to(newsExchange())
                .with("*.*");
    }
}
