# 前端架构概述

> 本文档描述 JavaInfoHunter 前端项目的整体架构设计、技术选型和项目结构。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.3.1 | UI 框架 |
| TypeScript | 5.6.2 | 类型安全 |
| Vite | 6.0.5 | 构建工具与开发服务器 |
| Tailwind CSS | 3.4.17 | 原子化 CSS 框架 |
| shadcn/ui | Radix UI 封装 | UI 组件库（Button, Card, Dialog, Table 等） |
| React Query (TanStack) | 5.62.11 | 服务端状态管理 |
| Zustand | 5.0.2 | 客户端状态管理 |
| React Router DOM | 7.1.1 | 客户端路由 |
| Recharts | 2.15.0 | 图表库（折线图、柱状图、饼图） |
| D3.js | 7.9.0 | 力导向知识图谱可视化 |
| Axios | 1.7.9 | HTTP 客户端 |
| Zod | 3.24.1 | 表单校验 Schema |
| React Hook Form | 7.54.2 | 表单管理 |
| date-fns | 4.1.0 | 日期格式化 |
| Lucide React | 0.468.0 | 图标库 |
| Sonner | 1.7.1 | Toast 通知 |
| Playwright | 1.58.2 | E2E 测试 |

## 架构设计

### 分层架构

```
┌──────────────────────────────────────────────────────────────┐
│                        App.tsx (入口)                         │
│  QueryClientProvider → BrowserRouter → Suspense              │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────┐
│                     MainLayout (布局)                          │
│  Sidebar + Header + Content Area                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
         ┌─────────────────┴─────────────────┐
         │                                   │
   Research 模块                       Admin 模块
   (研究/分析)                          (管理/运维)
         │                                   │
   Dashboard                         Sources (RSS 源)
   News (新闻)                        Agents (Agent 监控)
   Knowledge (知识图谱)               System (系统状态)
   Trends (趋势)
```

### 状态管理策略

项目采用**双状态管理**架构，明确区分客户端状态和服务端状态：

#### Zustand（客户端状态）

文件：`src/shared/stores/app-store.ts`

```typescript
interface AppState {
  // 导航模块切换
  activeModule: 'research' | 'admin';
  setActiveModule: (module: 'research' | 'admin') => void;

  // 新闻筛选条件（跨组件共享）
  newsFilters: {
    category?: string;
    sentiment?: string;
    startDate?: Date;
    endDate?: Date;
    searchQuery?: string;
  };
  setNewsFilters: (filters: Partial<AppState['newsFilters']>) => void;

  // 选中新闻 ID（用于详情面板）
  selectedNewsId: number | null;
  setSelectedNewsId: (id: number | null) => void;

  // UI 偏好（侧边栏折叠状态）
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
}
```

**设计原则：**
- Zustand 仅管理**纯客户端状态**：UI 偏好、筛选条件、导航状态
- 状态更新使用不可变模式（immer-free，依赖 Zustand 内置的不可变更新）
- `setNewsFilters` 使用浅合并，支持部分更新

#### React Query（服务端状态）

