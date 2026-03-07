# REST API 模块实现计划

> **创建日期:** 2026-03-06
> **目标:** 创建 REST API 层，提供 HTTP 接口访问系统功能
> **预估工时:** 1-2 周
> **依赖:** 所有核心模块（ai-service, crawler, processor）

---

## 📋 模块概述

**模块名称:** javainfohunter-api

**功能描述:**
创建 REST API 层，提供 HTTP 接口访问系统功能，包括：
- RSS 源管理（CRUD）
- 内容查询（分页、搜索、过滤）
- Agent 执行监控
- 系统管理（健康检查、手动触发爬取）

**技术栈:**
- Spring Boot 4.0.3
- Spring Web
- Spring Data JPA
- Spring Doc OpenAPI
- Knife4j 4.5.0

**依赖模块:**
- javainfohunter-ai-service
- javainfohunter-crawler
- javainfohunter-processor

---

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      REST API Layer                         │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Controller Layer                          │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  RssSourceController    │  NewsController            │  │
│  │  AgentController        │  AdminController            │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Service Layer                             │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  RssSourceService     │  NewsService                 │  │
│  │  AgentService         │  CrawlOrchestrationService    │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Repository Layer                          │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  RssSourceRepository  │  NewsRepository              │  │
│  │  AgentExecutionRepository                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 任务清单

### Task 3.1: 创建 Maven 模块结构
**预估:** 0.5 天 | **依赖:** 无

- [ ] 3.1.1 创建 javainfohunter-api 目录结构
- [ ] 3.1.2 创建 pom.xml
- [ ] 3.1.3 创建 ApiApplication.java
- [ ] 3.1.4 创建 application.yml
- [ ] 3.1.5 更新父 POM 添加 api 模块

**验证:**
- [ ] 模块可以独立编译

---

### Task 3.2: 实现 RSS 源管理 API
**预估:** 2 天 | **依赖:** Task 3.1

- [ ] 3.2.1 创建 RssSourceController
  - `POST /api/v1/rss-sources` - 创建 RSS 源
  - `GET /api/v1/rss-sources` - 列表（分页、过滤）
  - `GET /api/v1/rss-sources/{id}` - 详情
  - `PUT /api/v1/rss-sources/{id}` - 更新
  - `DELETE /api/v1/rss-sources/{id}` - 删除
  - `POST /api/v1/rss-sources/{id}/crawl` - 手动触发爬取

- [ ] 3.2.2 创建 RssSourceRequest DTO
- [ ] 3.2.3 创建 RssSourceResponse DTO
- [ ] 3.2.4 添加参数校验 (@Valid)
- [ ] 3.2.5 添加全局异常处理

**验证:**
- [ ] 所有接口测试通过
- [ ] 参数校验正常工作

---

### Task 3.3: 实现内容查询 API
**预估:** 2 天 | **依赖:** Task 3.1

- [ ] 3.3.1 创建 NewsController
  - `GET /api/v1/news` - 新闻列表（分页、排序）
  - `GET /api/v1/news/{id}` - 新闻详情
  - `GET /api/v1/news/search` - 全文搜索
  - `GET /api/v1/news/similar/{id}` - 相似内容（向量）
  - `GET /api/v1/news/trending` - 热点新闻
  - `GET /api/v1/news/category/{category}` - 按分类查询

- [ ] 3.3.2 创建 NewsRequest DTO（查询参数）
- [ ] 3.3.3 创建 NewsResponse DTO
- [ ] 3.3.4 实现分页和排序

**验证:**
- [ ] 所有接口测试通过
- [ ] 分页正常工作

---

### Task 3.4: 实现 Agent 执行监控 API
**预估:** 1 天 | **依赖:** Task 3.1

- [ ] 3.4.1 创建 AgentController
  - `GET /api/v1/agents/executions` - 执行记录列表
  - `GET /api/v1/agents/executions/{id}` - 执行详情
  - `GET /api/v1/agents/stats` - 统计信息

- [ ] 3.4.2 创建 AgentExecutionResponse DTO
- [ ] 3.4.3 创建 AgentStatsResponse DTO

**验证:**
- [ ] 所有接口测试通过

---

### Task 3.5: 实现系统管理 API
**预估:** 1 天 | **依赖:** Task 3.1

- [ ] 3.5.1 创建 AdminController
  - `POST /api/v1/admin/crawl/trigger` - 触发全站爬取
  - `POST /api/v1/admin/crawl/{sourceId}` - 触发单源爬取
  - `GET /api/v1/admin/status` - 系统状态

**验证:**
- [ ] 所有接口测试通过
- [ ] 权限验证正常（添加 @PreAuthorize）

---

### Task 3.6: 集成 Knife4j API 文档
**预估:** 0.5 天 | **依赖:** Task 3.1

- [ ] 3.6.1 添加 springdoc-openapi 依赖
- [ ] 3.6.2 添加 knife4j 依赖
- [ ] 3.6.3 配置 OpenAPI 信息
- [ ] 3.6.4 添加接口分组注解
- [ ] 3.6.5 验证文档可访问

