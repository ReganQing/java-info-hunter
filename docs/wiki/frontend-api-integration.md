# 前端 API 通信文档

> 本文档描述 JavaInfoHunter 前端与后端 API 的通信架构、端点定义和类型系统。

## API 架构总览

```
前端 (React)                     后端 (Spring Boot)
     │                                    │
     │  Axios Client                      │
     │  (baseURL: /api/v1)               │
     │                                    │
     ├──── GET  /news ──────────────────→ │  NewsController
     ├──── GET  /news/:id ─────────────→ │
     ├──── GET  /news/search ──────────→ │
     ├──── GET  /news/trending ────────→ │
     ├──── GET  /news/:id/similar ─────→ │
     ├──── GET  /news/category/:cat ───→ │
     │                                    │
     ├──── GET  /rss-sources ──────────→ │  RssSourceController
     ├──── POST /rss-sources ──────────→ │
     ├──── PUT  /rss-sources/:id ──────→ │
     ├──── DELETE /rss-sources/:id ────→ │
     ├──── POST /rss-sources/:id/crawl → │
     │                                    │
     ├──── GET  /agents/stats ─────────→ │  AgentController
     ├──── GET  /agents/executions ────→ │
     ├──── GET  /agents/executions/:id → │
     │                                    │
     ├──── GET  /admin/status ─────────→ │  AdminController
     ├──── GET  /admin/resources ──────→ │
     ├──── GET  /admin/metrics ────────→ │
     ├──── POST /admin/crawl/trigger ──→ │
     ├──── POST /admin/crawl/:id ──────→ │
     └──── POST /admin/crawl-by-category→│
```

---

## Axios 客户端配置

文件：`src/shared/api/client.ts`

```typescript
function createApiClient(): AxiosInstance {
  const baseURL = import.meta.env.VITE_API_URL
    || (import.meta.env.DEV ? '/api/v1' : 'http://localhost:8080/api/v1');

  const client = axios.create({
    baseURL,
    timeout: 30000,                    // 30 秒超时
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // 响应拦截器：自动解包 response.data
  client.interceptors.response.use(
    (response) => response.data,       // 直接返回 data，省去 .data 调用
    (error) => {
      const errorMsg = error.response?.data?.message || 'An error occurred';
      toast.error(errorMsg);            // Sonner Toast 弹出错误通知
      return Promise.reject(error);
    }
  );

  return client;
}
```

### 关键设计决策

| 决策 | 说明 |
|------|------|
| baseURL 策略 | 开发环境使用相对路径 `/api/v1`（走 Vite 代理）；生产环境使用环境变量或默认 `localhost:8080` |
| 响应解包 | 拦截器直接返回 `response.data`，调用方无需 `.data` |
| 错误处理 | 统一通过 Sonner Toast 弹出错误消息 |
| 超时设置 | 30 秒全局超时（AI 处理接口可能较慢） |
| 环境变量 | `VITE_API_URL` 可覆盖默认 API 地址 |

### 开发代理

```typescript
// vite.config.ts
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

开发环境下，前端请求 `/api/v1/news` 会被 Vite 代理到 `http://localhost:8080/api/v1/news`。

---

## API 端点定义

文件：`src/shared/api/endpoints.ts`

### newsApi - 新闻接口

| 方法 | 端点 | 参数 | 返回类型 | 描述 |
|------|------|------|----------|------|
| `getList` | `GET /news` | `page, size, category, sentiment, startDate, endDate, sortBy, sortDirection` | `NewsResponse[]` | 获取新闻列表 |
| `getById` | `GET /news/:id` | `id: number` | `NewsResponse` | 获取单条新闻详情 |
| `search` | `GET /news/search` | `query: string, page, size` | `NewsResponse[]` | 搜索新闻 |
| `getSimilar` | `GET /news/:id/similar` | `id: number, limit` | `NewsResponse[]` | 获取相似新闻 |
| `getTrending` | `GET /news/trending` | `limit: number` | `NewsResponse[]` | 获取热门新闻 |
| `getByCategory` | `GET /news/category/:category` | `category: string, page, size` | `NewsResponse[]` | 按分类获取新闻 |

### sourcesApi - RSS 源接口

| 方法 | 端点 | 参数 | 返回类型 | 描述 |
|------|------|------|----------|------|
| `getList` | `GET /rss-sources` | `page, size, category, isActive` | `RssSourceResponse[]` | 获取源列表 |
| `getById` | `GET /rss-sources/:id` | `id: number` | `RssSourceResponse` | 获取源详情 |
| `create` | `POST /rss-sources` | `{ name, url, category, tags? }` | `RssSourceResponse` | 创建源 |
| `update` | `PUT /rss-sources/:id` | `id, { name, url, category, tags? }` | `RssSourceResponse` | 更新源 |
| `delete` | `DELETE /rss-sources/:id` | `id: number` | `void` | 删除源 |
| `triggerCrawl` | `POST /rss-sources/:id/crawl` | `id: number` | `{ taskId: string }` | 触发单源爬取 |

