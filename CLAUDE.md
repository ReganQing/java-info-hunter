# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaInfoHunter is a high-performance distributed information collection system built with **Java 21** and **Spring Boot 3.3.5**. The project uses **Agent Orchestration** as its core intelligent processing pattern, powered by Spring AI and Alibaba DashScope.

The system follows a microservices architecture with clear separation of concerns across multiple modules.

## Project Structure

### Maven Multi-Module Architecture

```
JavaInfoHunter/                      # 父 POM (packaging=pom)
├── pom.xml                          # 父 POM，依赖管理
├── javainfohunter-ai-service/       # AI 服务模块 (独立、可复用的 Spring Boot Starter)
│   ├── pom.xml
│   └── src/main/java/.../ai/
│       ├── agent/                   # Agent 框架核心
│       │   ├── core/                # BaseAgent, ReActAgent, ToolCallAgent
│       │   ├── coordinator/         # AgentManager, TaskCoordinator
│       │   │   └── impl/            # CollaborationPattern, CoordinationResult
│       │   └── specialized/         # 预置 Agent
│       │       ├── CrawlerAgent     # 网页爬取
│       │       ├── AnalysisAgent    # 内容分析
│       │       ├── SummaryAgent     # 文本摘要
│       │       ├── ClassificationAgent # 内容分类
│       │       ├── CoordinatorAgent # Master-Worker 协调器
│       │       └── TrendingCoordinatorAgent # 趋势分析协调器
│       ├── tool/                    # 工具系统
│       │   ├── annotation/          # @Tool, @ToolParam
│       │   ├── core/                # ToolRegistry, ToolManager
│       │   └── impl/                # 预置工具
│       ├── service/                 # 服务层
│       ├── entity/                  # JPA 实体 (共享)
│       ├── repository/              # 数据访问层 (共享)
│       ├── autoconfigure/           # Spring Boot 自动配置
│       └── config/                  # Agent 配置类
├── javainfohunter-api/              # REST API 网关 (端口: 8080)
│   ├── src/main/java/.../api/
│   │   ├── controller/              # REST 控制器
│   │   ├── service/                 # 业务服务层
│   │   └── redis/                   # Redis 缓存服务
│   └── src/main/resources/
│       └── application.yml          # API 配置
├── javainfohunter-crawler/          # RSS 爬虫服务 (端口: 8081)
│   ├── src/main/java/.../crawler/
│   │   ├── publisher/               # RabbitMQ 消息发布
│   │   ├── service/                 # 爬虫服务
│   │   └── scheduler/               # 定时任务
│   └── src/main/resources/
│       └── application.yml          # Crawler 配置
├── javainfohunter-processor/        # AI 处理服务 (端口: 8082)
│   ├── src/main/java/.../processor/
│   │   ├── consumer/                # RabbitMQ 消息消费
│   │   └── service/                 # 处理服务
│   └── src/main/resources/
│       └── application.yml          # Processor 配置
├── javainfohunter-e2e/              # 端到端测试
├── scripts/                         # 部署脚本
│   ├── start-all.bat/sh             # 启动所有服务
│   └── stop-all.bat/sh              # 停止所有服务
└── docs/                            # 项目文档
    ├── DEPLOYMENT.md                # 部署指南
    ├── 技术方案.md                  # 系统技术方案
    ├── 数据传输架构设计.md          # 消息队列架构
    └── 数据库设计说明.md            # 数据库表结构
```

### 模块间通信架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        PostgreSQL Database                       │
│  (rss_sources, raw_content, news, agent_executions)             │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  API Service    │  │  Crawler Svc    │  │  Processor Svc  │
│  (Port 8080)    │  │  (Port 8081)    │  │  (Port 8082)    │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • REST API      │  │ • RSS Crawling  │  │ • AI Processing │
│ • Swagger UI    │  │ • Content Pub   │  │ • Agent Orchest.│
│ • Redis Cache   │  │ • Scheduled     │  │ • Message Cons. │
└─────────────────┘  └─────────────────┘  └─────────────────┘
                              ▲
                              │
                    ┌─────────────────┐
                    │    RabbitMQ     │
                    │  Message Queue  │
                    └─────────────────┘
