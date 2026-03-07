# Docs/Plans 目录索引

> **最后更新:** 2026-03-07 (P0 Task 2 Processor 存储逻辑已验证完成)
> **用途:** 记录项目计划、进度跟踪、实现总结

---

## 📁 目录结构

```
docs/plans/
├── 📋 README.md                              # 本文件 - 目录索引
├── 🗺️  roadmap.md                            # 项目路线图 - 总体任务清单
│
├── 📂 completed/                             # 已完成的实现文档
│   ├── 2026-03-05-crawler-module-implementation.md
│   ├── crawler-module-implementation-complete.md
│   ├── 2026-03-05-crawl-scheduler-implementation-summary.md
│   ├── 2026-03-05-content-processor-module.md
│   ├── 2026-03-05-content-processor-module-progress.md
│   └── 2026-03-05-content-processor-integration-summary.md
│
├── 📂 review/                                # 代码审查相关
│   └── code-review-summary.md
│
├── 📂 archive/                               # 归档文档
│   ├── crawler-module-progress.md            # 进度跟踪（已被完成总结替代）
│   ├── round2-agent-prompts.md               # Agent 提示词记录
│   └── monitor-agents.sh                     # 监控脚本
│
└── 📂 templates/                             # 模板文档（计划中）
    └── module-implementation-template.md     # 模块实现计划模板
```

---

## 📋 文档分类

### 🗺️ 总体规划

| 文档 | 说明 | 状态 |
|------|------|------|
| [roadmap.md](./roadmap.md) | 项目路线图，总体任务清单 | 🟢 最新 |

### 🚀 进行中的计划

| 文档 | 模块 | 状态 |
|------|------|------|
| - | - | - |

### ✅ 最近完成

| 文档 | 模块 | 完成日期 |
|------|------|----------|
| P0 Task 2: Processor 存储逻辑 | Processor 模块 | 2026-03-07 |
| [../Master-Worker模式实现总结.md](../Master-Worker模式实现总结.md) | Master-Worker 协作模式 | 2026-03-07 |
| P0 Task 1: Crawler Critical Issues | Crawler 模块 | 2026-03-07 |
| [2026-03-06-infrastructure-summary.md](./completed/2026-03-06-infrastructure-summary.md) | 基础设施集成 | 2026-03-06 |
| [2026-03-06-api-module-implementation-summary.md](./completed/2026-03-06-api-module-implementation-summary.md) | REST API 模块 | 2026-03-06 |
| [2026-03-06-processor-storage-summary.md](./completed/2026-03-06-processor-storage-summary.md) | Processor 存储 | 2026-03-06 |
| [2026-03-06-processor-storage-implementation.md](./completed/2026-03-06-processor-storage-implementation.md) | Processor 存储 | 2026-03-06 |

### ✅ 已完成模块 (completed/)

| 文档 | 模块 | 完成日期 | 说明 |
|------|------|----------|------|
| [2026-03-06-api-module-implementation-summary.md](./completed/2026-03-06-api-module-implementation-summary.md) | REST API | 2026-03-06 | REST API 模块完整实现 (18端点, 100测试) |
| [crawler-module-implementation-complete.md](./completed/crawler-module-implementation-complete.md) | Crawler | 2026-03-05 | 爬虫模块完整实现总结 |
| [2026-03-05-crawl-scheduler-implementation-summary.md](./completed/2026-03-05-crawl-scheduler-implementation-summary.md) | Scheduler | 2026-03-05 | 调度器实现总结 |
| [2026-03-05-content-processor-integration-summary.md](./completed/2026-03-05-content-processor-integration-summary.md) | Processor | 2026-03-05 | 处理模块集成总结 |
| [2026-03-05-content-processor-module-progress.md](./completed/2026-03-05-content-processor-module-progress.md) | Processor | 2026-03-05 | 处理模块进度跟踪 |
| [2026-03-05-content-processor-module.md](./completed/2026-03-05-content-processor-module.md) | Processor | 2026-03-05 | 处理模块实现计划 |
| [2026-03-05-crawler-module-implementation.md](./completed/2026-03-05-crawler-module-implementation.md) | Crawler | 2026-03-05 | 爬虫模块实现计划 |

