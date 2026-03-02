package com.ron.javainfohunter.ai.learning.consumer;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 阶段5: 死信队列消费者
 *
 * 职责: 处理重试多次仍失败的消息
 *
 * 应用场景:
 * - 记录失败日志
 * - 发送告警通知
 * - 人工介入处理
 */
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = "dead.letter.queue")
    public void handleDeadLetter(
            byte[] message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        String content = new String(message);
        System.err.println("💀 消息进入死信队列: " + content);

        try {
            // TODO: 发送告警邮件、记录到数据库、通知运维人员
            System.err.println("📧 发送告警邮件...");
            System.err.println("💾 记录失败日志...");

            // 确认消息（从死信队列删除）
            channel.basicAck(deliveryTag, false);
            System.err.println("✅ 死信消息已处理");

        } catch (IOException e) {
            System.err.println("❌ 死信消息处理失败: " + e.getMessage());
        }
    }
}
