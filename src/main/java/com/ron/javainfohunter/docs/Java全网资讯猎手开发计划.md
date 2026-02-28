这是一个非常扎实的工程化实战计划。为了让你在构建**"Java 版全网资讯猎手"**的过程中，既能产出代码，又能深刻理解架构概念，我将这个计划设计为 **5 个冲刺（Sprints）**，每个 Sprint 周期建议为 **1-2 周**。

我们将采用 **"MVP (Minimum Viable Product) + 迭代"** 的模式，从一个简单的脚本进化为高可用的分布式系统，最终实现 **Spring AI + Agent 编排**的智能处理架构。

------

### 🛠️ 前置准备 (环境搭建)

在开始 Sprint 1 之前，请确保你的开发环境就绪：

- **JDK:** 必须是 **JDK 21** (为了使用 Virtual Threads)。
- **IDE:** IntelliJ IDEA (Community 版即可)。
- **构建工具:** Maven (推荐，依赖管理直观)。
- **基础设施:** 安装 **Docker Desktop** (这是为了快速启动 Redis, RabbitMQ, PostgreSQL)。
- **AI 服务:** 申请阿里云 DashScope API Key (用于通义千问)，或安装 Ollama (本地开发)。

**环境变量配置：**
```bash
# 阿里云 DashScope API Key (必需)
export DASHSCOPE_API_KEY=your-api-key-here

# 数据库配置
export DATABASE_URL=jdbc:postgresql://localhost:5432/javainfohunter
export DATABASE_USERNAME=admin
export DATABASE_PASSWORD=admin

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379

# RabbitMQ 配置
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=admin
export RABBITMQ_PASSWORD=admin
```

------

### 🗓️ Sprint 1: 突破单机极限 —— 虚拟线程与爬虫核心

**核心目标：** 理解 JDK 21 虚拟线程如何解决"IO 阻塞"问题，实现单机极高吞吐的抓取。

**任务分解：**

1. **项目初始化：**
   - 创建一个 Spring Boot 4.0.3 项目。
   - 在 `application.yml` 中开启魔法开关：`spring.threads.virtual.enabled=true`。
   - 引入核心依赖：spring-boot-starter-web, jsoup, spring-boot-starter-amqp。

2. **构建 HTTP 客户端：**
   - 使用 Java 11 原生 `java.net.http.HttpClient`。
   - **关键点：** 封装一个工具类，配置连接超时（ConnectTimeout）和读取超时（ReadTimeout）。如果不配超时，网络波动会把你的线程池拖死。

3. **编写爬虫 Service：**
   - 目标：并发抓取 100 个 RSS XML 或 网页 URL。
   - 使用 `Jsoup` 解析 HTML 提取正文。
   - 设计数据结构：`RawContentEvent(url, title, content, timestamp)`。

4. **并发改造 (The "Aha!" Moment)：**
   - **V1 (传统版)：** 用 `parallelStream()` 跑，观察速度。
   - **V2 (虚拟线程版)：** 使用 `Executors.newVirtualThreadPerTaskExecutor()` 提交任务。
   - **验证：** 在日志中打印 `Thread.currentThread().toString()`，你会看到类似 `VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1`，证明你成功了。

**✅ 验收标准：**

- 能在一个 Main 方法中，10 秒内完成 500 个模拟 URL 的请求。
- 通过 JVisualVM 或 JConsole 看到系统平台线程数保持稳定（没有飙升到几百个）。
- 成功解析 RSS/HTML 内容，提取标题和正文。

**📚 学习重点：**

- 虚拟线程 vs 平台线程的内存占用对比
- `StructuredTaskScope` 用于结构化并发
- Jsoup 选择器语法

------

### 🗓️ Sprint 2: 架构解耦 —— 消息队列与可靠性

**核心目标：** 引入 RabbitMQ，防止爬虫太快把后续处理压垮，学习"生产者-消费者"模式。

**任务分解：**

