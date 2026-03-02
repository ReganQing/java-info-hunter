package com.ron.javainfohunter.ai.learning.consumer;

import com.ron.javainfohunter.ai.learning.dto.News;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段4: 所有新闻归档器
 *
 * 职责: 接收所有新闻并归档到数据库
 *
 * 路由键匹配: *.*
 * - 匹配所有两级的路由键
 * - 用于归档所有新闻，无论类别
 */
@Component
public class AllNewsArchiver {

    @RabbitListener(queues = "all.news.queue")
    public void archiveNews(News news) {
        System.out.println("📦 [归档器] 保存到数据库: " + news.getTitle());
        System.out.println("   类别: " + news.getCategory() + "." + news.getSubcategory());

        try {
            // 模拟数据库保存过程
            Thread.sleep(200);

            // TODO: 保存到 PostgreSQL + pgvector
            System.out.println("✅ [归档器] 保存成功");
            System.out.println("   → 数据库: PostgreSQL");
            System.out.println("   → 向量存储: pgvector");

        } catch (InterruptedException e) {
            System.err.println("❌ [归档器] 保存失败: " + news.getTitle());
            e.printStackTrace();
        }
    }
}