```

## Build Commands

```bash
# 构建整个项目
mvnw.cmd clean package

# 构建特定模块
mvnw.cmd clean package -pl javainfohunter-ai-service
mvnw.cmd clean package -pl javainfohunter-api

# 运行测试
mvnw.cmd test

# 运行测试并生成覆盖率报告
mvnw.cmd test jacoco:report

# 运行单个测试类
mvnw.cmd test -Dtest=BaseAgentTest -pl javainfohunter-ai-service

# 查看依赖树
mvnw.cmd dependency:tree
```

## Running the Services

### 使用部署脚本 (推荐)

```bash
# 启动所有服务
./scripts/start-all.sh     # Linux/Mac
scripts\start-all.bat      # Windows

# 停止所有服务
./scripts/stop-all.sh      # Linux/Mac
scripts\stop-all.bat       # Windows
```

### 手动启动单个服务

```bash
# API Service (Port 8080)
mvnw.cmd spring-boot:run -pl javainfohunter-api

# Crawler Service (Port 8081)
mvnw.cmd spring-boot:run -pl javainfohunter-crawler

# Processor Service (Port 8082)
mvnw.cmd spring-boot:run -pl javainfohunter-processor

# 使用特定 profile
mvnw.cmd spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=develop
```

## Architecture Overview

### Agent Orchestration Pattern (核心架构)

**设计理念：** 所有复杂的 AI 推理、多步处理、工具调用任务都应使用 Agent 编排，而非传统 Service 层代码。

**三层 Agent 架构：**
```
BaseAgent (状态管理、生命周期)
    ↓
ReActAgent (think-act 循环)
    ↓
ToolCallAgent (Spring AI ChatClient、工具回调)
    ↓
Specialized Agents (业务 Agent)
```

**四种协作模式：**

1. **Chain 模式** - 顺序执行，输出作为下一个的输入
   ```java
   taskCoordinator.executeChain("任务", List.of("agent1", "agent2", "agent3"));
   ```

2. **Parallel 模式** - 并行执行，使用虚拟线程（性能提升 3 倍）
   ```java
   taskCoordinator.executeParallel("任务", List.of("agent1", "agent2", "agent3"));
   ```

3. **Master-Worker 模式** - 主从协作（已实现）
   ```java
   taskCoordinator.executeMasterWorker("任务", "coordinator-agent", List.of("worker1", "worker2"));
   ```

4. **自定义协调模式** - 使用 CoordinatorAgent 实现复杂工作流

### 关键技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.5 | 核心框架 |
| Java | 21 | 虚拟线程支持 |
| Spring AI | 1.0.2 | AI 抽象层 |
| Spring AI Alibaba | 1.0.0-M2.1 | 阿里云通义千问 |
| PostgreSQL | Latest | 主数据库，支持 pgvector |
| RabbitMQ | Latest | 消息队列 |
| Redis | Latest | 缓存层 |
| Lombok | 1.18.36 | 代码生成 |
| Hutool | 5.8.38 | Java 工具库 |

### 安全与性能特性

**并发控制：**
- 最大并发 Worker 数: 100
- Worker 队列大小: 500
- Worker 超时: 30 秒
- Master Agent 超时: 300 秒

**Agent ID 验证：**
- 正则: `^[a-zA-Z0-9-_]{1,64}$`
- 防止注入攻击

**线程安全：**
- `ConcurrentHashMap` 用于 Agent 和 Tool 注册
- `volatile AgentState` 确保状态可见性
- `AtomicInteger` 用于步数计数
- `CopyOnWriteArrayList` 用于消息历史

## Agent Orchestration Rules (CRITICAL)

### General Principle
**使用 Agent 编排模式实现所有 AI 驱动的复杂业务逻辑。**

### When to Use Agent Orchestration

**✅ USE Agent Orchestration for:**
1. **Content Analysis**: 需要 AI 推理、情感分析、主题提取
2. **Multi-step Processing**: 顺序决策工作流 (Chain 模式)
3. **Parallel Analysis**: 可并行运行的独立分析任务 (Parallel 模式)
4. **Complex Decision Making**: 需要规划、委派和结果聚合 (Master-Worker 模式)
5. **Tool-Intensive Tasks**: 需要多次工具调用的任务
6. **AI-Native Features**: 摘要、分类、推荐等

**❌ DO NOT USE Agent Orchestration for:**
1. 简单 CRUD 操作
2. 不需要 AI 推理的直接数据库查询
3. 不涉及 AI 的静态业务规则
4. 性能关键型简单操作 (< 10ms)

### Agent Implementation Pattern

1. **继承合适的基类**：
   - 需要 AI 推理 + 工具调用 → `ToolCallAgent`
   - 只需要 AI 推理 → `ReActAgent`
   - 需要协调其他 Agent → `CoordinatorAgent`

2. **注册到 AgentManager**：
   ```java
   @Configuration
   public class Config {
       @Autowired
       private AgentManager agentManager;

       @PostConstruct
       public void register() {
           agentManager.registerAgent("my-agent", myAgent);
       }
   }
   ```

3. **使用 TaskCoordinator 编排**：
   ```java
   @Autowired
   private TaskCoordinator taskCoordinator;

   // Chain 模式
   CoordinationResult result = taskCoordinator.executeChain(
       "任务描述",
       List.of("agent1", "agent2", "agent3")
   );

   // Parallel 模式
   result = taskCoordinator.executeParallel(
       "任务描述",
       List.of("agent1", "agent2", "agent3")
   );

   // Master-Worker 模式
   result = taskCoordinator.executeMasterWorker(
       "任务描述",
       "coordinator-agent",
       List.of("worker1", "worker2", "worker3")
   );
   ```

## Module Details

### javainfohunter-ai-service (核心 AI 模块)

**核心特性：**
- ✅ **独立模块**: 可作为 Maven 依赖引入其他项目
- ✅ **Spring Boot Starter**: 遵循自动配置规范，引入即用
- ✅ **Agent 编排**: 提供完整的 Agent 协作框架
- ✅ **线程安全**: 使用 JDK 21 虚拟线程，支持高并发

**使用方式：**
```xml
<dependency>
    <groupId>com.ron</groupId>
    <artifactId>javainfohunter-ai-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**核心组件：**

