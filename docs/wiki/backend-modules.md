# JavaInfoHunter 模块详解

## 模块总览

| 模块 | 端口 | 职责 | Spring Boot 应用 |
|------|------|------|-----------------|
| ai-service | - | AI Agent 框架核心（库模块） | 否（Starter） |
| api | 8080 | REST API 网关 | 是 |
| crawler | 8081 | RSS 爬虫服务 | 是 |
| processor | 8082 | AI 内容处理服务 | 是 |

---

## 1. javainfohunter-ai-service（AI 服务模块）

### 定位

AI 服务模块是整个系统的智能核心，作为独立的 Spring Boot Starter，可被其他项目引入使用。它提供了完整的 Agent 编排框架、工具系统和数据访问层。

### 目录结构

```
javainfohunter-ai-service/
└── src/main/java/com/ron/javainfohunter/
    ├── ai/
    │   ├── agent/
    │   │   ├── core/                    # Agent 核心框架
    │   │   │   ├── BaseAgent            # 基础 Agent（状态管理、生命周期）
    │   │   │   ├── ReActAgent           # ReAct 推理 Agent（think-act 循环）
    │   │   │   ├── ToolCallAgent        # 工具调用 Agent（Spring AI ChatClient）
    │   │   │   └── AgentState           # Agent 状态枚举
    │   │   ├── coordinator/             # Agent 协调器
    │   │   │   ├── AgentManager         # Agent 注册管理器（接口）
    │   │   │   ├── TaskCoordinator      # 任务协调器（接口）
    │   │   │   ├── CollaborationPattern # 协作模式枚举
    │   │   │   ├── CoordinationResult   # 协调结果
    │   │   │   ├── pattern/             # 协作模式实现
    │   │   │   │   ├── TaskDelegation   # 任务委派
    │   │   │   │   └── WorkerResult     # Worker 结果
    │   │   │   └── impl/                # 协调器实现
    │   │   │       ├── AgentManagerImpl
    │   │   │       └── TaskCoordinatorImpl
    │   │   └── specialized/             # 预置业务 Agent
    │   │       ├── CrawlerAgent         # 网页爬取 Agent
    │   │       ├── AnalysisAgent        # 内容分析 Agent
    │   │       ├── SummaryAgent         # 文本摘要 Agent
    │   │       ├── ClassificationAgent  # 内容分类 Agent
    │   │       ├── CoordinatorAgent     # Master-Worker 协调器 Agent
    │   │       └── TrendingCoordinatorAgent # 趋势分析协调器 Agent
    │   ├── tool/                        # 工具系统
    │   │   ├── annotation/              # 工具注解
    │   │   │   ├── @Tool                # 工具注册注解
    │   │   │   └── @ToolParam           # 工具参数注解
    │   │   ├── core/                    # 工具核心
    │   │   │   ├── ToolRegistry         # 工具注册表
    │   │   │   └── ToolManager          # 工具管理器
    │   │   └── impl/                    # 预置工具
    │   │       ├── HtmlParserTool       # HTML 解析工具
    │   │       ├── TextSummarizationTool # 文本摘要工具
    │   │       └── CoordinatorTools     # 协调器工具集
    │   ├── service/                     # 服务层
    │   │   ├── ChatService              # AI 对话服务
    │   │   ├── EmbeddingService         # 向量嵌入服务
    │   │   └── AgentService             # Agent 执行服务
    │   ├── config/                      # 配置类
    │   │   ├── AgentAutoConfig          # Agent 自动配置
    │   │   └── DotenvEnvironmentPostProcessor # .env 文件加载
    │   └── autoconfigure/               # Spring Boot 自动配置
    │       ├── AiServiceAutoConfiguration # 自动配置入口
    │       └── AiServiceProperties       # 配置属性
    ├── entity/                          # JPA 实体（跨模块共享）
    │   ├── RssSource                    # RSS 订阅源
    │   ├── RawContent                   # 原始内容
    │   ├── News                         # 处理后新闻
    │   └── AgentExecution               # Agent 执行记录
    ├── repository/                      # 数据访问层（跨模块共享）
    │   ├── RssSourceRepository
    │   ├── RawContentRepository
    │   ├── NewsRepository
    │   └── AgentExecutionRepository
    └── dto/                             # 数据传输对象
        ├── RawContentMessage            # 原始内容消息
        ├── RssSourceStats               # RSS 源统计
        └── AgentExecutionStats          # Agent 执行统计
```

