# Processor 模块存储逻辑实现计划

> **创建日期:** 2026-03-06
> **目标:** 完善 Processor 模块的存储逻辑，包括向量生成、数据库持久化和消息发布
> **预估工时:** 3-5 天
> **依赖:** javainfohunter-ai-service 模块（已完成）

---

## 📋 模块概述

**模块名称:** javainfohunter-processor 存储逻辑增强

**功能描述:**
- 集成 EmbeddingService 生成向量
- 将处理后的内容持久化到 News 表
- 发布处理后消息到下游队列

**当前状态:** ResultAggregatorImpl.store() 方法中有 3 个 TODO

**技术栈:**
- Spring Boot 4.0.3
- Spring AI Embedding
- Spring Data JPA
- RabbitMQ Spring AMQP

**依赖模块:**
- javainfohunter-ai-service（EmbeddingService, News, NewsRepository）

---

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│              ResultAggregatorImpl.store() 流程               │
└─────────────────────────────────────────────────────────────┘
                            │
                            ↓
        ┌───────────────────────────────────────┐
        │  1. 查找 RawContent（通过 contentHash） │
        └───────────────────────────────────────┘
                            │
                            ↓
        ┌───────────────────────────────────────┐
        │  2. 生成向量（EmbeddingService）       │
        │     输入: title + summary             │
        │     输出: float[1536]                 │
        └───────────────────────────────────────┘
                            │
                            ↓
        ┌───────────────────────────────────────┐
        │  3. 创建 News 实体                    │
        │     - 关联 RawContent                 │
        │     - 设置 AI 分析结果                │
        │     - 存储向量                        │
        └───────────────────────────────────────┘
                            │
                            ↓
        ┌───────────────────────────────────────┐
        │  4. 持久化到数据库（NewsRepository）   │
        └───────────────────────────────────────┘
                            │
                            ↓
        ┌───────────────────────────────────────┐
        │  5. 更新 RawContent 状态为 COMPLETED  │
        └───────────────────────────────────────┘
                            │
                            ↓
        ┌───────────────────────────────────────┐
        │  6. 发布到 processor.processed.queue  │
        └───────────────────────────────────────┘
