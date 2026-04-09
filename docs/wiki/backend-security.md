# JavaInfoHunter 安全态势

## 概述

本文档记录 JavaInfoHunter 后端当前的安全配置、已知安全问题和改进建议。系统当前处于开发阶段，部分安全功能尚未完善。

---

## 当前安全配置

### 1. 认证与授权

**当前状态**: **未实现**

系统目前没有任何用户认证和授权机制。所有 API 接口均无需登录即可访问。

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户认证 | 未实现 | 无登录/注册功能 |
| JWT Token | 未实现 | 无 Token 验证 |
| API Key | 未实现 | 无接口密钥 |
| OAuth2 | 未实现 | 无第三方登录 |
| RBAC 权限 | 未实现 | 无角色权限控制 |
| 接口鉴权 | 未实现 | 所有接口公开可访问 |

**风险等级**: **高**

> 管理接口（`/api/v1/admin/*`）和爬虫触发接口（`POST /api/crawler/trigger`）无任何访问控制，任何人都可以触发全量爬取或删除 RSS 源。

### 2. CORS 跨域配置

**当前状态**: **通配符配置（开发环境）**

```yaml
javainfohunter:
  api:
    cors:
      enabled: true
      allowed-origins:
        - "*"           # 允许所有来源
      allowed-methods:
        - GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH
      allowed-headers:
        - "*"           # 允许所有请求头
      allow-credentials: false
      max-age: 3600
```

**配置类**: `CorsConfig.java`
- 通过 `ConfigurationProperties` 实现，支持环境化配置
- 默认 `enabled: false`（代码层面），但 YAML 配置中设为 `true`
- `allow-credentials: false`，避免了通配符 + 凭证的组合问题

**风险等级**: **中**

> 开发环境使用通配符可以接受，但生产环境必须配置具体的域名白名单。

**生产环境建议**:
```yaml
javainfohunter:
  api:
    cors:
      allowed-origins:
        - "https://javainfohunter.example.com"
        - "https://admin.javainfohunter.example.com"
```

### 3. 接口限流

**当前状态**: **框架已实现，未启用**

系统已实现基于 Redis 的限流切面（`RateLimitAspect`），但当前没有任何 Controller 方法使用 `@RateLimit` 注解。

**已实现的限流能力**:

| 功能 | 说明 |
|------|------|
| `@RateLimit` 注解 | 声明式限流 |
| IP 限流 | `KeyType.IP` |
| 用户 ID 限流 | `KeyType.USER_ID`（需认证） |
| 端点限流 | `KeyType.ENDPOINT` |
| 自定义 SpEL 限流 | `KeyType.CUSTOM` |
| Redis 计数 | 滑动窗口限流 |
| 限流异常 | `RateLimitExceededException` |

**限流注解参数**:
```java
@RateLimit(
    prefix = "api",           // Redis key 前缀
    keyType = KeyType.IP,     // 限流维度
    limit = 100,              // 限流阈值
    window = @Window(value = 1, unit = TimeUnit.MINUTES),  // 时间窗口
    includeMethod = true      // 是否包含方法名
)
```

**风险等级**: **中**

> 限流框架完善，但未在任何接口上启用。系统容易受到 DDoS 攻击和暴力请求。

### 4. 输入验证

**当前状态**: **部分实现**

| 层级 | 状态 | 说明 |
|------|------|------|
| Controller 层 | 已实现 | 使用 `@Valid`, `@NotBlank`, `@Size`, `@Min`, `@Max` |
| Entity 层 | 已实现 | JPA 实体使用 Jakarta Validation 注解 |
| SQL 注入防护 | 已实现 | 使用 Spring Data JPA（参数化查询） |
| XSS 防护 | **未实现** | 无 HTML 转义或过滤 |
| 请求体大小限制 | **未明确配置** | 无全局 `max-http-post-size` 限制 |

**输入验证示例**（RssSource 实体）:
```java
@NotBlank(message = "Name cannot be blank")
@Size(max = 255, message = "Name must not exceed 255 characters")
private String name;

@NotBlank(message = "URL cannot be blank")
@Size(max = 2048, message = "URL must not exceed 2048 characters")
private String url;
```

**风险等级**: **低**

> Spring Data JPA 默认使用参数化查询，有效防止 SQL 注入。但原始内容（`raw_content`）和 AI 处理结果可能包含恶意 HTML/JS。

### 5. 环境变量管理

**当前状态**: **使用 .env 文件 + 环境变量**

系统通过 `dotenv-java` 库加载 `.env` 文件，支持环境变量覆盖。

**敏感配置项**:

| 配置 | 环境变量 | 存储位置 | 泄露风险 |
|------|---------|---------|---------|
| 数据库密码 | `DB_PASSWORD` | .env 文件 | 中 |
| RabbitMQ 密码 | `RABBITMQ_PASSWORD` | .env 文件 | 中 |
| Redis 密码 | `REDIS_PASSWORD` | .env 文件 | 中 |
| DashScope API Key | `DASHSCOPE_API_KEY` | .env 文件 | 高 |

**.env 加载机制**: `DotenvEnvironmentPostProcessor`

**风险等级**: **中**

> `.env` 文件应确保不被提交到版本控制。需检查 `.gitignore` 是否包含 `.env`。

### 6. 数据库安全

**当前状态**: **基本配置**

