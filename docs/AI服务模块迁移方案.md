# Ron-AI-Agent 迁移方案

## 1. 迁移目标

将 ron-ai-agent 项目改造为独立的 AI 服务模块 `javainfohunter-ai-service`，作为 JavaInfoHunter 的可插拔智能处理引擎。

### 1.1 设计原则

- **独立模块**: AI 服务作为独立的 Maven 模块，可单独版本管理
- **可插拔**: 通过 Spring Auto-Configuration 实现"引入即用"
- **可扩展**: 支持自定义 Agent 和 Tool 注册
- **轻量级**: 移除不必要的依赖，保持核心功能
- **标准化**: 遵循 Spring Boot Starter 规范

### 1.2 模块定位

```
javainfohunter-ai-service
├── 核心功能: Agent 框架、工具管理、编排引擎
├── 对外接口: REST API、SDK、事件总线
└── 配置方式: Spring Boot Auto-Configuration
```

---

## 2. 架构设计

### 2.1 模块结构

```
javainfohunter-ai-service/
├── pom.xml                                    # Maven 配置
├── src/main/java/com/ron/javainfohunter/ai/
│   ├── autoconfigure/                         # 自动配置
│   │   ├── AiServiceAutoConfiguration.java
│   │   ├── AiServiceProperties.java
│   │   └── AgentToolAutoConfiguration.java
│   ├── agent/                                 # Agent 核心
│   │   ├── core/
│   │   │   ├── BaseAgent.java
│   │   │   ├── ReActAgent.java
│   │   │   ├── ToolCallAgent.java
│   │   │   ├── AgentState.java
│   │   │   └── AgentManager.java
│   │   ├── coordinator/
│   │   │   ├── TaskCoordinator.java
│   │   │   ├── CollaborationPattern.java
│   │   │   ├── CoordinationResult.java
│   │   │   └── impl/
│   │   │       ├── TaskCoordinatorImpl.java
│   │   │       └── AgentManagerImpl.java
│   │   └── specialized/                      # 预置 Agent
│   │       ├── ContentAnalysisAgent.java
│   │       ├── ContentSummaryAgent.java
│   │       └── ContentClassificationAgent.java
│   ├── tool/                                 # 工具系统
│   │   ├── core/
│   │   │   ├── ToolCallback.java
│   │   │   ├── ToolManager.java
│   │   │   └── ToolRegistry.java
│   │   ├── annotation/
│   │   │   ├── Tool.java
│   │   │   └── ToolParam.java
│   │   └── builtin/                          # 内置工具
│   │       ├── HtmlParserTool.java
│   │       ├── TextAnalysisTool.java
│   │       └── SummarizationTool.java
│   ├── service/                              # AI 服务
│   │   ├── ChatService.java
│   │   ├── EmbeddingService.java
│   │   └── AgentService.java
│   ├── model/                                # 数据模型
│   │   ├── dto/
│   │   │   ├── AgentRequest.java
│   │   │   ├── AgentResponse.java
│   │   │   └── ToolExecutionResult.java
│   │   └── entity/
│   │       └── AgentExecution.java
│   └── controller/                           # REST API（可选）
│       ├── AgentController.java
│       └── ChatController.java
└── src/main/resources/
    └── META-INF/
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 2.2 依赖管理

**javainfohunter-ai-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ron</groupId>
        <artifactId>javainfohunter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>javainfohunter-ai-service</artifactId>
    <name>JavaInfoHunter AI Service</name>
    <description>Intelligent Agent Orchestration Framework</description>

    <properties>
        <spring-ai.version>1.0.2</spring-ai.version>
        <spring-ai-alibaba.version>1.0.0-M2.1</spring-ai-alibaba.version>
    </properties>

    <dependencies>
        <!-- Spring Boot 核心 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring AI 核心 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-core</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>

        <!-- Spring AI Alibaba (可选) -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter</artifactId>
            <version>${spring-ai-alibaba.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- Spring AI Vector Store (可选) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-pgvector-store</artifactId>
            <version>${spring-ai.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool 工具 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-core</artifactId>
            <version>5.8.38</version>
        </dependency>

        <!-- JSON Schema 生成 -->
        <dependency>
            <groupId>com.github.victools</groupId>
            <artifactId>jsonschema-generator</artifactId>
            <version>4.38.0</version>
        </dependency>

        <!-- Web 依赖 (仅用于 REST API 模块) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### 2.3 父 POM 更新

**pom.xml**

```xml
<modules>
    <module>javainfohunter-ai-service</module>
    <module>javainfohunter-crawler</module>
    <module>javainfohunter-processor</module>
    <module>javainfohunter-api</module>
    <module>javainfohunter-common</module>
