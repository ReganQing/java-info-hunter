# Content Processor Module Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a message consumer module that processes RSS content using AI agents, aggregates results, and stores to database.

**Architecture:**
```
crawler.raw.content.queue → ContentProcessorConsumer → Fanout to Agent Queues
                                                     ↓
                        ┌──────────────────────────────────────┐
                        │  TaskCoordinator (Parallel Pattern)  │
                        │  • AnalysisAgent (sentiment, keywords)│
                        │  • SummaryAgent (summarization)       │
                        │  • ClassificationAgent (categories)   │
                        └──────────────────────────────────────┘
                                                     ↓
                        Aggregate Results → Generate Embedding → Store to DB
```

**Tech Stack:** Spring Boot 4.0.3, Java 21 Virtual Threads, Spring AI, RabbitMQ, PostgreSQL + pgvector

---

## Pre-Implementation Setup

### Task 0: Verify Prerequisites

**Files:**
- Check: `pom.xml` (root)
- Check: `javainfohunter-ai-service/pom.xml`
- Check: `javainfohunter-crawler/pom.xml`

**Step 1: Verify existing modules compile**

Run:
```bash
cd D:/Projects/BackEnd/JavaInfoHunter
./mvnw.cmd clean compile -pl javainfohunter-crawler,javainfohunter-ai-service -am
```

Expected: BUILD SUCCESS

**Step 2: Verify RabbitMQ configuration exists**

Check: `javainfohunter-crawler/src/main/java/com/ron/javainfohunter/crawler/config/RabbitMQConfig.java`

Verify these constants exist:
- `CRAWLER_EXCHANGE = "crawler.direct"`
- `RAW_CONTENT_QUEUE = "crawler.raw.content.queue"`
- `RAW_CONTENT_ROUTING_KEY = "raw.content"`

---

## Phase 1: Module Structure

### Task 1: Create Maven Module

**Files:**
- Create: `javainfohunter-processor/pom.xml`
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/ProcessorApplication.java`
- Modify: `pom.xml` (add module)

**Step 1: Create processor POM**

File: `javainfohunter-processor/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ron</groupId>
        <artifactId>JavaInfoHunter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <artifactId>javainfohunter-processor</artifactId>
    <name>javainfohunter-processor</name>
    <description>Content Processing Module with AI Agents</description>

    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Spring AMQP (RabbitMQ) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- pgvector -->
        <dependency>
            <groupId>com.pgvector</groupId>
            <artifactId>pgvector</artifactId>
            <version>0.1.4</version>
        </dependency>

        <!-- Internal AI Service Module -->
        <dependency>
            <groupId>com.ron</groupId>
            <artifactId>javainfohunter-ai-service</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Spring AMQP Test -->
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: Add module to parent POM**

File: `pom.xml` (modify `<modules>` section)

```xml
<modules>
    <module>javainfohunter-ai-service</module>
    <module>javainfohunter-crawler</module>
    <module>javainfohunter-processor</module>
</modules>
```

**Step 3: Create main application class**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/ProcessorApplication.java`

```java
package com.ron.javainfohunter.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {
    "com.ron.javainfohunter.processor",
    "com.ron.javainfohunter.ai"
})
@EnableJpaAuditing
public class ProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }
}
```

**Step 4: Create application.yml**

File: `javainfohunter-processor/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: content-processor

  # RabbitMQ Configuration
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:admin}
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 5
        max-concurrency: 20
        prefetch: 10

  # Database Configuration
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:javainfohunter}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Dialect
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

javainfohunter:
  processor:
    enabled: true
    agents:
      analysis:
        enabled: true
        timeout: 30000
      summary:
        enabled: true
        timeout: 30000
      classification:
        enabled: true
        timeout: 30000
    embedding:
      enabled: true
      model: text-embedding-v3
```

**Step 5: Verify build**

Run:
```bash
./mvnw.cmd clean compile -pl javainfohunter-processor -am
```

Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add javainfohunter-processor/ pom.xml
git commit -m "feat(processor): add content processor module structure"
```

---

## Phase 2: Message Configuration

### Task 2: Create RabbitMQ Consumer Configuration

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/config/RabbitMQConsumerConfig.java`

**Step 1: Write the configuration class**

```java
package com.ron.javainfohunter.processor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Consumer Configuration for Content Processor Module.
 *
 * Defines queues, exchanges, and bindings for consuming raw content
 * and distributing to AI agent processors.
 */
