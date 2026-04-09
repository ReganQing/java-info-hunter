# 前端组件库文档

> 本文档描述 JavaInfoHunter 前端的共享组件库，包括布局组件、UI 组件、数据展示组件和表单组件。

## 组件架构总览

```
src/shared/components/
├── layout/
│   └── MainLayout.tsx           # 主布局（Sidebar + Header + Content）
├── data-display/
│   └── StatCard.tsx             # 统计卡片
└── ui/                          # shadcn/ui 组件（18 个）
    ├── button.tsx               # 按钮
    ├── card.tsx                 # 卡片
    ├── badge.tsx                # 标签
    ├── dialog.tsx               # 对话框
    ├── sheet.tsx                # 侧边面板
    ├── table.tsx                # 表格
    ├── input.tsx                # 输入框
    ├── select.tsx               # 下拉选择
    ├── tabs.tsx                 # 标签页
    ├── form.tsx                 # 表单容器
    ├── label.tsx                # 表单标签
    ├── skeleton.tsx             # 骨架屏
    ├── scroll-area.tsx          # 滚动区域
    ├── tooltip.tsx              # 工具提示
    ├── popover.tsx              # 弹出层
    ├── avatar.tsx               # 头像
    ├── calendar.tsx             # 日历
    ├── progress.tsx             # 进度条
    ├── separator.tsx            # 分隔线
    ├── dropdown-menu.tsx        # 下拉菜单
    └── collapsible.tsx          # 折叠面板
```

---

## 布局组件

### MainLayout

文件：`src/shared/components/layout/MainLayout.tsx`

#### 功能描述

应用主布局组件，包含可折叠侧边栏、顶部导航栏和内容区域。

#### 结构

```
┌──────────────────────────────────────────────────────┐
│ ┌─────────┐ ┌──────────────────────────────────────┐ │
│ │         │ │ Header (sticky top)                   │ │
│ │ Sidebar │ │ [Toggle] ─────────── [Search] [Bell] │ │
│ │         │ ├──────────────────────────────────────┤ │
│ │ [Logo]  │ │                                      │ │
│ │         │ │                                      │ │
│ │ [Module │ │         Page Content                 │ │
│ │  Toggle]│ │         (children)                   │ │
│ │         │ │                                      │ │
│ │ [Nav    │ │                                      │ │
│ │  Items] │ │                                      │ │
│ │         │ │                                      │ │
│ └─────────┘ └──────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

#### Props

```typescript
interface MainLayoutProps {
  children: React.ReactNode;
}
```

#### 侧边栏导航

**Research 模块菜单项：**

| 路径 | 图标 | 标签 |
|------|------|------|
| `/research/dashboard` | `LayoutDashboard` | Dashboard |
| `/research/trends` | `TrendingUp` | Trend Charts |
| `/research/knowledge` | `Network` | Knowledge Graph |
| `/research/news` | `List` | News Browser |

**Admin 模块菜单项：**

| 路径 | 图标 | 标签 |
|------|------|------|
| `/admin/sources` | `Database` | RSS Sources |
| `/admin/agents` | `Activity` | Agent Monitoring |
| `/admin/system` | `Settings` | System Status |

#### 交互行为

- **模块切换**：顶部按钮在 Research 和 Admin 之间切换，仅显示当前模块的菜单项
- **侧边栏折叠**：Header 中的切换按钮控制侧边栏展开/折叠
  - 展开：宽度 256px (`w-64`)
  - 折叠：宽度 64px (`w-16`)，仅显示图标
  - 过渡动画：300ms (`transition-all duration-300`)
- **活跃状态**：当前路径的菜单项高亮 (`bg-primary text-primary-foreground`)
- **Logo 显示**：展开时显示 "JavaInfoHunter"，折叠时显示 "JIH"

#### 使用的共享状态

```typescript
const { sidebarCollapsed, toggleSidebar, activeModule, setActiveModule } = useAppStore();
```

---

## 数据展示组件

### StatCard

文件：`src/shared/components/data-display/StatCard.tsx`

#### 功能描述

通用统计卡片组件，支持数值展示、趋势指示和状态图标。

#### Props

```typescript
interface StatCardProps {
  title: string;           // 标题（如 "Total Articles"）
  value: number | string;  // 主数值
  prefix?: ReactNode;      // 前缀（如货币符号）
  suffix?: string;         // 后缀（如 "RSS feeds"）
  trend?: number;          // 趋势百分比（如 12 或 -5）
  loading?: boolean;       // 加载状态
  className?: string;      // 自定义类名
  status?: 'HEALTHY' | 'DEGRADED' | 'DOWN';  // 状态模式
}
```

#### 两种展示模式

**1. 数值 + 趋势模式**（默认）

```
┌──────────────────────┐
│ Total Articles  [⟳]  │  ← title + loading icon
│                      │
│ 1,247                │  ← value (绿色/红色)
│ ↑ 12% vs last period │  ← trend (绿色上升/红色下降)
└──────────────────────┘
```

**2. 状态模式**（传入 `status` prop）

```
┌──────────────────────┐
│ System Status  [⟳]   │
│                      │
│ ✓ HEALTHY            │  ← status icon + colored text
└──────────────────────┘
```

#### 状态图标映射

| 状态 | 图标 | 颜色 |
|------|------|------|
| `HEALTHY` | `CheckCircle2` | `green-600` |
| `DEGRADED` | `AlertTriangle` | `yellow-600` |
| `DOWN` | `XCircle` | `red-600` |

#### 使用示例

```tsx
// 数值 + 趋势
<StatCard
  title="Total Articles"
  value={1247}
  trend={12}
  loading={isLoading}
