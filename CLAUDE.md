# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaInfoHunter is a high-performance distributed information collection and intelligent analysis system built with **Java 21** and **Spring Boot 4.0.3**. The project uses virtual threads for high-concurrency crawling, message queues for reliable processing, and **Spring AI + Agent Orchestration** for intelligent content analysis.

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

## Project Architecture

### Package Structure
- `com.ron.javainfohunter` - Root package containing the main application class
- Standard Spring Boot layering will be organized as the project grows (controller, service, repository, etc.)

### Configuration Profiles
The application supports multiple Spring profiles for different environments:

- **develop** (`application-develop.yml`) - Development environment with virtual threads enabled
- **production** (`application-production.yml`) - Production environment with virtual threads enabled
- **example** (`application-example.yml`) - Example configuration reference

### Key Technologies
- **Spring Boot 4.0.3** - Core framework
- **Java 21** - Language version with Virtual Threads
- **Spring AI 1.0.2** - AI integration framework
- **Spring AI Alibaba** - Alibaba DashScope integration
- **Lombok** - Annotation-based code generation
- **Virtual Threads** - Enabled for improved concurrency (Spring Boot 4.0+ feature)
- **Agent Orchestration** - Multi-agent collaboration using ReAct pattern

## Agent Orchestration Rules (CRITICAL)

### General Principle
**JavaInfoHunter adopts Agent Orchestration as its core intelligent processing pattern.** All complex business logic that requires AI reasoning, multi-step processing, or tool usage should be implemented using Agent orchestration rather than traditional service layer code.

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

### Agent Architecture Hierarchy

```
BaseAgent (Abstract)
    ↓ - State management, lifecycle
ReActAgent (Abstract)
    ↓ - think()-act() cycle
ToolCallAgent (Abstract)
    ↓ - Spring AI ChatClient, tool callbacks
Specialized Agents (Concrete)
    - CrawlerAgent, AnalysisAgent, SummaryAgent, etc.
```

### Collaboration Patterns

**1. Chain Pattern** - Sequential processing where output of one agent feeds into the next:
```java
taskCoordinator.executeChain(
    "Process and analyze news content",
    List.of("crawler-agent", "analysis-agent", "summary-agent")
);
```

**2. Parallel Pattern** - Multiple agents work independently on the same task:
```java
taskCoordinator.executeParallel(
    "Multi-dimensional analysis",
    List.of("sentiment-agent", "classification-agent", "keyword-agent")
);
```

**3. Master-Worker Pattern** - Coordinator delegates tasks and aggregates results:
```java
taskCoordinator.executeMasterWorker(
    "Generate comprehensive report",
    "coordinator-agent",  // Master
    List.of("crawler-agent", "analysis-agent", "summary-agent")  // Workers
);
```

### Agent Implementation Guidelines

1. **Inherit from appropriate base class**:
   - Simple AI tasks: Extend `ReActAgent`
   - Tool-required tasks: Extend `ToolCallAgent`
   - Always use Spring AI `ChatClient` for LLM integration

2. **Define clear responsibilities**:
   - Each agent should have a single, well-defined purpose
   - Agent name should clearly indicate its function
   - System prompt should define role and constraints

3. **Implement tools properly**:
   - Tools should be simple, focused functions
   - Use `@ToolCallback` annotation for Spring AI tool registration
   - Tools should be stateless and thread-safe

4. **Handle errors gracefully**:
   - Set agent state to ERROR on failures
   - Log errors with context (agent name, step, task)
   - Implement retry logic for transient failures

5. **Resource management**:
   - Override `cleanup()` to release resources
   - Use virtual threads for blocking I/O
   - Limit max steps to prevent infinite loops

### Example: Creating a New Agent

```java
@Component
public class CustomAnalysisAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        You are a specialized content analysis agent.
        Your task is to analyze content and extract key insights.
        """;

    @Autowired
    private ChatClient chatClient;

    public CustomAnalysisAgent() {
        super(new ToolCallback[]{
            // Define tools here
        });
    }

    @Override
    public void cleanup() {
        // Release resources
    }
}
```