### Agent 框架（三层继承体系）

```
BaseAgent (状态管理、生命周期)
    ├── volatile AgentState             # 线程可见的状态
    ├── CopyOnWriteArrayList<Message>   # 线程安全的消息历史
    ├── AtomicInteger currentStep       # 原子步数计数器
    ├── maxSteps = 10                   # 最大执行步数
    └── ChatClient                      # Spring AI 对话客户端
        │
        ↓ 继承
ReActAgent (think-act 推理循环)
    ├── think() → 思考下一步
    └── act() → 执行动作
        │
        ↓ 继承
ToolCallAgent (工具回调)
    ├── Spring AI ChatClient            # 实际 AI 调用
    ├── ToolManager                     # 工具管理
    └── executeWithTools()              # 带工具的执行
        │
        ↓ 继承
Specialized Agents (业务 Agent)
    ├── CrawlerAgent                    # 网页爬取
    ├── AnalysisAgent                   # 内容分析
    ├── SummaryAgent                    # 文本摘要
    ├── ClassificationAgent             # 内容分类
    ├── CoordinatorAgent                # Master-Worker 协调
    └── TrendingCoordinatorAgent        # 趋势分析协调
```

### Agent 状态生命周期

```
IDLE (空闲) → RUNNING (运行中) → FINISHED (已完成)
                                → ERROR (错误)
```

### 四种协作模式

#### 1. Chain 模式（链式）

顺序执行，前一个 Agent 的输出作为下一个的输入。

```java
CoordinationResult result = taskCoordinator.executeChain(
    "任务描述",
    List.of("agent1", "agent2", "agent3")
);
```

**适用场景**：内容处理流水线（爬取 → 分析 → 摘要）

#### 2. Parallel 模式（并行）

并行执行多个 Agent，使用 Java 21 虚拟线程，性能提升约 3 倍。

```java
CoordinationResult result = taskCoordinator.executeParallel(
    "任务描述",
    List.of("agent1", "agent2", "agent3")
);
```

**适用场景**：多维度分析（情感分析 + 主题提取 + 关键词提取）

#### 3. Master-Worker 模式（主从协作）

主 Agent 负责任务规划和委派，Worker Agent 执行具体任务。

```java
CoordinationResult result = taskCoordinator.executeMasterWorker(
    "任务描述",
    "coordinator-agent",
    List.of("worker1", "worker2", "worker3")
);
```

**适用场景**：复杂分析任务（趋势分析、多源聚合）

#### 4. 自定义协调模式

使用 `CoordinatorAgent` 实现复杂工作流，通过 `CoordinatorTools` 进行任务委派和结果聚合。

### 工具系统

| 注解/类 | 用途 |
|---------|------|
| `@Tool` | 标记工具方法 |
| `@ToolParam` | 定义工具参数 |
| `ToolRegistry` | 工具注册表 |
| `ToolManager` | 工具管理器 |

**预置工具**：

| 工具 | 类名 | 功能 |
|------|------|------|
| HTML 解析 | `HtmlParserTool` | HTML 内容解析和提取 |
| 文本摘要 | `TextSummarizationTool` | 文本摘要生成 |
| 协调器工具 | `CoordinatorTools` | 任务委派和结果聚合 |

### 使用方式