</modules>

<dependencyManagement>
    <dependencies>
        <!-- Spring AI BOM -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 内部模块依赖管理 -->
        <dependency>
            <groupId>com.ron</groupId>
            <artifactId>javainfohunter-ai-service</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 3. 核心组件迁移

### 3.1 Agent 核心 (从 ron-ai-agent 复用并精简)

#### 3.1.1 AgentState.java

```java
package com.ron.javainfohunter.ai.agent.core;

import lombok.Getter;

/**
 * Agent 执行状态
 */
@Getter
public enum AgentState {
    /**
     * 空闲状态，可以接受新任务
     */
    IDLE("空闲"),

    /**
     * 运行中，正在执行任务
     */
    RUNNING("运行中"),

    /**
     * 已完成，任务执行成功
     */
    FINISHED("已完成"),

    /**
     * 错误状态，任务执行失败
     */
    ERROR("错误");

    private final String description;

    AgentState(String description) {
        this.description = description;
    }
}
```

#### 3.1.2 BaseAgent.java (精简版)

```java
package com.ron.javainfohunter.ai.agent.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 基础抽象类
 * <p>
 * 管理 Agent 生命周期、状态和执行流程
 * </p>
 */
@Slf4j
@Data
public abstract class BaseAgent {

    /**
     * Agent 名称
     */
    private String name;

    /**
     * Agent 描述
     */
    private String description;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 下一步提示词
     */
    private String nextStepPrompt;

    /**
     * 当前状态
     */
    private AgentState agentState = AgentState.IDLE;

    /**
     * ChatClient (Spring AI)
     */
    protected ChatClient chatClient;

    /**
     * 消息历史
     */
    protected List<Message> messages = new ArrayList<>();

    /**
     * 最大执行步数
     */
    private int maxSteps = 10;

    /**
     * 当前步数
     */
    private int currentStep = 0;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 取消标志
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 执行任务
     *
     * @param userPrompt 用户输入
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent is not idle, current state: " + agentState);
        }

        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt cannot be empty");
        }

        startTime = LocalDateTime.now();
        cancelled.set(false);
        agentState = AgentState.RUNNING;

        log.info("Agent {} starting execution - Prompt: {}", name,
                userPrompt.substring(0, Math.min(100, userPrompt.length())));

        // 添加用户消息
        messages.add(new UserMessage(userPrompt));

        // 执行步骤
        List<String> results = new ArrayList<>();

        try {
            while (currentStep < maxSteps && agentState == AgentState.RUNNING && !cancelled.get()) {
                String stepResult = step();
                String result = "Step" + currentStep + ": " + stepResult;
                results.add(result);
                currentStep++;

                log.debug("Agent {} step {} completed: {}", name, currentStep,
                        stepResult.substring(0, Math.min(100, stepResult.length())));

                if (agentState == AgentState.FINISHED) {
                    break;
                }

                if (currentStep >= maxSteps) {
                    agentState = AgentState.FINISHED;
                    log.info("Agent {} finished - reached max steps ({})", name, maxSteps);
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                }
            }

            String finalResult = String.join("\n", results);
            log.info("Agent {} execution completed - Duration: {}ms, Steps: {}",
                    name, getExecutionDurationMillis(), currentStep);
            return finalResult;

        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Agent {} error at step {}", name, currentStep, e);
            return "Error: " + e.getMessage();
        } finally {
            cleanup();
        }
    }

    /**
     * 获取执行持续时间（毫秒）
     */
    public long getExecutionDurationMillis() {
        if (startTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    /**
     * 取消执行
     */
    public void cancel() {
        log.info("Cancelling agent {} execution", name);
        cancelled.set(true);
    }

    /**
     * 执行单个步骤（子类实现）
     *
     * @return 步骤结果
     */
    public abstract String step();

    /**
     * 清理资源（子类实现）
     */
    public abstract void cleanup();
}
```