1. **基础设施搭建：**
   - 编写 `docker-compose.yml`，启动 RabbitMQ 管理控制台。
   - 配置 Exchange: `crawler.exchange` (Topic 类型)。
   - 配置 Queue: `crawler.queue`、`processing.queue`。
   - 配置 Binding: `crawler.queue` 绑定到 `crawler.#`。

2. **定义消息契约 (DTO)：**
   - 创建一个 Java Record：`RawContentEvent(String url, String html, String title, long timestamp)`。
   - 配置 Jackson 序列化器。

3. **生产者开发 (Crawler)：**
   - 改造 Sprint 1 的代码。抓取成功后，不再直接打印，而是通过 `RabbitTemplate` 发送到 `crawler.exchange`。
   - 实现批量发送，提升性能。

4. **消费者开发 (Processor)：**
   - 编写一个 `@RabbitListener` 监听队列。
   - **核心挑战：** 实现 **Manual ACK (手动确认)**。
   - *代码逻辑：* `try { 处理业务(); channel.basicAck(); } catch { channel.basicNack(); }`。这是保证**数据不丢**的关键。

5. **消息持久化：**
   - 配置 Queue 和 Message 的持久化（durable=true）。
   - 测试：重启 RabbitMQ，消息不丢失。

**✅ 验收标准：**

- 启动爬虫疯狂发送 1000 条消息。
- 启动消费者服务，看到控制台一条条平稳处理，没有报错。
- 手动停止消费者服务，再重启，确认未处理的消息没有丢失。
- RabbitMQ 管理界面能看到消息堆积和消费速率。

**📚 学习重点：**

- AMQP 协议基础（Exchange、Queue、Binding）
- 消息确认机制（ACK/NACK）
- 死信队列（DLQ）配置
- 消息幂等性设计

------

### 🗓️ Sprint 3: AI 赋能 —— Spring AI 集成

**核心目标：** 集成 Spring AI 和阿里云 DashScope，实现智能内容分析。

**任务分解：**

1. **基础设施搭建：**
   - 在 `docker-compose.yml` 中添加 **PostgreSQL + pgvector**。
   - 配置 Spring AI Alibaba 依赖。

2. **Spring AI 核心配置：**
   ```xml
   <!-- pom.xml -->
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-core</artifactId>
   </dependency>
   <dependency>
       <groupId>com.alibaba.cloud.ai</groupId>
       <artifactId>spring-ai-alibaba-starter</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-pgvector-store</artifactId>
   </dependency>
   ```

   ```java
   @Bean
   public ChatClient chatClient(ChatModel chatModel) {
       return ChatClient.builder(chatModel).build();
   }
   ```

3. **第一个 AI 功能 —— 摘要生成：**
   - 创建 `SummaryService`，使用 ChatClient。
   - 实现 Prompt Template："你是一个资讯分析师，请用 200 字总结以下新闻内容：{{text}}"。
   - 测试：传入一段新闻，观察 AI 生成的摘要。

4. **Embedding 向量化：**
   - 配置 EmbeddingModel（使用 DashScope Embedding）。
   - 实现文本转向量：`float[] vector = embeddingModel.embed(text).getContent()`。
   - 测试：计算两个文本的余弦相似度。

5. **数据库设计：**
   - 创建表：`news (id, title, summary, content, embedding vector(1536), created_at)`。
   - 创建向量索引：`CREATE INDEX idx_news_embedding ON news USING ivfflat (embedding vector_cosine_ops);`。

6. **完整链路打通：**
   - 消费者收到 HTML → 调用 Spring AI 生成摘要 → 向量化 → 写入 PostgreSQL。

**✅ 验收标准：**

- 数据库里成功存入了新闻的摘要和向量数据。
- 能通过向量相似度搜索找到相似新闻。
- 理解 ChatClient 的流式和非流式调用。

**📚 学习重点：**

- Spring AI ChatClient API
- Prompt Template 设计
- Embedding 和向量相似度
- pgvector 基础

**💡 成本控制：** 开发阶段使用阿里云 DashScope（有免费额度），生产环境按需调用。

