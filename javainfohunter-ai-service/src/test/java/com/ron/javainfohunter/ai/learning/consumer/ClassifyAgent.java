package com.ron.javainfohunter.ai.learning.consumer;

import com.ron.javainfohunter.ai.learning.dto.RawContent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 阶段3: 分类 Agent
 *
 * 职责: 接收原始内容并进行分类
 *
 * 应用场景:
 * - 内容分类（科技、财经、体育等）
 * - 标签提取
 * - 主题归类
 */
@Component
public class ClassifyAgent {

    @RabbitListener(queues = "classify.queue")
    public void classify(RawContent content) {
        System.out.println("🏷️ [分类 Agent] 收到内容: " + content.getTitle());

        try {
            // 模拟 AI 分类过程
            Thread.sleep(400);

            // TODO: 调用 Spring AI 进行分类
            System.out.println("✅ [分类 Agent] 分类完成");
            System.out.println("   → 类别: 科技");
            System.out.println("   → 子类别: AI");
            System.out.println("   → 标签: [GPT, OpenAI, 大模型]");

        } catch (InterruptedException e) {
            System.err.println("❌ [分类 Agent] 分类失败: " + content.getTitle());
            e.printStackTrace();
        }
    }
}