### Agent Registration

All agents must be registered with `AgentManager`:

```java
@Configuration
public class AgentConfig {

    @Autowired
    private AgentManager agentManager;

    @PostConstruct
    public void registerAgents() {
        agentManager.registerAgent("custom-agent", customAnalysisAgent);
    }
}
```

## Integration with Ron-AI-Agent

The project integrates the `ron-ai-agent` framework (located at `D:\Projects\BackEnd\ron-ai-agent`) as a Maven module. When working with agent code:

1. **Reference existing patterns** from `ron-ai-agent` before creating new agents
2. **Reuse core abstractions**: BaseAgent, ReActAgent, ToolCallAgent
3. **Follow naming conventions**: Agents end with "Agent", tools end with "Tool"
4. **Use Spring AI** instead of LangChain4j for LLM integration
5. **Leverage coordinator patterns** for multi-agent workflows

## Configuration Notes

- Virtual threads are explicitly enabled in both `develop` and `production` profiles
- The project uses the Maven Wrapper (`mvnw`/`mvnw.cmd`) for consistent builds across environments
- Lombok is configured as an annotation processor in the Maven compiler plugin
- Spring AI requires `DASHSCOPE_API_KEY` environment variable for Alibaba DashScope integration

## Technology Stack Details

### AI Framework
- **Spring AI 1.0.2**: Primary AI abstraction layer
- **Spring AI Alibaba 1.0.0-M2.1**: Alibaba DashScope (通义千问) integration
- **ChatClient**: Spring AI's fluent API for LLM interactions
- **Advisors**: Chat memory, RAG (Retrieval-Augmented Generation), question answering

### Data Storage
- **PostgreSQL 16 + pgvector**: Relational database with vector similarity search
- **Redis 7.x**: Caching, distributed locks, and rate limiting
- **HikariCP**: High-performance JDBC connection pooling

### Message Queue
- **RabbitMQ 3.x**: Message broker for async processing
- **Spring AMQP**: RabbitMQ integration with manual ACK support

### Web Scraping
- **Jsoup 1.17.x**: HTML parsing and content extraction
- **java.net.http.HttpClient**: Java 11+ native HTTP client with virtual thread support

### Monitoring & Documentation
- **Spring Boot Actuator**: Application health and metrics
- **Knife4j 4.5.0**: Enhanced Swagger UI (access at `/doc.html`)

### Utilities
- **Hutool 5.8.38**: Java utility library
- **Lombok 1.18.36**: Code generation annotations

## Module Structure (Planned)

```
JavaInfoHunter/
├── javainfohunter-agent/         # Agent orchestration framework
├── javainfohunter-crawler/       # Web crawling module
├── javainfohunter-processor/     # Content processing with agents
├── javainfohunter-api/           # REST API layer
└── javainfohunter-common/        # Shared utilities
```

## Environment Variables

Required environment variables for development:

```bash
# Alibaba DashScope API (Required for AI features)
export DASHSCOPE_API_KEY=your-api-key-here

# Database (Required)
export DATABASE_URL=jdbc:postgresql://localhost:5432/javainfohunter
export DATABASE_USERNAME=admin
export DATABASE_PASSWORD=admin

# Redis (Required)
export REDIS_HOST=localhost
export REDIS_PORT=6379

# RabbitMQ (Required)
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=admin
export RABBITMQ_PASSWORD=admin
```

## Quick Reference

### Starting Development Environment
```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Set environment variables
export DASHSCOPE_API_KEY=your-key

# 3. Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=develop
```

### Accessing Services
- Application API: http://localhost:8080
- API Documentation (Knife4j): http://localhost:8080/doc.html
- Health Check: http://localhost:8080/actuator/health
- RabbitMQ Management: http://localhost:15672 (admin/admin)
