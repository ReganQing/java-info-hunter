package com.ron.javainfohunter.ai.learning.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 阶段1: Hello World 生产者
 *
 * 职责: 发送简单的文本消息到队列
 */
@Service
public class HelloWorldProducer {

    private final RabbitTemplate rabbitTemplate;

    public HelloWorldProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送 Hello World 消息
     *
     * @param message 消息内容
     */
    public void sendHello(String message) {
        rabbitTemplate.convertAndSend("hello.queue", message);
        System.out.println("📤 发送消息: " + message);
    }
}
