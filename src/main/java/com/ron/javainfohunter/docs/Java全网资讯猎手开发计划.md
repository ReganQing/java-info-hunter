这是一个非常扎实的工程化实战计划。为了让你在构建**“Java 版全网资讯猎手”**的过程中，既能产出代码，又能深刻理解架构概念，我将这个计划设计为 **4 个冲刺（Sprints）**，每个 Sprint 周期建议为 **1-2 周**。

我们将采用 **"MVP (Minimum Viable Product) + 迭代"** 的模式，从一个简单的脚本进化为高可用的分布式系统。

------

### 🛠️ 前置准备 (环境搭建)

在开始 Sprint 1 之前，请确保你的开发环境就绪：

- **JDK:** 必须是 **JDK 21** (为了使用 Virtual Threads)。
- **IDE:** IntelliJ IDEA (Community 版即可)。
- **构建工具:** Maven 或 Gradle (推荐 Maven，依赖管理直观)。
- **基础设施:** 安装 **Docker Desktop** (这是为了快速启动 Redis, RabbitMQ, PostgreSQL)。

------

### 🗓️ Sprint 1: 突破单机极限 —— 虚拟线程与爬虫核心

**核心目标：** 理解 JDK 21 虚拟线程如何解决“IO 阻塞”问题，实现单机极高吞吐的抓取。

**任务分解：**

1. **项目初始化：**
   - 创建一个 Spring Boot 3.2+ 项目。
   - 在 `application.properties` 中开启魔法开关：`spring.threads.virtual.enabled=true`。
2. **构建 HTTP 客户端：**
   - 使用 Java 11 原生 `java.net.http.HttpClient`。
   - **关键点：** 封装一个工具类，配置连接超时（ConnectTimeout）和读取超时（ReadTimeout）。如果不配超时，网络波动会把你的线程池拖死。
3. **编写爬虫 Service：**
   - 目标：并发抓取 100 个 RSS XML 或 网页 URL。
   - 使用 `Jsoup` 解析 HTML 提取正文。
4. **并发改造 (The "Aha!" Moment)：**
   - **V1 (传统版)：** 用 `parallelStream()` 跑，观察速度。
   - **V2 (虚拟线程版)：** 使用 `Executors.newVirtualThreadPerTaskExecutor()` 提交任务。
   - **验证：** 在日志中打印 `Thread.currentThread().toString()`，你会看到类似 `VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1`，证明你成功了。

**✅ 验收标准：**

- 能在一个 Main 方法中，10 秒内完成 500 个模拟 URL 的请求。
- 通过 JVisualVM 或 JConsole 看到系统平台线程数保持稳定（没有飙升到几百个）。

------

### 🗓️ Sprint 2: 架构解耦 —— 消息队列与可靠性

**核心目标：** 引入 RabbitMQ，防止爬虫太快把后续处理压垮，学习“生产者-消费者”模式。

**任务分解：**

1. **基础设施搭建：**
   - 编写 `docker-compose.yml`，启动 RabbitMQ 管理控制台。
2. **定义消息契约 (DTO)：**
   - 创建一个 Java Record `RawContentEvent(String url, String html, long timestamp)`。
3. **生产者开发 (Crawler)：**
   - 改造 Sprint 1 的代码。抓取成功后，不再直接打印，而是通过 `RabbitTemplate` 发送到 `crawler.exchange`。
4. **消费者开发 (Processor)：**
   - 编写一个 `@RabbitListener` 监听队列。
   - **核心挑战：** 实现 **Manual ACK (手动确认)**。
   - *代码逻辑：* `try { 处理业务(); channel.basicAck(); } catch { channel.basicNack(); }`。这是保证**数据不丢**的关键。

**✅ 验收标准：**

- 启动爬虫疯狂发送 1000 条消息。
- 启动消费者服务，看到控制台一条条平稳处理，没有报错。
- 手动停止消费者服务，再重启，确认未处理的消息没有丢失。

------

### 🗓️ Sprint 3: 注入智能 —— AI 集成与向量数据库

**核心目标：** 集成 LLM 进行非结构化数据清洗，并学习数据库连接池的调优。

**任务分解：**

1. **基础设施搭建：**
   - 在 `docker-compose.yml` 中添加 **PostgreSQL**，并启用 `pgvector` 插件。
2. **AI 服务集成 (LangChain4j)：**
   - 引入 `langchain4j` 依赖。
   - 定义 Prompt Template：“你是一个资讯分析师，请总结以下新闻内容：{{text}}”。
   - **成本控制：** 开发阶段建议使用 **Ollama** (本地运行 DeepSeek 或 Llama3) 替代 OpenAI，省钱！
3. **数据库设计与连接池调优：**
   - 设计表结构：`news (id, summary_text, embedding_vector)`。
   - 配置 **HikariCP**。
   - **实验：** 故意把 `maximum-pool-size` 设置为 1，然后并发 50 个请求，观察会发生什么（报错：Connection is not available），从而理解连接池的作用。
4. **完整链路打通：**
   - 消费者收到 HTML -> 调用 AI 总结 -> 调用 Embedding 模型向量化 -> 写入 Postgres。

**✅ 验收标准：**

- 数据库里成功存入了新闻的摘要和向量数据。
- 通过日志看到 HikariCP 的连接复用情况。

------

### 🗓️ Sprint 4: 生产级治理 —— 限流与高可用

**核心目标：** 解决“分布式环境下资源竞争”问题，让系统坚不可摧。

**任务分解：**

1. **Redis 分布式限流：**
   - 场景：LLM API 限制每分钟只能调用 60 次。
   - 实现：使用 Redis 的 `INCR` 和 `EXPIRE` 命令，或者使用 **Redisson** 的 `RRateLimiter` 实现分布式限流。如果超限，消费者线程休眠或重试。
2. **优雅停机 (Graceful Shutdown)：**
   - 配置 Spring Boot 的 `server.shutdown=graceful`。
   - **测试：** 在消费者处理任务时按 `Ctrl+C`，观察日志，系统应该会等待当前任务处理完才关闭，而不是直接杀掉进程。
3. **可观测性 (监控)：**
   - 引入 `Spring Boot Actuator`。
   - 访问 `/actuator/health` 和 `/actuator/metrics`，查看当前的消息队列堆积情况和数据库连接池状态。

**✅ 验收标准：**

- 即使爬虫一秒钟发 1000 个请求，后端的 AI 处理逻辑依然严格按照每秒 1 个（或你设定的限制）的速度运行，没有 API 报错。
- 关机时没有抛出异常中断。

------

### 🚀 下一步行动：你的第一个 Hello World

现在的任务有点多，我们聚焦在 **Sprint 1 的第一天**。

请按照以下格式创建你的 `docker-compose.yml` 文件（作为你的基础设施地基），保存并运行 `docker-compose up -d`：

YAML

```
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
      POSTGRES_DB: agent_db

  # 缓存与锁
  redis:
    image: redis:alpine
    ports:
      - "6379:6379"
```

**你是否已经安装好了 Docker？如果准备好了，我们就开始写第一行 Java 代码。**