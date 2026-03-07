# JavaInfoHunter

<div align="center">

**高性能分布式信息采集系统**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

基于 **Agent 编排** 构建 Powered by Spring AI 和阿里云 DashScope

</div>

---

## 项目简介

JavaInfoHunter 是一个分布式信息采集与处理系统，利用 **AI Agent 编排** 技术实现智能内容分析。系统自动爬取 RSS 订阅源，通过多个 AI Agent 处理内容，并提供 RESTful API 进行数据访问。

### 核心特性

- **Agent 编排** - 支持 Chain、Parallel、Master-Worker 三种协作模式
- **高并发** - JDK 21 虚拟线程支持可扩展的异步处理
- **向量搜索** - 基于 pgvector 的语义相似度搜索
- **消息队列** - RabbitMQ 可靠异步处理
- **全面测试** - 80%+ 覆盖率，包含单元、集成和 E2E 测试

---

## 系统架构

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│  RSS 订阅源      │────▶│   爬虫模块    │────▶│   RabbitMQ      │
└─────────────────┘     └──────────────┘     └────────┬────────┘
                                                      │
                                                      ▼
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│  REST API       │◀────│  PostgreSQL  │◀────│   处理器模块     │
└─────────────────┘     └──────────────┘     └─────────────────┘
                                                      │
                                              ┌───────┴───────────┐
                                              │   AI Agent 层     │
                                              │  - 内容分析       │
                                              │  - 摘要生成       │
                                              │  - 内容分类       │
                                              └───────────────────┘
```

### Maven 模块

| 模块 | 说明 |
|------|------|
| `javainfohunter-ai-service` | 核心 AI 服务，包含 Agent 编排框架 |
| `javainfohunter-crawler` | RSS 订阅源爬虫，支持定时任务 |
| `javainfohunter-processor` | 基于 AI Agent 的内容处理器 |
| `javainfohunter-api` | 对外 REST API 接口 |
| `javainfohunter-e2e` | 端到端集成测试 |

---

## 快速开始

### 前置要求

- **JDK 21+**
- **Maven 3.9+**
- **PostgreSQL 16+**（需安装 pgvector 扩展）
- **RabbitMQ 3.12+**
- **Redis 7+**（可选，用于缓存）

### 环境变量

```bash
# 必需配置
export DASHSCOPE_API_KEY=your-api-key-here
export DB_USERNAME=postgres
export DB_PASSWORD=your-password

# 可选配置（本地开发）
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export REDIS_HOST=localhost
```

### 构建与运行

```bash
# 克隆仓库
git clone https://github.com/ReganQing/java-info-hunter.git
cd java-info-hunter

# 构建项目
./mvnw clean package

# 运行爬虫模块
./mvnw spring-boot:run -pl javainfohunter-crawler

# 运行处理器模块（新终端）
./mvnw spring-boot:run -pl javainfohunter-processor

# 运行 API 模块（新终端）
./mvnw spring-boot:run -pl javainfohunter-api
```

### Docker Compose（推荐本地开发）

```bash
docker-compose up -d postgres rabbitmq redis
```

---

## Agent 编排

JavaInfoHunter 的核心是 **Agent 编排框架**。

### Agent 层级结构

```
BaseAgent（状态与生命周期管理）
    ↓
ReActAgent（思考-行动循环）
    ↓
ToolCallAgent（Spring AI ChatClient 集成）
    ↓
Specialized Agents（业务逻辑）
```

### 预置 Agent

| Agent ID | 描述 |
|----------|------|
| `crawler-agent` | 网页爬取与内容提取 |
| `analysis-agent` | 内容深度分析 |
| `summary-agent` | 文本摘要生成 |
| `classification-agent` | 内容分类与标签 |

### 协作模式

```java
// Chain 模式 - 顺序执行
CoordinationResult result = taskCoordinator.executeChain(
    "处理内容",
    List.of("analysis-agent", "summary-agent", "classification-agent")
);

// Parallel 模式 - 并行执行
CoordinationResult result = taskCoordinator.executeParallel(
    "分析内容",
    List.of("sentiment-agent", "topic-agent", "keyword-agent")
);
```

---

## API 文档

API 模块启动后，访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

### 主要接口

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/v1/news` | 获取处理后的新闻列表 |
| GET | `/api/v1/news/{id}` | 根据 ID 获取新闻详情 |
| POST | `/api/v1/sources` | 添加 RSS 订阅源 |
| GET | `/api/v1/sources` | 获取 RSS 订阅源列表 |
| GET | `/actuator/health` | 健康检查 |

