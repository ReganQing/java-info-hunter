# RabbitMQ 渐进式学习代码 - 使用指南

## 概述

本模块提供了完整的 RabbitMQ 渐进式学习代码，从最简单的 Hello World 到生产级架构，共 5 个学习阶段。

**文件总数:** 26 个 Java 类 + 1 个配置文件
**包名:** `com.ron.javainfohunter.ai.learning.*`

---

## 目录结构

```
src/test/java/com/ron/javainfohunter/ai/learning/
├── config/              # 配置类（5个）
│   ├── HelloWorldConfig.java       # 阶段1: 基础队列配置
│   ├── WorkQueueConfig.java        # 阶段2: Work Queue 配置
│   ├── FanoutConfig.java           # 阶段3: Fanout Exchange 配置
│   ├── TopicConfig.java            # 阶段4: Topic Exchange 配置
│   └── ProductionConfig.java       # 阶段5: 生产级配置（含死信队列）
│
├── producer/            # 生产者（4个）
│   ├── HelloWorldProducer.java     # 阶段1: 简单消息发送
│   ├── CrawlerProducer.java        # 阶段2: URL 爬取任务发送
│   ├── RawContentProducer.java     # 阶段3: 原始内容广播
│   └── NewsProducer.java           # 阶段4: 新闻路由发送
│
├── consumer/            # 消费者（9个）
│   ├── HelloWorldConsumer.java     # 阶段1: 简单消息接收
│   ├── CrawlerWorker.java          # 阶段2: 爬虫工作线程
│   ├── AnalysisAgent.java          # 阶段3: 分析 Agent
│   ├── SummaryAgent.java           # 阶段3: 摘要 Agent
│   ├── ClassifyAgent.java          # 阶段3: 分类 Agent
│   ├── TechNewsProcessor.java      # 阶段4: 科技新闻处理器
│   ├── FinanceNewsProcessor.java   # 阶段4: 财经新闻处理器
│   ├── AllNewsArchiver.java        # 阶段4: 新闻归档器
│   ├── RobustConsumer.java         # 阶段5: 生产级消费者（手动ACK）
│   └── DeadLetterConsumer.java     # 阶段5: 死信队列消费者
│
├── dto/                 # 数据传输对象（2个）
│   ├── RawContent.java              # 原始内容 DTO
│   └── News.java                    # 新闻 DTO
│
└── stage/               # 测试类（5个）
    ├── Stage1HelloWorldTest.java   # 阶段1: Hello World 测试
    ├── Stage2WorkQueueTest.java    # 阶段2: Work Queue 测试
    ├── Stage3FanoutTest.java       # 阶段3: Fanout Exchange 测试
    ├── Stage4TopicTest.java        # 阶段4: Topic Exchange 测试
    └── Stage5ProductionTest.java   # 阶段5: 生产级测试

src/test/resources/
└── application-test.yml             # RabbitMQ 配置文件
```

---

## 快速开始

### 1. 启动 RabbitMQ

使用 Docker 启动 RabbitMQ（推荐）:

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

访问管理界面: http://localhost:15672 (guest/guest)

### 2. 运行测试

```bash
# 运行单个测试（推荐按顺序运行）
mvnw.cmd test -Dtest=Stage1HelloWorldTest -pl javainfohunter-ai-service
mvnw.cmd test -Dtest=Stage2WorkQueueTest -pl javainfohunter-ai-service
mvnw.cmd test -Dtest=Stage3FanoutTest -pl javainfohunter-ai-service
mvnw.cmd test -Dtest=Stage4TopicTest -pl javainfohunter-ai-service
mvnw.cmd test -Dtest=Stage5ProductionTest -pl javainfohunter-ai-service

# 运行所有学习测试
mvnw.cmd test -Dtest=*Stage* -pl javainfohunter-ai-service
```

---

## 学习路径

### 阶段 1: Hello World (30 分钟)

**学习目标:**
- 理解 RabbitMQ 基础概念（Queue, Producer, Consumer）
- 学习创建持久化队列
- 掌握最简单的消息发送和接收

**关键文件:**
- `config/HelloWorldConfig.java`
- `producer/HelloWorldProducer.java`
- `consumer/HelloWorldConsumer.java`
- `stage/Stage1HelloWorldTest.java`