```

---

## 📝 任务清单

### Task 2.1: 实现 RawContent 查找和向量生成
**预估:** 1 天 | **依赖:** 无

- [ ] 2.1.1 在 ResultAggregatorImpl 中注入依赖
  - `RawContentRepository rawContentRepository`
  - `EmbeddingService embeddingService`
  - `NewsRepository newsRepository`
  - `RabbitTemplate rabbitTemplate`

- [ ] 2.1.2 实现通过 contentHash 查找 RawContent
  ```java
  Optional<RawContent> rawContent = rawContentRepository.findByContentHash(message.getContentHash());
  ```

- [ ] 2.1.3 实现向量生成逻辑
  ```java
  String textForEmbedding = message.getTitle() + " " + message.getSummary();
  float[] embedding = embeddingService.embed(textForEmbedding);
  ```

- [ ] 2.1.4 添加错误处理和日志

**验证:**
- [ ] 单元测试通过
- [ ] 向量维度正确（1536）

---

### Task 2.2: 实现 News 实体创建和持久化
**预估:** 1-2 天 | **依赖:** Task 2.1

- [ ] 2.2.1 创建 ProcessedContentMessage 到 News 的转换方法
  ```java
  private News createNewsEntity(ProcessedContentMessage message, RawContent rawContent, float[] embedding)
  ```

- [ ] 2.2.2 处理数据类型转换
  - `Double sentimentScore` → `BigDecimal`
  - `Double importanceScore` → `BigDecimal`
  - `String sentimentLabel` → `News.Sentiment` enum
  - `List<String> tags/topics` → Entity collections

- [ ] 2.2.3 设置默认值
  - `language = "zh"` (中文内容)
  - `readingTimeMinutes` (基于内容长度计算)
  - `fullContent = rawContent.rawContent`

- [ ] 2.2.4 使用 NewsRepository 保存
  ```java
  News savedNews = newsRepository.save(news);
  ```

- [ ] 2.2.5 添加事务支持 (@Transactional)

**验证:**
- [ ] 单元测试通过
- [ ] 数据库记录正确创建
- [ ] 外键关联正确

---

### Task 2.3: 实现处理后消息发布
**预估:** 0.5-1 天 | **依赖:** Task 2.2

- [ ] 2.3.1 创建 ProcessedContentMessage DTO
  ```java
  @Data
  @Builder
  public class ProcessedContentMessage {
      private Long newsId;
      private String contentHash;
      private String title;
      private String summary;
      // ... 其他字段
  }
  ```

- [ ] 2.3.2 在 RabbitMQConsumerConfig 中添加队列定义
  ```java
  public static final String PROCESSED_QUEUE = "processor.processed.queue";
  ```

- [ ] 2.3.3 使用 RabbitTemplate 发布消息
  ```java
  rabbitTemplate.convertAndSend(
      "processor.direct",
      "processed.content",
      processedMessage
  );
  ```

- [ ] 2.3.4 添加发布确认机制

**验证:**
- [ ] 消息成功发布到队列
- [ ] 消息格式正确

---

### Task 2.4: 更新 RawContent 状态
**预估:** 0.5 天 | **依赖:** Task 2.2

- [ ] 2.4.1 在保存 News 后更新 RawContent 状态
  ```java
  rawContent.setProcessingStatus(ProcessingStatus.COMPLETED);
  rawContentRepository.save(rawContent);
  ```

- [ ] 2.4.2 处理异常情况（设置为 FAILED）

**验证:**
- [ ] 状态正确更新

---

## 📂 文件结构

```
javainfohunter-processor/
├── src/main/java/.../processor/
│   ├── service/
│   │   └── impl/
│   │       └── ResultAggregatorImpl.java     [修改]
│   ├── dto/
│   │       └── ProcessedContentMessage.java   [已存在]
│   ├── config/
│   │   └── RabbitMQConsumerConfig.java        [修改：添加 PROCESSED_QUEUE]
│   └── publisher/
│       └── ProcessedContentPublisher.java     [新建：可选]
```

---

## 📝 详细实现代码

### 1. 更新 ResultAggregatorImpl

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultAggregatorImpl implements ResultAggregator {

    private final EmbeddingService embeddingService;
    private final RawContentRepository rawContentRepository;
    private final NewsRepository newsRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public CompletionStage<Void> store(ProcessedContentMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Storing processed content: hash={}, title={}",
                        message.getContentHash(), message.getTitle());

                // 1. 查找 RawContent
                RawContent rawContent = rawContentRepository
                        .findByContentHash(message.getContentHash())
                        .orElseThrow(() -> new IllegalStateException(
                                "RawContent not found for hash: " + message.getContentHash()));

                // 2. 生成向量
                String textForEmbedding = buildTextForEmbedding(message);
                float[] embedding = embeddingService.embed(textForEmbedding);
                log.debug("Generated embedding with {} dimensions", embedding.length);

                // 3. 创建 News 实体
                News news = createNewsEntity(message, rawContent, embedding);

                // 4. 保存到数据库
                News savedNews = newsRepository.save(news);
                log.info("Saved news with ID: {}", savedNews.getId());

                // 5. 更新 RawContent 状态
                rawContent.setProcessingStatus(ProcessingStatus.COMPLETED);
                rawContentRepository.save(rawContent);

                // 6. 发布到下游队列
                publishProcessedMessage(savedNews, message);

                log.debug("Processed content stored successfully: hash={}", message.getContentHash());

            } catch (Exception e) {
                log.error("Failed to store processed content: hash={}",
                        message.getContentHash(), e);
                // 更新状态为 FAILED
                updateRawContentStatus(message.getContentHash(), ProcessingStatus.FAILED);
                throw new CompletionException(e);
            }
        });
    }

    private String buildTextForEmbedding(ProcessedContentMessage message) {
        // 使用标题 + 摘要生成向量
        String summary = message.getSummary() != null ? message.getSummary() : "";
        return message.getTitle() + " " + summary;
    }

    private News createNewsEntity(ProcessedContentMessage message,
                                   RawContent rawContent,
                                   float[] embedding) {
        // 向量转换为 pgvector 格式字符串
        String embeddingStr = convertEmbeddingToString(embedding);

        return News.builder()
                .rawContent(rawContent)
                .title(message.getTitle())
                .summary(message.getSummary())
                .fullContent(rawContent.getRawContent())
                .sentiment(parseSentiment(message.getSentimentLabel()))
                .sentimentScore(convertToBigDecimal(message.getSentimentScore()))
                .importanceScore(convertToBigDecimal(message.getImportanceScore()))
                .category(message.getCategory())
                .tags(message.getTags())
                .keywords(message.getKeywords())
                .language("zh")
                .readingTimeMinutes(calculateReadingTime(rawContent.getRawContent()))
                .isPublished(false)  // 需要人工审核后才发布
                .build();
    }

    private String convertEmbeddingToString(float[] embedding) {
        // 将 float[] 转换为 pgvector 格式 "[0.1,0.2,...]"
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private News.Sentiment parseSentiment(String sentimentLabel) {
        if (sentimentLabel == null) return News.Sentiment.NEUTRAL;
        return switch (sentimentLabel.toLowerCase()) {
            case "positive" -> News.Sentiment.POSITIVE;
            case "negative" -> News.Sentiment.NEGATIVE;
            default -> News.Sentiment.NEUTRAL;
        };
    }

    private BigDecimal convertToBigDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calculateReadingTime(String content) {
        // 平均阅读速度：300 字/分钟
        int wordCount = content.length();
        return Math.max(1, wordCount / 300);
    }

    private void publishProcessedMessage(News news, ProcessedContentMessage message) {
        ProcessedContentDTO dto = ProcessedContentDTO.builder()
                .newsId(news.getId())
                .contentHash(message.getContentHash())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategory())
                .sentiment(news.getSentiment().toString())
                .importanceScore(news.getImportanceScore())
                .processedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                "processor.direct",
                "processed.content",
                dto
        );
        log.debug("Published processed message for news ID: {}", news.getId());
    }

    private void updateRawContentStatus(String contentHash, ProcessingStatus status) {
        rawContentRepository.findByContentHash(contentHash).ifPresent(rawContent -> {
            rawContent.setProcessingStatus(status);
            rawContentRepository.save(rawContent);
        });
    }
}
```

