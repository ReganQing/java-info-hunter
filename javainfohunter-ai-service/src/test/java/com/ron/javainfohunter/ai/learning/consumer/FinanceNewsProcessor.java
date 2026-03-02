package com.ron.javainfohunter.ai.learning.consumer;

import com.ron.javainfohunter.ai.learning.dto.News;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段4: 财经新闻处理器
 *
 * 职责: 处理财经类新闻，进行情感分析和市场影响评估
 *
 * 路由键匹配: finance.*
 * - finance.stock
 * - finance.fund
 * - finance.bond
 * - etc.
 */
@Component
public class FinanceNewsProcessor {

    @RabbitListener(queues = "finance.queue")
    public void processFinanceNews(News news) {
        System.out.println("💰 [财经新闻处理器] 收到: " + news.getTitle());
        System.out.println("   类别: " + news.getCategory() + "." + news.getSubcategory());

        try {
            // 模拟情感分析过程
            Thread.sleep(700);

            // TODO: 情感分析、市场影响、投资建议
            System.out.println("✅ [财经新闻处理器] 情感分析完成");
            System.out.println("   → 情感倾向: 积极/中性/消极");
            System.out.println("   → 市场影响: 预计股市将上涨");
            System.out.println("   → 投资建议: 建议关注相关板块");

        } catch (InterruptedException e) {
            System.err.println("❌ [财经新闻处理器] 处理失败: " + news.getTitle());
            e.printStackTrace();
        }
    }
}