/>

// 状态模式
<StatCard
  title="System Status"
  value="HEALTHY"
  status="HEALTHY"
/>

// 带后缀
<StatCard
  title="Active Sources"
  value={14}
  suffix="RSS feeds"
/>
```

---

## shadcn/ui 组件

项目使用 **shadcn/ui** 作为基础 UI 组件库，基于 **Radix UI** 原语构建。所有组件位于 `src/shared/components/ui/` 目录。

### Button

文件：`button.tsx`

基于 `class-variance-authority` (CVA) 的变体系统：

| Variant | 用途 | 场景 |
|---------|------|------|
| `default` | 主要操作 | 提交、确认 |
| `destructive` | 危险操作 | 删除 |
| `outline` | 次要操作 | 取消、返回 |
| `secondary` | 辅助操作 | 触发分类爬取 |
| `ghost` | 透明按钮 | 表格操作、图标按钮 |
| `link` | 链接样式 | 文本链接 |

Size 变体：`default`, `sm`, `lg`, `icon`

### Card

文件：`card.tsx`

子组件：`Card`, `CardHeader`, `CardTitle`, `CardDescription`, `CardContent`, `CardFooter`

所有模块页面使用 Card 作为主要容器组件。

### Badge

文件：`badge.tsx`

变体在项目中的语义映射：

| Variant | 语义 | 使用场景 |
|---------|------|----------|
| `default` | 正面/成功 | POSITIVE 情感、healthy 状态、UP 服务 |
| `secondary` | 中性 | NEUTRAL 情感、DEGRADED 状态 |
| `destructive` | 负面/危险 | NEGATIVE 情感、DOWN 状态、failed 执行 |
| `outline` | 信息/标签 | 分类标签、关键词、UNKNOWN 状态 |

### Dialog

文件：`dialog.tsx`

子组件：`Dialog`, `DialogTrigger`, `DialogContent`, `DialogHeader`, `DialogFooter`, `DialogTitle`, `DialogDescription`

使用场景：
- **Sources 模块**：添加/编辑源表单、删除确认
- **Agents 模块**：执行详情弹窗

### Sheet

文件：`sheet.tsx`

侧边面板组件，用于详情展示。

子组件：`Sheet`, `SheetTrigger`, `SheetContent`, `SheetHeader`, `SheetFooter`, `SheetTitle`, `SheetDescription`

使用场景：
- **News 模块**：新闻详情面板（右侧滑出，宽度 `sm:max-w-xl`）

### Table

文件：`table.tsx`

子组件：`Table`, `TableHeader`, `TableBody`, `TableFooter`, `TableHead`, `TableRow`, `TableCell`, `TableCaption`

使用场景：
- **News 模块**：新闻列表表格
- **Sources 模块**：RSS 源管理表格
- **Agents 模块**：执行历史表格

### Tabs

文件：`tabs.tsx`

子组件：`Tabs`, `TabsList`, `TabsTrigger`, `TabsContent`

使用场景：
- **Dashboard**：情感快速筛选（All / + / +/- / -）
- **News**：情感筛选（All / Positive / Neutral / Negative）
- **Trends**：时间周期选择（24h / 7d / 30d / 90d）

### Skeleton

文件：`skeleton.tsx`

加载占位组件，提供视觉骨架效果。

使用模式：

```tsx
{isLoading ? (
  <Skeleton className="h-8 w-20" />        // 数值占位
) : (
  <div className="text-2xl font-bold">{value}</div>
)}

