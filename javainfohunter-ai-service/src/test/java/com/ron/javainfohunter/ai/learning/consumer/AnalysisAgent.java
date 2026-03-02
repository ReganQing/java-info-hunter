package com.ron.javainfohunter.ai.learning.consumer;

import com.ron.javainfohunter.ai.learning.dto.RawContent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段3: 分析 Agent
 *
 * 职责: 接收原始内容并进行深度分析
 *
 * 应用场景:
 * - 情感分析
 * - 关键词提取
 * - 主题识别
 * - 实体识别
 */
@Component
public class AnalysisAgent {

    @RabbitListener(queues = "analysis.queue")
    public void analyze(RawContent content) {
        System.out.println("🔍 [分析 Agent] 收到内容: " + content.getTitle());
        System.out.println("   URL: " + content.getUrl());

        try {
            // 模拟 AI 分析过程
            Thread.sleep(500);

            // TODO: 调用 Spring AI 进行情感分析、关键词提取
            System.out.println("✅ [分析 Agent] 分析完成");
            System.out.println("   → 情感倾向: 积极");
            System.out.println("   → 关键词: AI, GPT, 技术");

        } catch (InterruptedException e) {
            System.err.println("❌ [分析 Agent] 分析失败: " + content.getTitle());
            e.printStackTrace();
        }
    }
}
