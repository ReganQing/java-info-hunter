# 前端模块文档

> 本文档详细描述 JavaInfoHunter 前端各功能模块的实现细节、组件结构和数据流。

## 模块总览

项目分为两大模块域：**Research（研究分析）** 和 **Admin（管理运维）**，通过侧边栏的模块切换按钮进行切换。

```
Research 模块                        Admin 模块
├── Dashboard (总览)                 ├── Sources (RSS 源管理)
├── News (新闻浏览器)                ├── Agents (Agent 监控)
├── Knowledge (知识图谱)             └── System (系统状态)
└── Trends (趋势分析)
```

---

## Research 模块

### 1. Dashboard（总览仪表盘）

**路由**: `/research/dashboard`
**文件**: `src/modules/research/dashboard/index.tsx`

#### 功能描述

Dashboard 是系统的首页，提供全局概览信息，帮助用户快速了解系统运行状态和最新动态。

#### 核心功能

| 功能 | 描述 | 数据来源 |
|------|------|----------|
| 统计卡片 | 文章总数、活跃源数、系统状态、待处理数量 | `adminApi.getStatus()` |
| 热门新闻列表 | 展示最新 10 条热门新闻，含排名、情感、话题 | `useTrendingNews(10)` |
| 快速筛选 | 按情感（全部/正面/中性/负面）快速跳转新闻页 | Zustand `setNewsFilters` |
| 分类快捷入口 | 点击分类跳转到新闻页并应用分类筛选 | Zustand `setNewsFilters` |
| 最近活动 | 展示系统最近操作记录（静态数据） | 硬编码 |
| 系统健康 | 展示 API/Crawler/Database 健康状态 | 硬编码 |

#### 数据流

```
useTrendingNews(10) ──→ 热门新闻列表
       │
       └── queryKey: ['trending', 10]
           queryFn: newsApi.getTrending(10)
           refetchInterval: 60000ms (1 分钟)

useQuery(['system-status']) ──→ 统计卡片
       │
       └── queryFn: adminApi.getStatus()
           refetchInterval: 30000ms (30 秒)
```

#### 交互逻辑

- 点击热门新闻条目 → 导航到 `/research/news?id={id}`
- 点击情感快速筛选 → 更新 Zustand `newsFilters.sentiment`，导航到 `/research/news`
- 点击分类 → 更新 Zustand `newsFilters.category`，导航到 `/research/news`
- "Advanced Filters" 按钮 → 导航到 `/research/news`
- "View Trends" 按钮 → 导航到 `/research/trends`

#### 使用的共享组件

- `StatCard` - 统计卡片（支持 trend 和 status 模式）
- `Card`, `Badge`, `Skeleton`, `ScrollArea`, `Button`, `Tabs`

---

### 2. News（新闻浏览器）

**路由**: `/research/news`
**文件**: `src/modules/research/news/index.tsx`, `src/modules/research/news/NewsTableRow.tsx`

#### 功能描述

功能最丰富的模块，提供完整的新闻浏览体验，支持多维度筛选、排序、分页和详情查看。

#### 核心功能

| 功能 | 描述 | 实现方式 |
|------|------|----------|
| 全文搜索 | 在标题、摘要、来源、话题、关键词中搜索 | 客户端过滤（即时响应） |
| 分类筛选 | Technology, Business, Science 等分类下拉 | 服务端筛选 |
| 情感筛选 | 全部/正面/中性/负面 Tab 切换 | 服务端筛选 |
| 多列排序 | 标题、来源、情感、重要性、日期 | 客户端排序 |
| 分页 | 每页 20 条，前后翻页 | 客户端分页 |
| 活动筛选器 | 显示当前筛选条件，支持逐个清除或全部清除 | UI 状态 |
| 新闻详情 | 侧边 Sheet 展示完整元数据和摘要 | `useNewsById(id)` |
| 重要性可视化 | 彩色进度条（绿色 >= 70%, 黄色 >= 40%, 红色 < 40%） | CSS 内联样式 |

#### 数据流

```
useNews(true) ──→ 新闻列表
       │
       └── queryKey: ['news', newsFilters]
           queryFn: newsApi.getList({ page: 0, size: 100, ...filters })

useNewsById(selectedId) ──→ 新闻详情
       │
       └── queryKey: ['news', id]
           queryFn: newsApi.getById(id)
           enabled: !!id
```

