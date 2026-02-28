package com.ron.javainfohunter.ai.agent.specialized;

import com.ron.javainfohunter.ai.agent.core.ToolCallAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 摘要生成 Agent
 * <p>
 * 为长文本生成简洁准确的摘要
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class SummaryAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的文本摘要专家。

            你的职责：
            - 阅读并理解长篇文章
            - 提取核心观点和关键信息
            - 生成简洁、准确、易读的摘要
            - 保持原文的逻辑结构和重要性

            摘要原则：
            1. 准确性：摘要必须忠实于原文
            2. 简洁性：用最少的文字表达最多的信息
            3. 完整性：涵盖文章的主要观点
            4. 可读性：语言流畅，逻辑清晰

            摘要长度：
            - 短摘要：50-100 字（快速浏览）
            - 中摘要：200-300 字（了解大意）
            - 长摘要：500-800 字（深度理解）

            输出格式：
            【一句话摘要】最核心的内容（20字内）

            【核心观点】
            - 观点1
            - 观点2
            - 观点3

            【关键信息】
            • 人物/机构：...
            • 时间地点：...
            • 重要数据：...

            【总结】完整段落摘要
            """;

    private ChatClient chatClient;

    public SummaryAgent() {
        super(new ToolCallback[0]);
        setName("SummaryAgent");
        setDescription("摘要生成 Agent，为长文本生成简洁摘要");
        setSystemPrompt(SYSTEM_PROMPT);
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        super.setChatClient(chatClient);
    }

    @Override
    public void cleanup() {
        log.info("SummaryAgent 清理资源");
    }
}