### agentsApi - Agent 接口

| 方法 | 端点 | 参数 | 返回类型 | 描述 |
|------|------|------|----------|------|
| `getExecutions` | `GET /agents/executions` | `page, size` | `AgentExecutionResponse[]` | 获取执行列表 |
| `getExecutionById` | `GET /agents/executions/:id` | `id: number` | `AgentExecutionResponse` | 获取执行详情 |
| `getStats` | `GET /agents/stats` | - | `AgentStatsResponse` | 获取 Agent 统计 |

### adminApi - 管理接口

| 方法 | 端点 | 参数 | 返回类型 | 描述 |
|------|------|------|----------|------|
| `getStatus` | `GET /admin/status` | - | `SystemStatusResponse` | 系统状态 |
| `getResources` | `GET /admin/resources` | - | `ResourceUsage` | 资源使用 |
| `getMetrics` | `GET /admin/metrics` | - | `SystemMetrics` | 系统指标 |
| `triggerFullCrawl` | `POST /admin/crawl/trigger` | - | `{ triggered, sourcesTriggered }` | 全量爬取 |
| `triggerSourceCrawl` | `POST /admin/crawl/:sourceId` | `sourceId: number` | `{ triggered }` | 单源爬取 |
| `triggerCategoryCrawl` | `POST /admin/crawl-by-category` | `{ category }` | `{ triggered, sourcesTriggered }` | 分类爬取 |

### systemApi - 系统接口（adminApi 别名）

| 方法 | 端点 | 描述 |
|------|------|------|
| `getHealth` | `GET /admin/status` | 系统健康状态 |
| `getResources` | `GET /admin/resources` | 资源使用 |
| `getMetrics` | `GET /admin/metrics` | 系统指标（支持 period 参数） |
| `getCrawlProgress` | `GET /admin/crawl/progress/:taskId` | 爬取进度（后端未实现） |

---

## TypeScript 类型定义

文件：`src/shared/api/types.ts`

### 通用响应包装

```typescript
interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}
```

### 新闻类型

```typescript
interface NewsResponse {
  id: number;
  title: string;
  summary: string;
  category: string;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  topics: string[];
  keywords: string[];
  importanceScore: number;
  publishedAt: string;
  sourceName: string;
  url?: string;
}
```

### RSS 源类型

```typescript
interface RssSourceResponse {
  id: number;
  name: string;
  url: string;
  category: string;
  tags: string[];
  isActive: boolean;
  lastCrawledAt: string | null;
  totalArticles: number;
  failedCrawls: number;
}
```

### Agent 类型

```typescript
interface AgentExecutionResponse {
  id: number;
  agentId: string;
  agentType: string;
  status: 'running' | 'completed' | 'failed' | 'cancelled';
  startTime: string;
  durationMilliseconds: number;
  tokensUsed: number;
}

interface AgentStatsResponse {
  totalExecutions: number;
  runningExecutions: number;
  completedExecutions: number;
  failedExecutions: number;
  totalTokens: number;
  totalCostUsd: number;
}
```

### 系统状态类型

```typescript
type SystemHealthStatus = 'HEALTHY' | 'DEGRADED' | 'DOWN';
type ServiceStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';

interface ServiceInfo {
  status: ServiceStatus;
  responseTime?: number;
  lastCheck?: string;
  error?: string;
  active?: number;
  total?: number;
  message?: string;
  [key: string]: string | number | undefined;
}

interface SystemStatusResponse {
  status: SystemHealthStatus;
  totalRssSources: number;
  activeRssSources: number;
  totalNews: number;
  pendingProcessing: number;
  services: Record<string, ServiceInfo>;
  resources?: ResourceUsage;
  metrics?: SystemMetrics;
  uptimeSeconds?: number;
  version?: string;
}

interface ResourceUsage {
  cpuPercent: number;
  memoryUsed: number;
  memoryTotal: number;
  diskUsed: number;
  diskTotal: number;
}

interface SystemMetrics {
  uptime: number;
  requestRate: MetricPoint[];
  errorRate: MetricPoint[];
  activeConnections: number;
}

interface MetricPoint {
  timestamp: string;
  value: number;
}

interface CrawlProgress {
  taskId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  totalSources: number;
  completedSources: number;
  currentSource?: string;
}
```

