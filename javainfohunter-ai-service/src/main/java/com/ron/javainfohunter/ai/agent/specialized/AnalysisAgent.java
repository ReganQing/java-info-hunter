package com.ron.javainfohunter.ai.agent.specialized;

import com.ron.javainfohunter.ai.agent.core.ToolCallAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 内容分析 Agent
 * <p>
 * 对爬取的内容进行深度分析，提取关键信息
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class AnalysisAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的内容分析专家。

            你的职责：
            - 分析文章的主题和核心观点
            - 提取关键信息（人物、地点、事件、时间）
            - 识别文章的情感倾向（正面/负面/中性）
            - 分析文章的可信度和权威性
            - 提取统计数据和重要引用

            分析维度：
            1. 主题分析：确定文章的核心主题
            2. 实体识别：提取人名、地名、机构名
            3. 关键观点：总结文章的主要观点
            4. 情感分析：判断文章的情感倾向
            5. 数据提取：提取数字、统计、图表数据

            输出格式：
            - 主题：一句话概括
            - 关键词：5-10个标签
            - 情感倾向：正面/负面/中性（置信度）
            - 关键信息：结构化摘要
            - 重要引用：直接引用的要点
            """;

    private ChatClient chatClient;

    public AnalysisAgent() {
        super(new ToolCallback[0]);
        setName("AnalysisAgent");
        setDescription("内容分析 Agent，负责深度分析文章内容");
        setSystemPrompt(SYSTEM_PROMPT);
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        super.setChatClient(chatClient);
    }

    @Override
    public void cleanup() {
        log.info("AnalysisAgent 清理资源");
    }
}