{isLoading ? (
  <div className="space-y-4">
    {[...Array(5)].map((_, i) => (
      <div key={i} className="flex gap-4">
        <Skeleton className="h-16 w-16 rounded-lg" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      </div>
    ))}
  </div>
) : ( /* actual content */ )}
```

### ScrollArea

文件：`scroll-area.tsx`

自定义滚动条组件，基于 Radix UI。

使用场景：
- **Dashboard**：热门新闻列表（高度 500px）
- **News**：新闻表格（高度 600px）
- **Agents**：执行详情弹窗内容（高度 60vh）

### Form

文件：`form.tsx`

React Hook Form 集成组件。

子组件：`Form`, `FormField`, `FormItem`, `FormLabel`, `FormControl`, `FormDescription`, `FormMessage`

配合 Zod resolver 使用：

```tsx
<Form {...form}>
  <form onSubmit={form.handleSubmit(onSubmit)}>
    <FormField
      control={form.control}
      name="name"
      render={({ field }) => (
        <FormItem>
          <FormLabel>Source Name</FormLabel>
          <FormControl>
            <Input {...field} />
          </FormControl>
          <FormMessage />  {/* Zod 验证错误显示 */}
        </FormItem>
      )}
    />
  </form>
</Form>
```

### Progress

文件：`progress.tsx`

进度条组件。

使用场景：
- **System 模块**：CPU/内存/磁盘使用率（高度 `h-2`）
- **News 模块**：详情面板中的重要性分数展示

### 其他 UI 组件

| 组件 | 文件 | 使用场景 |
|------|------|----------|
| `Input` | `input.tsx` | 搜索框、表单输入 |
| `Select` | `select.tsx` | 分类下拉、时间周期选择 |
| `Label` | `label.tsx` | 表单字段标签 |
| `Tooltip` | `tooltip.tsx` | Knowledge Graph 工具栏提示 |
| `Popover` | `popover.tsx` | 弹出层 |
| `Avatar` | `avatar.tsx` | 用户头像（已引入，暂未使用） |
| `Calendar` | `calendar.tsx` | 日期选择（已引入，暂未使用） |
| `Separator` | `separator.tsx` | 内容分隔线 |
| `DropdownMenu` | `dropdown-menu.tsx` | 下拉菜单（已引入，暂未使用） |
| `Collapsible` | `collapsible.tsx` | 折叠面板（已引入，暂未使用） |

---

## 工具函数

### cn() - Tailwind 类合并

文件：`src/shared/lib/utils.ts`

```typescript
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

用于合并 Tailwind CSS 类名，自动解决冲突：

```tsx
cn(
  "text-sm",
  isActive && "bg-primary",           // 条件类
  !isActive && "text-muted-foreground",
  "px-3 py-2 rounded-md"
)
```

### 日期工具

文件：`src/shared/lib/date.ts`

| 函数 | 输出格式 | 使用场景 |
|------|----------|----------|
| `formatDateTime(date)` | `Jan 15, 2025 14:30` | 执行时间、爬取时间 |
| `formatDateShort(date)` | `Jan 15` | 短日期 |
| `formatRelative(date)` | `Jan 15, 14:30` | 图表 X 轴标签 |
| `formatFullDateTime(date)` | `Jan 15, 2025 14:30:00` | 完整时间戳 |
| `formatTime(date)` | `14:30` | 仅时间 |

### 日志工具

文件：`src/shared/lib/logger.ts`

```typescript
export const logger = {
  log: (...args) => { /* 仅开发环境 */ },
  info: (...args) => { /* 仅开发环境 */ },
  warn: (...args) => { /* 始终输出 */ },
  error: (...args) => { /* 始终输出 */ },
  debug: (...args) => { /* 仅开发环境 */ },
};
```

- 生产环境仅输出 `warn` 和 `error` 级别日志
- 所有日志带前缀：`[INFO]`, `[WARN]`, `[ERROR]`, `[DEBUG]`

---

## 常量

### 分类常量

文件：`src/shared/constants/categories.ts`

```typescript
// RSS 源分类（10 个）
const RSS_CATEGORIES = [
  'Java Core', 'Spring Framework', 'Microservices', 'Performance',
  'Security', 'Testing', 'Tools', 'Best Practices', 'Community', 'Other'
] as const;

// 爬取分类（11 个，含 "All Sources"）
const CRAWL_CATEGORIES = [
  ...RSS_CATEGORIES, 'All Sources'
] as const;
```

### 知识图谱常量

文件：`src/shared/constants/knowledge.ts`

```typescript
const D3_CONFIG = {
  LINK_DISTANCE: 80,
  CHARGE_STRENGTH: -300,
  ZOOM_SCALE_EXTENT: [0.1, 4],
  // ...
};

const NODE_COLORS = {
  topic: '#8b5cf6',    // 紫色
  article: '#3b82f6',  // 蓝色
  source: '#10b981',   // 绿色
};

const NODE_SIZES = {
  topic: 20,
  article: 12,
  source: 15,
};
```

### 趋势图表常量

文件：`src/shared/constants/trends.ts`

```typescript
const TRENDS_CONFIG = {
  MAX_PAGE_SIZE: 100,
  REFRESH_INTERVAL_MS: 300000,  // 5 分钟
  COLORS: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4'],
  MAX_TOPICS: 5,
  MAX_SOURCES: 10,
};
```