**验证:**
- [ ] 访问 /doc.html 查看文档
- [ ] 接口分组正确

---

## 📂 文件结构

```
javainfohunter-api/
├── pom.xml
├── src/main/java/.../api/
│   ├── ApiApplication.java                    # 主应用类
│   ├── config/
│   │   ├── OpenApiConfig.java                 # OpenAPI 配置
│   │   └── WebConfig.java                     # Web 配置
│   ├── controller/
│   │   ├── RssSourceController.java           # RSS 源管理
│   │   ├── NewsController.java                # 内容查询
│   │   ├── AgentController.java               # Agent 监控
│   │   └── AdminController.java               # 系统管理
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RssSourceRequest.java
│   │   │   └── NewsQueryRequest.java
│   │   └── response/
│   │       ├── RssSourceResponse.java
│   │       ├── NewsResponse.java
│   │       ├── AgentExecutionResponse.java
│   │       └── ApiResponse.java               # 统一响应封装
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java         # 全局异常处理
│   │   └── BusinessException.java
│   └── service/
│       └── (可能包含一些聚合服务)
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── src/test/java/.../api/
    └── controller/
        └── (Controller 测试)
```

---

## 📝 详细代码框架

### 1. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ron</groupId>
        <artifactId>JavaInfoHunter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>javainfohunter-api</artifactId>
    <name>JavaInfoHunter API Module</name>
    <description>REST API layer for JavaInfoHunter</description>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- SpringDoc OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>

        <!-- Knife4j -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>4.5.0</version>
        </dependency>

        <!-- AI Service Module -->
        <dependency>
            <groupId>com.ron</groupId>
            <artifactId>javainfohunter-ai-service</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 2. RssSourceController

```java
@RestController
@RequestMapping("/api/v1/rss-sources")
@RequiredArgsConstructor
@Tag(name = "RSS 源管理", description = "RSS 订阅源的增删改查操作")
@Slf4j
public class RssSourceController {

    private final RssSourceService rssSourceService;
    private final CrawlOrchestrator crawlOrchestrator;

    @PostMapping
    @Operation(summary = "创建 RSS 源")
    public ApiResponse<RssSourceResponse> createSource(
            @Valid @RequestBody RssSourceRequest request) {
        RssSource source = rssSourceService.createSource(request);
        return ApiResponse.success(toResponse(source));
    }

    @GetMapping
    @Operation(summary = "获取 RSS 源列表")
    public ApiResponse<Page<RssSourceResponse>> getSources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RssSource> sources = rssSourceService.getSources(category, isActive, pageable);
        return ApiResponse.success(sources.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取 RSS 源详情")
    public ApiResponse<RssSourceResponse> getSource(@PathVariable Long id) {
        RssSource source = rssSourceService.getSourceById(id);
        return ApiResponse.success(toResponse(source));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 RSS 源")
    public ApiResponse<RssSourceResponse> updateSource(
            @PathVariable Long id,
            @Valid @RequestBody RssSourceRequest request) {
        RssSource source = rssSourceService.updateSource(id, request);
        return ApiResponse.success(toResponse(source));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 RSS 源")
    public ApiResponse<Void> deleteSource(@PathVariable Long id) {
        rssSourceService.deleteSource(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/crawl")
    @Operation(summary = "手动触发爬取")
    public ApiResponse<CrawlResultMessage> triggerCrawl(@PathVariable Long id) {
        RssSource source = rssSourceService.getSourceById(id);
        CrawlResultMessage result = crawlOrchestrator.executeCrawlJob(List.of(source));
        return ApiResponse.success(result);
    }

    private RssSourceResponse toResponse(RssSource source) {
        return RssSourceResponse.builder()
                .id(source.getId())
                .name(source.getName())
                .url(source.getUrl())
                .category(source.getCategory())
                .tags(source.getTags())
                .isActive(source.getIsActive())
                .crawlIntervalSeconds(source.getCrawlIntervalSeconds())
                .lastCrawledAt(source.getLastCrawledAt())
                .totalArticles(source.getTotalArticles())
                .failedCrawls(source.getFailedCrawls())
                .build();
    }
}
```

### 3. 统一响应封装

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
```

### 4. OpenAPI 配置

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JavaInfoHunter API")
                        .version("1.0.0")
                        .description("全网资讯猎手 REST API")
                        .contact(new Contact()
                                .name("JavaInfoHunter Team")
                                .email("contact@javainfohunter.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("开发环境"),
                        new Server().url("https://api.javainfohunter.com").description("生产环境")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt", List.of("read", "write")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

---

## ✅ 完成标准

- [ ] 所有 Controller 实现完成
- [ ] 统一响应格式
- [ ] 参数校验
- [ ] 全局异常处理
- [ ] API 文档可访问
- [ ] 单元测试覆盖率 ≥ 80%

---

## 📖 相关文档

- [技术方案.md](../../技术方案.md)
- [roadmap.md](../roadmap.md)

---

**最后更新:** 2026-03-06
**维护者:** JavaInfoHunter Team
