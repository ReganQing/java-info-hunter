package com.ron.javainfohunter.ai.learning.stage;

import com.ron.javainfohunter.ai.learning.dto.RawContent;
import com.ron.javainfohunter.ai.learning.producer.RawContentProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

/**
 * 阶段3: Fanout Exchange 测试
 *
 * 学习目标:
 * 1. 理解发布订阅模式（Pub/Sub）
 * 2. 学习 Fanout Exchange 的工作原理
 * 3. 掌握一对多广播的实现方式
 *
 * 运行步骤:
 * 1. 启动 RabbitMQ
 * 2. 运行测试: mvnw.cmd test -Dtest=Stage3FanoutTest -pl javainfohunter-ai-service
 * 3. 观察一条消息被多个消费者同时处理
 * 4. 在 RabbitMQ 管理界面查看 Exchange 和 Binding
 *
 * 预期输出:
 * 📡 广播原始内容: AI 技术突破：GPT-5 发布
 *    → 分析队列 ✅
 *    → 摘要队列 ✅
 *    → 分类队列 ✅
 * 🔍 [分析 Agent] 收到内容: AI 技术突破：GPT-5 发布
 * 📝 [摘要 Agent] 收到内容: AI 技术突破：GPT-5 发布
 * 🏷️ [分类 Agent] 收到内容: AI 技术突破：GPT-5 发布
 * ...
 *
 * 说明:
 * - 一条消息被广播到 3 个队列
 * - 3 个 Agent 并行处理，互不影响
 * - 适合并行处理场景（Parallel Pattern）
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
public class Stage3FanoutTest {

    @Autowired
    private RawContentProducer producer;

    /**
     * 测试 Fanout Exchange
     *
     * 线程休眠 2 秒，等待所有 Agent 处理完成
     */
    @Test
    public void testFanout() throws InterruptedException {
        System.out.println("========================================");
        System.out.println("阶段3: Fanout Exchange 测试开始");
        System.out.println("========================================");

        RawContent content = new RawContent(
                "https://example.com/news/1",
                "AI 技术突破：GPT-5 发布",
                "今日，OpenAI 发布了 GPT-5，性能比上一代提升了 5 倍...",
                LocalDateTime.now()
        );

        producer.broadcastRawContent(content);

        // 等待所有 Agent 处理完成（分析、摘要、分类并行处理）
        Thread.sleep(2000);

        System.out.println("========================================");
        System.out.println("阶段3: Fanout Exchange 测试完成");
        System.out.println("========================================");
    }
}