#### 3.1.3 ReActAgent.java

```java
package com.ron.javainfohunter.ai.agent.core;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct (Reasoning and Acting) 模式 Agent
 * <p>
 * 实现思考-行动循环模式
 * </p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    /**
     * 思考阶段：决定是否需要行动
     *
     * @return true 表示需要行动，false 表示可以结束
     */
    public abstract boolean think();

    /**
     * 行动阶段：执行具体操作
     *
     * @return 行动结果
     */
    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                agentState = AgentState.FINISHED;
                return "思考完成，无需行动";
            }
            return act();
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            return "步骤执行发生错误：" + e.getMessage();
        }
    }
}
```

#### 3.1.4 ToolCallAgent.java (集成 Spring AI)

```java
package com.ron.javainfohunter.ai.agent.core;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallback;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支持工具调用的 Agent
 * <p>
 * 集成 Spring AI Tool Callback 机制
 * </p>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ToolCallAgent extends ReActAgent {

    /**
     * 可用工具列表
     */
    private ToolCallback[] availableTools;

    /**
     * 工具调用响应
     */
    private ChatResponse toolCallResponse;

    /**
     * 工具调用管理器
     */
    private final ToolCallingManager toolCallingManager;

    /**
     * 聊天选项（禁用内置工具执行）
     */
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();

        // 禁用 Spring AI 内置的工具执行，由我们手动管理
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        // 添加下一步提示
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessages().add(userMessage);
        }

        List<Message> messages = getMessages();
        Prompt prompt = new Prompt(messages, chatOptions);

        try {
            // 调用 LLM，获取工具调用决策
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallResponse = chatResponse;

            // 分析响应
            if (chatResponse != null && chatResponse.getResult() != null) {
                AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
                String result = assistantMessage.getText();
                List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

                log.info("{} 的思考：{}", getName(), result);
                log.info("{} 选择了 {} 个工具", getName(), toolCalls.size());

                if (toolCalls.isEmpty()) {
                    // 没有工具调用，添加助手消息并结束
                    getMessages().add(assistantMessage);
                    return false;
                } else {
                    return true; // 需要执行工具
                }
            }
        } catch (Exception e) {
            log.error("{} 的思考出错：{}", getName(), e.getMessage());
            getMessages().add(new AssistantMessage("处理时遇到错误" + e.getMessage()));
            return false;
        }

        return false;
    }

    @Override
    public String act() {
        if (!toolCallResponse.hasToolCalls()) {
            return "没有调用工具";
        }

        try {
            // 执行工具调用
            Prompt prompt = new Prompt(getMessages(), chatOptions);
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
                    prompt, toolCallResponse);

            // 更新消息历史
            setMessages(toolExecutionResult.conversationHistory());

            // 处理工具响应
            List<Message> conversationHistory = toolExecutionResult.conversationHistory();
            if (!conversationHistory.isEmpty() &&
                conversationHistory.getLast() instanceof ToolResponseMessage toolResponseMessage) {

                // 提取工具执行结果
                String results = toolResponseMessage.getResponses().stream()
                        .filter(Objects::nonNull)
                        .map(response -> "工具 " + response.name() +
                                " 完成了任务！结果: " + response.responseData())
                        .collect(Collectors.joining("\n"));

                log.info("{} 的 act 结果：{}", getName(), results);

                // 检查是否调用了终止工具
                boolean hasTerminateTool = toolResponseMessage.getResponses().stream()
                        .anyMatch(response -> response != null &&
                                ("doTerminate".equals(response.name()) ||
                                 response.name().toLowerCase().contains("terminate")));

                if (hasTerminateTool) {
                    log.info("{} 已终止", getName());
                    setAgentState(AgentState.FINISHED);
                }

                return results;
            } else {
                log.warn("{}: 工具执行后未找到有效的 ToolResponseMessage", getName());
                return "工具执行完成，但未获取到有效响应";
            }
        } catch (Exception e) {
            log.error("{}: 工具执行过程中发生错误", getName(), e);
            return "工具执行失败：" + e.getMessage();
        }
    }

    @Override
    public void cleanup() {
        // 子类可重写此方法进行资源清理
    }
}
```