---

## 数据库设计

### 核心数据表

| 表名 | 用途 |
|------|------|
| `rss_sources` | RSS 订阅源配置 |
| `raw_content` | 爬取的原始内容（含向量） |
| `news` | 处理并丰富后的内容 |
| `agent_executions` | Agent 执行追踪记录 |

### 数据库迁移

```bash
# 查看迁移状态
./mvnw flyway:info

# 执行迁移
./mvnw flyway:migrate
```

---

## 测试

```bash
# 运行所有测试
./mvnw test

# 运行特定模块测试
./mvnw test -pl javainfohunter-ai-service

# 生成覆盖率报告
./mvnw test jacoco:report

# 运行 E2E 测试
./mvnw test -pl javainfohunter-e2e
```

### 覆盖率要求

- 最低 **80%** 代码覆盖率
- 所有业务逻辑需单元测试
- API 接口需集成测试
- 关键流程需 E2E 测试

---

## 技术栈

### 核心框架
- **Java 21** - 虚拟线程、模式匹配、Record
- **Spring Boot 3.3.5** - 应用框架
- **Spring AI 1.0.2** - AI 抽象层
- **Maven** - 依赖管理

### AI 与机器学习
- **Spring AI Alibaba** - 阿里云通义千问集成
- **pgvector** - 向量相似度搜索

### 数据与消息
- **PostgreSQL 16** - 主数据库
- **Flyway** - 数据库迁移
- **RabbitMQ** - 消息队列
- **Redis** - 缓存层

### 可观测性
- **Spring Actuator** - 健康检查
- **Micrometer** - 指标收集
- **Prometheus** - 指标聚合
- **Zipkin** - 分布式追踪

---

## 配置说明

### 环境配置

```yaml
spring:
  profiles:
    active: develop  # develop | staging | prod
```

### 主要配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | 基础配置 |
| `application-develop.yml` | 开发环境设置 |
| `application-staging.yml` | 预发布环境 |
| `application-prod.yml` | 生产环境设置 |

---

## 开发指南

### 项目结构

```
JavaInfoHunter/
├── javainfohunter-ai-service/      # AI 服务模块
│   └── src/main/java/.../ai/
│       ├── agent/                  # Agent 框架
│       ├── tool/                   # 工具系统
│       └── service/                # AI 服务
├── javainfohunter-crawler/         # 爬虫模块
├── javainfohunter-processor/       # 处理器模块
├── javainfohunter-api/             # API 模块
├── javainfohunter-e2e/             # E2E 测试
├── docs/                           # 文档
└── pom.xml                         # 父 POM
```

### 添加自定义 Agent

```java
@Component
public class MyCustomAgent extends ToolCallAgent {

    @Override
    protected AgentResponse executeLogic(AgentContext context) {
        // 你的 Agent 逻辑
        return AgentResponse.success(result);
    }
}

// 注册 Agent
@Configuration
public class AgentConfig {
    @PostConstruct
    public void registerAgents(AgentManager agentManager) {
        agentManager.registerAgent("my-agent", myCustomAgent);
    }
}
```

### 添加自定义工具

```java
@Component
public class MyTools {

    @Tool(name = "my-tool", description = "执行有用的操作")
    public String myTool(
        @ToolParam(name = "input", description = "输入数据") String input
    ) {
        // 工具实现
        return "result";
    }
}
```

---

## 文档

- [技术架构](docs/技术方案.md)
- [数据库设计](docs/数据库设计说明.md)
- [数据传输架构](docs/数据传输架构设计.md)
- [CI/CD 流程](docs/ci-cd-guide.md)
- [AI 服务使用指南](javainfohunter-ai-service/USAGE.md)

---

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 先编写测试 (TDD)
4. 提交更改 (`git commit -m 'feat: 添加新特性'`)
5. 推送到分支 (`git push origin feature/amazing-feature`)
6. 创建 Pull Request

---

## 开源协议

本项目基于 MIT 协议开源 - 详见 [LICENSE](LICENSE) 文件

---

## 致谢

- [Spring AI](https://spring.io/projects/spring-ai) - AI 集成框架
- [阿里云 DashScope](https://dashscope.aliyun.com/) - LLM API 提供商
- [pgvector](https://github.com/pgvector/pgvector) - 向量相似度搜索

---

<div align="center">

**Made with ❤️ by JavaInfoHunter Team**

[English](README.md) | 简体中文

</div>
