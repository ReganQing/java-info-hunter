# JavaInfoHunter Content Processor

## Overview

**javainfohunter-processor** is a message-driven content processing module that consumes raw RSS content from the crawler module and processes it using AI agent orchestration. Built with Spring Boot 4.0.3, Java 21 virtual threads, and Spring AI, it provides intelligent content analysis, summarization, and classification.

## Core Features

- **AI Agent Orchestration**: Parallel processing using Analysis, Summary, and Classification agents
- **Virtual Threads**: Java 21 virtual threads for high-throughput concurrent processing
- **Message-Driven Architecture**: RabbitMQ consumer with manual acknowledgment and DLQ support
- **Result Aggregation**: Unified aggregation of multi-agent results into ProcessedContentMessage
- **Embedding Generation**: Vector embeddings for semantic similarity search (via pgvector)
- **Thread-Safe Design**: Concurrent result collection with atomic operations

## Architecture

```
+-----------------------------------------------------------------------+
|                     RabbitMQ Message Queue                            |
|           crawler.raw.content.queue (Input from Crawler)              |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                      RawContentConsumer                               |
|  • JSON deserialization                                              |
|  • Manual acknowledgment                                              |
|  • Error handling with DLQ                                            |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                    ContentRoutingService                              |
|            (Virtual Thread Executor - Parallel)                       |
+----------------+------------------+------------------+-----------------+
                 |                  |                  |
                 v                  v                  v
+----------------+-----+ +----------+----------+ +---+---------------+
| AnalysisAgentProcessor | | SummaryAgentProcessor | | ClassificationAgent |
| • Sentiment analysis  | | • Text summarization  | | • Categorization     |
| • Topic extraction   | | • Key points         | | • Tagging            |
| • Importance scoring | | • Concise output     | | • Topic classification|
+----------+-----------+ +-----------+----------+ +--------+-----------+
           |                          |                       |
           +--------------------------+-----------------------+
                                      |
                                      v
+-----------------------------------------------------------------------+
|                      ResultAggregator                                 |
|  • Merge agent outputs                                                |
|  • Create ProcessedContentMessage                                    |
|  • Generate embeddings                                                |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                    Database Storage                                  |
|  • news table (JPA)                                                  |
|  • embedding column (pgvector)                                        |
+-----------------------------------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|              processor.processed.content.queue (Output)               |
+-----------------------------------------------------------------------+
```

## Module Structure

```
javainfohunter-processor/
├── src/main/java/com/ron/javainfohunter/processor/
│   ├── ProcessorApplication.java          # Main Spring Boot application
│   ├── agent/
│   │   ├── AgentProcessor.java            # Agent processor interface
│   │   └── impl/
│   │       ├── AnalysisAgentProcessor.java    # Sentiment, topics, importance
│   │       ├── SummaryAgentProcessor.java     # Text summarization
│   │       └── ClassificationAgentProcessor.java  # Categories and tags
│   ├── config/
│   │   ├── ProcessorProperties.java      # Configuration properties
│   │   └── RabbitMQConsumerConfig.java    # RabbitMQ consumer configuration
│   ├── consumer/
│   │   └── RawContentConsumer.java        # RabbitMQ message consumer
│   ├── dto/
│   │   ├── AgentResult.java               # Agent execution result
│   │   ├── RawContentMessage.java         # Input message from crawler
│   │   └── ProcessedContentMessage.java   # Aggregated output message
│   ├── exception/
│   │   └── ConsumerException.java         # Consumer-specific exceptions
│   └── service/
│       ├── ContentRoutingService.java     # Routing service interface
│       ├── ResultAggregator.java          # Aggregator interface
│       ├── impl/
│       │   ├── ContentRoutingServiceImpl.java  # Parallel routing implementation
│       │   └── ResultAggregatorImpl.java       # Result aggregation implementation
│       └── impl/
├── src/main/resources/
│   └── application.yml                   # Application configuration
└── src/test/
    └── java/com/ron/javainfohunter/processor/
        └── ContentProcessorTest.java     # Integration test
```

## Quick Start

### 1. Build the Module

```bash
# Build the processor module with dependencies
mvnw.cmd clean package -pl javainfohunter-processor

# Or build the entire project
mvnw.cmd clean package
```

### 2. Configure Environment Variables

Required environment variables:

```bash
# Database (PostgreSQL)
export DB_USERNAME=postgres
export DB_PASSWORD=your-password

# RabbitMQ
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=admin
export RABBITMQ_PASSWORD=admin

# Alibaba DashScope API (for AI processing)
export DASHSCOPE_API_KEY=your-api-key-here
```

### 3. Configure application.yml

The module uses the following default configuration (in `application.yml`):