```xml
<dependency>
    <groupId>com.ron</groupId>
    <artifactId>javainfohunter-ai-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## 2. javainfohunter-api（REST API 网关）

### 定位

API 服务是对外提供 REST 接口的网关层，负责新闻查询、RSS 源管理、系统管理和 Agent 监控。

### 目录结构

```
javainfohunter-api/
└── src/main/java/com/ron/javainfohunter/api/
    ├── controller/                      # REST 控制器
    │   ├── NewsController               # 新闻查询控制器
    │   ├── RssSourceController          # RSS 源管理控制器
    │   ├── AdminController              # 系统管理控制器
    │   ├── AgentController              # Agent 监控控制器
    │   └── TestController               # 测试控制器
    ├── service/                         # 业务服务层
    │   ├── NewsService                  # 新闻服务
    │   ├── RssSourceService             # RSS 源服务
    │   ├── AgentService                 # Agent 服务
    │   └── impl/                        # 服务实现
    ├── redis/                           # Redis 缓存
    │   ├── RedisService                 # 缓存服务接口
    │   └── RedisServiceImpl             # 缓存服务实现
    ├── dto/                             # 数据传输对象
    │   ├── ApiResponse<T>               # 统一响应包装
    │   ├── request/                     # 请求 DTO
    │   └── response/                    # 响应 DTO
    ├── config/                          # 配置类
    │   ├── CorsConfig                   # CORS 跨域配置
    │   ├── RedisConfig                  # Redis 配置
    │   └── OpenApiConfig                # OpenAPI 配置
    ├── aspect/                          # AOP 切面
    │   ├── RateLimit                    # 限流注解
    │   └── RateLimitAspect              # 限流切面实现
    └── exception/                       # 异常处理
        ├── GlobalExceptionHandler       # 全局异常处理器
        ├── BusinessException            # 业务异常
        ├── ResourceNotFoundException    # 资源未找到异常
        └── RateLimitExceededException   # 限流异常
```

### 核心功能

| 功能 | 说明 |
|------|------|
| REST API | 提供标准化 REST 接口 |
| 统一响应 | `ApiResponse<T>` 包装所有响应 |
| Swagger UI | 在线 API 文档 (`/swagger-ui.html`) |
| Redis 缓存 | 热数据缓存，减少数据库压力 |
| CORS 配置 | 支持跨域请求（可配置） |
| 限流切面 | 基于 Redis 的接口限流 |
| 全局异常处理 | 统一异常响应格式 |
| Actuator | 健康检查和指标监控 |

### 统一响应格式

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2026-04-08T12:00:00Z"
}
```

### AI 功能

API 服务默认关闭 AI 功能（`javainfohunter.ai.enabled: false`），同时排除了 DashScope 和 PgVectorStore 自动配置。

---

## 3. javainfohunter-crawler（RSS 爬虫服务）

### 定位

爬虫服务负责从 RSS 源定期抓取内容，进行去重处理后发布到 RabbitMQ 消息队列。

### 目录结构

```
javainfohunter-crawler/
└── src/main/java/com/ron/javainfohunter/crawler/
    ├── controller/
    │   └── CrawlController              # 手动触发爬取（开发/测试用）
    ├── scheduler/
    │   ├── CrawlScheduler               # 定时任务调度器
    │   └── CrawlOrchestrator            # 爬取编排器
    ├── service/
    │   ├── RssFeedCrawler               # RSS Feed 爬取器
    │   ├── RssSourceService             # RSS 源服务
    │   └── CrawlCoordinator             # 爬取协调器
    ├── publisher/
    │   ├── ContentPublisher             # 内容消息发布
    │   ├── CrawlResultPublisher         # 爬取结果发布
    │   ├── ErrorPublisher               # 错误消息发布
    │   └── PublishResult                # 发布结果
    ├── handler/
    │   ├── CrawlErrorHandler            # 爬取错误处理器
    │   ├── RetryHandler                 # 重试处理器
    │   └── ErrorType                    # 错误类型枚举
    ├── exception/                       # 自定义异常
    │   ├── FeedConnectionException      # Feed 连接异常
    │   ├── FeedParseException           # Feed 解析异常
    │   ├── PublishException             # 消息发布异常
    │   └── ConfirmTimeoutException      # 确认超时异常
    ├── health/
    │   └── CrawlerHealthIndicator       # 爬虫健康指标
    ├── metrics/
    │   └── CrawlMetricsCollector        # 爬取指标收集
    ├── config/
    │   ├── CrawlerProperties            # 爬虫配置属性
    │   ├── SchedulerConfiguration       # 调度器配置
    │   └── RabbitMQConfig               # RabbitMQ 配置
    └── dto/
        ├── CrawlResultMessage           # 爬取结果消息
        ├── CrawlResult                  # 爬取结果
        └── CrawlErrorMessage            # 爬取错误消息
```