### 3.2 Agent 协调器

#### 3.2.1 CollaborationPattern.java

```java
package com.ron.javainfohunter.ai.agent.coordinator;

/**
 * Agent 协作模式
 */
public enum CollaborationPattern {
    /**
     * 主从模式：一个协调者 Agent 分配任务给多个 Worker Agent
     */
    MASTER_WORKER,

    /**
     * 链式模式：Agent 按顺序执行，输出作为下一个的输入
     */
    CHAIN,

    /**
     * 并行模式：多个 Agent 同时执行，结果聚合
     */
    PARALLEL
}
```

#### 3.2.2 CoordinationResult.java

```java
package com.ron.javainfohunter.ai.agent.coordinator;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 协作执行结果
 */
@Data
@Builder
public class CoordinationResult {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 最终输出
     */
    private String finalOutput;

    /**
     * 各 Agent 的输出
     */
    private Map<String, String> agentOutputs;

    /**
     * 执行时长
     */
    private Duration duration;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    public static CoordinationResult success(String finalOutput, Map<String, String> agentOutputs, Duration duration) {
        return CoordinationResult.builder()
                .success(true)
                .finalOutput(finalOutput)
                .agentOutputs(agentOutputs)
                .duration(duration)
                .build();
    }

    public static CoordinationResult failure(String errorMessage) {
        return CoordinationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
```

#### 3.2.3 TaskCoordinator.java (接口)

```java
package com.ron.javainfohunter.ai.agent.coordinator;

import java.util.List;

/**
 * Agent 任务协调器接口
 * <p>
 * 负责编排多个 Agent 的协作执行
 * </p>
 */
public interface TaskCoordinator {

    /**
     * Master-Worker 模式执行
     *
     * @param taskDescription 任务描述
     * @param masterAgentId  主 Agent ID
     * @param workerAgentIds  Worker Agent IDs
     * @return 执行结果
     */
    CoordinationResult executeMasterWorker(
            String taskDescription,
            String masterAgentId,
            List<String> workerAgentIds
    );

    /**
     * Chain 模式执行
     *
     * @param taskDescription 任务描述
     * @param agentIds       Agent IDs（按执行顺序）
     * @return 执行结果
     */
    CoordinationResult executeChain(
            String taskDescription,
            List<String> agentIds
    );

    /**
     * Parallel 模式执行
     *
     * @param taskDescription 任务描述
     * @param agentIds       Agent IDs
     * @return 执行结果
     */
    CoordinationResult executeParallel(
            String taskDescription,
            List<String> agentIds
    );

    /**
     * 通用执行方法
     *
     * @param taskDescription 任务描述
     * @param pattern        协作模式
     * @param agentIds       Agent IDs
     * @return 执行结果
     */
    CoordinationResult execute(
            String taskDescription,
            CollaborationPattern pattern,
            List<String> agentIds
    );
}
```

### 3.3 工具系统

#### 3.3.1 @Tool 注解

```java
package com.ron.javainfohunter.ai.tool.annotation;

import java.lang.annotation.*;

/**
 * 标记方法为 AI 可调用工具
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {
    /**
     * 工具描述
     */
    String description();

    /**
     * 工具名称（默认使用方法名）
     */
    String name() default "";
}
```

#### 3.3.2 @ToolParam 注解

```java
package com.ron.javainfohunter.ai.tool.annotation;

import java.lang.annotation.*;

/**
 * 标记工具参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {
    /**
     * 参数描述
     */
    String value() default "";
}
```

---

## 4. 自动配置

### 4.1 AiServiceAutoConfiguration.java