### 🔍 代码审查 (review/)

| 文档 | 审查对象 | 日期 | 关键发现 |
|------|----------|------|----------|
| [code-review-summary.md](./review/code-review-summary.md) | Crawler 模块 | 2026-03-05 | 3 critical, 5 important, 5 minor issues |

### 📦 归档文档 (archive/)

| 文档 | 归档原因 | 替代文档 |
|------|----------|----------|
| [crawler-module-progress.md](./archive/crawler-module-progress.md) | 已被完成总结替代 | crawler-module-implementation-complete.md |
| [round2-agent-prompts.md](./archive/round2-agent-prompts.md) | Agent 执行记录 | 仅用于历史参考 |
| [monitor-agents.sh](./archive/monitor-agents.sh) | 监控脚本 | 可迁移到 scripts/ |

---

## 🔄 文档生命周期

```
创建 → 实现中 → 已完成 → 归档
  ↓       ↓        ↓        ↓
templates/  plans/  completed/  archive/
```

1. **创建阶段:** 使用模板创建实现计划
2. **实现阶段:** 在 plans/ 根目录跟踪进度
3. **完成阶段:** 移动到 completed/ 子目录
4. **归档阶段:** 过时文档移到 archive/ 子目录

---

## 📝 文档命名规范

### 实现计划文档
```
YYYY-MM-DD-{module}-module-implementation.md
例如: 2026-03-05-crawler-module-implementation.md
```

### 进度跟踪文档
```
{module}-module-progress.md
例如: crawler-module-progress.md
```

### 完成总结文档
```
YYYY-MM-DD-{module}-module-implementation-complete.md
或者: YYYY-MM-DD-{feature}-implementation-summary.md
```

### 代码审查文档
```
code-review-summary.md
或者: {module}-code-review-YYYY-MM-DD.md
```

---

## 🔗 相关文档

### 项目根文档
- [技术方案.md](../技术方案.md) - 完整系统技术方案
- [数据传输架构设计.md](../数据传输架构设计.md) - 消息队列架构
- [数据库设计说明.md](../数据库设计说明.md) - 数据库表结构
- [数据库使用指南.md](../数据库使用指南.md) - Repository 使用指南

### 模块文档
- [javainfohunter-ai-service/README.md](../javainfohunter-ai-service/README.md) - AI 服务模块
- [javainfohunter-ai-service/USAGE.md](../javainfohunter-ai-service/USAGE.md) - AI 服务使用指南
- [javainfohunter-crawler/README.md](../javainfohunter-crawler/README.md) - 爬虫模块
- [javainfohunter-processor/README.md](../javainfohunter-processor/README.md) - 处理模块

---

## 📊 快速查找

### 按模块查找
- **AI 服务:** [javainfohunter-ai-service/README.md](../javainfohunter-ai-service/README.md)
- **爬虫模块:** [completed/crawler-module-implementation-complete.md](./completed/crawler-module-implementation-complete.md)
- **处理模块:** [completed/2026-03-05-content-processor-module-progress.md](./completed/2026-03-05-content-processor-module-progress.md)
- **API 模块:** [roadmap.md](./roadmap.md) (计划中)

### 按类型查找
- **实现计划:** [completed/](./completed/) 目录
- **代码审查:** [review/](./review/) 目录
- **总体规划:** [roadmap.md](./roadmap.md)

---

## 🚀 下一步

1. **执行 P0 任务:** 修复 Crawler 模块的 critical issues
2. **创建新模块计划:** 参考 [roadmap.md](./roadmap.md) 中的 Task 3 (API 模块)
3. **更新进度:** 完成任务后更新 [roadmap.md](./roadmap.md) 中的进度条

---

**维护者:** JavaInfoHunter Team
**最后更新:** 2026-03-07
