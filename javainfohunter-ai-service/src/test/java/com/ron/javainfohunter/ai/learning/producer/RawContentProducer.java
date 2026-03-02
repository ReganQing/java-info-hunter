package com.ron.javainfohunter.ai.learning.producer;

import com.ron.javainfohunter.ai.learning.dto.RawContent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 阶段3: 原始内容生产者
 *
 * 职责: 发送爬取的原始内容到 Fanout Exchange，广播给多个 Agent
 *
 * 应用场景:
 * - 爬虫完成内容爬取后，需要多个 Agent 并行处理
 * - 分析、摘要、分类等 Agent 同时接收相同的数据
 */
@Service
public class RawContentProducer {

    private final RabbitTemplate rabbitTemplate;

    public RawContentProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 广播原始内容到所有绑定的队列
     *
     * 注意: 发送到 Exchange（不是队列），routing key 为空（Fanout 模式不需要）
     *
     * @param content 原始内容对象
     */
    public void broadcastRawContent(RawContent content) {
        // 发送到 Fanout Exchange，routing key 为空字符串
        rabbitTemplate.convertAndSend("raw.content.exchange", "", content);
        System.out.println("📡 广播原始内容: " + content.getTitle());
        System.out.println("   → 分析队列 ✅");
        System.out.println("   → 摘要队列 ✅");
        System.out.println("   → 分类队列 ✅");
    }
}