### 核心功能

| 功能 | 说明 |
|------|------|
| 定时爬取 | 可配置间隔（默认 1 小时） |
| RSS 解析 | 使用 Rome 库解析 RSS/Atom Feed |
| 内容去重 | SHA-256 哈希去重 |
| 消息发布 | 通过 RabbitMQ 发布到处理服务 |
| 错误处理 | 分类错误处理和自动重试 |
| 健康检查 | 自定义 Actuator 健康指标 |
| 指标收集 | Prometheus 格式指标 |

### 爬取配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `CRAWLER_ENABLED` | true | 是否启用爬虫 |
| `CRAWLER_SCHEDULER_ENABLED` | true | 是否启用定时任务 |
| `CRAWLER_INITIAL_DELAY` | 30s | 首次执行延迟 |
| `CRAWLER_FIXED_RATE` | 1h | 爬取间隔 |
| `MAX_ARTICLES_PER_FEED` | 100 | 单次最大文章数 |
| `FEED_CONNECTION_TIMEOUT` | 30s | Feed 连接超时 |
| `FEED_READ_TIMEOUT` | 60s | Feed 读取超时 |
| `DEDUPLICATION_ENABLED` | true | 是否启用去重 |

### RabbitMQ 消息发布

- **Exchange**: `crawler.direct`（Direct 类型）
- **Routing Key**: 基于消息类型
- **确认模式**: `correlated`（发布者确认）
- **失败处理**: 自动重试 + 死信队列

---

## 4. javainfohunter-processor（AI 处理服务）

### 定位

处理服务是 AI 处理的核心，消费 RabbitMQ 中的原始内容消息，使用 Agent 编排进行智能分析，并将结果持久化到数据库。

### 目录结构

```
javainfohunter-processor/
└── src/main/java/com/ron/javainfohunter/processor/
    ├── consumer/
    │   └── RawContentConsumer           # 原始内容消息消费者
    ├── agent/
    │   ├── AgentProcessor               # Agent 处理器接口
    │   └── impl/                        # Agent 处理器实现
    │       ├── AnalysisAgentProcessor   # 分析 Agent 处理器
    │       ├── SummaryAgentProcessor    # 摘要 Agent 处理器
    │       └── ClassificationAgentProcessor # 分类 Agent 处理器
    ├── service/
    │   ├── ContentRoutingService        # 内容路由服务（接口）
    │   ├── ResultAggregator             # 结果聚合器（接口）
    │   └── impl/
    │       ├── ContentRoutingServiceImpl # 内容路由实现
    │       ├── TransactionalStoreService # 事务存储服务
    │       ├── MessagePublisher          # 消息发布服务
    │       └── ResultAggregatorImpl      # 结果聚合实现
    ├── config/
    │   ├── ProcessorProperties          # 处理器配置属性
    │   ├── AsyncConfig                  # 异步配置
    │   ├── RabbitMQConsumerConfig       # RabbitMQ 消费者配置
    │   └── RabbitListenerConfiguration  # RabbitMQ 监听器配置
    ├── dto/
    │   ├── AgentResult                  # Agent 处理结果
    │   └── ProcessedContentMessage      # 处理后内容消息
    ├── exception/
    │   └── ConsumerException            # 消费者异常
    └── util/
        └── ContentPreprocessor          # 内容预处理工具
```

