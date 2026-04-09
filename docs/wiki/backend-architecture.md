# JavaInfoHunter 后端架构

## 项目概述

JavaInfoHunter 是一个高性能分布式信息采集系统，基于 **Agent 编排** 作为核心智能处理模式，由 Spring AI 和阿里云通义千问驱动。系统采用微服务架构，各模块职责清晰、独立部署。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 核心语言（虚拟线程支持） |
| Spring Boot | 3.5.12 | 应用框架 |
| Spring AI | 1.1.2 | AI 抽象层 |
| Spring AI Alibaba | 1.1.2.0 | 阿里云通义千问集成 |
| PostgreSQL | 16+ | 主数据库（支持 pgvector） |
| RabbitMQ | Latest | 消息队列 |
| Redis | Latest | 缓存层 |
| Lombok | 1.18.36 | 代码生成 |
| Hutool | 5.8.38 | Java 工具库 |
| Rome | 1.19.0 | RSS/Atom 解析 |
| Flyway | Boot 管理 | 数据库版本迁移 |
| Springdoc OpenAPI | 2.8.6 | API 文档 |
| Knife4j | 4.5.0 | API 增强文档 |
| JaCoCo | 0.8.12 | 代码覆盖率 |

## 微服务架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL 数据库                              │
│   (rss_sources, raw_content, news, agent_executions)                 │
└─────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ JDBC
         ┌────────────────────┼────────────────────┐
         │                    │                    │
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  API 服务        │  │  爬虫服务        │  │  处理服务        │
│  (端口: 8080)    │  │  (端口: 8081)    │  │  (端口: 8082)    │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • REST API      │  │ • RSS 爬取      │  │ • AI 处理        │
│ • Swagger UI    │  │ • 内容提取      │  │ • Agent 编排     │
│ • Redis 缓存    │  │ • RabbitMQ 发布 │  │ • 消息消费       │
│ • 健康检查       │  │ • 定时任务      │  │ • 事务存储       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
                              ▲                    ▲
                              │                    │
                    ┌─────────────────┐
                    │    RabbitMQ     │
                    │   消息队列       │
                    │                 │
                    │  Exchange:      │
                    │  crawler.direct │
                    │                 │
                    │  Queue:         │
                    │  processor.raw  │
                    │  .content.queue │
                    └─────────────────┘
```

## 模块间通信架构

系统采用 **事件驱动架构**，模块间通过 RabbitMQ 消息队列进行异步通信：

1. **爬虫服务** 从 RSS 源抓取内容后，将原始内容消息发布到 RabbitMQ
2. **处理服务** 消费消息，使用 AI Agent 编排进行内容分析和处理
3. **API 服务** 通过 REST 接口提供查询服务，使用 Redis 缓存热数据
4. 所有服务共享同一个 PostgreSQL 数据库

### 数据流

```
RSS 源 → 爬虫服务 → RabbitMQ → 处理服务(AI Agent) → PostgreSQL
                                                    ↓
                                              API 服务 → 客户端
```

## Maven 多模块结构

```
JavaInfoHunter/                          # 父 POM (packaging=pom)
├── pom.xml                              # 父 POM，依赖管理
├── javainfohunter-ai-service/           # AI 服务模块 (Spring Boot Starter)
├── javainfohunter-api/                  # REST API 网关 (端口: 8080)
├── javainfohunter-crawler/              # RSS 爬虫服务 (端口: 8081)
├── javainfohunter-processor/            # AI 处理服务 (端口: 8082)
├── javainfohunter-e2e/                  # 端到端测试
├── scripts/                             # 部署脚本
└── docs/                                # 项目文档
```

### 依赖关系

- **ai-service** 作为共享模块，被 api、crawler、processor 依赖
- **crawler** 和 **processor** 之间不直接依赖，通过 RabbitMQ 通信
- **api** 仅读取数据库，不参与数据处理流程

## 环境要求

### 运行时依赖

| 组件 | 版本要求 | 用途 |
|------|---------|------|
| JDK | 21+ | 运行环境 |
| PostgreSQL | 16+ | 主数据库（需安装 pgvector 扩展） |
| RabbitMQ | 3.x | 消息队列 |
| Redis | 7.x | 缓存层 |

### 环境变量

```bash
# 数据库
DB_HOST=localhost
DB_PORT=5432
DB_NAME=javainfohunter
DB_USERNAME=admin
DB_PASSWORD=<your_password>

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=25672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=<your_password>

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<your_password>

# AI (处理服务需要)
DASHSCOPE_API_KEY=<your_api_key>
```

## 构建与运行

### 构建项目

```bash
# 构建整个项目
./mvnw.cmd clean package

# 构建特定模块
./mvnw.cmd clean package -pl javainfohunter-ai-service
./mvnw.cmd clean package -pl javainfohunter-api

# 跳过测试构建
./mvnw.cmd clean package -DskipTests
```

### 运行服务

```bash
# 启动 API 服务 (端口 8080)
./mvnw.cmd spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=develop

# 启动爬虫服务 (端口 8081)
./mvnw.cmd spring-boot:run -pl javainfohunter-crawler -Dspring-boot.run.profiles=develop

# 启动处理服务 (端口 8082)
./mvnw.cmd spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=develop
```

### 测试

```bash
# 运行所有测试
./mvnw.cmd test

# 运行特定模块测试
./mvnw.cmd test -pl javainfohunter-ai-service

# 运行单个测试类
./mvnw.cmd test -Dtest=BaseAgentTest -pl javainfohunter-ai-service

# 生成覆盖率报告
./mvnw.cmd clean verify -P coverage
```

## 环境配置 Profile

| Profile | 用途 | 说明 |
|---------|------|------|
| `dev` / `develop` | 本地开发 | 开启调试日志 |
| `staging` | 预发布 | 接近生产环境配置 |
| `prod` | 生产环境 | 优化性能配置 |

## 并发与安全配置

| 参数 | 值 | 说明 |
|------|-----|------|
| 最大并发 Worker 数 | 100 | Agent 协调器上限 |
| Worker 队列大小 | 500 | 等待队列容量 |
| Worker 超时 | 30 秒 | 单个 Worker 执行超时 |
| Master Agent 超时 | 300 秒 | 协调 Agent 执行超时 |
| Agent ID 正则 | `^[a-zA-Z0-9-_]{1,64}$` | 防止注入攻击 |
| Agent 最大步数 | 10 | 防止无限循环 |
| RabbitMQ prefetch | 10 | 消费者预取消息数 |
| RabbitMQ 并发 | 5-20 | 消费者并发数 |
| Hikari 最大连接数 | 10 | 数据库连接池上限 |