#### 筛选架构

```
Zustand Store (newsFilters)
       │
       ├── category ──→ 服务端筛选（API 参数）
       ├── sentiment ──→ 服务端筛选（API 参数）
       │
       └── searchQuery ──→ 客户端筛选（useMemo 过滤）
```

**设计决策**：分类和情感通过服务端筛选（减少数据传输量），搜索通过客户端筛选（提供即时响应体验）。

#### 子组件

**NewsTableRow** (`NewsTableRow.tsx`)

独立的表格行组件，负责渲染单条新闻的展示逻辑：

| 列 | 内容 | 特殊处理 |
|----|------|----------|
| Title | 新闻标题 | `line-clamp-2` 限制两行 |
| Source | 来源名称 | 灰色文本 |
| Sentiment | 情感标签 | Badge 颜色映射 |
| Importance | 重要性分数 | 进度条 + 百分比 |
| Topics | 话题标签 | 最多显示 2 个，溢出显示 `+N` |
| Date | 发布日期 | `date-fns` 格式化 |

---

### 3. Knowledge（知识图谱）

**路由**: `/research/knowledge`
**文件**: `src/modules/research/knowledge/index.tsx`, `src/modules/research/knowledge/types.ts`

#### 功能描述

基于 D3.js 力导向图的交互式知识图谱，可视化话题、文章和来源之间的关系。

#### 核心功能

| 功能 | 描述 |
|------|------|
| 力导向布局 | D3.js force simulation，含碰撞检测 |
| 节点拖拽 | 支持拖拽节点重新定位 |
| 缩放平移 | 滚轮缩放、按钮缩放、拖拽平移 |
| 节点悬停 | 高亮节点，显示 Tooltip |
| 节点点击 | 按节点类型筛选新闻（文章→搜索, 话题→分类, 来源→搜索） |
| 全屏模式 | 切换全屏显示 |
| 图例 | 展示三种节点类型 |

#### 节点类型

| 类型 | 颜色 | 大小 | 描述 |
|------|------|------|------|
| Topic (话题) | `#8b5cf6` (紫色) | 20px | Java, Spring Boot, Microservices 等 |
| Article (文章) | `#3b82f6` (蓝色) | 12px | 具体新闻文章 |
| Source (来源) | `#10b981` (绿色) | 15px | Baeldung, InfoQ, DZone 等 |

#### D3.js 配置

文件：`src/shared/constants/knowledge.ts`

```typescript
const D3_CONFIG = {
  LINK_DISTANCE: 80,           // 连线默认距离
  CHARGE_STRENGTH: -300,       // 排斥力强度
  ZOOM_SCALE_EXTENT_MIN: 0.1,  // 最小缩放
  ZOOM_SCALE_EXTENT_MAX: 4,    // 最大缩放
  ZOOM_IN_SCALE: 1.3,          // 放大倍率
  ZOOM_OUT_SCALE: 0.7,         // 缩小倍率
  RESET_ZOOM_DURATION_MS: 500, // 重置动画时长
  ZOOM_BUTTON_DURATION_MS: 300,// 按钮缩放动画时长
};
```

#### 数据架构

```typescript
interface GraphNode {
  id: string;
  type: 'topic' | 'article' | 'source';
  label: string;
  color: string;
  size: number;
  description?: string;
  articleId?: number;     // 文章节点关联的新闻 ID
  sourceUrl?: string;
}

interface GraphLink {
  source: string | GraphNode;
  target: string | GraphNode;
  value?: number;         // 连线权重
}
```

#### 注意事项

当前使用**静态 Mock 数据**（`generateMockData()`），尚未对接后端 API。未来需要设计知识图谱数据端点。

---

### 4. Trends（趋势分析）

**路由**: `/research/trends`
**文件**: `src/modules/research/trends/index.tsx`, `src/modules/research/trends/types.ts`, `src/modules/research/trends/trend-utils.ts`

#### 功能描述

时间序列趋势分析仪表盘，使用 Recharts 展示话题频率、情感演化和来源活跃度。

#### 核心功能

| 功能 | 描述 |
|------|------|
| 时间周期选择 | 24 小时 / 7 天 / 30 天 / 90 天 |
| 话题趋势图 | 折线图展示 Top 5 话题频率随时间变化，支持 Brush 缩放 |
| 情感演化图 | 折线图展示正面/中性/负面情感趋势 |
| 来源活跃度 | 横向柱状图展示 Top 10 来源的文章数量 |
| 汇总统计 | 总文章数、活跃话题数、活跃来源数 |
| 数据导出 | 导出为 JSON 文件（含图表数据和摘要） |

