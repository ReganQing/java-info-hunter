# JavaInfoHunter 项目路线图

> **创建日期:** 2026-03-06
> **项目阶段:** Phase 5 - 协作模式完善与质量保障
> **距离生产级:** 预计 4-6 周

---

## 📊 整体进度概览

```
Phase 1: AI 服务框架         [████████████████████████████████████] 100% ✅
Phase 2: 数据层与架构设计     [████████████████████████████████████] 100% ✅
Phase 3: 核心业务模块         [████████████████████████████████████] 100% ✅
Phase 4: API 层与基础设施     [████████████████████████████████████] 100% ✅
Phase 5: 协作模式完善         [████████████████████████████████████] 100% ✅
Phase 6: 测试与质量保障       [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  30% 🟡
Phase 7: 生产就绪             [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  30% 🟡

总体进度: [██████████████████████████░░░░░░░░░░░░░░] 68%
```

---

## ✅ 已完成模块

| 模块 | 状态 | 完成日期 | 说明 |
|------|------|----------|------|
| **javainfohunter-ai-service** | ✅ 完成 | 2026-03-01 | Agent 编排框架、工具系统、协作模式 |
| **javainfohunter-crawler** | ✅ 完成 | 2026-03-07 | RSS 爬虫模块（所有 Critical Issues 已验证解决） |
| **javainfohunter-processor** | ✅ 完成 | 2026-03-05 | AI 内容处理模块 |
| **数据库架构** | ✅ 完成 | 2026-03-01 | PostgreSQL + pgvector + Flyway |
| **消息队列架构** | ✅ 完成 | 2026-03-01 | RabbitMQ 设计文档 |

---

## 🚀 任务清单

### 🔴 P0 - 立即执行（阻塞生产）

#### Task 1: 修复爬虫模块 Critical Issues
**预估:** 1-2 天 | **负责人:** 待定 | **依赖:** 无 | **状态:** ✅ 完成 (2026-03-07)

- [x] **1.1** 替换 CrawlOrchestrator 中的 placeholder 代码
  - 文件: `javainfohunter-crawler/.../scheduler/CrawlOrchestrator.java`
  - 当前: 使用 Thread.sleep() + 随机数据
  - 目标: 集成真实的 RssFeedCrawler
  - **验证**: ✅ RssFeedCrawler 已正确注入和使用

- [x] **1.2** 实现内容去重逻辑（content_hash 数据库查询）
  - 文件: `javainfohunter-crawler/.../service/CrawlCoordinator.java`
  - 当前: content_hash 计算但未检查
  - 目标: 发布前查询 RawContentRepository 避免重复
  - **验证**: ✅ 批量查询实现（非 N+1），重复过滤正常

- [x] **1.3** 添加缺失的 crawlExecutor Bean
  - 文件: `javainfohunter-crawler/.../config/SchedulerConfiguration.java`
  - 当前: NoSuchBeanDefinitionException
  - 目标: 添加 @Bean("crawlExecutor") 虚拟线程执行器
  - **验证**: ✅ Bean 已用 @Qualifier 正确声明

- [x] **1.4** 验证修复后的爬虫流程
  - 运行单元测试
  - 端到端测试真实 RSS feed
  - 验证 RabbitMQ 消息流
  - **结果**: ✅ 所有 121 个测试通过

**相关文档:** [crawler-module-implementation-complete.md](./completed/crawler-module-implementation-complete.md)

---

#### Task 2: 完善 Processor 模块存储逻辑
**预估:** 2-3 天 | **负责人:** 待定 | **依赖:** Task 1 | **状态:** ✅ 完成 (2026-03-07)

- [x] **2.1** 实现向量生成（Embedding Service 调用）
  - 文件: `TransactionalStoreService.java`
  - 目标: 调用 EmbeddingService 生成 1536 维向量
  - **验证**: ✅ 向量生成逻辑完整实现

- [x] **2.2** 实现 NewsRepository 持久化
  - 创建: News 实体并存储到数据库
  - 关联: RawContent -> News (一对一)
  - 字段: summary, topics, keywords, sentiment, importance_score
  - **验证**: ✅ NewsRepository.save() 正常工作

- [x] **2.3** 实现处理后消息发布
  - 队列: processor.direct
  - Routing key: processed.content
  - **验证**: ✅ 事务提交后发布

**相关文档:** [2026-03-06-processor-storage-summary.md](./completed/2026-03-06-processor-storage-summary.md)

---

### 🟠 P1 - 核心功能（2-4 周）

#### Task 3: REST API 模块 (javainfohunter-api)
**预估:** 1-2 周 | **负责人:** 待定 | **依赖:** Task 2 | **状态:** ✅ 完成 (2026-03-06)