### 核心功能

| 功能 | 说明 |
|------|------|
| 消息消费 | 从 RabbitMQ 消费原始内容 |
| 内容路由 | 根据内容类型路由到对应 Agent |
| Agent 编排 | 使用 Agent 框架进行 AI 处理 |
| 事务存储 | 处理结果事务性持久化 |
| 错误重试 | 失败消息重试和死信队列 |
| 内容预处理 | 清洗和标准化原始内容 |

### 处理流程

```
RabbitMQ 消息
    ↓
RawContentConsumer (消费消息)
    ↓
ContentPreprocessor (内容预处理)
    ↓
ContentRoutingService (内容路由)
    ↓
AgentProcessor (Agent 处理)
    ├── AnalysisAgentProcessor    → 内容分析
    ├── SummaryAgentProcessor     → 文本摘要
    └── ClassificationAgentProcessor → 内容分类
    ↓
ResultAggregator (结果聚合)
    ↓
TransactionalStoreService (事务存储)
    ↓
PostgreSQL
```

### 处理配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `PROCESSOR_ENABLED` | true | 是否启用处理器 |
| `AGENT_MAX_STEPS` | 10 | Agent 最大执行步数 |
| `AGENT_TIMEOUT` | 300s | Agent 执行超时 |
| `AGENT_MAX_RETRIES` | 3 | Agent 最大重试次数 |
| `EMBEDDING_ENABLED` | true | 是否生成向量嵌入 |
| `EMBEDDING_MODEL` | text-embedding-v3 | 嵌入模型 |
| `PROCESSING_THREAD_POOL_SIZE` | 10 | 处理线程池大小 |
| `INPUT_QUEUE` | processor.raw.content.queue | 输入队列名 |
| `DEAD_LETTER_QUEUE` | processor.dead.letter.queue | 死信队列名 |
| `QUEUE_MAX_RETRIES` | 3 | 队列最大重试次数 |

### RabbitMQ 消费配置

- **队列**: `processor.raw.content.queue`
- **死信队列**: `processor.dead.letter.queue`
- **确认模式**: `manual`（手动确认）
- **预取数量**: 10
- **并发消费者**: 5-20
- **连接超时**: 15s

---

## 5. javainfohunter-e2e（端到端测试）

端到端测试模块，用于验证整个系统的集成行为，使用 Testcontainers 进行容器化测试。

## Agent 使用规则

### 适用场景

| 使用 Agent 编排 | 不使用 Agent 编排 |
|----------------|-----------------|
| 内容分析（需要 AI 推理） | 简单 CRUD 操作 |
| 多步处理工作流 | 直接数据库查询 |
| 并行独立分析任务 | 静态业务规则 |
| 复杂决策和委派 | 性能关键型简单操作 |
| 工具密集型任务 | 不涉及 AI 的操作 |

### 预置 Agent 列表

| Agent ID | 类名 | 功能 | 基类 |
|----------|------|------|------|
| `crawler-agent` | CrawlerAgent | 网页爬取和内容提取 | ToolCallAgent |
| `analysis-agent` | AnalysisAgent | 内容深度分析 | ToolCallAgent |
| `summary-agent` | SummaryAgent | 文本摘要生成 | ReActAgent |
| `classification-agent` | ClassificationAgent | 内容分类和标签 | ReActAgent |
| `coordinator-agent` | CoordinatorAgent | Master-Worker 协调器 | ToolCallAgent |
| `trending-coordinator-agent` | TrendingCoordinatorAgent | 趋势分析协调器 | ToolCallAgent |
