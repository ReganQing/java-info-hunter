# JavaInfoHunter AI Service

## 概述

**javainfohunter-ai-service** 是一个独立的 AI 服务模块，提供了完整的 Agent 编排框架。基于 Spring AI 和 Spring Boot 构建，支持多种协作模式。

## 核心特性

- ✅ **Agent 框架**: BaseAgent → ReActAgent → ToolCallAgent 三层架构
- ✅ **协作模式**: 支持 Chain、Parallel、Master-Worker 三种模式
- ✅ **工具系统**: @Tool 注解自动发现和注册
- ✅ **Spring AI 集成**: 完全集成 Spring AI Alibaba (DashScope)
- ✅ **虚拟线程**: 使用 JDK 21 虚拟线程实现高并发
- ✅ **自动配置**: Spring Boot Starter 规范，引入即用
- ✅ **数据持久化**: 完整的 JPA Entity 和 Repository 层，支持 PostgreSQL + pgvector
- ✅ **向量搜索**: 集成 pgvector 扩展，支持语义相似度搜索
- ✅ **数据库迁移**: 使用 Flyway 进行版本化管理

## 快速开始

### 0. 配置 Maven 仓库（必需）

在项目的父 POM 或 `~/.m2/settings.xml` 中添加 Spring Milestone 仓库，否则无法解析 Spring AI 依赖：

**在 pom.xml 中添加：**
```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

### 1. 添加依赖

在主项目的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.ron</groupId>
    <artifactId>javainfohunter-ai-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 配置文件

在 `application.yml` 中配置：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}  # 必需：阿里云通义千问 API Key

javainfohunter:
  ai:
    enabled: true  # 是否启用 AI 服务，默认 true
    agent:
      max-steps: 10  # Agent 最大执行步数，防止无限循环，默认 10
      timeout: 300  # Agent 执行超时时间（秒），默认 300
    tool:
      auto-discovery: true  # 是否自动扫描并注册 @Tool 注解的方法，默认 true
```

### 3. 创建自定义 Agent

```java
@Component
public class MyAnalysisAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的内容分析专家。
            你的任务是分析新闻内容并提取关键信息。
            """;

    public MyAnalysisAgent() {
        super(new ToolCallback[0]);  // 空数组，稍后通过 setter 注入
        setName("MyAnalysisAgent");
        setDescription("自定义分析 Agent");
        setSystemPrompt(SYSTEM_PROMPT);
    }

    @Override
    public void cleanup() {
        // 清理资源
    }
}
```

### 4. 注册 Agent

```java
@Configuration
public class AgentConfig {

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private MyAnalysisAgent myAnalysisAgent;

    @PostConstruct
    public void registerAgents() {
        // 注入依赖
        ToolCallback[] tools = toolRegistry.getAllTools().toArray(new ToolCallback[0]);
        myAnalysisAgent.setChatClient(chatClient);
        myAnalysisAgent.setAvailableTools(tools);

        // 注册到 AgentManager
        agentManager.registerAgent("analysis-agent", myAnalysisAgent);
    }
}
```

### 5. 使用 Agent 编排

```java
@Service
public class ContentProcessingService {

    @Autowired
    private AgentService agentService;

    public void processContent(String content) {
        // Parallel 模式：并行执行多个 Agent
        CoordinationResult result = agentService.executeParallel(
                "分析以下内容：" + content,
                List.of("analysis-agent", "summary-agent", "classification-agent")
        );

        if (result.isSuccess()) {
            log.info("处理成功：{}", result.getFinalOutput());
            result.getAgentOutputs().forEach((agentId, output) -> {
                log.info("{} 的输出：{}", agentId, output);
            });
        } else {
            log.error("处理失败：{}", result.getErrorMessage());
        }
    }
}
```

## 协作模式

### Chain 模式（链式）

Agent 按顺序执行，每个 Agent 的输出作为下一个 Agent 的输入：

```java
CoordinationResult result = agentService.executeChain(
    "任务描述",
    List.of("crawler-agent", "analysis-agent", "summary-agent")
);
```

### Parallel 模式（并行）

多个 Agent 同时执行同一个任务，结果聚合：

```java
CoordinationResult result = agentService.executeParallel(
    "任务描述",
    List.of("sentiment-agent", "classification-agent", "keyword-agent")
);
```

### Master-Worker 模式（主从）

一个 Master Agent 协调多个 Worker Agent：

```java
CoordinationResult result = agentService.executeMasterWorker(
    "任务描述",
    "coordinator-agent",  // Master
    List.of("worker-1", "worker-2", "worker-3")  // Workers
);
```

## 自定义工具

使用 `@Tool` 注解标记方法即可：

```java
@Component
public class MyTools {

    @Tool(description = "解析 HTML 内容并提取正文")
    public String parseHtml(@ToolParam("HTML 内容") String html) {
        Document doc = Jsoup.parse(html);
        return doc.text();
    }

    @Tool(description = "生成内容摘要")
    public String summarize(@ToolParam("要摘要的文本") String text) {
        // 实现摘要逻辑
        return "摘要结果";
    }
}
```