| 组件 | 路径 | 职责 |
|------|------|------|
| **Agent 核心** | `agent/core/` | BaseAgent, ReActAgent, ToolCallAgent |
| **协调器** | `agent/coordinator/` | AgentManager, TaskCoordinator, CollaborationPattern |
| **工具系统** | `tool/` | @Tool 注解, ToolRegistry, ToolManager |
| **预置 Agent** | `agent/specialized/` | 6 个预置业务 Agent |
| **预置工具** | `tool/impl/` | HtmlParserTool, TextSummarizationTool |
| **服务层** | `service/` | ChatService, EmbeddingService, AgentService |
| **实体** | `entity/` | JPA 实体 (跨模块共享) |
| **仓储** | `repository/` | Spring Data JPA 仓储 |
| **自动配置** | `autoconfigure/` | AiServiceAutoConfiguration |

### javainfohunter-api (REST API 网关)

**端口**: 8080

**主要功能：**
- REST API 接口
- Swagger/OpenAPI 文档
- Redis 缓存
- 健康检查 (Actuator)

**主要控制器：**
- `NewsController` - 新闻查询、搜索、趋势
- `RssSourceController` - RSS 源管理、手动触发爬取
- `AgentController` - Agent 执行监控
- `AdminController` - 系统管理

### javainfohunter-crawler (RSS 爬虫服务)

**端口**: 8081

**主要功能：**
- 定时 RSS 爬取
- 内容去重 (SHA-256 哈希)
- RabbitMQ 消息发布
- 错误处理和重试

**主要组件：**
- `CrawlScheduler` - 定时任务调度
- `ContentPublisher` - 消息发布
- `FeedService` - RSS 解析 (Rome 库)
- `ContentExtractor` - HTML 内容提取

### javainfohunter-processor (AI 处理服务)

**端口**: 8082

**主要功能：**
- RabbitMQ 消息消费
- AI Agent 编排
- 内容路由和处理
- 事务性数据存储

**主要组件：**
- `RawContentConsumer` - 消息消费
- `ContentRoutingService` - 内容路由
- `TransactionalStoreService` - 事务存储

