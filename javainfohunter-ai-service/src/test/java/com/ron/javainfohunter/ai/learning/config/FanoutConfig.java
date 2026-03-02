package com.ron.javainfohunter.ai.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段3: Fanout Exchange 配置
 *
 * 学习目标:
 * 1. 理解发布订阅模式（Pub/Sub）
 * 2. 学习 Fanout Exchange 的工作原理
 * 3. 掌握一对多广播的实现方式
 *
 * 应用场景:
 * - 原始内容需要同时发送给多个 Agent 处理
 * - 事件通知（一个事件触发多个处理流程）
 * - 日志分发（同时写入文件、数据库、远程服务器）
 *
 * 关键概念:
 * - Exchange: 交换机，接收生产者发送的消息并路由到队列
 * - Fanout Exchange: 广播模式，将消息发送到所有绑定的队列
 * - Binding: 绑定，将队列和交换机关联起来
 */
@Configuration
public class FanoutConfig {

    /**
     * 原始内容交换机（Fanout 类型）
     *
     * Fanout: 广播模式，忽略 routing key，将消息发送到所有绑定的队列
     */
    @Bean
    public FanoutExchange rawContentExchange() {
        return new FanoutExchange("raw.content.exchange");
    }

    /**
     * 分析队列 - 用于内容分析 Agent
     */
    @Bean
    public Queue analysisQueue() {
        return QueueBuilder.durable("analysis.queue")
                .build();
    }

    /**
     * 摘要队列 - 用于摘要生成 Agent
     */
    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable("summary.queue")
                .build();
    }

    /**
     * 分类队列 - 用于内容分类 Agent
     */
    @Bean
    public Queue classifyQueue() {
        return QueueBuilder.durable("classify.queue")
                .build();
    }

    /**
     * 将分析队列绑定到原始内容交换机
     * Fanout 模式不需要 routing key
     */
    @Bean
    public Binding analysisBinding() {
        return BindingBuilder.bind(analysisQueue())
                .to(rawContentExchange());
    }

    /**
     * 将摘要队列绑定到原始内容交换机
     */
    @Bean
    public Binding summaryBinding() {
        return BindingBuilder.bind(summaryQueue())
                .to(rawContentExchange());
    }

    /**
     * 将分类队列绑定到原始内容交换机
     */
    @Bean
    public Binding classifyBinding() {
        return BindingBuilder.bind(classifyQueue())
                .to(rawContentExchange());
    }
}