**运行命令:**
```bash
mvnw.cmd test -Dtest=Stage1HelloWorldTest -pl javainfohunter-ai-service
```

**预期输出:**
```
📤 发送消息: Hello RabbitMQ!
📥 接收消息: Hello RabbitMQ!
```

---

### 阶段 2: Work Queue (1 小时)

**学习目标:**
- 理解 Work Queue 模式（多个消费者竞争消费）
- 学习负载均衡实现
- 掌握并发消费者配置

**关键文件:**
- `config/WorkQueueConfig.java`
- `producer/CrawlerProducer.java`
- `consumer/CrawlerWorker.java`
- `stage/Stage2WorkQueueTest.java`

**应用场景:**
- 爬虫任务分发
- 耗时任务并行处理

**运行命令:**
```bash
mvnw.cmd test -Dtest=Stage2WorkQueueTest -pl javainfohunter-ai-service
```

**预期输出:**
```
🕷️ [Container-0] 开始爬取: https://example.com/news/1
🕷️ [Container-1] 开始爬取: https://example.com/news/2
🕷️ [Container-2] 开始爬取: https://example.com/news/3
```

---

### 阶段 3: Fanout Exchange (1.5 小时)

**学习目标:**
- 理解发布订阅模式（Pub/Sub）
- 学习 Fanout Exchange 工作原理
- 掌握一对多广播实现

**关键文件:**
- `config/FanoutConfig.java`
- `producer/RawContentProducer.java`
- `consumer/AnalysisAgent.java`
- `consumer/SummaryAgent.java`
- `consumer/ClassifyAgent.java`
- `dto/RawContent.java`
- `stage/Stage3FanoutTest.java`

**应用场景:**
- 原始内容需要多个 Agent 并行处理
- 事件通知、日志分发

**运行命令:**
```bash
mvnw.cmd test -Dtest=Stage3FanoutTest -pl javainfohunter-ai-service
```

**预期输出:**
```
📡 广播原始内容: AI 技术突破：GPT-5 发布
   → 分析队列 ✅
   → 摘要队列 ✅
   → 分类队列 ✅
🔍 [分析 Agent] 收到内容: AI 技术突破：GPT-5 发布
📝 [摘要 Agent] 收到内容: AI 技术突破：GPT-5 发布
🏷️ [分类 Agent] 收到内容: AI 技术突破：GPT-5 发布
```

---

### 阶段 4: Topic Exchange (2 小时)

**学习目标:**
- 理解路由模式（Routing）
- 学习 Topic Exchange 路由键匹配规则
- 掌握按类别分发消息

**关键文件:**
- `config/TopicConfig.java`
- `producer/NewsProducer.java`
- `consumer/TechNewsProcessor.java`
- `consumer/FinanceNewsProcessor.java`
- `consumer/AllNewsArchiver.java`
- `dto/News.java`
- `stage/Stage4TopicTest.java`

**应用场景:**
- 不同类别的新闻需要不同的处理流程
- 日志按级别分发

**运行命令:**
```bash
mvnw.cmd test -Dtest=Stage4TopicTest -pl javainfohunter-ai-service
```

**预期输出:**
```
📰 发送新闻到 [tech.ai]: GPT-5 发布
💻 [科技新闻处理器] 收到: GPT-5 发布
📦 [归档器] 保存到数据库: GPT-5 发布

📰 发送新闻到 [finance.stock]: 股市大涨
💰 [财经新闻处理器] 收到: 股市大涨
📦 [归档器] 保存到数据库: 股市大涨
```

---

### 阶段 5: 生产级架构 (3 小时)

**学习目标:**
- 理解消息可靠性保障机制
- 学习死信队列（DLQ）配置
- 掌握生产级环境最佳实践

**关键文件:**
- `config/ProductionConfig.java`
- `consumer/RobustConsumer.java`
- `consumer/DeadLetterConsumer.java`
- `stage/Stage5ProductionTest.java`

**可靠性保障:**
1. 消息持久化: `MessageDeliveryMode.PERSISTENT`
2. 队列持久化: `durable = true`
3. 手动 ACK: 处理成功后才确认
4. 发布确认: `publisher-confirm-type = correlated`
5. 返回回调: `publisher-returns = true`
6. 死信队列: 重试失败后进入 DLQ

