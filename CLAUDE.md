# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaInfoHunter is a high-performance distributed information collection system built with **Java 21** and **Spring Boot 4.0.3**. The project uses **Agent Orchestration** as its core intelligent processing pattern, powered by Spring AI and Alibaba DashScope.

## Project Structure

### Maven Multi-Module Architecture

```
JavaInfoHunter/                      # 父 POM (packaging=pom)
├── pom.xml                          # 父 POM，依赖管理
├── javainfohunter-ai-service/       # ⭐ AI 服务模块 (独立、可复用)
│   ├── pom.xml
│   └── src/main/java/.../ai/
│       ├── agent/                   # Agent 框架核心
│       │   ├── core/                # BaseAgent, ReActAgent, ToolCallAgent
│       │   ├── coordinator/         # AgentManager, TaskCoordinator
│       │   └── specialized/         # 预置 Agent (Crawler, Analysis, etc.)
│       ├── tool/                    # 工具系统
│       │   ├── annotation/          # @Tool, @ToolParam
│       │   ├── core/                # ToolRegistry, ToolManager
│       │   └── impl/                # 预置工具
│       ├── service/                 # 服务层 (ChatService, AgentService)
│       ├── autoconfigure/           # Spring Boot 自动配置
│       └── config/                  # Agent 配置类
└── (future modules)                 # 其他业务模块（待添加）
```

### javainfohunter-ai-service 模块

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

## Build Commands

```bash
# 构建整个项目
mvnw.cmd clean package

# 构建 AI 服务模块
mvnw.cmd clean package -pl javainfohunter-ai-service

# 运行测试
mvnw.cmd test -pl javainfohunter-ai-service

# 运行单个测试类
mvnw.cmd test -Dtest=BaseAgentTest -pl javainfohunter-ai-service
```

## Build and Development Commands

### Building the Project
```bash
# Unix/Linux/MacOS
./mvnw clean package

# Windows
mvnw.cmd clean package
```

### Running the Application
```bash
# Unix/Linux/MacOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=develop
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=JavaInfoHunterApplicationTests

# Run with coverage
./mvnw test jacoco:report
```

### Other Useful Commands
```bash
# Clean build artifacts
./mvnw clean

# Verify the project (compile + test)
./mvnw verify

# Skip tests during build
./mvnw clean package -DskipTests

# Generate dependency tree
./mvnw dependency:tree
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

**三种协作模式：**

1. **Chain 模式** - 顺序执行，输出作为下一个的输入
   ```java
   taskCoordinator.executeChain("任务", List.of("agent1", "agent2", "agent3"));
   ```

2. **Parallel 模式** - 并行执行，使用虚拟线程（性能提升 3 倍）
   ```java
   taskCoordinator.executeParallel("任务", List.of("agent1", "agent2", "agent3"));
   ```

3. **Master-Worker 模式** - 主从协作（待实现）
   ```java
   taskCoordinator.executeMasterWorker("任务", "master", List.of("worker1", "worker2"));
   ```

### 关键技术决策

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.3 | 核心框架 |
| Java | 21 | 虚拟线程支持 |
| Spring AI | 1.0.2 | AI 抽象层 |
| Spring AI Alibaba | 1.0.0-M2.1 | 阿里云通义千问 |
| Lombok | 1.18.36 | 代码生成 |

## Agent Orchestration Rules (CRITICAL)

### General Principle
**使用 Agent 编排模式实现所有 AI 驱动的复杂业务逻辑。**

### When to Use Agent Orchestration

**✅ USE Agent Orchestration for:**
1. **Content Analysis**: Tasks requiring AI reasoning, sentiment analysis, topic extraction
2. **Multi-step Processing**: Workflows with sequential decision-making (Chain pattern)
3. **Parallel Analysis**: Independent analysis tasks that can run concurrently (Parallel pattern)
4. **Complex Decision Making**: Tasks requiring planning, delegation, and result aggregation (Master-Worker pattern)
5. **Tool-Intensive Tasks**: Workflows requiring multiple tool calls (search, database, API, etc.)
6. **AI-Native Features**: Summarization, classification, recommendation, etc.

**❌ DO NOT USE Agent Orchestration for:**
1. Simple CRUD operations
2. Direct database queries without AI reasoning
3. Static business rules without AI involvement
4. Performance-critical simple operations (< 10ms)


### Agent Implementation Pattern

1. **继承合适的基类**：
   - 需要 AI 推理 + 工具调用 → `ToolCallAgent`
   - 只需要 AI 推理 → `ReActAgent`

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

   CoordinationResult result = taskCoordinator.executeParallel(
       "任务描述",
       List.of("agent1", "agent2", "agent3")
   );
   ```

## javainfohunter-ai-service 模块详解

### 核心组件

| 组件 | 路径 | 职责 |
|------|------|------|
| **Agent 核心** | `agent/core/` | BaseAgent, ReActAgent, ToolCallAgent |
| **协调器** | `agent/coordinator/` | AgentManager, TaskCoordinator, CollaborationPattern |
| **工具系统** | `tool/` | @Tool 注解, ToolRegistry, ToolManager |
| **预置 Agent** | `agent/specialized/` | CrawlerAgent, AnalysisAgent, SummaryAgent, ClassificationAgent |
| **预置工具** | `tool/impl/` | HtmlParserTool, TextSummarizationTool |
| **服务层** | `service/` | ChatService, EmbeddingService, AgentService |
| **自动配置** | `autoconfigure/` | AiServiceAutoConfiguration, AiServiceProperties |

### 线程安全设计

**并发安全组件：**
- `AgentManager`: ConcurrentHashMap 存储 Agent
- `ToolRegistry`: ConcurrentHashMap 存储工具
- `BaseAgent`:
  - `volatile AgentState` - 状态可见性
  - `AtomicInteger currentStep` - 步数原子性
  - `CopyOnWriteArrayList<Message>` - 消息历史线程安全
  - `synchronized` 状态检查块

**虚拟线程使用：**
```java
// TaskCoordinatorImpl.java
Executors.newVirtualThreadPerTaskExecutor()
```

### Spring Boot Auto-Configuration

**自动配置的 Bean：**
- ChatClient (Spring AI 聊天客户端)
- AgentManager (Agent 管理器)
- TaskCoordinator (任务协调器)
- ToolRegistry (工具注册表)
- ToolManager (工具管理器)
- ChatService, EmbeddingService, AgentService

**配置文件：**
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

## Environment Variables

### Required Environment Variables

```bash
# Alibaba DashScope API (必需，用于 AI 功能)
export DASHSCOPE_API_KEY=your-api-key-here
```

## Quick Reference

### 构建和运行
```bash
# 构建项目
mvnw.cmd clean package

# 运行测试
mvnw.cmd test -pl javainfohunter-ai-service

# 查看依赖树
mvnw.cmd dependency:tree
```

### 关键文档
- [USAGE.md](javainfohunter-ai-service/USAGE.md) - AI 服务模块使用指南
- [README.md](javainfohunter-ai-service/README.md) - 模块说明文档
- [迁移完成总结.md](docs/迁移完成总结.md) - 技术实现细节

### 预置 Agent 列表
| Agent ID | 名称 | 功能 |
|----------|------|------|
| `crawler-agent` | CrawlerAgent | 网页爬取和内容提取 |
| `analysis-agent` | AnalysisAgent | 内容深度分析 |
| `summary-agent` | SummaryAgent | 文本摘要生成 |
| `classification-agent` | ClassificationAgent | 内容分类和标签 |