------

### 🗓️ Sprint 4: Agent 编排 —— 智能体协作框架

**核心目标：** 集成 Ron-AI-Agent 框架，实现多智能体协作的内容处理。

**任务分解：**

1. **Agent 框架集成：**
   - 将 `ron-ai-agent` 作为 Maven 子模块引入。
   - 复制核心类：`BaseAgent`, `ReActAgent`, `ToolCallAgent`。
   - 配置 `AgentManager` 和 `TaskCoordinator`。

2. **创建第一个 Agent —— CrawlerAgent：**
   ```java
   @Component
   public class CrawlerAgent extends ToolCallAgent {

       @Autowired
       private ChatClient chatClient;

       public CrawlerAgent() {
           super(new ToolCallback[]{
               // HtmlParserTool
               // ContentExtractorTool
           });
       }

       @Override
       public void cleanup() {
           // 清理资源
       }
   }
   ```

3. **实现工具 (Tools)：**
   - `HtmlParserTool`: 解析 HTML，提取结构化数据
   - `ContentExtractorTool`: 提取正文、元数据
   - `SummarizationTool`: 调用 Spring AI 生成摘要

4. **多 Agent 协作 —— Parallel 模式：**
   - 创建 `AnalysisAgent`：情感分析、关键词提取
   - 创建 `SummaryAgent`：生成摘要
   - 创建 `ClassificationAgent`：内容分类
   - 使用 `TaskCoordinator.executeParallel()` 并行执行

5. **Agent 执行监控：**
   - 创建表：`agent_executions (id, agent_name, task_description, status, duration_ms)`。
   - 记录每次 Agent 执行的输入、输出、耗时。

6. **完整内容处理流程：**
   ```
   RabbitMQ 消息
     ↓
   CrawlerAgent (解析内容)
     ↓
   Parallel(AnalysisAgent, SummaryAgent, ClassificationAgent)
     ↓
   CoordinatorAgent (汇总结果)
     ↓
   PostgreSQL + pgvector (存储)
   ```

**✅ 验收标准：**

- 成功注册 5 个 Agent：Crawler, Analysis, Summary, Classification, Coordinator。
- Parallel 模式下，3 个 Agent 能同时处理一条内容。
- Agent 执行记录表能追踪每次执行的详细信息。
- 理解 ReAct 模式（think-act 循环）。

**📚 学习重点：**

- Agent 编排模式（Chain, Parallel, Master-Worker）
- ReAct 模式（Reasoning and Acting）
- Spring AI Tool Callback 机制
- 多智能体协作设计

**🔗 参考资料：**
- ron-ai-agent 项目：`D:\Projects\BackEnd\ron-ai-agent`
- Agent 实现示例：参考 `ron-ai-agent/src/main/java/com/ron/ronaiagent/agent/specialized/`

------

### 🗓️ Sprint 5: 生产级治理 —— 限流与高可用

**核心目标：** 解决"分布式环境下资源竞争"问题，让系统坚不可摧。

**任务分解：**

1. **Redis 分布式限流：**
   - 场景：DashScope API 限制每分钟只能调用 60 次。
   - 实现：使用 **Redisson** 的 `RRateLimiter` 实现分布式限流。
   - 配置：每秒 1 次调用，超出限制的请求进入等待队列。
   - Agent 集成：在 `AnalysisAgent.think()` 前检查限流。

2. **优雅停机 (Graceful Shutdown)：**
   - 配置 Spring Boot：`server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s`。
   - **测试：** 在 Agent 处理任务时按 `Ctrl+C`，观察日志，系统应该等待当前 Agent 执行完成才关闭。

3. **Agent 容错与重试：**
   - 实现指数退避重试（Exponential Backoff）。
   - Agent 执行失败时，记录错误并重试（最多 3 次）。
   - 超过重试次数的消息进入死信队列。