**运行命令:**
```bash
mvnw.cmd test -Dtest=Stage5ProductionTest -pl javainfohunter-ai-service
```

**预期输出:**
```
✅ 消息成功到达 Exchange
📨 收到消息: 正常消息
✅ 处理成功，已确认

❌ 业务异常，丢弃消息: 模拟业务异常
💀 消息进入死信队列: 包含error的业务异常

⚠️ 系统异常，消息重新入队: 模拟系统异常
... (重试 3 次)
💀 消息进入死信队列: 包含fail的系统异常
```

---

## 配置说明

### application-test.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    publisher-confirm-type: correlated  # 发布确认
    publisher-returns: true             # 返回回调

rabbitmq:
  listener:
    simple:
      acknowledge-mode: manual          # 手动 ACK
      retry:
        enabled: true
        max-attempts: 3                 # 最多重试 3 次
        initial-interval: 2000          # 重试间隔 2 秒
```

---

## 技术栈

- **Spring Boot**: 4.0.3
- **Spring AMQP**: 最新版本（通过 spring-boot-starter-amqp 引入）
- **RabbitMQ**: 3.x
- **Lombok**: 1.18.36（用于简化 DTO）
- **JUnit 5**: 测试框架

---

## 依赖已添加

`pom.xml` 已更新，包含以下依赖:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 关键概念总结

### Exchange 类型

| 类型 | 特点 | 路由键 | 适用场景 |
|------|------|--------|----------|
| **Default** | 直接发送到队列 | 队列名 | 简单任务 |
| **Fanout** | 广播到所有绑定队列 | 无 | 一对多广播 |
| **Direct** | 精确匹配路由键 | 完全匹配 | 点对点路由 |
| **Topic** | 模式匹配路由键 | 通配符匹配 | 按类别分发 |

### 消息可靠性

```
生产者发送 → Exchange → Queue → 消费者处理
   ↓          ↓        ↓         ↓
确认机制    持久化    持久化    手动ACK
```

### 异常处理策略

- **业务异常**: 直接进入死信队列，不重试（如数据格式错误）
- **系统异常**: 重试 3 次，失败后进入死信队列（如数据库连接失败）

---

## 学习建议

1. **逐个阶段实践**: 不要跳过任何阶段，按顺序学习
2. **运行代码**: 每个示例都要实际运行并观察输出
3. **观察 RabbitMQ 管理界面**: http://localhost:15672
   - 查看 Queue 的消息数量
   - 查看 Exchange 和 Binding
   - 查看消息详情
4. **故意制造错误**: 测试异常处理机制
5. **阅读日志**: 理解每个步骤的执行流程

---

## 下一步

完成以上学习后，您可以:

1. ✅ 设计 JavaInfoHunter 的完整消息架构
2. ✅ 结合 Agent 编排模式实现端到端流程
3. ✅ 添加监控和告警机制
4. ✅ 性能优化和压测

---

## 文件清单

### 配置类 (5 个)
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\config\HelloWorldConfig.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\config\WorkQueueConfig.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\config\FanoutConfig.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\config\TopicConfig.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\config\ProductionConfig.java

### 生产者 (4 个)
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\producer\HelloWorldProducer.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\producer\CrawlerProducer.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\producer\RawContentProducer.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\producer\NewsProducer.java

### 消费者 (9 个)
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\HelloWorldConsumer.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\CrawlerWorker.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\AnalysisAgent.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\SummaryAgent.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\ClassifyAgent.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\TechNewsProcessor.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\FinanceNewsProcessor.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\AllNewsArchiver.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\RobustConsumer.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\consumer\DeadLetterConsumer.java

### DTO (2 个)
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\dto\RawContent.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\dto\News.java

### 测试类 (5 个)
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\stage\Stage1HelloWorldTest.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\stage\Stage2WorkQueueTest.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\stage\Stage3FanoutTest.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\stage\Stage4TopicTest.java
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\java\com\ron\javainfohunter\ai\learning\stage\Stage5ProductionTest.java

### 配置文件 (1 个)
- D:\Projects\BackEnd\JavaInfoHunter\javainfohunter-ai-service\src\test\resources\application-test.yml

---

**文档版本:** v1.0
**创建日期:** 2026-03-01
**作者:** JavaInfoHunter Team
