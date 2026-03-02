package com.ron.javainfohunter.ai.learning.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段1: Hello World 消费者
 *
 * 职责: 接收并打印简单的文本消息
 */
@Component
public class HelloWorldConsumer {

    /**
     * 监听 hello.queue 队列
     *
     * @RabbitListener: 自动监听队列，Spring AMQP 自动创建监听容器
     *
     * @param message 接收到的消息
     */
    @RabbitListener(queues = "hello.queue")
    public void receiveHello(String message) {
        System.out.println("📥 接收消息: " + message);
    }
}
