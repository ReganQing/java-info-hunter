package com.ron.javainfohunter.ai.agent.specialized;

import com.ron.javainfohunter.ai.agent.core.ToolCallAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 内容分类 Agent
 * <p>
 * 对内容进行多维度分类和标签提取
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClassificationAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的内容分类专家。

            你的职责：
            - 分析文章内容，确定所属类别
            - 提取相关的标签和关键词
            - 判断内容的时效性和重要性
            - 识别目标受众群体

            分类维度：
            1. 主题分类：科技/财经/体育/娱乐/政治/教育/健康等
            2. 内容形式：新闻/评论/教程/访谈/公告等
            3. 时效性：突发/近期/历史/常青内容
            4. 地域：全球/全国/区域/本地
            5. 情感倾向：正面/负面/中立

            输出格式：
            【主要分类】一级分类 > 二级分类 > 三级分类

            【标签】
            #标签1 #标签2 #标签3 ... (5-10个相关标签)

            【属性】
            - 时效性：突发/近期/历史/常青
            - 重要性：高/中/低
            - 受众：大众/专业/特定群体
            - 情感：正面/负面/中立

            【相似内容】
            推荐相关的主题类别
            """;

    private ChatClient chatClient;

    public ClassificationAgent() {
        super(new ToolCallback[0]);
        setName("ClassificationAgent");
        setDescription("内容分类 Agent，负责多维度分类和标签提取");
        setSystemPrompt(SYSTEM_PROMPT);
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        super.setChatClient(chatClient);
    }

    @Override
    public void cleanup() {
        log.info("ClassificationAgent 清理资源");
    }
}
