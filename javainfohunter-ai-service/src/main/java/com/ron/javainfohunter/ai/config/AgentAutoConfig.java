package com.ron.javainfohunter.ai.config;

import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.specialized.AnalysisAgent;
import com.ron.javainfohunter.ai.agent.specialized.ClassificationAgent;
import com.ron.javainfohunter.ai.agent.specialized.CoordinatorAgent;
import com.ron.javainfohunter.ai.agent.specialized.CrawlerAgent;
import com.ron.javainfohunter.ai.agent.specialized.SummaryAgent;
import com.ron.javainfohunter.ai.agent.specialized.TrendingCoordinatorAgent;
import com.ron.javainfohunter.ai.tool.core.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Agent 自动配置类
 * <p>
 * 自动注册所有预置 Agent 到 AgentManager
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfig {

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private CrawlerAgent crawlerAgent;

    @Autowired
    private AnalysisAgent analysisAgent;

    @Autowired
    private SummaryAgent summaryAgent;

    @Autowired
    private ClassificationAgent classificationAgent;

    @Autowired
    private CoordinatorAgent coordinatorAgent;

    @Autowired
    private TrendingCoordinatorAgent trendingCoordinatorAgent;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolRegistry toolRegistry;

    /**
     * 自动注册所有 Agent
     */
    @PostConstruct
    public void registerAgents() {
        log.info("========== 开始注册预置 Agent ==========");

        // 获取所有可用工具
        ToolCallback[] tools = toolRegistry.getAllTools();
        log.info("已加载 {} 个工具", tools.length);

        // 注册 CrawlerAgent
        crawlerAgent.setChatClient(chatClient);
        crawlerAgent.setAvailableTools(tools);  // 使用父类的 setter
        agentManager.registerAgent("crawler-agent", crawlerAgent);
        log.info("✓ 已注册 CrawlerAgent");

        // 注册 AnalysisAgent
        analysisAgent.setChatClient(chatClient);
        analysisAgent.setAvailableTools(tools);  // 使用父类的 setter
        agentManager.registerAgent("analysis-agent", analysisAgent);
        log.info("✓ 已注册 AnalysisAgent");

        // 注册 SummaryAgent
        summaryAgent.setChatClient(chatClient);
        summaryAgent.setAvailableTools(tools);  // 使用父类的 setter
        agentManager.registerAgent("summary-agent", summaryAgent);
        log.info("✓ 已注册 SummaryAgent");

        // 注册 ClassificationAgent
        classificationAgent.setChatClient(chatClient);
        classificationAgent.setAvailableTools(tools);  // 使用父类的 setter
        agentManager.registerAgent("classification-agent", classificationAgent);
        log.info("✓ 已注册 ClassificationAgent");

        // 注册 CoordinatorAgent
        agentManager.registerAgent("coordinator-agent", coordinatorAgent);
        log.info("✓ 已注册 CoordinatorAgent");

        // 注册 TrendingCoordinatorAgent
        agentManager.registerAgent("trending-coordinator-agent", trendingCoordinatorAgent);
        log.info("✓ 已注册 TrendingCoordinatorAgent");

        log.info("========== Agent 注册完成 ==========");
        log.info("当前可用 Agent: {}", agentManager.getAgentNames());
    }
}