工具会自动注册到 `ToolRegistry`，供 Agent 使用。

## API 文档

### AgentManager

Agent 生命周期管理：

```java
// 注册 Agent
agentManager.registerAgent("agent-name", agent);

// 获取 Agent
Optional<BaseAgent> agent = agentManager.getAgent("agent-name");

// 列出所有 Agent
List<String> names = agentManager.getAgentNames();
```

### TaskCoordinator

多 Agent 协作编排：

```java
// Chain 模式
CoordinationResult result = taskCoordinator.executeChain(
    "任务", List.of("agent1", "agent2")
);

// Parallel 模式
CoordinationResult result = taskCoordinator.executeParallel(
    "任务", List.of("agent1", "agent2")
);

// Master-Worker 模式
CoordinationResult result = taskCoordinator.executeMasterWorker(
    "任务", "master", List.of("worker1", "worker2")
);
```

### ChatService

简单的聊天接口：

```java
// 简单对话
String response = chatService.chat("你好");

// 带系统提示的对话
String response = chatService.chat(
    "你是一个专业的分析师",
    "请分析以下内容..."
);
```

### EmbeddingService

文本向量化：

```java
// 单个文本转向量
float[] vector = embeddingService.embed("文本内容");

// 批量转向量
List<float[]> vectors = embeddingService.embedBatch(List.of("文本1", "文本2"));

// 计算相似度
double similarity = embeddingService.cosineSimilarity(vector1, vector2);
```

## 架构

```
┌─────────────────────────────────────┐
│      Application Layer               │
│  (Your Service/Controller)          │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      AiServiceAutoConfiguration      │
│  • ChatClient                        │
│  • AgentManager                      │
│  • TaskCoordinator                   │
│  • ToolRegistry / ToolManager         │
└──────────────┬──────────────────────┘
               │
       ┌───────┴────────┐
       │                │
┌──────▼────────┐  ┌───▼──────────────┐
│  Agent Core   │  │   Tool System    │
│  • BaseAgent  │  │  • @Tool         │
│  • ReActAgent │  │  • ToolRegistry  │
│  • ToolCall   │  │  • ToolManager   │
└──────┬────────┘  └───┴──────────────┘
       │                │
┌──────▼────────────────▼──────────────┐
│       Spring AI + DashScope          │
│  • ChatModel                        │
│  • EmbeddingModel                   │
│  • ToolCallback                     │
└──────────────────────────────────────┘
```

## 版本信息

- **Java**: 21+
- **Spring Boot**: 4.0.3
- **Spring AI Alibaba**: 1.0.0.2
- **数据库**: PostgreSQL 16+ with pgvector
- **ORM**: Spring Data JPA (Hibernate)
- **迁移工具**: Flyway
- **构建工具**: Maven

## 数据库支持

本模块包含完整的数据库持久化层，支持以下特性：

### Entity 实体类

- `RssSource`: RSS 订阅源管理
- `RawContent`: 原始内容存储
- `News`: 处理后的新闻文章
- `AgentExecution`: Agent 执行追踪

### Repository 接口

- `RssSourceRepository`: RSS 源数据访问
- `RawContentRepository`: 原始内容数据访问
- `NewsRepository`: 新闻文章数据访问
- `AgentExecutionRepository`: Agent 执行记录访问

### 数据库特性

- ✅ **向量搜索**: 使用 pgvector 进行语义相似度搜索
- ✅ **全文搜索**: 使用 PostgreSQL GIN 索引进行全文搜索
- ✅ **自动迁移**: Flyway 自动执行数据库迁移脚本
- ✅ **连接池**: HikariCP 高性能连接池
- ✅ **审计日志**: 自动记录创建和更新时间

### 快速启用数据库功能

1. **配置数据源**:

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/javainfohunter
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.flyway.enabled=true
```

2. **创建数据库**:

```bash
psql -U postgres -c "CREATE DATABASE javainfohunter;"
psql -U postgres -d javainfohunter -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

3. **启动应用**:

```bash
mvnw.cmd spring-boot:run
```

Flyway 会自动执行迁移脚本，创建所有表和索引。

### 使用示例

```java
@Autowired
private RssSourceRepository rssSourceRepository;

@Autowired
private RawContentRepository rawContentRepository;

@Autowired
private NewsRepository newsRepository;

// 查找活跃的 RSS 源
List<RssSource> sources = rssSourceRepository.findByIsActiveTrue();

// 查找待处理内容
List<RawContent> pending = rawContentRepository.findByProcessingStatus(
    RawContent.ProcessingStatus.PENDING
);

// 查找已发布的新闻
Page<News> news = newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(
    PageRequest.of(0, 20)
);
```

详细文档请参考：
- [数据库设计说明.md](../docs/数据库设计说明.md)
- [数据库使用指南.md](../docs/数据库使用指南.md)

## 许可证

MIT License
