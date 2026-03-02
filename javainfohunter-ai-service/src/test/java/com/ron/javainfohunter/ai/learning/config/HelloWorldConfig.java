package com.ron.javainfohunter.ai.learning.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段1: Hello World 配置
 *
 * 学习目标:
 * 1. 理解 RabbitMQ 的基础概念（Queue, Producer, Consumer）
 * 2. 学习如何创建持久化队列
 * 3. 掌握最简单的消息发送和接收
 *
 * 关键概念:
 * - Queue: 消息队列，用于存储消息
 * - durable: 持久化队列，RabbitMQ 重启后不丢失
 */
@Configuration
public class HelloWorldConfig {

    /**
     * 创建 Hello World 队列
     *
     * durable = true: 队列持久化，RabbitMQ 重启后队列仍然存在
     */
    @Bean
    public Queue helloQueue() {
        return QueueBuilder.durable("hello.queue")
                .build();
    }
}