```java
package com.ron.javainfohunter.ai.autoconfigure;

import com.ron.javainfohunter.ai.agent.coordinator.AgentManager;
import com.ron.javainfohunter.ai.agent.coordinator.TaskCoordinator;
import com.ron.javainfohunter.ai.agent.coordinator.impl.AgentManagerImpl;
import com.ron.javainfohunter.ai.agent.coordinator.impl.TaskCoordinatorImpl;
import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.ai.service.EmbeddingService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AI 服务自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(AiServiceProperties.class)
@ConditionalOnProperty(prefix = "javainfohunter.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatModel.class)
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentManager agentManager() {
        return new AgentManagerImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCoordinator taskCoordinator(AgentManager agentManager) {
        return new TaskCoordinatorImpl(agentManager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatModel.class)
    public ChatService chatService(ChatClient chatClient) {
        return new ChatService(chatClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(EmbeddingModel.class)
    public EmbeddingService embeddingService(EmbeddingModel embeddingModel) {
        return new EmbeddingService(embeddingModel);
    }
}
```

### 4.2 AiServiceProperties.java

```java
package com.ron.javainfohunter.ai.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务配置属性
 */
@Data
@ConfigurationProperties(prefix = "javainfohunter.ai")
public class AiServiceProperties {

    /**
     * 是否启用 AI 服务
     */
    private boolean enabled = true;

    /**
     * Agent 配置
     */
    private Agent agent = new Agent();

    /**
     * 工具配置
     */
    private Tool tool = new Tool();

    @Data
    public static class Agent {
        /**
         * 最大执行步数
         */
        private int maxSteps = 10;

        /**
         * 执行超时时间（秒）
         */
        private int timeout = 300;
    }

    @Data
    public static class Tool {
        /**
         * 是否启用工具自动发现
         */
        private boolean autoDiscovery = true;
    }
}
```

### 4.3 自动配置注册

**src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports**

```
com.ron.javainfohunter.ai.autoconfigure.AiServiceAutoConfiguration
```

---

## 5. 使用示例

### 5.1 在 JavaInfoHunter 中集成

**1. 添加依赖**

```xml
<dependency>
    <groupId>com.ron</groupId>
    <artifactId>javainfohunter-ai-service</artifactId>
</dependency>
```

**2. 配置文件**

```yaml
javainfohunter:
  ai:
    enabled: true
    agent:
      max-steps: 10
      timeout: 300
    tool:
      auto-discovery: true

spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
```

**3. 创建自定义 Agent**

```java
@Component
public class ContentAnalysisAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的内容分析专家。
            你的任务是对新闻内容进行深度分析，提取关键信息。
            """;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolRegistry toolRegistry;

    public ContentAnalysisAgent() {
        super(toolRegistry.getAllTools().toArray(new ToolCallback[0]));
        setName("ContentAnalysisAgent");
        setSystemPrompt(SYSTEM_PROMPT);
        setChatClient(chatClient);
    }

    @Override
    public void cleanup() {
        // 清理资源
    }
}
```

**4. 使用协调器**

```java
@Service
public class ContentProcessingService {

    @Autowired
    private TaskCoordinator taskCoordinator;

    @Autowired
    private AgentManager agentManager;

    public void processContent(String content) {
        // 注册 Agent
        agentManager.registerAgent("analysis-agent", analysisAgent);
        agentManager.registerAgent("summary-agent", summaryAgent);
        agentManager.registerAgent("classification-agent", classificationAgent);

        // Parallel 模式执行
        CoordinationResult result = taskCoordinator.executeParallel(
                "分析以下内容：" + content,
                List.of("analysis-agent", "summary-agent", "classification-agent")
        );

        if (result.isSuccess()) {
            log.info("处理完成：{}", result.getFinalOutput());
        } else {
            log.error("处理失败：{}", result.getErrorMessage());
        }
    }
}
```

---

## 6. 迁移步骤

### Phase 1: 核心框架迁移 (1-2 天)

1. 创建 javainfohunter-ai-service 模块
2. 迁移 BaseAgent、ReActAgent、ToolCallAgent
3. 迁移 AgentManager、TaskCoordinator
4. 编写单元测试

### Phase 2: 工具系统迁移 (1 天)

1. 迁移 @Tool、@ToolParam 注解
2. 迁移 ToolRegistry
3. 实现内置工具