## 预置 Agent 列表

| Agent ID | 类名 | 功能描述 |
|----------|------|---------|
| `crawler-agent` | CrawlerAgent | 网页爬取和内容提取 |
| `analysis-agent` | AnalysisAgent | 内容深度分析 |
| `summary-agent` | SummaryAgent | 文本摘要生成 |
| `classification-agent` | ClassificationAgent | 内容分类和标签 |
| `coordinator-agent` | CoordinatorAgent | Master-Worker 协调器 |
| `trending-coordinator-agent` | TrendingCoordinatorAgent | 趋势分析协调器 |

## Environment Variables

### 必需的环境变量

```bash
# Alibaba DashScope API (必需，用于 AI 功能)
export DASHSCOPE_API_KEY=your-api-key-here

# Database (必需，用于数据持久化)
export DB_USERNAME=postgres
export DB_PASSWORD=your-password

# RabbitMQ (可选，有默认值)
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=admin
export RABBITMQ_PASSWORD=admin

# Redis (可选，API 模块使用)
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
```

### 配置文件示例

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-max

  datasource:
    url: jdbc:postgresql://localhost:5432/javainfohunter
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:admin}

javainfohunter:
  ai:
    enabled: true
    agent:
      max-steps: 10
      timeout: 300
    tool:
      auto-discovery: true
```

## Database Quick Reference

### 核心表

| 表名 | 用途 | 主要字段 |
|------|------|---------|
| **rss_sources** | RSS 订阅源 | url, name, category, is_active, last_crawled_at |
| **raw_content** | 原始内容 | rss_source_id, title, content_hash, processing_status, embedding |
| **news** | 处理后新闻 | raw_content_id, summary, topics, keywords, sentiment, importance_score |
| **agent_executions** | Agent 执行记录 | agent_id, agent_type, execution_id, status, tools_used, duration_ms |

### 数据库优化

**性能优化：**
- Agent 执行记录使用 PostgreSQL `text[]` 数组存储工具列表，避免额外关联表
- 向量搜索使用 IVFFlat 索引
- 全文搜索使用 GIN 索引

```sql
-- 向量相似度搜索
SELECT * FROM raw_content
ORDER BY embedding <=> '[0.1, 0.2, ...]'
LIMIT 10;

-- 全文搜索
SELECT * FROM news
WHERE to_tsvector('english', title || ' ' || summary)
      @@ to_tsquery('english', 'machine & learning')
ORDER BY ts_rank(...) DESC
LIMIT 20;
```

### 数据库迁移

```bash
# 查看迁移状态
mvnw.cmd flyway:info

# 手动执行迁移
mvnw.cmd flyway:migrate

# 修复迁移状态（仅开发环境）
mvnw.cmd flyway:repair
```

## Quick Reference

### 关键文档
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) - 部署指南和 Docker Compose 配置
- [USAGE.md](javainfohunter-ai-service/USAGE.md) - AI 服务模块使用指南
- [README.md](javainfohunter-ai-service/README.md) - 模块说明文档
- [技术方案.md](docs/技术方案.md) - 完整的系统技术方案
- [数据传输架构设计.md](docs/数据传输架构设计.md) - 消息队列架构设计
- [数据库设计说明.md](docs/数据库设计说明.md) - 数据库表结构和索引设计
- [数据库使用指南.md](docs/数据库使用指南.md) - Repository 使用示例和最佳实践

### CI/CD

- **JaCoCo 覆盖率**: 80% 最低覆盖率要求
- **GitHub Actions**: 自动构建、测试、部署
- **Testcontainers**: 集成测试使用真实容器

### API 文档

启动 API 服务后访问：
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator Health: http://localhost:8080/actuator/health

### 常用命令

```bash
# 启动所有服务
./scripts/start-all.sh

# 停止所有服务
./scripts/stop-all.sh

# 查看日志
tail -f javainfohunter-crawler/logs/javainfohunter-crawler.log

# 构建并跳过测试
mvnw.cmd clean package -DskipTests

# 运行特定测试
mvnw.cmd test -Dtest=CoordinatorAgentTest
```
