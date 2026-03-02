package com.ron.javainfohunter.ai.learning.stage;

import com.ron.javainfohunter.ai.learning.producer.CrawlerProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 阶段2: Work Queue 测试
 *
 * 学习目标:
 * 1. 理解 Work Queue 模式（多个消费者竞争消费）
 * 2. 学习如何实现负载均衡
 * 3. 掌握并发消费者的配置
 *
 * 运行步骤:
 * 1. 启动 RabbitMQ
 * 2. 运行测试: mvnw.cmd test -Dtest=Stage2WorkQueueTest -pl javainfohunter-ai-service
 * 3. 观察多个消费者并发处理
 * 4. 在 RabbitMQ 管理界面查看队列状态
 *
 * 预期输出:
 * 🕷️ 开始发送 5 个 URL 到队列...
 * 📤 发送 URL 到队列: https://example.com/news/1
 * ...
 * 🕷️ [Container-0] 开始爬取: https://example.com/news/1
 * 🕷️ [Container-1] 开始爬取: https://example.com/news/2
 * 🕷️ [Container-2] 开始爬取: https://example.com/news/3
 * ...
 *
 * 说明:
 * - 默认有 3 个消费者（可以在 application-test.yml 中配置 concurrency）
 * - 消息会轮询分发到不同的消费者
 * - 每个消费者独立处理，互不干扰
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
public class Stage2WorkQueueTest {

    @Autowired
    private CrawlerProducer crawlerProducer;

    /**
     * 测试 Work Queue 模式
     *
     * 线程休眠 10 秒，等待所有消费者处理完成
     */
    @Test
    public void testWorkQueue() throws InterruptedException {
        System.out.println("========================================");
        System.out.println("阶段2: Work Queue 测试开始");
        System.out.println("========================================");

        crawlerProducer.sendUrlsToCrawl();

        // 等待所有消费者处理完成（5个URL，每个耗时1秒，3个消费者并行处理）
        Thread.sleep(10000);

        System.out.println("========================================");
        System.out.println("阶段2: Work Queue 测试完成");
        System.out.println("========================================");
    }
}
