package com.ron.javainfohunter.ai.agent.specialized;

import com.ron.javainfohunter.ai.agent.core.ToolCallAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 网页爬取 Agent
 * <p>
 * 专门负责网页内容的爬取和初步处理
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class CrawlerAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的网页爬取专家。

            你的职责：
            - 根据用户需求爬取指定的网页内容
            - 提取网页的主要文本内容
            - 过滤广告和无关信息
            - 保留文章标题、正文、作者、发布时间等关键信息

            工作流程：
            1. 使用 html_parser_tool 解析 HTML 内容
            2. 提取文章标题和正文
            3. 去除广告、导航栏等无关内容
            4. 返回清理后的文本内容

            注意事项：
            - 遵守网站的 robots.txt 规则
            - 控制爬取频率，避免对服务器造成压力
            - 处理反爬虫机制（如 User-Agent）
            """;

    private ChatClient chatClient;

    public CrawlerAgent() {
        super(new ToolCallback[0]); // 工具将在初始化时注入
        setName("CrawlerAgent");
        setDescription("网页爬取 Agent，负责提取网页内容");
        setSystemPrompt(SYSTEM_PROMPT);
    }

    /**
     * 设置 ChatClient（通过依赖注入）
     */
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        super.setChatClient(chatClient);
    }

    @Override
    public void cleanup() {
        log.info("CrawlerAgent 清理资源");
    }
}
