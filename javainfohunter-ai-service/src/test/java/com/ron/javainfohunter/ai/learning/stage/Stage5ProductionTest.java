package com.ron.javainfohunter.ai.learning.stage;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * 阶段5: 生产级测试
 *
 * 学习目标:
 * 1. 理解消息可靠性保障机制
 * 2. 学习如何配置死信队列（DLQ）
 * 3. 掌握生产级环境的最佳实践
 *
 * 运行步骤:
 * 1. 启动 RabbitMQ
 * 2. 运行测试: mvnw.cmd test -Dtest=Stage5ProductionTest -pl javainfohunter-ai-service
 * 3. 观察消息确认、重试、死信队列的完整流程
 * 4. 在 RabbitMQ 管理界面查看队列和消息
 *
 * 预期输出:
 * ✅ 消息成功到达 Exchange
 * 📨 收到消息: 正常消息
 * → 处理中...
 * ✅ 处理成功，已确认
 *
 * ✅ 消息成功到达 Exchange
 * 📨 收到消息: 包含error的业务异常
 * ❌ 业务异常，丢弃消息: 模拟业务异常
 * 💀 消息进入死信队列: 包含error的业务异常
 *
 * ✅ 消息成功到达 Exchange
 * 📨 收到消息: 包含fail的系统异常
 * ⚠️ 系统异常，消息重新入队: 模拟系统异常
 * 📨 收到消息: 包含fail的系统异常
 * ⚠️ 系统异常，消息重新入队: 模拟系统异常
 * ... (重试 3 次)
 * 💀 消息进入死信队列: 包含fail的系统异常
 *
 * 说明:
 * - 正常消息：处理成功，ACK 确认
 * - 业务异常（包含error）：直接进入死信队列，不重试
 * - 系统异常（包含fail）：重试 3 次后进入死信队列
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
public class Stage5ProductionTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 测试生产级可靠性保障
     *
     * 线程休眠 10 秒，等待所有处理完成（包括重试）
     */
    @Test
    public void testReliability() throws InterruptedException {
        System.out.println("========================================");
        System.out.println("阶段5: 生产级测试开始");
        System.out.println("========================================");

        List<String> messages = List.of(
                "正常消息",
                "包含error的业务异常",
                "包含fail的系统异常"
        );

        for (String msg : messages) {
            // 发送持久化消息
            rabbitTemplate.convertAndSend(
                    "business.exchange",  // 注意：需要先创建这个 Exchange
                    "business.key",
                    msg,
                    message -> {
                        // 设置消息持久化
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }
            );
        }

        // 等待所有处理完成（包括重试）
        Thread.sleep(10000);

        System.out.println("========================================");
        System.out.println("阶段5: 生产级测试完成");
        System.out.println("========================================");
    }
}
