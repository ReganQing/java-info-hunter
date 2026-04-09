# JavaInfoHunter 数据库设计

## 概述

- **数据库**: PostgreSQL 16+
- **迁移工具**: Flyway
- **迁移目录**: `javainfohunter-ai-service/src/main/resources/db/migration/`
- **扩展**: pgvector（向量相似度搜索）

## 数据库连接

```
jdbc:postgresql://localhost:5432/javainfohunter
```

所有服务共享同一个数据库，使用 HikariCP 连接池（最大 10 连接）。

---

## 表结构

### ER 关系图

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  rss_sources │ 1   N │  raw_content │ 1   1 │     news     │
│              │───────│              │───────│              │
│              │       │              │       │              │
└──────────────┘       └──────┬───────┘       └──────┬───────┘
                              │                      │
                              │ N                    │ N
                              │                      │
                       ┌──────┴───────┐       ┌──────┴───────┐
                       │ agent_       │       │ agent_       │
                       │ executions   │       │ executions   │
                       └──────────────┘       └──────────────┘
```

---

### 1. rss_sources（RSS 订阅源表）

**用途**：存储 RSS 订阅源信息，包括爬取配置和统计数据。

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 主键 |
| `name` | VARCHAR(255) | NOT NULL | 源名称 |
| `url` | VARCHAR(2048) | NOT NULL, UNIQUE | RSS Feed URL |
| `description` | TEXT | - | 描述 |
| `category` | VARCHAR(100) | - | 分类 |
| `tags` | TEXT[] | - | 标签数组 |
| `crawl_interval_seconds` | INTEGER | NOT NULL, DEFAULT 3600 | 爬取间隔（秒） |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否激活 |
| `max_retries` | INTEGER | NOT NULL, DEFAULT 3 | 最大重试次数 |
| `retry_backoff_seconds` | INTEGER | NOT NULL, DEFAULT 60 | 重试退避（秒） |
| `language` | VARCHAR(10) | DEFAULT 'en' | 语言 |
| `timezone` | VARCHAR(50) | DEFAULT 'UTC' | 时区 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新时间 |
| `last_crawled_at` | TIMESTAMPTZ | - | 最后爬取时间 |
| `total_articles` | BIGINT | NOT NULL, DEFAULT 0 | 总文章数 |
| `failed_crawls` | BIGINT | NOT NULL, DEFAULT 0 | 失败次数 |

**约束**：
- `chk_crawl_interval_positive`: `crawl_interval_seconds > 0`
- `chk_max_retries_positive`: `max_retries >= 0`
- `chk_retry_backoff_positive`: `retry_backoff_seconds >= 0`

**索引**：

| 索引名 | 类型 | 列 | 说明 |
|--------|------|-----|------|
| `idx_rss_sources_active` | B-tree | `is_active` WHERE is_active = TRUE | 部分索引，仅索引活跃源 |
| `idx_rss_sources_category` | B-tree | `category` | 分类查询 |
| `idx_rss_sources_tags` | GIN | `tags` | 数组查询 |
| `idx_rss_sources_last_crawled` | B-tree | `last_crawled_at` | 按爬取时间排序 |

---

### 2. raw_content（原始内容表）

**用途**：存储从 RSS Feed 抓取的原始内容，等待 AI 处理。

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 主键 |
| `rss_source_id` | BIGINT | NOT NULL, FK → rss_sources(id) ON DELETE CASCADE | 所属 RSS 源 |
| `guid` | VARCHAR(255) | NOT NULL | RSS Feed 中的唯一标识 |
| `title` | VARCHAR(500) | NOT NULL | 文章标题 |
| `link` | VARCHAR(2048) | - | 文章链接 |
| `raw_content` | TEXT | NOT NULL | 原始 HTML 或纯文本 |
| `content_hash` | VARCHAR(64) | NOT NULL, UNIQUE | SHA-256 哈希（去重用） |
| `author` | VARCHAR(255) | - | 作者 |
| `publish_date` | TIMESTAMPTZ | - | 原始发布日期 |
| `crawl_date` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 抓取日期 |
| `processing_status` | VARCHAR(20) | NOT NULL, DEFAULT 'pending' | 处理状态 |
| `error_message` | TEXT | - | 错误信息 |
| `embedding` | vector(1536) | - | 向量嵌入 |
| `embedding_generated_at` | TIMESTAMPTZ | - | 嵌入生成时间 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新时间 |

**处理状态枚举**：`pending` → `processing` → `completed` / `failed`

**约束**：
- `chk_processing_status`: `processing_status IN ('pending', 'processing', 'completed', 'failed')`
- `uq_raw_content_guid_source`: UNIQUE `(guid, rss_source_id)` — 同源去重
- `uq_raw_content_hash`: UNIQUE `(content_hash)` — 全局去重

**索引**：

| 索引名 | 类型 | 列 | 说明 |
|--------|------|-----|------|
| `idx_raw_content_rss_source` | B-tree | `rss_source_id` | 按源查询 |
| `idx_raw_content_status` | B-tree | `processing_status` | 按状态查询 |
| `idx_raw_content_publish_date` | B-tree | `publish_date DESC` | 按发布时间排序 |
| `idx_raw_content_crawl_date` | B-tree | `crawl_date DESC` | 按抓取时间排序 |
| `idx_raw_content_embedding` | **IVFFlat** | `embedding vector_cosine_ops` (lists=100) | 向量相似度搜索 |

---

### 3. news（处理后新闻表）

**用途**：存储经过 AI 分析处理后的新闻文章，包含摘要、分类、情感等丰富元数据。

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 主键 |
| `raw_content_id` | BIGINT | NOT NULL, UNIQUE, FK → raw_content(id) ON DELETE CASCADE | 原始内容（一对一） |
| `title` | VARCHAR(500) | NOT NULL | 标题 |
| `summary` | TEXT | NOT NULL | AI 生成摘要 |
| `full_content` | TEXT | - | 清洗后的全文 |
| `topics` | TEXT[] | - | AI 提取的主题 |
| `keywords` | TEXT[] | - | AI 提取的关键词 |
| `sentiment` | VARCHAR(20) | - | 情感分类（positive, neutral, negative） |
| `sentiment_score` | DECIMAL(3,2) | - | 情感分数（-1.0 到 1.0） |
| `importance_score` | DECIMAL(3,2) | - | 重要度分数（0.0 到 1.0） |
| `category` | VARCHAR(100) | - | AI 分配的分类 |
| `tags` | TEXT[] | - | AI 分配的标签 |
| `language` | VARCHAR(10) | NOT NULL, DEFAULT 'en' | 语言 |
| `reading_time_minutes` | INTEGER | - | 预计阅读时间 |
| `slug` | VARCHAR(255) | UNIQUE | URL 友好标识 |
| `featured_image_url` | VARCHAR(2048) | - | 特色图片 URL |
| `view_count` | BIGINT | NOT NULL, DEFAULT 0 | 浏览次数 |
| `like_count` | INTEGER | NOT NULL, DEFAULT 0 | 点赞次数 |
| `share_count` | INTEGER | NOT NULL, DEFAULT 0 | 分享次数 |
| `is_published` | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否发布 |
| `is_featured` | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否精选 |
| `published_at` | TIMESTAMPTZ | - | 发布时间 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新时间 |

**约束**：
- `chk_sentiment_values`: `sentiment IN ('positive', 'neutral', 'negative')`
- `chk_sentiment_score_range`: `sentiment_score BETWEEN -1.0 AND 1.0`
- `chk_importance_score_range`: `importance_score BETWEEN 0.0 AND 1.0`
- `chk_reading_time_positive`: `reading_time_minutes IS NULL OR reading_time_minutes > 0`

**索引**：

| 索引名 | 类型 | 列 | 说明 |
|--------|------|-----|------|
| `idx_news_raw_content` | B-tree | `raw_content_id` | 关联查询 |
| `idx_news_published` | B-tree | `is_published, published_at DESC` | 已发布新闻排序 |
| `idx_news_category` | B-tree | `category` | 分类查询 |
| `idx_news_tags` | GIN | `tags` | 标签数组查询 |
| `idx_news_topics` | GIN | `topics` | 主题数组查询 |
| `idx_news_keywords` | GIN | `keywords` | 关键词数组查询 |
| `idx_news_sentiment` | B-tree | `sentiment` | 情感筛选 |
| `idx_news_importance` | B-tree | `importance_score DESC` | 按重要度排序 |
| `idx_news_slug` | B-tree | `slug` | URL 路由 |
| `idx_news_created_at` | B-tree | `created_at DESC` | 按创建时间排序 |
| `idx_news_fulltext` | **GIN** | `to_tsvector('english', title \|\| ' ' \|\| COALESCE(summary, '') \|\| ' ' \|\| COALESCE(full_content, ''))` | 全文搜索 |

---

### 4. agent_executions（Agent 执行记录表）

**用途**：记录 AI Agent 的执行过程，用于调试、分析和性能优化。

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 主键 |
| `agent_id` | VARCHAR(100) | NOT NULL | Agent 注册 ID |
| `agent_type` | VARCHAR(50) | NOT NULL | Agent 类型 |
| `agent_name` | VARCHAR(255) | - | Agent 名称 |
| `execution_id` | VARCHAR(100) | NOT NULL, UNIQUE | 执行 ID（UUID） |
| `task_type` | VARCHAR(100) | - | 任务类型 |
| `coordination_pattern` | VARCHAR(50) | - | 协作模式 |
| `input_data` | JSONB | - | 输入参数（JSON） |
| `output_data` | JSONB | - | 输出结果（JSON） |
| `error_trace` | TEXT | - | 错误堆栈 |
| `status` | VARCHAR(20) | NOT NULL | 状态 |
| `start_time` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 开始时间 |
| `end_time` | TIMESTAMPTZ | - | 结束时间 |
| `duration_milliseconds` | INTEGER | - | 执行时长（毫秒） |
| `total_steps` | INTEGER | - | 总步数 |
| `tokens_used` | INTEGER | - | 使用 Token 数 |
| `estimated_cost_usd` | DECIMAL(10,4) | - | 估算成本（USD） |
| `tools_used` | TEXT[] | - | 使用的工具列表 |
| `tool_call_count` | INTEGER | - | 工具调用次数 |
| `raw_content_id` | BIGINT | FK → raw_content(id) ON DELETE SET NULL | 关联原始内容 |
| `news_id` | BIGINT | FK → news(id) ON DELETE SET NULL | 关联新闻 |
| `parent_execution_id` | VARCHAR(100) | - | 父执行 ID（链式/协调） |
| `correlation_id` | VARCHAR(100) | - | 关联 ID（跨 Agent 追踪） |
| `retry_count` | INTEGER | NOT NULL, DEFAULT 0 | 当前重试次数 |
| `max_retries` | INTEGER | NOT NULL, DEFAULT 3 | 最大重试次数 |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新时间 |

**状态枚举**：`running` → `completed` / `failed` / `cancelled`

**协作模式枚举**：`chain`, `parallel`, `master_worker`, `standalone`

**约束**：
- `chk_agent_execution_status`: `status IN ('running', 'completed', 'failed', 'cancelled')`
- `chk_duration_positive`: `duration_milliseconds IS NULL OR duration_milliseconds >= 0`
- `chk_tokens_positive`: `tokens_used IS NULL OR tokens_used >= 0`
- `chk_retry_count_valid`: `retry_count >= 0 AND retry_count <= max_retries`

**索引**：

| 索引名 | 类型 | 列 | 说明 |
|--------|------|-----|------|
| `idx_agent_executions_agent_id` | B-tree | `agent_id` | 按 Agent 查询 |
| `idx_agent_executions_agent_type` | B-tree | `agent_type` | 按类型查询 |
| `idx_agent_executions_execution_id` | B-tree | `execution_id` | 唯一执行 ID |
| `idx_agent_executions_status` | B-tree | `status` | 按状态查询 |
| `idx_agent_executions_start_time` | B-tree | `start_time DESC` | 按时间排序 |
| `idx_agent_executions_task_type` | B-tree | `task_type` | 按任务类型查询 |
| `idx_agent_executions_coordination_pattern` | B-tree | `coordination_pattern` | 按协作模式查询 |
| `idx_agent_executions_correlation_id` | B-tree | `correlation_id` | 关联查询 |
| `idx_agent_executions_parent_execution_id` | B-tree | `parent_execution_id` | 父子关系查询 |
| `idx_agent_executions_raw_content_id` | B-tree | `raw_content_id` | 关联原始内容 |
| `idx_agent_executions_news_id` | B-tree | `news_id` | 关联新闻 |
| `idx_agent_executions_agent_time` | 复合 B-tree | `agent_id, start_time DESC` | Agent 时间范围查询 |
| `idx_agent_executions_status_time` | 复合 B-tree | `status, start_time DESC` | 状态时间范围查询 |

---

## 索引策略

### B-tree 索引

用于等值查询、范围查询和排序：
- 主键、外键、状态字段、时间字段

### GIN 索引（Generalized Inverted Index）

用于数组类型和全文搜索：
- `tags`, `topics`, `keywords` — PostgreSQL 数组查询
- 全文搜索 — `to_tsvector()` 生成的倒排索引

```sql
-- 数组查询示例
SELECT * FROM news WHERE 'java' = ANY(keywords);

