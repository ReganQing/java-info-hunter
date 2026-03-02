package com.ron.javainfohunter.ai.learning.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段2: Work Queue 配置
 *
 * 学习目标:
 * 1. 理解 Work Queue 模式（多个消费者竞争消费）
 * 2. 学习如何实现负载均衡
 * 3. 掌握并发消费者的配置
 *
 * 应用场景:
 * - 爬虫任务分发
 * - 耗时任务并行处理
 * - 生产者和消费者速率不同步
 *
 * 关键概念:
 * - Work Queue: 多个消费者从同一个队列消费消息
 * - 轮询分发: RabbitMQ 默认使用轮询方式分发消息
 * - 并发控制: 通过配置文件控制消费者数量
 */
@Configuration
public class WorkQueueConfig {

    /**
     * URL 爬取队列
     *
     * 用于存储待爬取的 URL，多个爬虫工作线程会从该队列获取任务
     */
    @Bean
    public Queue urlQueue() {
        return QueueBuilder.durable("url.queue")
                .build();
    }
}
