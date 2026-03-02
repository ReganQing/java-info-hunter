package com.ron.javainfohunter.ai.learning.stage;

import com.ron.javainfohunter.ai.learning.dto.News;
import com.ron.javainfohunter.ai.learning.producer.NewsProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * 阶段4: Topic Exchange 测试
 *
 * 学习目标:
 * 1. 理解路由模式（Routing）
 * 2. 学习 Topic Exchange 的路由键匹配规则
 * 3. 掌握按类别分发消息的实现方式
 *
 * 运行步骤:
 * 1. 启动 RabbitMQ
 * 2. 运行测试: mvnw.cmd test -Dtest=Stage4TopicTest -pl javainfohunter-ai-service
 * 3. 观察消息根据路由键分发到不同的队列
 * 4. 在 RabbitMQ 管理界面查看 Exchange 和 Binding
 *
 * 预期输出:
 * 📰 发送新闻到 [tech.ai]: GPT-5 发布
 * 📰 发送新闻到 [tech.blockchain]: 比特币突破10万
 * 📰 发送新闻到 [finance.stock]: 股市大涨
 * 📰 发送新闻到 [sports.nba]: 湖人夺冠
 *
 * 💻 [科技新闻处理器] 收到: GPT-5 发布
 * 📦 [归档器] 保存到数据库: GPT-5 发布
 *
 * 💻 [科技新闻处理器] 收到: 比特币突破10万
 * 📦 [归档器] 保存到数据库: 比特币突破10万
 *
 * 💰 [财经新闻处理器] 收到: 股市大涨
 * 📦 [归档器] 保存到数据库: 股市大涨
 *
 * 📦 [归档器] 保存到数据库: 湖人夺冠  (只匹配 *.*)
 *
 * 说明:
 * - tech.ai 匹配 tech.* 和 *.*
 * - finance.stock 匹配 finance.* 和 *.*
 * - sports.nba 只匹配 *.*
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
public class Stage4TopicTest {

    @Autowired
    private NewsProducer producer;

    /**
     * 测试 Topic Exchange
     *
     * 线程休眠 2 秒，等待所有处理器完成
     */
    @Test
    public void testTopicRouting() throws InterruptedException {
        System.out.println("========================================");
        System.out.println("阶段4: Topic Exchange 测试开始");
        System.out.println("========================================");

        List<News> newsList = List.of(
                new News("tech", "ai", "GPT-5 发布", "OpenAI 发布 GPT-5..."),
                new News("tech", "blockchain", "比特币突破10万", "比特币价格创历史新高..."),
                new News("finance", "stock", "股市大涨", "今日股市全面上涨..."),
                new News("sports", "nba", "湖人夺冠", "洛杉矶湖人队获得总冠军...")
        );

        for (News news : newsList) {
            producer.sendNews(news);
        }

        // 等待所有处理器完成
        Thread.sleep(2000);

        System.out.println("========================================");
        System.out.println("阶段4: Topic Exchange 测试完成");
        System.out.println("========================================");
    }
}
