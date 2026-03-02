package com.ron.javainfohunter.ai.learning.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段2: 爬虫工作线程
 *
 * 职责: 从队列获取 URL 并执行爬取任务
 *
 * 特点:
 * - 可以有多个实例（通过配置文件的 concurrency 控制）
 * - 自动实现负载均衡（轮询分发）
 * - 每个线程独立工作，互不干扰
 */
@Component
public class CrawlerWorker {

    /**
     * 监听 url.queue 队列，执行爬取任务
     *
     * 注意: 可以在配置文件中设置 concurrency 来创建多个消费者实例
     *
     * @param url 待爬取的 URL
     */
    @RabbitListener(queues = "url.queue")
    public void crawlUrl(String url) {
        String threadName = Thread.currentThread().getName();
        System.out.println("🕷️ [" + threadName + "] 开始爬取: " + url);

        try {
            // 模拟爬取过程（耗时 1 秒）
            Thread.sleep(1000);

            // 模拟爬取成功
            String content = "爬取的内容: " + url;
            System.out.println("✅ [" + threadName + "] 爬取完成: " + url);

            // TODO: 将爬取的内容发送到下一个队列（原始内容队列）

        } catch (InterruptedException e) {
            System.err.println("❌ [" + threadName + "] 爬取失败: " + url);
            e.printStackTrace();
        }
    }
}
