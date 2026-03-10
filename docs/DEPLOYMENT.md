# JavaInfoHunter 部署指南

本文档详细说明 JavaInfoHunter 系统的部署步骤、配置要求和运维操作。

## 目录

- [系统架构](#系统架构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细部署步骤](#详细部署步骤)
- [生产环境部署](#生产环境部署)
- [运维操作](#运维操作)
- [故障排查](#故障排查)

---

## 系统架构

### 服务组件

```
┌─────────────────────────────────────────────────────────────────┐
│                     JavaInfoHunter 系统                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │     API      │   │   Crawler    │   │  Processor   │        │
│  │   :8080      │   │   (后台)      │   │   (后台)      │        │
│  │              │   │              │   │              │        │
│  │ REST API     │   │ RSS 爬虫     │   │ AI 内容处理  │        │
│  │ Swagger UI   │   │ 定时调度     │   │ Agent 编排   │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             ▼                                   │
│                    ┌──────────────┐                             │
│                    │  PostgreSQL  │                             │
│                    │   :5432      │                             │
│                    │  数据持久化   │                             │
│                    └──────────────┘                             │
│                             ▼                                   │
│                    ┌──────────────┐                             │
│                    │   RabbitMQ   │                             │
│                    │   :5672      │                             │
│                    │  消息队列     │                             │
│                    └──────────────┘                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 模块说明

| 模块 | 端口 | 功能 | 依赖 |
|------|------|------|------|
| **javainfohunter-api** | 8080 | REST API 服务 | PostgreSQL |
| **javainfohunter-crawler** | - | RSS 爬虫服务 | PostgreSQL, RabbitMQ |
| **javainfohunter-processor** | - | AI 内容处理服务 | PostgreSQL, RabbitMQ, DashScope API |

---

## 环境要求

### 基础设施

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| **Java** | 21 | 21 LTS | 运行时环境 |
| **PostgreSQL** | 14 | 16 | 数据库 |
| **RabbitMQ** | 3.12 | 3.13 | 消息队列 |
| **Maven** | 3.9 | 3.9.x | 构建工具 |

### 外部服务

| 服务 | 用途 | 获取方式 |
|------|------|---------|
| **阿里云 DashScope API** | AI 模型调用 (通义千问) | https://dashscope.aliyun.com |

---

## 快速开始

### 1. 前置条件检查

```bash
# 检查 Java 版本
java -version

# 检查 Maven
mvnw.cmd --version
```

### 2. 设置环境变量

**Windows (PowerShell):**
```powershell
$env:DB_PASSWORD = "your_password"
$env:DASHSCOPE_API_KEY = "sk-your-api-key"
```

**Windows (CMD):**
```cmd
set DB_PASSWORD=your_password
set DASHSCOPE_API_KEY=sk-your-api-key
```

**Linux/Mac:**
```bash
export DB_PASSWORD="your_password"
export DASHSCOPE_API_KEY="sk-your-api-key"
```

### 3. 启动依赖服务 (Docker)

```bash
# PostgreSQL
docker run -d \
  --name javainfohunter-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=javainfohunter \
  -p 5432:5432 \
  postgres:16-alpine

# RabbitMQ
docker run -d \
  --name javainfohunter-rabbitmq \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.12-management
```

### 4. 一键启动服务

**Windows:**
```cmd
cd scripts
start-all.bat
```

**Linux/Mac:**
```bash
cd scripts
chmod +x *.sh
./start-all.sh
```

### 5. 验证部署

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 访问 Swagger UI
# http://localhost:8080/swagger-ui.html
```

---

## 详细部署步骤

### 步骤 1: 数据库初始化

#### 1.1 创建数据库

```sql
-- 连接到 PostgreSQL
psql -U postgres

-- 创建数据库
CREATE DATABASE javainfohunter;

-- 创建用户 (可选)
CREATE USER javainfohunter WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE javainfohunter TO javainfohunter;
```

#### 1.2 Flyway 自动迁移

项目使用 Flyway 进行数据库版本管理，首次启动时会自动执行迁移。

```bash
# 查看迁移状态
mvnw.cmd flyway:info -pl javainfohunter-api

# 手动执行迁移
mvnw.cmd flyway:migrate -pl javainfohunter-api
```

### 步骤 2: 构建项目

```bash
# 完整构建（包含测试）
mvnw.cmd clean package

# 跳过测试快速构建
mvnw.cmd clean package -DskipTests

# 构建单个模块
mvnw.cmd clean package -pl javainfohunter-api
```

### 步骤 3: 配置文件

各服务的配置文件位于 `src/main/resources/application.yml`。

#### 3.1 API 服务配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/javainfohunter
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

  springdoc:
    swagger-ui:
      enabled: true
      path: /swagger-ui.html
```

#### 3.2 Crawler 服务配置

```yaml
javainfohunter:
  crawler:
    enabled: ${CRAWLER_ENABLED:true}
    scheduler:
      enabled: ${CRAWLER_SCHEDULER_ENABLED:true}
      fixed-rate: ${CRAWLER_FIXED_RATE:3600000}  # 1小时
```

#### 3.3 Processor 服务配置

```yaml
javainfohunter:
  processor:
    enabled: ${PROCESSOR_ENABLED:true}
    embedding:
      enabled: ${EMBEDDING_ENABLED:true}

spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-max
```

### 步骤 4: 启动服务

#### 4.1 API 服务

```bash
# Maven 直接运行
mvnw.cmd spring-boot:run -pl javainfohunter-api

# JAR 运行
java -jar javainfohunter-api/target/javainfohunter-api-0.0.1-SNAPSHOT.jar

# 指定 profile
mvnw.cmd spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=prod
```

#### 4.2 Crawler 服务

```bash
mvnw.cmd spring-boot:run -pl javainfohunter-crawler
```

#### 4.3 Processor 服务

```bash
mvnw.cmd spring-boot:run -pl javainfohunter-processor
```

---

## 生产环境部署

### Docker Compose 部署

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  # PostgreSQL 数据库
  postgres:
    image: postgres:16-alpine
    container_name: javainfohunter-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: javainfohunter
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # RabbitMQ 消息队列
  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: javainfohunter-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:-admin}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-admin}
    ports:
      - "5672:5672"
      - "15672:15672"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # API 服务
  api:
    build:
      context: .
      dockerfile: Dockerfile.api
    container_name: javainfohunter-api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-admin}
      - RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-admin}
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  # Crawler 服务
  crawler:
    build:
      context: .
      dockerfile: Dockerfile.crawler
    container_name: javainfohunter-crawler
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-admin}
      - RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-admin}
      - CRAWLER_SCHEDULER_ENABLED=true
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  # Processor 服务
  processor:
    build:
      context: .
      dockerfile: Dockerfile.processor
    container_name: javainfohunter-processor
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_PORT=5672
      - RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-admin}
      - RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-admin}
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres-data:
```

### 启动命令

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并清理数据
docker-compose down -v
```

---

## 运维操作

### 健康检查

```bash
# API 服务健康检查
curl http://localhost:8080/actuator/health

# 预期响应
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 日志查看

```bash
# 实时查看 API 日志
tail -f logs/api.log

# 查看最近 100 行
tail -n 100 logs/crawler.log

# 搜索错误日志
grep -i error logs/*.log
```

### 数据库操作

```bash
# 连接数据库
psql -h localhost -U postgres -d javainfohunter

# 常用查询
SELECT COUNT(*) FROM rss_sources;
SELECT COUNT(*) FROM raw_content;
SELECT COUNT(*) FROM news;
SELECT * FROM agent_executions ORDER BY start_time DESC LIMIT 10;
```

### RabbitMQ 管理

访问管理界面：http://localhost:15672 (admin/admin)

监控队列状态：
- `crawler.raw.content.queue` - 爬虫原始内容队列
- `processor.dead.letter.queue` - 死信队列

---

## 故障排查

### 问题 1: 服务启动失败

**症状:** 服务无法启动，报连接错误

**解决方案:**
```bash
# 检查 PostgreSQL 是否启动
docker ps | grep postgres
psql -h localhost -U postgres -c 'SELECT 1'

# 检查 RabbitMQ 是否启动
docker ps | grep rabbitmq
curl http://localhost:15672

# 检查端口占用
netstat -ano | findstr "8080"
netstat -ano | findstr "5432"
```

### 问题 2: AI 功能不可用

**症状:** Processor 服务报 API Key 错误

**解决方案:**
```bash
# 检查环境变量
echo $DASHSCOPE_API_KEY

# 重新设置
export DASHSCOPE_API_KEY=sk-your-api-key

# 重启 Processor 服务
./stop-all.sh
./start-all.sh
```

### 问题 3: 数据库迁移失败

**症状:** Flyway 迁移报错

**解决方案:**
```bash
# 查看迁移状态
mvnw.cmd flyway:info -pl javainfohunter-api

# 修复迁移状态 (仅开发环境)
mvnw.cmd flyway:repair -pl javainfohunter-api

# 重新迁移
mvnw.cmd flyway:migrate -pl javainfohunter-api
```

### 问题 4: 内存不足

**症状:** OutOfMemoryError

**解决方案:**
```bash
# 增加 JVM 堆内存
java -Xmx2g -Xms1g -jar javainfohunter-api.jar

# 或在 Maven 启动时设置
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Xms1g"
```

---

## 配置参数参考

### JVM 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `-Xmx` | 1g | 最大堆内存 |
| `-Xms` | 512m | 初始堆内存 |
| `-XX:+UseVirtualThreads` | - | 启用虚拟线程 (JDK 21+) |

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_USERNAME` | postgres | 数据库用户名 |
| `DB_PASSWORD` | postgres | 数据库密码 |
| `DASHSCOPE_API_KEY` | - | 阿里云 API Key (必需) |
| `RABBITMQ_HOST` | localhost | RabbitMQ 地址 |
| `RABBITMQ_PORT` | 5672 | RabbitMQ 端口 |
| `CRAWLER_ENABLED` | true | 是否启用爬虫 |
| `CRAWLER_FIXED_RATE` | 3600000 | 爬取间隔 (毫秒) |
| `PROCESSOR_ENABLED` | true | 是否启用处理器 |
| `EMBEDDING_ENABLED` | true | 是否启用向量化 |

---

## 脚本参考

项目提供了便捷的管理脚本：

| 脚本 | 平台 | 功能 |
|------|------|------|
| `scripts/start-all.bat` | Windows | 启动所有服务 |
| `scripts/stop-all.bat` | Windows | 停止所有服务 |
| `scripts/start-all.sh` | Linux/Mac | 启动所有服务 |
| `scripts/stop-all.sh` | Linux/Mac | 停止所有服务 |

---

## 附录

### 端口清单

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | API | REST API 服务 |
| 5432 | PostgreSQL | 数据库 |
| 5672 | RabbitMQ | AMQP 端口 |
| 15672 | RabbitMQ | 管理界面 |

### 相关文档

- [CLAUDE.md](../CLAUDE.md) - 项目概览
- [数据库使用指南](./数据库使用指南.md)
- [技术方案.md](./技术方案.md)