文件：`src/shared/api/query-client.ts`

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,      // 5 分钟内数据视为新鲜
      gcTime: 10 * 60 * 1000,         // 10 分钟后垃圾回收
      retry: 1,                        // 失败重试 1 次
      refetchOnWindowFocus: false,     // 不在窗口聚焦时自动刷新
    },
  },
});
```

**设计原则：**
- 所有 API 数据获取通过 React Query 管理
- 每个查询使用 `queryKey` 进行缓存和失效管理
- 支持自动轮询刷新（Dashboard: 30s, Agents: 15s, Trends: 5min）
- Mutation 操作成功后自动 `invalidateQueries` 更新相关缓存

### 路由设计

文件：`src/App.tsx`

采用 **React Router v7** 的 `BrowserRouter`，所有页面模块通过 `React.lazy()` 实现代码分割和按需加载：

```typescript
// 所有模块使用 lazy loading
const ResearchDashboard = lazy(() => import('./modules/research/dashboard'));
const ResearchTrends = lazy(() => import('./modules/research/trends'));
const ResearchKnowledge = lazy(() => import('./modules/research/knowledge'));
const ResearchNews = lazy(() => import('./modules/research/news'));
const AdminSources = lazy(() => import('./modules/admin/sources'));
const AdminAgents = lazy(() => import('./modules/admin/agents'));
const AdminSystem = lazy(() => import('./modules/admin/system'));
```

| 路由路径 | 模块 | 功能 |
|----------|------|------|
| `/` | - | 重定向至 `/research/dashboard` |
| `/research/dashboard` | ResearchDashboard | 总览仪表盘 |
| `/research/trends` | ResearchTrends | 趋势分析 |
| `/research/knowledge` | ResearchKnowledge | 知识图谱 |
| `/research/news` | ResearchNews | 新闻浏览器 |
| `/admin/sources` | AdminSources | RSS 源管理 |
| `/admin/agents` | AdminAgents | Agent 监控 |
| `/admin/system` | AdminSystem | 系统状态 |
| `*` | - | 重定向至 `/research/dashboard` |

**加载策略：**
- 使用 `<Suspense>` 包裹路由，全局 loading fallback 为旋转动画
- 首屏仅加载 MainLayout 和当前路由模块

### API 层设计

采用**分层 API 架构**，分离客户端配置、端点定义和类型定义：

```
src/shared/api/
├── client.ts          # Axios 实例配置（baseURL, 拦截器）
├── endpoints.ts       # API 端点定义（按领域分组）
├── types.ts           # TypeScript 类型定义
└── query-client.ts    # React Query 客户端配置
```

详见 [frontend-api-integration.md](./frontend-api-integration.md)。

## 项目结构

```
src/
├── App.tsx                              # 应用入口，路由和全局 Provider
├── main.tsx                             # React DOM 渲染入口
├── styles/
│   └── globals.css                      # Tailwind CSS 全局样式 + CSS 变量
│
├── shared/                              # 共享层（跨模块复用）
│   ├── api/
│   │   ├── client.ts                    # Axios 客户端
│   │   ├── endpoints.ts                 # API 端点
│   │   ├── types.ts                     # API 类型
│   │   └── query-client.ts             # React Query 配置
│   ├── components/
│   │   ├── layout/
│   │   │   └── MainLayout.tsx           # 主布局（Sidebar + Header）
│   │   ├── data-display/
│   │   │   └── StatCard.tsx             # 统计卡片组件
│   │   └── ui/                          # shadcn/ui 组件
│   │       ├── button.tsx, card.tsx, dialog.tsx, table.tsx
│   │       ├── input.tsx, select.tsx, badge.tsx, tabs.tsx
│   │       ├── skeleton.tsx, scroll-area.tsx, sheet.tsx
│   │       ├── form.tsx, calendar.tsx, progress.tsx
│   │       ├── tooltip.tsx, popover.tsx, avatar.tsx
│   │       ├── separator.tsx, label.tsx, dropdown-menu.tsx
│   │       └── collapsible.tsx
│   ├── hooks/
│   │   └── useNews.ts                   # 新闻相关 React Query Hooks
│   ├── stores/
│   │   └── app-store.ts                 # Zustand 全局状态
│   ├── constants/
│   │   ├── categories.ts                # RSS/爬取分类常量
│   │   ├── knowledge.ts                 # D3.js 知识图谱配置
│   │   └── trends.ts                    # 趋势图表配置
│   ├── lib/
│   │   ├── utils.ts                     # cn() Tailwind 类合并工具
│   │   ├── date.ts                      # 日期格式化工具
│   │   └── logger.ts                    # 开发环境日志工具
│   └── mocks/
│       └── index.ts                     # Mock 数据生成器
│
├── modules/                             # 功能模块
│   ├── research/
│   │   ├── dashboard/index.tsx          # 总览仪表盘
│   │   ├── news/
│   │   │   ├── index.tsx                # 新闻浏览器
│   │   │   └── NewsTableRow.tsx         # 新闻表格行组件
│   │   ├── knowledge/
│   │   │   ├── index.tsx                # D3.js 知识图谱
│   │   │   └── types.ts                 # 图谱类型定义
│   │   └── trends/
│   │       ├── index.tsx                # 趋势分析图表
│   │       ├── types.ts                 # 趋势类型定义
│   │       └── trend-utils.ts           # 趋势数据转换工具
│   └── admin/
│       ├── sources/index.tsx            # RSS 源管理
│       ├── agents/index.tsx             # Agent 监控
│       └── system/index.tsx             # 系统状态监控
│
└── e2e/                                 # Playwright E2E 测试
```

## 构建配置

### Vite 配置

文件：`vite.config.ts`

```typescript
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve('./src'),             // 根路径
      '@shared': path.resolve('./src/shared'), // 共享层
      '@modules': path.resolve('./src/modules'), // 模块层
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### TypeScript 配置

文件：`tsconfig.json`

- **Target**: ES2020
- **Module**: ESNext (bundler mode)
- **Strict**: 启用全部严格检查
- **Path Aliases**: `@/*`, `@shared/*`, `@modules/*`

### Tailwind CSS 配置

文件：`tailwind.config.ts`

- **Dark Mode**: class 策略
- **自定义颜色**: 使用 CSS 变量体系（shadcn/ui 标准）
  - `primary`, `secondary`, `destructive`, `muted`, `accent`, `popover`, `card`
  - `sentiment` (positive/neutral/negative) - 情感分析专用颜色
- **插件**: `tailwindcss-animate` - 动画支持

## 常用命令

```bash
# 开发
npm run dev                # 启动开发服务器 (端口 5173)

# 构建
npm run build              # TypeScript 编译 + Vite 生产构建

# 代码质量
npm run lint               # ESLint 检查

# 预览
npm run preview            # 预览生产构建

# E2E 测试
npm run test:e2e           # 运行 Playwright E2E 测试
npm run test:e2e:ui        # Playwright UI 模式
npm run test:e2e:headed    # 有头模式运行
npm run test:e2e:debug     # 调试模式
npm run test:e2e:report    # 查看测试报告
```