### Phase 3: 自动配置 (1 天)

1. 实现自动配置类
2. 编写配置属性类
3. 注册自动配置

### Phase 4: 预置 Agent (2-3 天)

1. 实现 ContentAnalysisAgent
2. 实现 ContentSummaryAgent
3. 实现 ContentClassificationAgent
4. 集成测试

### Phase 5: 文档与示例 (1 天)

1. 编写使用文档
2. 创建示例代码
3. 更新 CLAUDE.md

---

## 7. 依赖冲突处理

### 7.1 冲突识别

| 依赖 | ron-ai-agent | JavaInfoHunter | 解决方案 |
|------|--------------|----------------|----------|
| Spring Boot | 3.5.5 | 4.0.3 | 使用 4.0.3 (兼容) |
| Spring AI | 1.0.2 | 1.0.2 | 无冲突 |
| DashScope | 2.21.5 | - | 标记为 optional |
| LangChain4j | 1.3.0-beta9 | - | 移除（使用 Spring AI） |
| Hutool | 5.8.38 | 5.8.38 | 无冲突 |
| PostgreSQL | - | pgvector | 标记为 optional |

### 7.2 解决策略

1. **移除 LangChain4j**: 完全使用 Spring AI
2. **DashScope 标记 optional**: 不强制依赖
3. **Web 标记 optional**: REST API 可选
4. **pgvector 标记 optional**: 向量数据库可选

---

## 8. 测试策略

### 8.1 单元测试

```java
@SpringBootTest
class TaskCoordinatorTest {

    @Autowired
    private TaskCoordinator taskCoordinator;

    @Test
    void testParallelExecution() {
        CoordinationResult result = taskCoordinator.executeParallel(
                "测试任务",
                List.of("agent1", "agent2")
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getFinalOutput());
    }
}
```

### 8.2 集成测试

```java
@SpringBootTest
@Import(AiServiceAutoConfiguration.class)
class AiServiceIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Test
    void testChatService() {
        String response = chatService.chat("你好");
        assertNotNull(response);
    }
}
```

---

## 9. 性能优化

### 9.1 虚拟线程集成

```java
@Component
public class VirtualThreadTaskCoordinator implements TaskCoordinator {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public CoordinationResult executeParallel(String taskDescription, List<String> agentIds) {
        // 使用虚拟线程并发执行
        List<CompletableFuture<String>> futures = agentIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> executeAgent(id), executor))
                .toList();

        // 等待所有完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
```

### 9.2 连接池配置

```yaml
spring:
  ai:
    dashscope:
      client:
        connect-timeout: 5s
        read-timeout: 30s
        max-connections: 50
```

---

## 10. 监控与可观测性

### 10.1 Agent 执行记录

```java
@Data
@Entity
@Table(name = "agent_executions")
public class AgentExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agentName;
    private String agentType;
    private String coordinationPattern;
    private String taskDescription;
    private String inputData;
    private String outputData;
    private String status;
    private Integer steps;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

### 10.2 Metrics

```java
@Component
public class AgentMetrics {

    private final MeterRegistry meterRegistry;

    public void recordAgentExecution(String agentName, long durationMs, boolean success) {
        Timer.builder("agent.execution.duration")
                .tag("agent", agentName)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

---

## 11. 总结

### 11.1 迁移收益

- ✅ **复用成熟框架**: ron-ai-agent 已验证的 Agent 编排模式
- ✅ **模块化设计**: AI 服务独立，易于升级和维护
- ✅ **Spring 生态**: 完全集成 Spring Boot 和 Spring AI
- ✅ **可插拔**: 通过依赖引入即可使用
- ✅ **可扩展**: 支持自定义 Agent 和 Tool

### 11.2 风险与挑战

- ⚠️ **依赖管理**: 需要仔细处理 optional 依赖
- ⚠️ **配置复杂度**: 需要清晰的文档和示例
- ⚠️ **性能调优**: Agent 协作需要合理设置并发数和超时

### 11.3 下一步行动

1. 创建 javainfohunter-ai-service 模块
2. 迁移核心 Agent 框架
3. 实现自动配置
4. 编写测试和文档