```yaml
javainfohunter:
  processor:
    enabled: true
    agent:
      max-steps: 10
      timeout: 300
      max-retries: 3
    embedding:
      enabled: true
      model: text-embedding-v3
    processing:
      thread-pool-size: 10
      batch-size: 50
```

### 4. Run the Application

```bash
# Run the processor module
mvnw.cmd spring-boot:run -pl javainfohunter-processor

# Run with specific profile
mvnw.cmd spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=develop
```

## Configuration

### Processor Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `javainfohunter.processor.enabled` | boolean | true | Enable/disable processor |
| `javainfohunter.processor.agent.max-steps` | int | 10 | Maximum agent execution steps |
| `javainfohunter.processor.agent.timeout` | int | 300 | Agent timeout (seconds) |
| `javainfohunter.processor.embedding.enabled` | boolean | true | Enable embedding generation |
| `javainfohunter.processor.embedding.model` | String | text-embedding-v3 | Embedding model |
| `javainfohunter.processor.processing.thread-pool-size` | int | 10 | Virtual thread pool size |

### Agent-Specific Configuration

Each agent can be individually enabled/disabled:

```yaml
javainfohunter:
  processor:
    agents:
      analysis:
        enabled: true
        timeout: 30000
      summary:
        enabled: true
        timeout: 30000
        max-summary-length: 500
      classification:
        enabled: true
        timeout: 30000
```

## Usage

### Message Flow

1. **Crawler Module** publishes raw content to `crawler.raw.content.queue`
2. **RawContentConsumer** receives and parses the message
3. **ContentRoutingService** distributes to all enabled agents (parallel)
4. **Agent Processors** execute analysis using AI models
5. **ResultAggregator** combines results into ProcessedContentMessage
6. **Database Storage** persists the processed content with embeddings

### Processing Result Format

```java
ProcessedContentMessage {
    String contentHash;           // Unique content identifier
    String rssSourceId;           // Source RSS feed ID
    String title;                 // Article title
    String summary;               // AI-generated summary
    Double sentimentScore;        // Sentiment (-1.0 to 1.0)
    String sentimentLabel;        // positive/negative/neutral
    List<String> keywords;        // Extracted keywords
    String category;              // Content category
    List<String> tags;            // Content tags
    Double importanceScore;       // Importance (0.0 to 1.0)
    Map<AgentType, AgentResult> agentResults;  // Raw agent outputs
    Instant processedAt;          // Processing timestamp
}
```

## Testing

### Run Unit Tests

```bash
# Run all tests in the processor module
mvnw.cmd test -pl javainfohunter-processor

# Run with coverage
mvnw.cmd test -pl javainfohunter-processor jacoco:report
```

### Integration Testing

```bash
# Run with test profile (uses H2 in-memory database)
mvnw.cmd test -pl javainfohunter-processor -Dspring.profiles.active=test
```

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Framework** | Spring Boot | 4.0.3 |
| **Language** | Java | 21 (Virtual Threads) |
| **Build Tool** | Maven | - |
| **Message Queue** | RabbitMQ | 3.12+ |
| **Database** | PostgreSQL | 16+ (pgvector) |
| **JPA** | Spring Data JPA | - |
| **AI Framework** | Spring AI Alibaba | 1.0.0-M2.1 |
| **Migration** | Flyway | - |
| **Monitoring** | Spring Boot Actuator | - |

## API Endpoints (Actuator)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/health` | GET | Health check status |
| `/actuator/info` | GET | Application information |
| `/actuator/metrics` | GET | Application metrics |
| `/actuator/prometheus` | GET | Prometheus metrics |

## Dependencies

### Internal Dependencies

- `javainfohunter-ai-service`: Agent orchestration framework

### External Dependencies

- Spring Boot Starter (core, AMQP, Data JPA, Validation, Actuator)
- PostgreSQL Driver + pgvector
- Spring AI Alibaba (DashScope)
- Flyway (database migrations)
- Lombok (code generation)
- Hutool (utilities)

## Monitoring and Metrics

### Consumer Metrics

```bash
# Successfully processed messages
curl http://localhost:8080/actuator/metrics/processor.consumer.processed

# Failed messages
curl http://localhost:8080/actuator/metrics/processor.consumer.failed
```

### Health Check

```bash
# Overall health status
curl http://localhost:8080/actuator/health

# RabbitMQ connection status
curl http://localhost:8080/actuator/health/rabbit

# Database connection status
curl http://localhost:8080/actuator/health/db
```

## Related Documentation

- [Technical Specification](../../docs/技术方案.md)
- [Database Design](../../docs/数据库设计说明.md)
- [Data Transmission Architecture](../../docs/数据传输架构设计.md)
- [AI Service Module](../javainfohunter-ai-service/README.md)

## License

MIT License
