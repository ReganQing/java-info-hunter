package com.ron.javainfohunter.ai.autoconfigure;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.coordinator.TaskCoordinator;
import com.ron.javainfohunter.ai.agent.coordinator.impl.AgentManagerImpl;
import com.ron.javainfohunter.ai.agent.coordinator.impl.TaskCoordinatorImpl;
import com.ron.javainfohunter.ai.service.AgentService;
import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.ai.service.EmbeddingService;
import com.ron.javainfohunter.ai.tool.core.ToolManager;
import com.ron.javainfohunter.ai.tool.core.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AI 服务自动配置
 * <p>
 * 自动配置以下 Bean：
 * - ChatClient: Spring AI 聊天客户端
 * - AgentManager: Agent 管理器
 * - TaskCoordinator: 任务协调器
 * - ToolRegistry: 工具注册表
 * - ToolManager: 工具管理器
 * </p>
 *
 * <p>启用条件：</p>
 * <ul>
 * <li>javainfohunter.ai.enabled=true（默认启用）</li>
 * <li>classpath 中存在 ChatModel.class</li>
 * </ul>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AiServiceProperties.class)
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiServiceAutoConfiguration {

    /**
     * 配置 ChatClient
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatModel.class)
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Initializing ChatClient with model: {}", chatModel.getClass().getSimpleName());
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 配置 ToolRegistry
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry() {
        log.info("Initializing ToolRegistry");
        return new ToolRegistry();
    }

    /**
     * 配置 ToolManager
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolManager toolManager(ToolRegistry toolRegistry) {
        log.info("Initializing ToolManager");
        return new ToolManager(toolRegistry);
    }

    /**
     * 配置 AgentManager
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentManager agentManager() {
        log.info("Initializing AgentManager");
        return new AgentManagerImpl();
    }

    /**
     * 配置 TaskCoordinator
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskCoordinator taskCoordinator(AgentManager agentManager) {
        log.info("Initializing TaskCoordinator");
        return new TaskCoordinatorImpl(agentManager);
    }

    /**
     * 配置 ChatService（可选）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatModel.class)
    public ChatService chatService(ChatClient chatClient) {
        log.info("Initializing ChatService");
        return new ChatService(chatClient);
    }

    /**
     * 配置 EmbeddingService（可选）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(org.springframework.ai.embedding.EmbeddingModel.class)
    public EmbeddingService embeddingService(EmbeddingModel embeddingModel) {
        log.info("Initializing EmbeddingService");
        return new EmbeddingService(embeddingModel);
    }

    /**
     * 配置 AgentService（可选）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatModel.class)
    public AgentService agentService(TaskCoordinator taskCoordinator, AgentManager agentManager) {
        log.info("Initializing AgentService");
        return new AgentService(taskCoordinator, agentManager);
    }
}
