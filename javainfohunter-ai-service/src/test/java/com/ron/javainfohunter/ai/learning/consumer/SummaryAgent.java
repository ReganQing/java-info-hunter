package com.ron.javainfohunter.ai.learning.consumer;

import com.ron.javainfohunter.ai.learning.dto.RawContent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段3: 摘要 Agent
 *
 * 职责: 接收原始内容并生成摘要
 *
 * 应用场景:
 * - 生成文章摘要
 * - 提取关键信息
 * - 生成标题
 */
@Component
public class SummaryAgent {

    @RabbitListener(queues = "summary.queue")
    public void summarize(RawContent content) {
        System.out.println("📝 [摘要 Agent] 收到内容: " + content.getTitle());
        System.out.println("   原文长度: " + content.getContent().length() + " 字符");

        try {
            // 模拟 AI 摘要生成过程
            Thread.sleep(600);

            // TODO: 调用 Spring AI 生成摘要
            System.out.println("✅ [摘要 Agent] 摘要生成完成");
            System.out.println("   → 摘要: GPT-5 发布，性能提升显著...");

        } catch (InterruptedException e) {
            System.err.println("❌ [摘要 Agent] 摘要生成失败: " + content.getTitle());
            e.printStackTrace();
        }
    }
}
