package com.ron.javainfohunter.ai.learning.consumer;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 阶段5: 生产级消费者
 *
 * 职责: 演示生产环境的可靠性保障机制
 *
 * 特点:
 * 1. 手动 ACK（确认成功后才删除消息）
 * 2. 异常分类处理（业务异常 vs 系统异常）
 * 3. 重试机制
 * 4. 死信队列
 */
@Component
public class RobustConsumer {

    /**
     * 生产级消息处理
     *
     * @param message 消息对象
     * @param channel RabbitMQ 通道（用于手动 ACK）
     * @param deliveryTag 消息投递标签（用于确认消息）
     */
    @RabbitListener(queues = "business.queue")
    public void processMessage(
            byte[] message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        String content = new String(message);
        System.out.println("📨 收到消息: " + content);

        try {
            // 1. 业务处理
            processBusinessLogic(content);

            // 2. 手动 ACK（确认成功）
            // basicAck(deliveryTag, multiple)
            //   - deliveryTag: 消息投递标签
            //   - multiple: false = 只确认当前消息，true = 确认所有小于等于 deliveryTag 的消息
            channel.basicAck(deliveryTag, false);
            System.out.println("✅ 处理成功，已确认");

        } catch (BusinessException e) {
            // 业务异常：拒绝消息，不重新入队（直接进入死信队列）
            try {
                // basicNack(deliveryTag, multiple, requeue)
                //   - requeue: false = 不重新入队，消息进入死信队列
                channel.basicNack(deliveryTag, false, false);

                System.err.println("❌ 业务异常，丢弃消息: " + e.getMessage());
            } catch (IOException ex) {
                System.err.println("❌ NACK 失败: " + ex.getMessage());
            }
        } catch (Exception e) {
            // 系统异常：拒绝消息，重新入队（会重试 3 次）
            try {
                // requeue: true = 重新入队，消息会重新消费
                channel.basicNack(deliveryTag, false, true);
                System.err.println("⚠️ 系统异常，消息重新入队: " + e.getMessage());
            } catch (IOException ex) {
                System.err.println("❌ NACK 失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 模拟业务逻辑
     *
     * @param content 消息内容
     * @throws BusinessException 业务异常（不重试）
     * @throws RuntimeException 系统异常（重试）
     */
    private void processBusinessLogic(String content) {
        if (content.contains("error")) {
            throw new BusinessException("模拟业务异常：数据格式错误");
        }
        if (content.contains("fail")) {
            throw new RuntimeException("模拟系统异常：数据库连接失败");
        }
        // 正常处理
        System.out.println("→ 处理中...");
    }

    /**
     * 自定义业务异常
     * 业务异常通常是数据问题，重试无意义，直接进入死信队列
     */
    static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