-- 全文搜索示例
SELECT * FROM news
WHERE to_tsvector('english', title || ' ' || COALESCE(summary, '') || ' ' || COALESCE(full_content, ''))
      @@ to_tsquery('english', 'machine & learning')
ORDER BY ts_rank(...) DESC
LIMIT 20;
```

### IVFFlat 索引（向量索引）

用于向量相似度搜索：
- `raw_content.embedding` — 1536 维向量

```sql
-- 向量相似度搜索示例
SELECT * FROM raw_content
ORDER BY embedding <=> '[0.1, 0.2, ...]'
LIMIT 10;
```

> **注意**：IVFFlat 索引在数据量较少时（< 1000 行）可能不如顺序扫描快，建议在数据量较大后再评估是否需要调整 `lists` 参数。

---

## 数据库视图

### active_rss_sources

活跃 RSS 源及爬取统计：

```sql
SELECT id, name, url, category, tags, crawl_interval_seconds,
       last_crawled_at, total_articles, failed_crawls,
       ROUND(100.0 * failed_crawls / NULLIF(total_articles, 0), 2) AS failure_rate
FROM rss_sources WHERE is_active = TRUE
ORDER BY last_crawled_at ASC NULLS LAST;
```

### pending_content

待处理内容：

```sql
SELECT rc.id, rc.rss_source_id, rs.name AS source_name,
       rc.title, rc.link, rc.publish_date, rc.crawl_date, rc.processing_status
