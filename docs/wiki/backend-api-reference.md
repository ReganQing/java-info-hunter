# JavaInfoHunter API 接口参考

## 概述

所有 REST 接口使用 `/api/v1/` 前缀，返回统一的 `ApiResponse<T>` 格式。

### 基础 URL

```
http://localhost:8080/api/v1
```

### 统一响应格式

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2026-04-08T12:00:00Z"
}
```

错误响应：

```json
{
  "success": false,
  "message": "错误描述",
  "data": null,
  "timestamp": "2026-04-08T12:00:00Z"
}
```

### API 文档

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator: http://localhost:8080/actuator/health

---

## 一、新闻接口（NewsController）

基础路径：`/api/v1/news`

### 1.1 获取新闻列表

```
GET /api/v1/news
```

**描述**：获取分页新闻列表，支持分类、情感、日期范围筛选和排序。

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `category` | String | 否 | - | 按分类筛选 |
| `sentiment` | String | 否 | - | 按情感筛选（POSITIVE, NEGATIVE, NEUTRAL） |
| `startDate` | Instant | 否 | - | 起始日期 |
| `endDate` | Instant | 否 | - | 结束日期 |
| `sortBy` | String | 否 | `publishedAt` | 排序字段（publishedAt, importanceScore, createdAt） |
| `sortDirection` | String | 否 | `DESC` | 排序方向（ASC, DESC） |
| `page` | int | 否 | `0` | 页码（从 0 开始） |
| `size` | int | 否 | `20` | 每页大小（1-100） |

**响应**：`ApiResponse<Page<NewsResponse>>`

**示例**：
```
GET /api/v1/news?category=technology&sentiment=POSITIVE&page=0&size=10
```

### 1.2 根据 ID 获取新闻

```
GET /api/v1/news/{id}
```

**描述**：获取指定新闻的详细信息。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 新闻 ID |

**响应**：`ApiResponse<NewsResponse>`

### 1.3 全文搜索新闻

```
GET /api/v1/news/search
```

**描述**：根据标题、摘要或内容搜索新闻，支持中文全文搜索。

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `query` | String | 是 | - | 搜索关键词（不能为空） |
| `language` | String | 否 | `null` | 内容语言过滤（如 `zh`=中文, `en`=英文，不传则搜索所有语言） |
| `page` | int | 否 | `0` | 页码（从 0 开始） |
| `size` | int | 否 | `20` | 每页大小（1-100） |

**响应**：`ApiResponse<Page<NewsResponse>>`

**示例**：
```
GET /api/v1/news/search?query=machine+learning&page=0&size=10
GET /api/v1/news/search?query=人工智能&language=zh&page=0&size=10
GET /api/v1/news/search?query=spring+boot&language=en
```

### 1.4 获取相似新闻

```
GET /api/v1/news/{id}/similar
```

**描述**：根据主题和标签获取与指定新闻相似的文章。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 新闻 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `limit` | int | 否 | `5` | 最大返回数量（1-50） |

**响应**：`ApiResponse<List<SimilarNewsResponse>>`

### 1.5 获取热门新闻

```
GET /api/v1/news/trending
```

**描述**：获取过去 24 小时的热门新闻，按参与度排序。

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `limit` | int | 否 | `10` | 最大返回数量（1-100） |

**响应**：`ApiResponse<List<NewsResponse>>`

### 1.6 按分类获取新闻

```
GET /api/v1/news/category/{category}
```

**描述**：获取指定分类的分页新闻。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `category` | String | 分类名称 |

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | `0` | 页码（从 0 开始） |
| `size` | int | 否 | `20` | 每页大小（1-100） |

**响应**：`ApiResponse<Page<NewsResponse>>`

---

## 二、RSS 源管理接口（RssSourceController）

基础路径：`/api/v1/rss-sources`

### 2.1 创建 RSS 源

```
POST /api/v1/rss-sources
```

**描述**：创建新的 RSS 订阅源。

**请求体**：`RssSourceRequest`

```json
{
  "name": "TechCrunch",
  "url": "https://techcrunch.com/feed/",
  "description": "Technology news",
  "category": "technology",
  "tags": ["tech", "startups"],
  "crawlIntervalSeconds": 3600,
  "isActive": true
}
```

**响应**：`ApiResponse<RssSourceResponse>`（HTTP 201）

### 2.2 获取 RSS 源列表

```
GET /api/v1/rss-sources
```

**描述**：获取分页 RSS 源列表，支持筛选。

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | `0` | 页码（从 0 开始） |
| `size` | int | 否 | `20` | 每页大小 |
| `category` | String | 否 | - | 按分类筛选 |
| `isActive` | Boolean | 否 | - | 按激活状态筛选 |

**响应**：`ApiResponse<Page<RssSourceResponse>>`

### 2.3 根据 ID 获取 RSS 源

```
GET /api/v1/rss-sources/{id}
```

**描述**：获取指定 RSS 源的详细信息。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | RSS 源 ID |

**响应**：`ApiResponse<RssSourceResponse>`

### 2.4 更新 RSS 源

```
PUT /api/v1/rss-sources/{id}
```

**描述**：更新指定的 RSS 源。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | RSS 源 ID |

**请求体**：`RssSourceRequest`（同创建）

**响应**：`ApiResponse<RssSourceResponse>`

### 2.5 删除 RSS 源

```
DELETE /api/v1/rss-sources/{id}
```

**描述**：删除指定的 RSS 源。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | RSS 源 ID |

**响应**：HTTP 204 No Content

### 2.6 手动触发爬取

```
POST /api/v1/rss-sources/{id}/crawl
```

**描述**：手动触发指定 RSS 源的爬取任务。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | RSS 源 ID |

**响应**：`ApiResponse<Map<String, Object>>`

---

## 三、系统管理接口（AdminController）

基础路径：`/api/v1/admin`

### 3.1 获取系统状态

```
GET /api/v1/admin/status
```

**描述**：获取系统整体健康状态。

**响应**：`ApiResponse<SystemStatusResponse>`

```json
{
  "success": true,
  "data": {
    "status": "HEALTHY",
    "timestamp": "2026-04-08T12:00:00Z",
    "totalRssSources": 5,
    "activeRssSources": 4,
    "totalNews": 1200,
    "pendingProcessing": 3,
    "services": {
      "rssSources": { "total": 5, "active": 4, "status": "UP" },
      "newsProcessing": { "totalArticles": 1200, "pending": 3, "status": "UP" },
      "agentSystem": { "runningExecutions": 2, "totalExecutions": 500, "status": "UP" }
    },
    "uptimeSeconds": 86400,
    "version": "0.0.1-SNAPSHOT"
  }
}
```

**状态说明**：

| 状态 | 条件 |
|------|------|
| HEALTHY | 正常运行 |
| DEGRADED | 待处理数 > 100 或活跃源 < 总源 80% |
| DOWN | 没有活跃 RSS 源 |

### 3.2 获取系统资源使用

```
GET /api/v1/admin/resources
```

**描述**：获取当前系统资源使用情况。

**响应**：`ApiResponse<ResourceUsageResponse>`

```json
{
  "success": true,
  "data": {
    "memoryUsed": 268435456,
    "memoryTotal": 536870912,
    "diskUsed": 0,
    "diskTotal": 0,
    "cpuPercent": 0.0
  }
}
```

### 3.3 获取系统指标

```
GET /api/v1/admin/metrics
```

**描述**：获取系统性能指标。

**响应**：`ApiResponse<SystemMetricsResponse>`

### 3.4 触发全量爬取

```
POST /api/v1/admin/crawl/trigger
```

**描述**：触发所有活跃 RSS 源的爬取任务。

**响应**：`ApiResponse<CrawlTriggerResponse>`

```json
{
  "success": true,
  "data": {
    "triggered": true,
    "message": "Triggered crawl for 4 sources",
    "sourcesTriggered": 4,
    "triggeredAt": "2026-04-08T12:00:00Z",
    "taskIds": "task1,task2,task3,task4",
    "estimatedArticles": 40
  }
}
```

### 3.5 按分类触发爬取

```
POST /api/v1/admin/crawl-by-category
```

**描述**：触发指定分类下所有 RSS 源的爬取。

**请求体**：

```json
{
  "category": "technology"
}
```

**响应**：`ApiResponse<CrawlTriggerResponse>`

### 3.6 触发单个源爬取

```
POST /api/v1/admin/crawl/{sourceId}
```

**描述**：触发指定 RSS 源的爬取。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `sourceId` | Long | RSS 源 ID |

**响应**：`ApiResponse<CrawlTriggerResponse>`

---

## 四、Agent 监控接口（AgentController）

基础路径：`/api/v1/agents`

### 4.1 获取 Agent 执行列表

```
GET /api/v1/agents/executions
```

**描述**：获取分页的 Agent 执行记录。

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | `0` | 页码（从 0 开始） |
| `size` | int | 否 | `20` | 每页大小 |

**响应**：`ApiResponse<Page<AgentExecutionResponse>>`

### 4.2 获取执行详情

```
GET /api/v1/agents/executions/{id}
```

**描述**：获取指定 Agent 执行的详细信息。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 执行记录 ID |

**响应**：`ApiResponse<AgentExecutionResponse>`

### 4.3 获取 Agent 统计

```
GET /api/v1/agents/stats
```

**描述**：获取 Agent 执行的聚合统计数据。

**响应**：`ApiResponse<AgentStatsResponse>`

---

## 五、测试接口（TestController）

基础路径：`/api/v1/test`

### 5.1 健康检查

```
GET /api/v1/test/hello
```

**响应**：

```json
{
  "success": true,
  "data": "Hello from TestController!"
}
```

### 5.2 服务信息

```
GET /api/v1/test/ping
```

**响应**：

```json
{
  "success": true,
  "data": {
    "status": "pong",
    "service": "javainfohunter-api",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

---

## 六、爬虫服务接口（CrawlController）

基础路径：`/api/crawler`（端口 8081，仅开发/测试使用）

### 6.1 触发全量爬取

```
POST /api/crawler/trigger
```

**描述**：手动触发所有活跃 RSS 源的爬取。

**响应**：

```json
{
  "success": true,
  "message": "Manual crawl completed",
  "articlesCrawled": 50,
  "newArticles": 30,
  "duplicateArticles": 15,
  "failedArticles": 5,
  "status": "SUCCESS",
  "triggeredAt": "2026-04-08T12:00:00Z",
  "durationMs": 5000
}
```

### 6.2 触发单个源爬取

```
POST /api/crawler/trigger/{sourceId}
```

**描述**：手动触发指定 RSS 源的爬取。

---

## 响应数据模型

### NewsResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 新闻 ID |
| `title` | String | 标题 |
| `summary` | String | AI 生成摘要 |
| `category` | String | 分类 |
| `tags` | String[] | 标签 |
| `topics` | String[] | 主题 |
| `keywords` | String[] | 关键词 |
| `sentiment` | String | 情感（POSITIVE, NEUTRAL, NEGATIVE） |
| `sentimentScore` | BigDecimal | 情感分数（-1.0 到 1.0） |
| `importanceScore` | BigDecimal | 重要度分数（0.0 到 1.0） |
| `sourceName` | String | 来源名称 |
| `publishedAt` | Instant | 发布时间 |

### RssSourceResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | RSS 源 ID |
| `name` | String | 名称 |
| `url` | String | RSS Feed URL |
| `description` | String | 描述 |
| `category` | String | 分类 |
| `tags` | String[] | 标签 |
| `isActive` | Boolean | 是否激活 |
| `crawlIntervalSeconds` | Integer | 爬取间隔（秒） |
| `lastCrawledAt` | Instant | 最后爬取时间 |
| `totalArticles` | Long | 总文章数 |
| `failedCrawls` | Long | 失败次数 |

### AgentExecutionResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 记录 ID |
| `agentId` | String | Agent ID |
| `agentType` | String | Agent 类型 |
| `executionId` | String | 执行 ID（UUID） |
| `taskType` | String | 任务类型 |
| `status` | String | 状态（RUNNING, COMPLETED, FAILED, CANCELLED） |
| `startTime` | Instant | 开始时间 |
| `endTime` | Instant | 结束时间 |
| `durationMilliseconds` | Integer | 执行时长（毫秒） |
| `totalSteps` | Integer | 总步数 |
| `tokensUsed` | Integer | 使用的 Token 数 |
| `estimatedCostUsd` | BigDecimal | 估算成本（USD） |
| `toolsUsed` | String[] | 使用的工具列表 |
| `coordinationPattern` | String | 协作模式 |

### AgentStatsResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalExecutions` | Long | 总执行次数 |
| `completedExecutions` | Long | 成功次数 |
| `failedExecutions` | Long | 失败次数 |
| `runningExecutions` | Long | 运行中次数 |
| `averageDurationMs` | Double | 平均执行时长 |
| `totalTokensUsed` | Long | 总 Token 使用量 |
| `totalEstimatedCost` | BigDecimal | 总估算成本 |