**目标:** 创建 REST API 层，提供 HTTP 接口访问系统功能

- [x] **3.1** 创建 Maven 模块结构
  ```
  javainfohunter-api/
  ├── pom.xml
  ├── src/main/java/.../api/
  │   ├── ApiApplication.java
  │   ├── controller/          # REST 控制器
  │   ├── dto/                 # 请求/响应 DTO
  │   ├── config/              # Web 配置
  │   └── exception/           # 全局异常处理
  └── src/main/resources/
      └── application.yml
  ```

- [ ] **3.2** 实现 RSS 源管理 API
  - `POST   /api/v1/rss-sources`        - 创建 RSS 源
  - `GET    /api/v1/rss-sources`        - 列表（分页、过滤）
  - `GET    /api/v1/rss-sources/{id}`   - 详情
  - `PUT    /api/v1/rss-sources/{id}`   - 更新
  - `DELETE /api/v1/rss-sources/{id}`   - 删除
  - `POST   /api/v1/rss-sources/{id}/crawl` - 手动触发爬取

- [ ] **3.3** 实现内容查询 API
  - `GET /api/v1/news`                  - 新闻列表（分页、排序）
  - `GET /api/v1/news/{id}`             - 新闻详情
  - `GET /api/v1/news/search`           - 全文搜索
  - `GET /api/v1/news/similar/{id}`     - 相似内容（向量）

- [ ] **3.4** 实现 Agent 执行监控 API
  - `GET /api/v1/agents/executions`     - 执行记录列表
  - `GET /api/v1/agents/executions/{id}` - 执行详情
  - `GET /api/v1/agents/stats`          - 统计信息

- [ ] **3.5** 实现系统管理 API
  - `GET /actuator/health`              - 健康检查
  - `GET /actuator/metrics`             - 指标
  - `POST /api/v1/admin/crawl/trigger`  - 触发全站爬取

- [ ] **3.6** 集成 Knife4j API 文档
  - 访问: `/doc.html`
  - 接口分组: RSS 源、新闻、Agent、系统

**技术栈:** Spring Boot 4.0.3, Spring Web, Spring Doc OpenAPI, Knife4j 4.5.0

---

#### Task 4: 基础设施集成
**预估:** 3-5 天 | **负责人:** 待定 | **依赖:** 无 | **状态:** ✅ 完成 (2026-03-06)

- [x] **4.1** Docker Compose 完整配置
  - PostgreSQL 16 + pgvector
  - RabbitMQ 3.x (Management UI)
  - Redis 7.x
  - 可选: Prometheus + Grafana

- [x] **4.2** Redis 集成
  - 配置: Spring Data Redis
  - 用途:
    - RSS 源缓存
    - 去重缓存（content_hash）
    - 分布式锁（防止重复爬取）
    - 限流计数

- [x] **4.3** 分布式限流实现
  - 注解: `@RateLimit`
  - 算法: 滑动窗口
  - 存储: Redis + Lua 脚本
  - 应用: API 端点保护

- [x] **4.4** 环境配置管理
  - `application-dev.yml`    - 开发环境
  - `application-staging.yml` - 预发布环境
  - `application-prod.yml`   - 生产环境
  - `.env.example` 模板

---

#### Task 5: Master-Worker 协作模式
**预估:** 3-5 天 | **负责人:** 待定 | **依赖:** 无 | **状态:** ✅ 完成 (2026-03-07)

- [x] **5.1** 实现 MasterWorkerCollaborationPattern
  - 接口: `CollaborationPattern`
  - 方法: `execute(masterAgentId, workerAgentIds, input)`

- [x] **5.2** 实现 CoordinatorAgent
  - 功能: 任务分配、结果汇总、进度跟踪
  - 模式: ReAct + 工具调用

- [x] **5.3** 集成到 TaskCoordinator
  - 方法: `executeMasterWorker(task, master, workers)`

- [x] **5.4** 安全修复
  - JSON解析: Jackson ObjectMapper替代正则解析
  - DoS防护: Worker数量限制(MAX=500) + 并发限制(100)
  - 输入验证: Agent ID格式验证 + 重复检测
  - 错误净化: 安全的错误消息
  - 超时机制: Worker(30s) + Master(300s)

**应用场景:**
- 热点追踪（TrendingCoordinatorAgent → Crawler + Analysis + Alert）
- 综合报告（Coordinator → Crawler + Analysis + Summary + Classification）

**相关文档:** [Master-Worker模式实现总结.md](../Master-Worker模式实现总结.md)

---

### 🟡 P2 - 质量保障（1-2 周）