FROM raw_content rc JOIN rss_sources rs ON rc.rss_source_id = rs.id
WHERE rc.processing_status = 'pending'
ORDER BY rc.crawl_date ASC;
```

### published_news

已发布新闻及完整元数据：

```sql
SELECT n.id, n.title, n.summary, n.category, n.tags, n.topics, n.keywords,
       n.sentiment, n.importance_score, n.view_count, n.like_count, n.published_at,
       rs.name AS source_name, rc.link AS original_link,
       rc.author AS original_author, rc.publish_date AS original_publish_date
FROM news n
JOIN raw_content rc ON n.raw_content_id = rc.id
JOIN rss_sources rs ON rc.rss_source_id = rs.id
WHERE n.is_published = TRUE
ORDER BY n.published_at DESC;
```

### agent_execution_stats

Agent 执行统计（最近 7 天）：

```sql
SELECT agent_id, agent_type,
       COUNT(*) AS total_executions,
       COUNT(*) FILTER (WHERE status = 'completed') AS completed_count,
       COUNT(*) FILTER (WHERE status = 'failed') AS failed_count,
       COUNT(*) FILTER (WHERE status = 'running') AS running_count,
       ROUND(AVG(duration_milliseconds) / 1000.0, 2) AS avg_duration_seconds,
       ROUND(SUM(tokens_used), 0) AS total_tokens,
       ROUND(SUM(estimated_cost_usd), 4) AS total_cost_usd,
       ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'failed') / NULLIF(COUNT(*), 0), 2) AS failure_rate