#### 数据处理管线

```
原始新闻数据 (NewsItem[])
       │
       ├── 1. 按时间范围过滤 (getDateRangeConfig)
       ├── 2. 初始化时间桶 (initializeTimeBuckets)
       ├── 3. 按时间桶聚合 (aggregateDataByTimeBucket)
       └── 4. 转换为图表格式 (transformToChartFormat)
              │
              ├── topicData: TrendDataPoint[]    // 话题趋势
              ├── sentimentData: SentimentDataPoint[]  // 情感演化
              └── sourceData: SourceDataPoint[]    // 来源活跃度
```

#### 图表配置

文件：`src/shared/constants/trends.ts`

```typescript
const TRENDS_CONFIG = {
  MAX_PAGE_SIZE: 100,         // API 请求最大页大小
  REFRESH_INTERVAL_MS: 300000, // 5 分钟自动刷新
  COLORS: [
    '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#ec4899', '#06b6d4',
  ],
  MAX_TOPICS: 5,              // 展示前 5 个话题
  MAX_SOURCES: 10,            // 展示前 10 个来源
};
```

#### 数据流

```
useQuery(['news-trends', timePeriod]) ──→ 原始新闻数据
       │
       └── queryFn: newsApi.getList({ size: 100, startDate, endDate })
           refetchInterval: 300000ms (5 分钟)

useMemo(transformToTrendData) ──→ 图表数据
       │
       └── 依赖: newsData, timePeriod

useMemo(topTopics) ──→ Top 5 话题列表
       │
       └── 依赖: chartData.topicData
```

---

## Admin 模块

### 5. Sources（RSS 源管理）

**路由**: `/admin/sources`
**文件**: `src/modules/admin/sources/index.tsx`

#### 功能描述

RSS 订阅源的 CRUD 管理界面，支持创建、编辑、删除和手动触发爬取。

#### 核心功能

| 功能 | 描述 |
|------|------|
| 健康指标 | 总源数、活跃源数、健康/警告/错误源数、总文章数 |
| 源列表表格 | 名称、分类、状态、最后爬取时间、失败次数、文章数 |
| 添加源 | Dialog 表单：名称、URL、分类、标签（逗号分隔） |
| 编辑源 | 复用添加表单，预填现有数据 |
| 删除源 | 二次确认 Dialog |
| 触发爬取 | 单个源手动触发爬取 |

#### 表单验证

使用 **Zod** Schema 进行验证：

```typescript
const sourceSchema = z.object({
  name: z.string().min(1, 'Source name is required'),
  url: z.string().url('Invalid URL'),
  category: z.string().min(1, 'Category is required'),
  tags: z.array(z.string()).optional(),
});
```

配合 **React Hook Form** + `@hookform/resolvers/zod` 进行表单绑定。

#### 健康状态规则

| 状态 | 条件 |
|------|------|
| healthy | `failedCrawls === 0` 且 `lastCrawledAt` 存在 |
| warning | `failedCrawls > 0` 或 `lastCrawledAt` 为空 |
| error | `failedCrawls > 3` |

#### Mutation 操作

```typescript
createMutation → sourcesApi.create(data) → invalidateQueries(['sources'])
updateMutation → sourcesApi.update(id, data) → invalidateQueries(['sources'])
deleteMutation → sourcesApi.delete(id) → invalidateQueries(['sources'])
crawlMutation  → sourcesApi.triggerCrawl(id) → invalidateQueries(['sources'])
```

---

### 6. Agents（Agent 监控）

**路由**: `/admin/agents`
**文件**: `src/modules/admin/agents/index.tsx`

#### 功能描述

AI Agent 执行监控仪表盘，展示 Agent 运行状态、性能指标和执行历史。

#### 核心功能

