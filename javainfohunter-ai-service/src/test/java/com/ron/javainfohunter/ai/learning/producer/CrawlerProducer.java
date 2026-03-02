package com.ron.javainfohunter.ai.learning.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 阶段2: 爬虫生产者
 *
 * 职责: 模拟爬虫调度器，发送待爬取的 URL 到队列
 *
 * 应用场景:
 * - 从数据库读取待爬取的 URL
 * - 从 RSS Feed 解析文章链接
 * - 从网站采集页面链接
 */
@Service
public class CrawlerProducer {

    private final RabbitTemplate rabbitTemplate;

    public CrawlerProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送待爬取的 URL 列表到队列
     *
     * 模拟场景: 从数据库或配置文件读取待爬取的 URL
     */
    public void sendUrlsToCrawl() {
        List<String> urls = List.of(
                "https://example.com/news/1",
                "https://example.com/news/2",
                "https://example.com/news/3",
                "https://example.com/news/4",
                "https://example.com/news/5"
        );

        System.out.println("🕷️ 开始发送 " + urls.size() + " 个 URL 到队列...");
        for (String url : urls) {
            rabbitTemplate.convertAndSend("url.queue", url);
            System.out.println("📤 发送 URL 到队列: " + url);
        }
        System.out.println("✅ 所有 URL 已发送完成");
    }

    /**
     * 发送单个 URL
     *
     * @param url 待爬取的 URL
     */
    public void sendUrl(String url) {
        rabbitTemplate.convertAndSend("url.queue", url);
        System.out.println("📤 发送 URL 到队列: " + url);
    }
}