@Configuration
public class RabbitMQConsumerConfig {

    // Exchange names (must match crawler module)
    public static final String CRAWLER_EXCHANGE = "crawler.direct";

    // Queue names
    public static final String RAW_CONTENT_QUEUE = "crawler.raw.content.queue";
    public static final String ANALYSIS_QUEUE = "processor.analysis.queue";
    public static final String SUMMARY_QUEUE = "processor.summary.queue";
    public static final String CLASSIFICATION_QUEUE = "processor.classification.queue";
    public static final String AGGREGATED_QUEUE = "processor.aggregated.queue";

    // Routing keys
    public static final String RAW_CONTENT_ROUTING_KEY = "raw.content";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue rawContentQueue() {
        return QueueBuilder.durable(RAW_CONTENT_QUEUE)
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue analysisQueue() {
        return QueueBuilder.durable(ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable(SUMMARY_QUEUE)
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue classificationQueue() {
        return QueueBuilder.durable(CLASSIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue aggregatedQueue() {
        return QueueBuilder.durable(AGGREGATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }
}
```

**Step 2: Run verify compile**

Run: `./mvnw.cmd compile -pl javainfohunter-processor -q`

Expected: No errors

**Step 3: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/config/
git commit -m "feat(processor): add RabbitMQ consumer configuration"
```

---

## Phase 3: Message DTOs

### Task 3: Create Shared Message DTOs

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/dto/ProcessedContentMessage.java`
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/dto/AgentResult.java`

**Step 1: Create AgentResult DTO**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/dto/AgentResult.java`

```java
package com.ron.javainfohunter.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Result from an AI agent processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /**
     * Type of agent that produced this result.
     */
    private AgentType agentType;

    /**
     * Original content hash being processed.
     */
    private String contentHash;

    /**
     * Processing result data.
     */
    private Map<String, Object> result;

    /**
     * Whether processing was successful.
     */
    private boolean success;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;

    /**
     * Processing duration in milliseconds.
     */
    private long durationMs;

    /**
     * Timestamp when processing completed.
     */
    private Instant processedAt;

    public enum AgentType {
        ANALYSIS,      // Sentiment analysis, keywords
        SUMMARY,       // Text summarization
        CLASSIFICATION // Category classification, tags
    }
}
```

**Step 2: Create ProcessedContentMessage DTO**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/dto/ProcessedContentMessage.java`

```java
package com.ron.javainfohunter.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregated processed content message ready for database storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedContentMessage {

    /**
     * Original content hash (unique identifier).
     */
    private String contentHash;

    /**
     * Source RSS ID.
     */
    private Long rssSourceId;

    /**
     * Source URL.
     */
    private String sourceUrl;

    /**
     * Original title.
     */
    private String title;

    /**
     * Original content.
     */
    private String rawContent;

    /**
     * Generated summary.
     */
    private String summary;

    /**
     * Sentiment score (-1.0 to 1.0).
     */
    private Double sentimentScore;

    /**
     * Sentiment label (positive, negative, neutral).
     */
    private String sentimentLabel;

    /**
     * Extracted keywords.
     */
    private List<String> keywords;

    /**
     * Assigned category.
     */
    private String category;

    /**
     * Assigned tags.
     */
    private List<String> tags;

    /**
     * Importance score (0.0 to 1.0).
     */
    private Double importanceScore;

    /**
     * Generated embedding vector (for similarity search).
     */
    private float[] embedding;

    /**
     * All agent results (for debugging).
     */
    private Map<AgentResult.AgentType, AgentResult> agentResults;

    /**
     * Processing timestamp.
     */
    private Instant processedAt;
}
```

**Step 3: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/dto/
git commit -m "feat(processor): add message DTOs for agent results"
```

---

## Phase 4: Raw Content Consumer

### Task 4: Create Raw Content Message Consumer

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/consumer/RawContentConsumer.java`
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/exception/ConsumerException.java`

**Step 1: Create custom exception**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/exception/ConsumerException.java`

```java
package com.ron.javainfohunter.processor.exception;

import lombok.Getter;

@Getter
public class ConsumerException extends RuntimeException {
    private final String contentHash;

    public ConsumerException(String message, String contentHash) {
        super(message);
        this.contentHash = contentHash;
    }

    public ConsumerException(String message, String contentHash, Throwable cause) {
        super(message, cause);
        this.contentHash = contentHash;
    }
}
```

**Step 2: Create the consumer**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/consumer/RawContentConsumer.java`

```java
package com.ron.javainfohunter.processor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.service.ContentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Consumer for raw RSS content messages from the crawler module.
 *
 * Receives messages from crawler.raw.content.queue and routes
 * them to AI agent processors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "javainfohunter.processor.enabled", havingValue = "true", matchIfMissing = true)
public class RawContentConsumer {

    private final ObjectMapper objectMapper;
    private final ContentRoutingService routingService;

    @RabbitListener(
        queues = "crawler.raw.content.queue",
        containerFactory = "rabbitListenerContainerFactory",
        ackMode = "MANUAL"
    )
    public void consumeRawContent(Message message) {
        Long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // Parse message
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            RawContentMessage contentMessage = objectMapper.readValue(json, RawContentMessage.class);

            log.debug("Received raw content: guid={}, hash={}, title={}",
                    contentMessage.getGuid(),
                    contentMessage.getContentHash(),
                    contentMessage.getTitle());

            // Route to AI processors
            routingService.routeToAgents(contentMessage);

            // Acknowledge successful processing
            // Note: In production, you'd get the channel and ack
            log.debug("Successfully routed content: hash={}", contentMessage.getContentHash());

        } catch (Exception e) {
            log.error("Error consuming raw content message: deliveryTag={}", deliveryTag, e);
            // In production: nack with requeue=false to send to DLQ
        }
    }
}
```

**Step 3: Create routing service interface**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/ContentRoutingService.java`

```java
package com.ron.javainfohunter.processor.service;

import com.ron.javainfohunter.crawler.dto.RawContentMessage;

/**
 * Service for routing content to AI agent processors.
 */
public interface ContentRoutingService {
    void routeToAgents(RawContentMessage contentMessage);
}
```

**Step 4: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/
git commit -m "feat(processor): add raw content consumer and routing service"
```

---

## Phase 5: Agent Integration

### Task 5: Create Agent Integration Service

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/impl/ContentRoutingServiceImpl.java`
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/AgentProcessor.java`
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/config/ProcessorProperties.java`

**Step 1: Create processor properties**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/config/ProcessorProperties.java`

```java
package com.ron.javainfohunter.processor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "javainfohunter.processor")
public class ProcessorProperties {

    private boolean enabled = true;

    private AgentConfig agents = new AgentConfig();

    private EmbeddingConfig embedding = new EmbeddingConfig();

    @Data
    public static class AgentConfig {
        private AnalysisConfig analysis = new AnalysisConfig();
        private SummaryConfig summary = new SummaryConfig();
        private ClassificationConfig classification = new ClassificationConfig();
    }

    @Data
    public static class AnalysisConfig {
        private boolean enabled = true;
        private long timeout = 30000;
    }

    @Data
    public static class SummaryConfig {
        private boolean enabled = true;
        private long timeout = 30000;
        private int maxSummaryLength = 500;
    }

    @Data
    public static class ClassificationConfig {
        private boolean enabled = true;
        private long timeout = 30000;
    }

    @Data
    public static class EmbeddingConfig {
        private boolean enabled = true;
        private String model = "text-embedding-v3";
        private int dimensions = 1024;
    }
}
```

**Step 2: Create agent processor interface**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/AgentProcessor.java`

```java
package com.ron.javainfohunter.processor.agent;

import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.dto.AgentResult;

/**
 * Interface for AI agent processors.
 */
public interface AgentProcessor {

    /**
     * Process content with this agent.
     *
     * @param content the raw content to process
     * @return agent processing result
     */
    AgentResult process(RawContentMessage content);

    /**
     * Get the agent type.
     *
     * @return agent type
     */
    AgentResult.AgentType getAgentType();
}
```

**Step 3: Create routing service implementation**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/impl/ContentRoutingServiceImpl.java`

```java
package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.service.ContentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of content routing service that distributes
 * processing to AI agents using parallel execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRoutingServiceImpl implements ContentRoutingService {

    private final ProcessorProperties properties;
    private final List<AgentProcessor> agentProcessors;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Store results for aggregation
    private final Map<String, Map<AgentResult.AgentType, AgentResult>> processingResults =
            new ConcurrentHashMap<>();

    @Override
    public void routeToAgents(RawContentMessage contentMessage) {
        String contentHash = contentMessage.getContentHash();

        // Initialize result map for this content
        Map<AgentResult.AgentType, AgentResult> results = new EnumMap<>(AgentResult.AgentType.class);
        processingResults.put(contentHash, results);

        // Submit parallel agent processing tasks
        for (AgentProcessor processor : agentProcessors) {
            executor.submit(() -> {
                try {
                    AgentResult result = processor.process(contentMessage);
                    results.put(processor.getAgentType(), result);
                    log.debug("Agent {} completed for hash={}: success={}",
                            processor.getAgentType(), contentHash, result.isSuccess());
                } catch (Exception e) {
                    log.error("Agent {} failed for hash={}",
                            processor.getAgentType(), contentHash, e);
                    results.put(processor.getAgentType(), AgentResult.builder()
                            .agentType(processor.getAgentType())
                            .contentHash(contentHash)
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build());
                }
            });
        }

        // TODO: Add aggregation callback when all agents complete
        // This will be implemented in a later task
    }

    /**
     * Get processing results for a content hash.
     *
     * @param contentHash the content hash
     * @return map of agent results
     */
    public Map<AgentResult.AgentType, AgentResult> getResults(String contentHash) {
        return processingResults.get(contentHash);
    }

    /**
     * Remove processing results (called after aggregation).
     *
     * @param contentHash the content hash
     */
    public void removeResults(String contentHash) {
        processingResults.remove(contentHash);
    }
}
```

**Step 4: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/
git commit -m "feat(processor): add agent routing service and processor properties"
```

---

## Phase 6: Agent Implementations

### Task 6: Create Analysis Agent Implementation

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/AnalysisAgentProcessor.java`

**Step 1: Create the analysis processor**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/AnalysisAgentProcessor.java`

```java
package com.ron.javainfohunter.processor.agent.impl;

import com.ron.javainfohunter.ai.agent.ToolCallAgent;
import com.ron.javainfohunter.ai.service.AgentService;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent processor for content analysis (sentiment, keywords).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "javainfohunter.processor.agents.analysis.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AnalysisAgentProcessor implements AgentProcessor {

    private final AgentService agentService;
    private final ProcessorProperties properties;

    @Override
    public AgentResult process(RawContentMessage content) {
        long startTime = System.currentTimeMillis();

        try {
            // Build analysis prompt
            String prompt = buildAnalysisPrompt(content);

            // Execute analysis via AgentService
            String response = agentService.chat(
                    "analysis-agent",
                    prompt,
                    Map.of() // No tools needed for basic analysis
            );

            // Parse response
            Map<String, Object> result = parseAnalysisResponse(response);

            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .result(result)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Analysis failed for hash={}", content.getContentHash(), e);
            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();
        }
    }

    @Override
    public AgentResult.AgentType getAgentType() {
        return AgentResult.AgentType.ANALYSIS;
    }

    private String buildAnalysisPrompt(RawContentMessage content) {
        return String.format("""
                Please analyze the following news article and provide:

                1. Sentiment analysis (score from -1.0 to 1.0, and label: positive/negative/neutral)
                2. Key topics/themes (extract 3-5 main topics)
                3. Important keywords (extract 5-10 keywords)
                4. Importance score (0.0 to 1.0 based on news significance)

                Title: %s

                Content: %s

                Respond in JSON format:
                {
                  "sentiment": {"score": 0.5, "label": "positive"},
                  "topics": ["AI", "Technology", "Innovation"],
                  "keywords": ["artificial intelligence", "breakthrough", "research"],
                  "importance": 0.8
                }
                """,
                content.getTitle(),
                content.getRawContent() != null && content.getRawContent().length() > 5000
                        ? content.getRawContent().substring(0, 5000) + "..."
                        : content.getRawContent()
        );
    }

    private Map<String, Object> parseAnalysisResponse(String response) {
        // TODO: Implement proper JSON parsing
        // For now, return placeholder
        Map<String, Object> result = new HashMap<>();
        result.put("rawResponse", response);
        result.put("sentimentScore", 0.0);
        result.put("sentimentLabel", "neutral");
        return result;
    }
}
```

**Step 2: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/
git commit -m "feat(processor): add analysis agent processor"
```

---

### Task 7: Create Summary Agent Implementation

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/SummaryAgentProcessor.java`

**Step 1: Create the summary processor**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/SummaryAgentProcessor.java`

```java
package com.ron.javainfohunter.processor.agent.impl;

import com.ron.javainfohunter.ai.service.AgentService;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent processor for content summarization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "javainfohunter.processor.agents.summary.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SummaryAgentProcessor implements AgentProcessor {

    private final AgentService agentService;
    private final ProcessorProperties properties;

    @Override
    public AgentResult process(RawContentMessage content) {
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildSummaryPrompt(content);

            String response = agentService.chat(
                    "summary-agent",
                    prompt,
                    Map.of()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("summary", response);

            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .result(result)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Summary generation failed for hash={}", content.getContentHash(), e);
            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();
        }
    }

    @Override
    public AgentResult.AgentType getAgentType() {
        return AgentResult.AgentType.SUMMARY;
    }

    private String buildSummaryPrompt(RawContentMessage content) {
        int maxLength = properties.getAgents().getSummary().getMaxSummaryLength();

        return String.format("""
                Please provide a concise summary of the following news article.

                Requirements:
                - Summary length: maximum %d characters
                - Capture the main point and key details
                - Maintain factual accuracy
                - Use clear, professional language

                Title: %s

                Content: %s

                Summary:
                """,
                maxLength,
                content.getTitle(),
                content.getRawContent()
        );
    }
}
```

**Step 2: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/
git commit -m "feat(processor): add summary agent processor"
```

---

### Task 8: Create Classification Agent Implementation

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/ClassificationAgentProcessor.java`

**Step 1: Create the classification processor**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/ClassificationAgentProcessor.java`

```java
package com.ron.javainfohunter.processor.agent.impl;

import com.ron.javainfohunter.ai.service.AgentService;
import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent processor for content classification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "javainfohunter.processor.agents.classification.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ClassificationAgentProcessor implements AgentProcessor {

    private final AgentService agentService;
    private final ProcessorProperties properties;

    // Predefined categories
    private static final String[] CATEGORIES = {
        "Technology", "Business", "Science", "Health", "Politics",
        "Sports", "Entertainment", "World", "Environment", "Other"
    };

    @Override
    public AgentResult process(RawContentMessage content) {
        long startTime = System.currentTimeMillis();

        try {
            String prompt = buildClassificationPrompt(content);

            String response = agentService.chat(
                    "classification-agent",
                    prompt,
                    Map.of()
            );

            Map<String, Object> result = parseClassificationResponse(response, content);

            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .result(result)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Classification failed for hash={}", content.getContentHash(), e);
            return AgentResult.builder()
                    .agentType(getAgentType())
                    .contentHash(content.getContentHash())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .processedAt(Instant.now())
                    .build();
        }
    }

    @Override
    public AgentResult.AgentType getAgentType() {
        return AgentResult.AgentType.CLASSIFICATION;
    }

    private String buildClassificationPrompt(RawContentMessage content) {
        StringBuilder categoriesBuilder = new StringBuilder("Available categories:\n");
        for (String cat : CATEGORIES) {
            categoriesBuilder.append("- ").append(cat).append("\n");
        }

        return String.format("""
                %s
                Please classify the following news article into ONE primary category
                and suggest 3-5 relevant tags.

                Title: %s
                Author: %s
                Original Category: %s
                Tags: %s

                Content: %s

                Respond in JSON format:
                {
                  "category": "Technology",
                  "tags": ["AI", "Machine Learning", "Research"]
                }
                """,
                categoriesBuilder.toString(),
                content.getTitle(),
                content.getAuthor() != null ? content.getAuthor() : "Unknown",
                content.getCategory() != null ? content.getCategory() : "Unknown",
                content.getTags() != null ? String.join(", ", content.getTags()) : "None",
                content.getRawContent() != null && content.getRawContent().length() > 3000
                        ? content.getRawContent().substring(0, 3000) + "..."
                        : content.getRawContent()
        );
    }

    private Map<String, Object> parseClassificationResponse(String response, RawContentMessage content) {
        // TODO: Implement proper JSON parsing
        Map<String, Object> result = new HashMap<>();

        // Fallback to original category if AI fails
        result.put("category", content.getCategory() != null ? content.getCategory() : "Other");
        result.put("tags", content.getTags() != null ? content.getTags() : new String[0]);
        result.put("rawResponse", response);

        return result;
    }
}
```

**Step 2: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/agent/impl/
git commit -m "feat(processor): add classification agent processor"
```

---

## Phase 7: Result Aggregation

### Task 9: Create Result Aggregator

**Files:**
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/ResultAggregator.java`
- Create: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/impl/ResultAggregatorImpl.java`

**Step 1: Create aggregator interface**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/ResultAggregator.java`

```java
package com.ron.javainfohunter.processor.service;

import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;

import java.util.concurrent.CompletionStage;

/**
 * Service for aggregating results from multiple AI agents.
 */
public interface ResultAggregator {

    /**
     * Aggregate agent results into a processed content message.
     *
     * @param content the original content
     * @param agentResults map of agent results
     * @return completion stage with processed message
     */
    CompletionStage<ProcessedContentMessage> aggregate(
            RawContentMessage content,
            java.util.Map<com.ron.javainfohunter.processor.dto.AgentResult.AgentType, com.ron.javainfohunter.processor.dto.AgentResult> agentResults
    );

    /**
     * Process aggregated message (generate embedding, store to database).
     *
     * @param message the processed message
     * @return completion stage
     */
    CompletionStage<Void> store(ProcessedContentMessage message);
}
```

**Step 2: Create aggregator implementation**

File: `javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/impl/ResultAggregatorImpl.java`

```java
package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.crawler.dto.RawContentMessage;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.processor.service.ResultAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of result aggregator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultAggregatorImpl implements ResultAggregator {

    @Override
    public CompletionStage<ProcessedContentMessage> aggregate(
            RawContentMessage content,
            Map<AgentResult.AgentType, AgentResult> agentResults) {

        return CompletableFuture.supplyAsync(() -> {
            ProcessedContentMessage.ProcessedContentMessageBuilder builder =
                    ProcessedContentMessage.builder()
                            .contentHash(content.getContentHash())
                            .rssSourceId(content.getRssSourceId())
                            .sourceUrl(content.getRssSourceUrl())
                            .title(content.getTitle())
                            .rawContent(content.getRawContent())
                            .processedAt(Instant.now());

            // Extract analysis results
            AgentResult analysis = agentResults.get(AgentResult.AgentType.ANALYSIS);
            if (analysis != null && analysis.isSuccess() && analysis.getResult() != null) {
                Map<String, Object> result = analysis.getResult();
                builder.sentimentScore(extractDouble(result, "sentimentScore"));
                builder.sentimentLabel(extractString(result, "sentimentLabel"));
                builder.importanceScore(extractDouble(result, "importance"));
                // Keywords extraction would be here
            }

            // Extract summary results
            AgentResult summary = agentResults.get(AgentResult.AgentType.SUMMARY);
            if (summary != null && summary.isSuccess() && summary.getResult() != null) {
                builder.summary(extractString(summary.getResult(), "summary"));
            }

            // Extract classification results
            AgentResult classification = agentResults.get(AgentResult.AgentType.CLASSIFICATION);
            if (classification != null && classification.isSuccess() && classification.getResult() != null) {
                builder.category(extractString(classification.getResult(), "category"));
                // Tags extraction would be here
            }

            builder.agentResults(agentResults);

            ProcessedContentMessage message = builder.build();
            log.info("Aggregated results for hash={}, category={}, summaryLength={}",
                    message.getContentHash(),
                    message.getCategory(),
                    message.getSummary() != null ? message.getSummary().length() : 0);

            return message;
        });
    }

    @Override
    public CompletionStage<Void> store(ProcessedContentMessage message) {
        return CompletableFuture.runAsync(() -> {
            // TODO: Implement embedding generation
            // TODO: Implement database storage
            log.info("Storing processed content: hash={}", message.getContentHash());
        });
    }

    private Double extractDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
```

**Step 3: Commit**

```bash
git add javainfohunter-processor/src/main/java/com/ron/javainfohunter/processor/service/
git commit -m "feat(processor): add result aggregator service"
```

---

## Phase 8: Testing

### Task 10: Create Unit Tests

**Files:**
- Create: `javainfohunter-processor/src/test/java/com/ron/javainfohunter/processor/ContentProcessorTest.java`

**Step 1: Write integration test skeleton**

File: `javainfohunter-processor/src/test/java/com/ron/javainfohunter/processor/ContentProcessorTest.java`

```java
package com.ron.javainfohunter.processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for content processor module.
 */
@SpringBootTest(classes = ProcessorApplication.class)
@ActiveProfiles("test")
class ContentProcessorTest {

    @Test
    void contextLoads() {
        // Verify application context loads successfully
    }
}
```

**Step 2: Create test properties**

File: `javainfohunter-processor/src/test/resources/application-test.yml`

```yaml
spring:
  profiles:
    active: test

  # Use H2 for testing
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop

  # Mock RabbitMQ
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

javainfohunter:
  processor:
    enabled: true
```

**Step 3: Run test**

Run: `./mvnw.cmd test -pl javainfohunter-processor`

Expected: Tests pass

**Step 4: Commit**

```bash
git add javainfohunter-processor/src/test/
git commit -m "test(processor): add integration test skeleton"
```

---

## Phase 9: Documentation

### Task 11: Update Documentation

**Files:**
- Create: `javainfohunter-processor/README.md`

**Step 1: Create module README**

File: `javainfohunter-processor/README.md`

```markdown
# Content Processor Module

## Overview

This module consumes raw RSS content from the crawler module and processes it
using AI agents for analysis, summarization, and classification.

## Architecture

```
crawler.raw.content.queue → RawContentConsumer → ContentRoutingService
                                                         ↓
                                            ┌────────────────────────────┐
                                            │  Parallel Agent Execution  │
                                            │  • AnalysisAgent           │
                                            │  • SummaryAgent            │
                                            │  • ClassificationAgent     │
                                            └────────────────────────────┘
                                                         ↓
                                            ResultAggregator → Database
```

## Configuration

```yaml
javainfohunter:
  processor:
    enabled: true
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
```

## Running

```bash
./mvnw.cmd spring-boot:run -pl javainfohunter-processor
```

## Testing

```bash
./mvnw.cmd test -pl javainfohunter-processor
```
```

**Step 2: Update project documentation**

Update: `docs/plans/2026-03-05-content-processor-module-progress.md`

```markdown
# Content Processor Module Progress

> **Started:** 2026-03-05
> **Status:** 🚀 In Progress

## Completed

- [x] Maven module structure
- [x] RabbitMQ consumer configuration
- [x] Message DTOs (AgentResult, ProcessedContentMessage)
- [x] Raw content consumer
- [x] Content routing service
- [x] Agent processor implementations (Analysis, Summary, Classification)
- [x] Result aggregator

## In Progress

- [ ] Embedding generation
- [ ] Database persistence
- [ ] End-to-end testing

## Next Steps

1. Implement embedding service using Spring AI
2. Add database repository and save processed content
3. Integration testing with full message flow
```

**Step 3: Commit**

```bash
git add javainfohunter-processor/ docs/plans/
git commit -m "docs(processor): add module documentation and progress tracker"
```

---

## Final Steps

### Task 12: Integration and Verification

**Step 1: Full build verification**

Run:
```bash
./mvnw.cmd clean package
```

Expected: All modules build successfully

**Step 2: Verify module dependencies**

Run:
```bash
./mvnw.cmd dependency:tree -pl javainfohunter-processor
```

Expected: Shows javainfohunter-ai-service as dependency

**Step 3: Create integration test**

Test the complete flow: Crawler → RabbitMQ → Processor → Database

**Step 4: Final commit**

```bash
git add .
git commit -m "feat(processor): complete content processor module implementation"
```

---

## Summary

This plan creates a complete content processor module that:
1. ✅ Consumes messages from the crawler module via RabbitMQ
2. ✅ Routes content to AI agent processors (Analysis, Summary, Classification)
3. ✅ Uses javainfohunter-ai-service for agent orchestration
4. ✅ Aggregates results from multiple agents
5. ⏳ Stores processed content to database (requires completion)
6. ⏳ Generates embeddings for similarity search (requires completion)

**Total Tasks:** 12
**Estimated Time:** 2-3 hours for basic implementation
**Dependencies:** javainfohunter-crawler ✅, javainfohunter-ai-service ✅