4. **可观测性 (监控)：**
   - 引入 `Spring Boot Actuator`。
   - 自定义 Metrics：Agent 执行次数、成功率、延迟。
   - 访问 `/actuator/health` 和 `/actuator/metrics`，查看：
     - 当前消息队列堆积情况
     - 数据库连接池状态
     - Agent 执行统计

5. **性能优化：**
   - 虚拟线程 + Agent 并发：同时运行 100 个 Agent 实例。
   - 批量处理：Agent 每次处理 10 条消息，减少数据库往返。
   - 连接池调优：HikariCP `maximum-pool-size=20`。

**✅ 验收标准：**

- 即使爬虫一秒钟发 1000 个请求，Agent 处理逻辑依然严格按照限流策略运行，没有 API 报错。
- 关机时没有异常中断，所有正在执行的 Agent 完成当前步骤。
- Agent 执行失败后能自动重试，或进入死信队列。
- 能通过 Actuator 监控到 Agent 的执行指标。

**📚 学习重点：**

- 分布式限流算法（令牌桶、漏桶）
- 优雅停机最佳实践
- Agent 错误处理与重试
- Spring Boot Actuator 自定义 Metrics

------

### 🎯 最终交付成果

完成 5 个 Sprint 后，你将拥有：

1. **高并发爬虫**：基于虚拟线程，单机支持 10,000+ req/min
2. **可靠消息队列**：RabbitMQ + Manual ACK，零数据丢失
3. **智能 Agent 系统**：Spring AI + Agent 编排，多智能体协作
4. **向量检索能力**：PostgreSQL + pgvector，语义搜索
5. **生产级系统**：限流、监控、优雅停机，可水平扩展

**技术栈总结：**

- **核心框架**：Java 21 + Spring Boot 4.0.3 + Spring AI 1.0.2
- **AI 服务**：Spring AI Alibaba (DashScope/通义千问)
- **Agent 框架**：Ron-AI-Agent (ReAct 模式)
- **数据存储**：PostgreSQL 16 + pgvector, Redis 7.x
- **消息队列**：RabbitMQ 3.x
- **监控**：Spring Boot Actuator + Knife4j

------

### 🚀 下一步行动：你的第一个 Hello World

现在的任务有点多，我们聚焦在 **Sprint 1 的第一天**。

请按照以下格式创建你的 `docker-compose.yml` 文件（作为你的基础设施地基），保存并运行 `docker-compose up -d`：

```yaml
version: '3.8'
services:
  # 消息队列
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"   # 通信端口
      - "15672:15672" # 管理后台端口
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin

  # 向量数据库
  postgres:
    image: pgvector/pgvector:pg16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
      POSTGRES_DB: javainfohunter
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # 缓存与锁
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

**你是否已经安装好了 Docker？如果准备好了，我们就开始写第一行 Java 代码。**

------

### 📖 附录：关键技术概念速查

#### Virtual Threads（虚拟线程）
- **是什么**：JDK 21 引入的轻量级线程，由 JVM 管理，不绑定平台线程。
- **优势**：内存占用小（几 KB），可创建百万级，阻塞友好。
- **使用**：`Executors.newVirtualThreadPerTaskExecutor()`

#### Spring AI ChatClient
- **是什么**：Spring AI 的统一 LLM 调用 API。
- **使用**：
  ```java
  String response = chatClient.prompt()
      .system("你是一个资讯分析师")
      .user("总结以下新闻：{{text}}")
      .call()
      .content();
  ```

#### Agent 编排模式
- **ReAct**：思考（think）→ 行动（act）循环
- **Chain**：Agent 顺序执行，输出作为下一个的输入
- **Parallel**：多个 Agent 同时执行，结果聚合
- **Master-Worker**：主 Agent 分配任务，汇总结果

#### 向量相似度
- **是什么**：通过计算向量夹角余弦值，衡量文本相似度
- **应用**：相似新闻推荐、内容去重、语义搜索
- **SQL**：`ORDER BY embedding <=> '[0.1, 0.2, ...]'`

------

**祝你学习愉快！如果有任何问题，随时查阅 CLAUDE.md 和技术方案文档。**