| 项目 | 状态 | 说明 |
|------|------|------|
| DDL 模式 | `validate` | 生产安全，不自动修改表结构 |
| Flyway 迁移 | 启用 | 数据库版本管理 |
| 连接池 | HikariCP | 成熟稳定的连接池 |
| SSL 连接 | **未配置** | 数据库连接未加密 |
| 最小权限 | **未确认** | 可能使用超级用户权限 |

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # 仅验证，不修改表结构
    open-in-view: false     # 避免懒加载问题
```

**风险等级**: **低-中**

> `ddl-auto: validate` 确保实体与数据库一致，不会意外修改表结构。但数据库连接未使用 SSL。

### 7. RabbitMQ 安全

**当前状态**: **基本配置**

| 项目 | 配置 |
|------|------|
| 认证 | 用户名/密码 |
| 虚拟主机 | 可配置 (`RABBITMQ_VHOST`) |
| 消息确认 | 手动确认模式 |
| 发布者确认 | 启用（`correlated`） |
| 预取限制 | 10 条/消费者 |
| 死信队列 | 已配置 |

**风险等级**: **低**

> RabbitMQ 连接使用认证，消息确认机制完善。

### 8. Agent 安全

**当前状态**: **已实现防护**

| 防护措施 | 说明 |
|---------|------|
| Agent ID 验证 | 正则 `^[a-zA-Z0-9-_]{1,64}$`，防止注入 |
| 最大步数限制 | 默认 10 步，防止无限循环 |
| 超时控制 | Worker 30s，Master 300s |
| 并发控制 | 最大 100 Worker，队列 500 |
| 线程安全 | `ConcurrentHashMap`, `volatile`, `AtomicInteger` |

**风险等级**: **低**

> Agent 框架已实现多层防护，有效防止资源耗尽和注入攻击。

### 9. 日志安全

**当前状态**: **基本配置**

| 项目 | 说明 |
|------|------|
| 日志级别 | 生产 `INFO`，开发 `DEBUG` |
| SQL 日志 | 关闭（`show-sql: false`） |
| 参数日志 | 关闭（`BasicBinder: WARN`） |
| 敏感数据 | 未在日志中打印密码等敏感信息 |

**风险等级**: **低**

### 10. 全局异常处理

**当前状态**: **已实现**

系统使用 `GlobalExceptionHandler` 统一处理异常：

| 异常类型 | 处理方式 |
|---------|---------|
| `ResourceNotFoundException` | 404 Not Found |
| `BusinessException` | 对应业务错误码 |
| `RateLimitExceededException` | 429 Too Many Requests |
| `MethodArgumentNotValidException` | 400 Bad Request |
| 其他 | 500 Internal Server Error |

**风险等级**: **低**

> 异常处理完善，不会泄露堆栈信息给客户端。

---

## 已知安全问题清单

| # | 问题 | 风险等级 | 状态 | 建议 |
|---|------|---------|------|------|
| 1 | 无用户认证和授权 | **高** | 待实现 | 引入 Spring Security + JWT |
| 2 | 管理接口无访问控制 | **高** | 待实现 | 至少添加 API Key 鉴权 |
| 3 | CORS 通配符配置 | **中** | 需改进 | 生产环境配置具体域名 |
| 4 | 限流未启用 | **中** | 待启用 | 为管理接口和搜索接口添加 `@RateLimit` |
| 5 | 无 XSS 防护 | **中** | 待实现 | 对用户输入进行 HTML 转义 |
| 6 | 数据库连接无 SSL | **低** | 待配置 | 生产环境启用 SSL 连接 |
| 7 | 无请求体大小限制 | **低** | 待配置 | 配置 `server.tomcat.max-http-form-post-size` |
| 8 | 无 HTTPS 强制 | **低** | 待配置 | 生产环境强制 HTTPS |
| 9 | 无 CSRF 防护 | **低** | 待评估 | REST API 通常不需要，但管理接口应考虑 |
| 10 | 无安全响应头 | **低** | 待实现 | 添加 CSP, X-Frame-Options 等安全头 |

---

## 安全改进路线图

### 第一阶段：基础安全（优先级：高）

1. **API Key 鉴权**：为管理接口添加 API Key 验证
2. **启用限流**：为 `/api/v1/admin/*` 和 `/api/v1/news/search` 添加 `@RateLimit`
3. **CORS 白名单**：生产环境配置具体域名
4. **请求体大小限制**：配置 Tomcat 最大请求大小
5. **安全响应头**：添加 CSP、X-Content-Type-Options、X-Frame-Options

### 第二阶段：认证授权（优先级：高）

1. **Spring Security 集成**：引入安全框架
2. **JWT 认证**：实现无状态认证
3. **RBAC 权限**：角色权限控制（admin, user, readonly）
4. **接口权限**：管理接口仅 admin 角色可访问

### 第三阶段：深度防护（优先级：中）

1. **XSS 防护**：输入过滤和输出转义
2. **审计日志**：记录敏感操作日志
3. **数据库 SSL**：加密数据库连接
4. **HTTPS 强制**：安全传输
5. **Secrets 管理**：集成 Vault 或云密钥管理服务

---

## 敏感信息检查清单

在提交代码前，确保：

- [ ] 无硬编码密码或 API Key
- [ ] `.env` 文件在 `.gitignore` 中
- [ ] 无测试凭据提交到代码仓库
- [ ] 日志中无敏感数据输出
- [ ] 异常响应中无内部信息泄露
- [ ] Swagger 文档未暴露生产环境（可通过 `knife4j.production: true` 关闭）
