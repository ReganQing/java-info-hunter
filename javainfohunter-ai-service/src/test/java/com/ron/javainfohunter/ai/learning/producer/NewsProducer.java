package com.ron.javainfohunter.ai.learning.producer;

import com.ron.javainfohunter.ai.learning.dto.News;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 阶段4: 新闻生产者
 *
 * 职责: 发送带路由键的新闻消息到 Topic Exchange
 *
 * 应用场景:
 * - 根据新闻类别（科技、财经、体育）分发到不同的处理队列
 * - 路由键格式: category.subcategory（如 tech.ai, finance.stock）
 */
@Service
public class NewsProducer {

    private final RabbitTemplate rabbitTemplate;

    public NewsProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送新闻到 Topic Exchange
     *
     * 路由键格式: {category}.{subcategory}
     * 示例:
     * - tech.ai → 匹配 tech.queue 和 all.news.queue
     * - finance.stock → 匹配 finance.queue 和 all.news.queue
     *
     * @param news 新闻对象
     */
    public void sendNews(News news) {
        // 构造路由键：category.subcategory
        String routingKey = news.getCategory() + "." + news.getSubcategory();

        rabbitTemplate.convertAndSend("news.exchange", routingKey, news);
        System.out.println("📰 发送新闻到 [" + routingKey + "]: " + news.getTitle());
    }
}