---

## React Query Hooks

文件：`src/shared/hooks/useNews.ts`

### useNews

获取新闻列表，自动关联 Zustand 中的 `newsFilters` 作为查询参数。

```typescript
const { data, isLoading, refetch } = useNews(enabled = true);

// 等效于：
useQuery({
  queryKey: ['news', newsFilters],
  queryFn: () => newsApi.getList({
    page: 0,
    size: 100,
    category: newsFilters.category,
    sentiment: newsFilters.sentiment,
    sortBy: 'publishedAt',
    sortDirection: 'DESC',
  }),
  enabled,
});
```

**特点**：
- 查询键包含 `newsFilters` 对象，任何筛选条件变化自动触发重新查询
- 默认按 `publishedAt DESC` 排序
- 最多获取 100 条记录（客户端分页）

### useNewsById

根据 ID 获取单条新闻详情。

```typescript
const { data, isLoading } = useNewsById(id, enabled = true);

// 等效于：
useQuery({
  queryKey: ['news', id],
  queryFn: () => newsApi.getById(id),
  enabled: enabled && !!id,
});
```

**特点**：
- `enabled` 取决于 `id` 是否存在，避免无效请求

### useTrendingNews

获取热门新闻，支持自动刷新。

```typescript
const { data, isLoading } = useTrendingNews(limit = 10, enabled = true);

// 等效于：
useQuery({
  queryKey: ['trending', limit],
  queryFn: () => newsApi.getTrending(limit),
  enabled,
  refetchInterval: 60000,  // 每分钟自动刷新
});
```

**特点**：
- 默认每 60 秒自动刷新，保持热门新闻的时效性

---

## QueryClient 全局配置

文件：`src/shared/api/query-client.ts`

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,       // 5 分钟内数据视为新鲜
      gcTime: 10 * 60 * 1000,          // 缓存数据 10 分钟后回收
      retry: 1,                         // 失败重试 1 次
      refetchOnWindowFocus: false,      // 禁止窗口聚焦自动刷新
    },
  },
});
```

### 各模块刷新策略

| 模块 | Query Key | 刷新间隔 | 说明 |
|------|-----------|----------|------|
| Dashboard | `['system-status']` | 30s | 系统状态需要较频繁更新 |
| Dashboard | `['trending', 10]` | 60s | 热门新闻 1 分钟更新 |
| News | `['news', newsFilters]` | 手动 | 用户手动刷新或筛选变化时触发 |
| Trends | `['news-trends', period]` | 5min | 趋势数据更新频率较低 |
| Agents Stats | `['agent-stats']` | 30s | Agent 统计 30 秒更新 |
| Agents Executions | `['agent-executions']` | 15s | 执行历史 15 秒更新 |
| System | `['system-status']` | 30s | 系统状态 30 秒更新 |
| Sources | `['sources']` | 手动 | 仅在 mutation 成功后刷新 |

---

## Mock 数据策略

文件：`src/shared/mocks/index.ts`

当后端 API 不可用时，前端使用 Mock 数据保证开发体验：

| Mock 函数 | 模拟数据 | 使用位置 |
|-----------|----------|----------|
| `generateMockExecutions()` | 25 条 Agent 执行记录 | `AdminAgents` |
| `generateMockStats()` | Agent 统计数据 | `AdminAgents` |
| `generateMockMetrics()` | 执行时间趋势、任务分布、状态分布 | `AdminAgents` |
| `generateMockSystemStatus()` | 系统状态、资源、指标 | `AdminSystem` |
| `generateMockResourceUsage()` | CPU/内存/磁盘使用 | `AdminSystem` |

**Mock 回退策略**：

```typescript
// AdminAgents：API 失败时使用 Mock
const stats = statsData?.data ?? generateMockStats();
const executions = executionsData?.data ?? generateMockExecutions();

// AdminSystem：catch 中使用 Mock
try {
  const response = await adminApi.getStatus();
  return response;
} catch (error) {
  logger.warn('Using mock data for system status', error);
  return { data: generateMockSystemStatus() };
}
```

---

## 生产部署注意事项

### 环境变量

```bash
# 设置 API 地址（可选，默认 http://localhost:8080/api/v1）
VITE_API_URL=https://api.yourdomain.com/api/v1
```

### Nginx 配置建议

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # 前端静态资源
    location / {
        root /var/www/javainfohunter-frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 30s;
    }
}
```

### 构建部署

```bash
# 构建
npm run build
# 输出目录: dist/

# 部署 dist/ 到静态服务器
# 确保 SPA 路由正确配置（所有路径指向 index.html）
```