### 2. 新建 ProcessedContentDTO

```java
package com.ron.javainfohunter.processor.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ProcessedContentDTO {
    private Long newsId;
    private String contentHash;
    private String title;
    private String summary;
    private String category;
    private List<String> tags;
    private List<String> keywords;
    private String sentiment;
    private Double importanceScore;
    private Instant processedAt;
}
```

### 3. 更新 RabbitMQConsumerConfig

```java
/**
 * Queue for processed content to be published downstream
 */
public static final String PROCESSED_QUEUE = "processor.processed.queue";
public static final String PROCESSED_ROUTING_KEY = "processed.content";

@Bean
public Queue processedQueue() {
    return QueueBuilder
            .durable(PROCESSED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "processed.dlq")
            .build();
}

@Bean
public Binding processedBinding(
        @Qualifier("processedQueue") Queue processedQueue,
        @Qualifier("processorExchange") DirectExchange processorExchange
) {
    return BindingBuilder
            .bind(processedQueue)
            .to(processorExchange)
            .with(PROCESSED_ROUTING_KEY);
}
```

---

## ✅ 完成标准

- [ ] 所有 TODO 完成
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] 集成测试通过（使用 Testcontainers）
- [ ] 向量正确生成并存储
- [ ] News 实体正确持久化
- [ ] 消息成功发布到队列
- [ ] 异常处理完善

---

## 🐛 问题与风险

| 问题 | 影响 | 缓解措施 | 状态 |
|------|------|----------|------|
| Embedding API 限流 | 中 | 添加重试逻辑和降级 | ⏳ |
| 向量存储格式 | 低 | 使用 pgvector 兼容格式 | ⏳ |
| 数据库约束冲突 | 中 | 处理唯一约束冲突 | ⏳ |
| 消息发布失败 | 低 | 记录日志，更新状态为 FAILED | ⏳ |

---

## 📖 相关文档

- [技术方案.md](../../技术方案.md)
- [数据库设计说明.md](../../数据库设计说明.md)
- [roadmap.md](../roadmap.md)
- [2026-03-05-content-processor-module-progress.md](./2026-03-05-content-processor-module-progress.md)

---

**最后更新:** 2026-03-06
**维护者:** JavaInfoHunter Team
