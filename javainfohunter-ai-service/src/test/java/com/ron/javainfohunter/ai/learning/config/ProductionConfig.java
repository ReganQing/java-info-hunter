package com.ron.javainfohunter.ai.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段5: 生产级配置
 *
 * 学习目标:
 * 1. 理解消息可靠性保障机制
 * 2. 学习如何配置死信队列（DLQ）
 * 3. 掌握生产级环境的最佳实践
 *
 * 可靠性保障:
 * 1. 消息持久化: MessageDeliveryMode.PERSISTENT
 * 2. 队列持久化: durable = true
 * 3. 手动 ACK: 处理成功后才确认
 * 4. 发布确认: publisher-confirm-type = correlated
 * 5. 返回回调: publisher-returns = true
 *
 * 死信队列（DLQ）:
 * - 重试多次仍失败的消息会被路由到死信队列
 * - 用于异常告警和人工介入
 */
@Configuration
public class ProductionConfig {

    /**
     * 死信交换机（Direct 类型）
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dead.letter.exchange");
    }

    /**
     * 死信队列
     * 用于存储处理失败的消息
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue")
                .build();
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter");
    }

    /**
     * 业务队列（指定死信交换机）
     *
     * x-dead-letter-exchange: 死信交换机名称
     * x-dead-letter-routing-key: 死信路由键
     */
    @Bean
    public Queue businessQueue() {
        return QueueBuilder.durable("business.queue")
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    /**
     * JSON 消息转换器
     * 自动将 Java 对象转换为 JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 生产级 RabbitTemplate 配置
     *
     * 1. 开启强制回调（消息无法路由时触发）
     * 2. 开启确认回调（消息到达 Exchange）
     * 3. 开启返回回调（消息无法路由到队列）
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());

        // 开启强制回调
        template.setMandatory(true);

        // 确认回调（消息到达 Exchange）
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("✅ 消息成功到达 Exchange");
            } else {
                System.err.println("❌ 消息未到达 Exchange: " + cause);
            }
        });

        // 返回回调（消息无法路由到队列）
        template.setReturnsCallback(returned -> {
            System.err.println("❌ 消息无法路由: " + returned.getMessage());
        });

        return template;
    }
}