#### Task 6: 测试完善
**预估:** 1 周 | **负责人:** 待定 | **依赖:** 所有开发任务 | **状态:** 🟡 进行中 (Phase 3-6 完成)

- [x] **6.1** 集成测试（Testcontainers）- Phase 1-2 完成
  - ✅ 父 POM 配置 Testcontainers BOM (1.20.4)
  - ✅ 父 POM 配置 JaCoCo 插件 (0.8.12)
  - ✅ E2E 模块创建 (javainfohunter-e2e)
  - ✅ BaseE2ETest.java (Testcontainers 基类)
  - ✅ BaseExternalServiceTest.java (外部服务基类)
  - ✅ PostgreSQLIntegrationTest.java (7 个数据库测试)
  - ✅ RabbitMQIntegrationTest.java (6 个消息队列测试)
  - ✅ RedisIntegrationTest.java (8 个缓存测试)
  - ✅ TestApplication.java 配置类

- [x] **6.2** E2E 测试 - Phase 3 完成
  - ✅ ApiTestHelper.java (REST API 测试工具类)
  - ✅ CrawlProcessApiE2ETest.java (7 个端到端测试)
  - ✅ ApiEndpointsE2ETest.java (20 个 API 端点测试)
  - ✅ test-rss-feed.xml (测试数据)

- [x] **6.3** 性能测试 - Phase 4 完成
  - ✅ VirtualThreadStressTest.java (虚拟线程压力测试)
  - ✅ JavaInfoHunter.jmx (JMeter 测试计划)
  - ✅ 1,000 并发 RSS 创建测试
  - ✅ 10,000 并发新闻查询测试
  - ✅ 5,000 并发搜索测试

- [x] **6.4** 测试覆盖率 ≥ 80% - Phase 5 完成
  - ✅ JaCoCo 配置 (80% 目标)
  - ✅ test-coverage-guide.md (覆盖率指南)
  - ⏳ 待 API 模块完成后执行

- [x] **6.5** 测试文档 - Phase 6 完成
  - ✅ test-execution-guide.md (执行指南)
  - ✅ test-coverage-guide.md (覆盖率指南)
  - ✅ PHASE_3_6_SUMMARY.md (实施总结)

- [x] **6.6** CI/CD 集成 - Phase 7 完成 (新增)
  - ✅ `.github/workflows/ci.yml` (主 CI 管道)
  - ✅ `.github/workflows/e2e-tests.yml` (E2E 测试)
  - ✅ `.github/workflows/coverage.yml` (覆盖率报告)
  - ✅ `.github/workflows/performance.yml` (性能测试)
  - ✅ `docker-compose.test.yml` (本地测试环境)
  - ✅ `docs/ci-cd-setup.md` (CI/CD 设置指南)
  - ✅ `docs/ci-cd-quick-reference.md` (快速参考)
  - ✅ `javainfohunter-e2e/performance-baseline.md` (性能基线)

**相关文档:**
- [2026-03-07-p2-task6-infrastructure.md](./completed/2026-03-07-p2-task6-infrastructure.md)
- [javainfohunter-e2e/PHASE_3_6_SUMMARY.md](../../javainfohunter-e2e/PHASE_3_6_SUMMARY.md)
- [docs/ci-cd-setup.md](../ci-cd-setup.md)

---

#### Task 7: 监控和运维
**预估:** 3-5 天 | **负责人:** 待定 | **依赖:** Task 4

- [ ] **7.1** Prometheus 指标导出
  - Spring Boot Actuator + Micrometer
  - 自定义指标: 爬取速率、处理延迟、Agent 执行时间

- [ ] **7.2** 分布式追踪
  - OpenTelemetry 集成
  - 追踪: 消息流完整链路

- [ ] **7.3** 日志聚合
  - ELK Stack 或 Loki
  - 结构化日志（JSON）

- [ ] **7.4** 告警规则配置
  - Prometheus Alertmanager
  - 场景: 爬取失败率、队列积压、处理延迟

---

#### Task 8: 安全加固
**预估:** 3-5 天 | **负责人:** 待定 | **依赖:** Task 3

- [ ] **8.1** 认证授权
  - Spring Security 6
  - JWT Token
  - 角色权限: ADMIN, USER, READONLY

- [ ] **8.2** API 限流
  - 基于用户/IP
  - 限流规则: 100 req/min 默认

- [ ] **8.3** 密钥管理
  - 环境变量
  - Docker Secrets
  - Vault（可选）

- [ ] **8.4** 安全审计
  - 审计日志: 操作记录
  - 定期扫描: dependency-check

---

### 🟢 P3 - 增强功能（后续迭代）