FROM agent_executions WHERE start_time >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY agent_id, agent_type
ORDER BY total_executions DESC;
```

---

## 自动更新触发器

所有四张表都配置了 `updated_at` 自动更新触发器：

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## Flyway 数据库迁移

### 迁移文件位置

```
javainfohunter-ai-service/src/main/resources/db/migration/
└── V1__init_schema.sql    # 初始数据库架构
```

### 命名规范

```
V{version}__{description}.sql
```

- `V1__init_schema.sql` — 初始架构
- `V2__add_xxx.sql` — 后续迁移（版本号递增）

### 迁移命令

```bash
# 查看迁移状态
./mvnw.cmd flyway:info

# 执行迁移
./mvnw.cmd flyway:migrate

# 修复迁移状态（仅开发环境）
./mvnw.cmd flyway:repair

# 带环境配置的迁移
./mvnw.cmd flyway:migrate -Dflyway.configFiles=src/main/resources/application.yml
```

### 迁移配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### 添加新迁移

1. 在 `javainfohunter-ai-service/src/main/resources/db/migration/` 下创建 SQL 文件
2. 遵循 Flyway 命名规范（V2, V3, ...）
3. 所有数据库变更必须在此模块中进行
4. 新实体需要对应的 Flyway 迁移脚本

---

## Repository 接口

所有 Repository 位于 `javainfohunter-ai-service` 模块，跨模块共享。

| Repository | 实体 | 常用方法 |
|-----------|------|---------|
| `RssSourceRepository` | RssSource | `findByIsActiveTrue()`, `findByCategory()` |
| `RawContentRepository` | RawContent | `findByProcessingStatus()`, `findByContentHash()` |
| `NewsRepository` | News | `findByIsPublishedTrue()`, `findByCategory()` |
| `AgentExecutionRepository` | AgentExecution | `findByAgentId()`, `findByStatus()` |
