package com.ron.javainfohunter.ai.learning.stage;

import com.ron.javainfohunter.ai.learning.producer.HelloWorldProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 阶段1: Hello World 测试
 *
 * 学习目标:
 * 1. 理解 RabbitMQ 的基础概念（Queue, Producer, Consumer）
 * 2. 学习如何创建持久化队列
 * 3. 掌握最简单的消息发送和接收
 *
 * 运行步骤:
 * 1. 启动 RabbitMQ: docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management
 * 2. 运行测试: mvnw.cmd test -Dtest=Stage1HelloWorldTest -pl javainfohunter-ai-service
 * 3. 观察控制台输出
 * 4. 访问 RabbitMQ 管理界面: http://localhost:15672 (guest/guest)
 *
 * 预期输出:
 * 📤 发送消息: Hello RabbitMQ!
 * 📥 接收消息: Hello RabbitMQ!
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.dashscope.api-key=test-placeholder-key-not-for-production",
    "spring.ai.dashscope.enabled=false",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=25672",
    "spring.rabbitmq.username=admin",
    "spring.rabbitmq.password=admin123"
})
public class Stage1HelloWorldTest {

    @Autowired
    private HelloWorldProducer producer;

    /**
     * 测试 Hello World
     *
     * 线程休眠 1 秒，等待消费者处理消息
     */
    @Test
    public void testHelloWorld() throws InterruptedException {
        System.out.println("========================================");
        System.out.println("阶段1: Hello World 测试开始");
        System.out.println("========================================");

        producer.sendHello("Hello RabbitMQ!");

        // 等待消费者处理
        Thread.sleep(1000);

        System.out.println("========================================");
        System.out.println("阶段1: Hello World 测试完成");
        System.out.println("========================================");
    }
}