| 功能 | 描述 |
|------|------|
| 统计仪表盘 | 总执行数、活跃 Agent、平均执行时间、成功率、Token 消耗、成本 |
| 执行时间趋势图 | 折线图展示最近 24 小时的执行时间趋势 |
| 成功率饼图 | 环形图展示 completed/failed/running 的分布 |
| 任务分布图 | 横向柱状图展示各 Agent 类型的执行数量 |
| 执行历史表格 | ID、Agent 类型、状态、开始时间、持续时间、Token、结果摘要 |
| 状态筛选 | 按执行状态（全部/Running/Completed/Failed/Cancelled） |
| 类型筛选 | 按照动态获取的 Agent 类型列表筛选 |
| 执行详情弹窗 | 完整的执行详情：配置、输入数据、输出数据、错误日志 |

#### 数据流

```
useQuery(['agent-stats']) ──→ Agent 统计数据
       │
       └── queryFn: agentsApi.getStats()
           refetchInterval: 30000ms (30 秒)
           fallback: generateMockStats()

useQuery(['agent-executions']) ──→ 执行历史列表
       │
       └── queryFn: agentsApi.getExecutions(0, 50)
           refetchInterval: 15000ms (15 秒)
           fallback: generateMockExecutions()

generateMockMetrics() ──→ 图表数据
       │
       └── executionTimeTrend, taskDistribution, statusDistribution
```

#### Mock 数据

当前 Agent 模块大量使用 Mock 数据（`src/shared/mocks/index.ts`），包含：

- `generateMockExecutions()` - 生成 25 条模拟执行记录
- `generateMockStats()` - 生成模拟统计数据
- `generateMockMetrics()` - 生成模拟图表数据

#### Agent 类型（Mock）

- NewsAnalyzer - 新闻分析
- SourceCrawler - 源爬取
- SentimentAnalyzer - 情感分析
- TopicExtractor - 话题提取
- Summarizer - 摘要生成

---

### 7. System（系统状态）

**路由**: `/admin/system`
**文件**: `src/modules/admin/system/index.tsx`

#### 功能描述

系统健康监控界面，展示资源使用、服务状态和系统指标，并提供手动爬取触发功能。

#### 核心功能

| 功能 | 描述 |
|------|------|
| 系统概览 | RSS 源数、新闻总数、待处理数、系统运行时间 |
| 资源使用 | CPU、内存、磁盘使用率（带进度条） |
| 服务状态网格 | Backend、Database、Crawler、Cache、Search 等服务状态 |
| 手动爬取 | 全量爬取按钮 + 按分类爬取选择器 |
| 请求速率图 | 最近 24 小时请求速率折线图 |
| 错误率图 | 最近 24 小时错误率折线图 |

#### 资源使用组件

```typescript
interface ResourceGaugeProps {
  label: string;     // "CPU Usage"
  used: number;      // 使用量
  total: number;     // 总量
  unit: string;      // "percent" | "bytes"
  icon: ReactNode;   // Lucide 图标
}
```

- CPU：百分比显示（`used/total * 100`）
- 内存/磁盘：字节数显示（自动转换为 GB/MB）

#### 爬取触发

- **全量爬取**：`adminApi.triggerFullCrawl()`
- **分类爬取**：`adminApi.triggerCategoryCrawl(category)`
- **冷却时间**：60 秒（`CRAWL_COOLDOWN`），防止频繁触发
- **分类白名单**：使用 `CRAWL_CATEGORIES` 常量验证

#### 数据流

```
useQuery(['system-status']) ──→ 系统状态
       │
       └── queryFn: adminApi.getStatus()
           refetchInterval: 30000ms (30 秒)
           fallback: generateMockSystemStatus()

// 资源和指标数据（后端暂未提供，使用 Mock）
useMemo(generateMockResourceUsage) ──→ 资源数据
useMemo(generateMockMetrics) ──→ 指标数据
```

#### 服务状态兼容性

代码兼容两种 API 返回格式：

```typescript
// 旧格式：扁平字符串
{ services: { backend: "UP", database: "DOWN" } }

// 新格式：嵌套对象
{ services: { backend: { status: "UP", responseTime: 50 } } }
```

---

## 模块间通信

### 跨模块状态共享

通过 **Zustand `app-store`** 实现跨模块通信：

```
Dashboard ──setNewsFilters──→ News
Knowledge ──setNewsFilters──→ News
Dashboard ──navigate──→ News / Trends
```

### 页面标题管理

每个模块在 `useEffect` 中设置页面标题，组件卸载时恢复默认标题：

```typescript
useEffect(() => {
  document.title = 'News Browser - JavaInfoHunter';
  return () => { document.title = 'JavaInfoHunter'; };
}, []);
```
