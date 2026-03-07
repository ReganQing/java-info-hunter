# JavaInfoHunter

<div align="center">

**A High-Performance Distributed Information Collection System**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Built with **Agent Orchestration** powered by Spring AI and Alibaba DashScope

</div>

---

## Overview

JavaInfoHunter is a distributed information collection and processing system that leverages **AI Agent Orchestration** for intelligent content analysis. The system crawls RSS feeds, processes content using multiple AI agents, and provides RESTful APIs for data access.

### Key Features

- **Agent Orchestration** - Chain, Parallel, and Master-Worker collaboration patterns
- **High Concurrency** - JDK 21 Virtual Threads for scalable async processing
- **Vector Search** - Semantic similarity search with pgvector
- **Message Queue** - RabbitMQ for reliable asynchronous processing
- **Comprehensive Testing** - 80%+ coverage with unit, integration, and E2E tests

---

## Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│  RSS Sources    │────▶│   Crawler    │────▶│   RabbitMQ      │
└─────────────────┘     └──────────────┘     └────────┬────────┘
                                                      │
                                                      ▼
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│  REST API       │◀────│  PostgreSQL  │◀────│   Processor     │
└─────────────────┘     └──────────────┘     └─────────────────┘
                                                      │
                                              ┌───────┴───────────┐
                                              │  AI Agent Layer   │
                                              │  - Analysis       │
                                              │  - Summary        │
                                              │  - Classification │
                                              └───────────────────┘
```

### Maven Modules

| Module | Description |
|--------|-------------|
| `javainfohunter-ai-service` | Core AI service with Agent orchestration framework |
| `javainfohunter-crawler` | RSS feed crawler with scheduled tasks |
| `javainfohunter-processor` | Content processor using AI agents |
| `javainfohunter-api` | REST API for external integration |
| `javainfohunter-e2e` | End-to-end integration tests |

---

## Quick Start

### Prerequisites

- **JDK 21+**
- **Maven 3.9+**
- **PostgreSQL 16+** with pgvector extension
- **RabbitMQ 3.12+**
- **Redis 7+** (optional, for caching)

### Environment Variables

```bash
# Required
export DASHSCOPE_API_KEY=your-api-key-here
export DB_USERNAME=postgres
export DB_PASSWORD=your-password

# Optional (for local development)
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export REDIS_HOST=localhost
```

### Build & Run

```bash
# Clone the repository
git clone https://github.com/ReganQing/java-info-hunter.git
cd java-info-hunter

# Build the project
./mvnw clean package

# Run the Crawler module
./mvnw spring-boot:run -pl javainfohunter-crawler

# Run the Processor module (in another terminal)
./mvnw spring-boot:run -pl javainfohunter-processor

# Run the API module (in another terminal)
./mvnw spring-boot:run -pl javainfohunter-api
```

### Using Docker Compose (Recommended for Local Development)

```bash
docker-compose up -d postgres rabbitmq redis
```

---

## Agent Orchestration

The core of JavaInfoHunter is its **Agent Orchestration Framework**.

### Agent Hierarchy

```
BaseAgent (State & Lifecycle Management)
    ↓
ReActAgent (Think-Act Loop)
    ↓
ToolCallAgent (Spring AI ChatClient Integration)
    ↓
Specialized Agents (Business Logic)
```

### Pre-built Agents

| Agent ID | Description |
|----------|-------------|
| `crawler-agent` | Web crawling and content extraction |
| `analysis-agent` | Content depth analysis |
| `summary-agent` | Text summarization |
| `classification-agent` | Content categorization and tagging |

### Collaboration Patterns

```java
// Chain Pattern - Sequential Execution
CoordinationResult result = taskCoordinator.executeChain(
    "Process content",
    List.of("analysis-agent", "summary-agent", "classification-agent")
);

// Parallel Pattern - Concurrent Execution
CoordinationResult result = taskCoordinator.executeParallel(
    "Analyze content",
    List.of("sentiment-agent", "topic-agent", "keyword-agent")
);
```

---

## API Documentation

Once the API module is running, access Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

### Main Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/news` | List processed news |
| GET | `/api/v1/news/{id}` | Get news by ID |
| POST | `/api/v1/sources` | Add RSS source |
| GET | `/api/v1/sources` | List RSS sources |
| GET | `/actuator/health` | Health check |