#### Task 9: 工具扩展
**预估:** 1-2 周 | **负责人:** 待定 | **依赖:** 无

- [ ] **9.1** RssParserTool（RSS 解析）
- [ ] **9.2** SentimentAnalysisTool（情感分析）
- [ ] **9.3** KeywordExtractionTool（关键词提取）
- [ ] **9.4** VectorSearchTool（向量搜索）
- [ ] **9.5** UserPreferenceTool（用户偏好）

---

#### Task 10: 推荐系统
**预估:** 2 周 | **负责人:** 待定 | **依赖:** Task 9

- [ ] **10.1** RecommendationAgent 实现
- [ ] **10.2** 向量相似度搜索
- [ ] **10.3** 个性化推荐算法
- [ ] **10.4** 推荐结果 API

---

#### Task 11: 管理界面
**预估:** 2-3 周 | **负责人:** 待定 | **依赖:** Task 3

- [ ] **11.1** Admin 前端（Vue3/React）
- [ ] **11.2** RSS 源管理界面
- [ ] **11.3** 内容监控仪表板
- [ ] **11.4** Agent 执行监控界面

---

#### Task 12: 高可用
**预估:** 1-2 周 | **负责人:** 待定 | **依赖:** 所有基础任务

- [ ] **12.1** 集群部署支持
- [ ] **12.2** 优雅停机机制
- [ ] **12.3** 数据库读写分离
- [ ] **12.4** 消息队列集群化

---

## 📅 时间规划

```
Week 1 (Mar 6-12):     P0 修复 + 基础设施
├── Task 1: 修复 Crawler Critical Issues
├── Task 2: 完善 Processor 存储
└── Task 4.1: Docker Compose 配置

Week 2-3 (Mar 13-26):  P1 API 层
├── Task 3: REST API 模块
└── Task 4.2-4.3: Redis + 限流

Week 4 (Mar 27-Apr 2): P1 协作模式
└── Task 5: Master-Worker 模式

Week 5-6 (Apr 3-16):    P2 质量保障
├── Task 6: 测试完善
├── Task 7: 监控运维
└── Task 8: 安全加固

Week 7+ (Apr 17+):      P3 增强功能
├── Task 9: 工具扩展
├── Task 10: 推荐系统
└── Task 11: 管理界面
```

---

## 🎯 里程碑

| 里程碑 | 目标日期 | 状态 | 说明 |
|--------|----------|------|------|
| **M1** | 2026-03-01 | ✅ 完成 | AI 服务模块 |
| **M2** | 2026-03-05 | ✅ 完成 | Crawler + Processor 模块 |
| **M3** | 2026-03-12 | ✅ 完成 | P0 修复 + Docker Compose |
| **M4** | 2026-03-26 | ✅ 完成 | REST API + 基础设施 |
| **M5** | 2026-04-02 | ✅ 完成 | Master-Worker 模式完成 |
| **M6** | 2026-04-16 | ⏳ 计划中 | 测试覆盖率 ≥ 80% |
| **M7** | 2026-04-30 | ⏳ 计划中 | 监控告警就绪 |
| **M8** | 2026-05-15 | ⏳ 计划中 | 生产部署就绪 |

---

## 📊 风险与阻塞

| 风险 | 影响 | 缓解措施 | 状态 |
|------|------|----------|------|
| DashScope API 限流 | 高 | 实现多 API Key 轮换、降级策略 | ⏳ |
| RabbitMQ 消息积压 | 中 | 动态消费者扩容、监控告警 | ⏳ |
| 向量搜索性能 | 中 | IVFFlat 索引优化、分区 | ⏳ |
| 测试环境不稳定 | 低 | Testcontainers 稳定化 | ⏳ |

---

## 📖 相关文档

### 架构文档
- [技术方案.md](../技术方案.md) - 完整系统技术方案
- [数据传输架构设计.md](../数据传输架构设计.md) - 消息队列架构
- [数据库设计说明.md](../数据库设计说明.md) - 数据库表结构

### 实现文档
- [crawler-module-implementation-complete.md](./crawler-module-implementation-complete.md) - 爬虫模块完成总结
- [2026-03-05-content-processor-module-progress.md](./2026-03-05-content-processor-module-progress.md) - 处理模块进度
- [code-review-summary.md](./code-review-summary.md) - 代码审查总结

### AI 服务文档
- [javainfohunter-ai-service/README.md](../javainfohunter-ai-service/README.md) - AI 服务模块说明
- [javainfohunter-ai-service/USAGE.md](../javainfohunter-ai-service/USAGE.md) - AI 服务使用指南

---

**最后更新:** 2026-03-07
**维护者:** JavaInfoHunter Team
**下次审查:** 2026-03-14