---

## Database Schema

### Core Tables

| Table | Purpose |
|-------|---------|
| `rss_sources` | RSS subscription sources |
| `raw_content` | Crawled raw content with embeddings |
| `news` | Processed and enriched content |
| `agent_executions` | Agent execution tracking |

### Database Migration

```bash
# View migration status
./mvnw flyway:info

# Run migrations
./mvnw flyway:migrate
```

---

## Testing

```bash
# Run all tests
./mvnw test

# Run specific module tests
./mvnw test -pl javainfohunter-ai-service

# Run with coverage
./mvnw test jacoco:report

# Run E2E tests
./mvnw test -pl javainfohunter-e2e
```

### Coverage Requirements

- Minimum **80%** code coverage
- Unit tests for all business logic
- Integration tests for API endpoints
- E2E tests for critical user flows

---

## Technology Stack

### Core
- **Java 21** - Virtual Threads, Pattern Matching, Records
- **Spring Boot 3.3.5** - Application framework
- **Spring AI 1.0.2** - AI abstraction layer
- **Maven** - Dependency management

### AI & ML
- **Spring AI Alibaba** - DashScope (通义千问) integration
- **pgvector** - Vector similarity search

### Data & Messaging
- **PostgreSQL 16** - Primary database
- **Flyway** - Database migrations
- **RabbitMQ** - Message queue
- **Redis** - Caching layer

### Observability
- **Spring Actuator** - Health checks
- **Micrometer** - Metrics
- **Prometheus** - Metrics aggregation
- **Zipkin** - Distributed tracing

---

## Configuration

### Application Profiles

```yaml
spring:
  profiles:
    active: develop  # develop | staging | prod
```

### Key Configuration Files

| File | Purpose |
|------|---------|
| `application.yml` | Base configuration |
| `application-develop.yml` | Development settings |
| `application-staging.yml` | Staging environment |
| `application-prod.yml` | Production settings |

---

## Development

### Project Structure

```
JavaInfoHunter/
├── javainfohunter-ai-service/      # AI Service Module
│   └── src/main/java/.../ai/
│       ├── agent/                  # Agent framework
│       ├── tool/                   # Tool system
│       └── service/                # AI services
├── javainfohunter-crawler/         # Crawler Module
├── javainfohunter-processor/       # Processor Module
├── javainfohunter-api/             # API Module
├── javainfohunter-e2e/             # E2E Tests
├── docs/                           # Documentation
└── pom.xml                         # Parent POM
```

### Adding a New Agent

```java
@Component
public class MyCustomAgent extends ToolCallAgent {

    @Override
    protected AgentResponse executeLogic(AgentContext context) {
        // Your agent logic here
        return AgentResponse.success(result);
    }
}

// Register the agent
@Configuration
public class AgentConfig {
    @PostConstruct
    public void registerAgents(AgentManager agentManager) {
        agentManager.registerAgent("my-agent", myCustomAgent);
    }
}
```

### Adding a New Tool

```java
@Component
public class MyTools {

    @Tool(name = "my-tool", description = "Does something useful")
    public String myTool(
        @ToolParam(name = "input", description = "Input data") String input
    ) {
        // Tool implementation
        return "result";
    }
}
```

---

## Documentation

- [Technical Architecture](docs/技术方案.md)
- [Database Design](docs/数据库设计说明.md)
- [Data Transfer Architecture](docs/数据传输架构设计.md)
- [CI/CD Pipeline](docs/ci-cd-guide.md)
- [AI Service Usage](javainfohunter-ai-service/USAGE.md)

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Write tests first (TDD)
4. Commit your changes (`git commit -m 'feat: add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [Spring AI](https://spring.io/projects/spring-ai) - AI integration framework
- [Alibaba DashScope](https://dashscope.aliyun.com/) - LLM API provider
- [pgvector](https://github.com/pgvector/pgvector) - Vector similarity search

---

<div align="center">

**Made with ❤️ by the JavaInfoHunter Team**

</div>
