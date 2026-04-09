# 前端安全文档

> 本文档分析 JavaInfoHunter 前端当前的安全态势，识别安全差距，并提供生产部署安全建议。

## 当前安全态势

### 安全评级：高风险（开发阶段）

前端当前处于**开发阶段**，缺少多项关键安全机制。以下按照严重程度排列。

---

## 关键安全差距

### 1. 无身份认证和授权（严重）

**现状**：系统没有任何身份认证机制。所有 API 端点和页面完全开放。

**风险**：
- 任何人可以访问所有管理功能（RSS 源增删改、手动爬取触发、系统状态查看）
- 数据完全暴露，无访问控制
- 恶意用户可以删除所有 RSS 源或触发大量爬取操作

**影响范围**：
- `/admin/sources` - RSS 源 CRUD 操作
- `/admin/system` - 手动爬取触发
- `/agents/executions` - Agent 执行记录查看
- 所有 API 端点

**建议修复方案**：

| 方案 | 描述 | 复杂度 |
|------|------|--------|
| JWT Token 认证 | 后端签发 JWT，前端在请求头中携带 `Authorization: Bearer <token>` | 中 |
| OAuth 2.0 / OIDC | 集成第三方身份提供商（Google, GitHub） | 中 |
| Session Cookie | 传统 Session 认证，配合 CSRF Token | 低 |

**实现建议**（JWT 方案）：

```typescript
// 1. Axios 拦截器添加 Token
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 2. 401 响应自动跳转登录
client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 3. 路由守卫
function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/login" />;
  return children;
}
```

---

### 2. 无 CSRF 保护（严重）

**现状**：所有 POST/PUT/DELETE 请求没有 CSRF Token 保护。

**风险**：
- 跨站请求伪造攻击
- 恶意网站可以代用户执行管理操作（删除源、触发爬取等）

**建议修复方案**：

```typescript
// 方案 1：SameSite Cookie（推荐）
// 后端设置 Cookie：Set-Cookie: session=xxx; SameSite=Strict; HttpOnly; Secure

// 方案 2：CSRF Token Header
// 后端在响应中返回 CSRF Token，前端在每个修改请求中携带
client.interceptors.request.use((config) => {
  if (['POST', 'PUT', 'DELETE'].includes(config.method?.toUpperCase())) {
    const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
    if (csrfToken) {
      config.headers['X-CSRF-Token'] = csrfToken;
    }
  }
  return config;
});
```

---

### 3. 无 React Error Boundaries（中等）

**现状**：应用没有 React Error Boundary 组件。任何组件渲染错误会导致整个应用白屏崩溃。

**风险**：
- 单个组件错误导致全局 UI 崩溃
- 用户体验差，无法恢复
- 错误信息可能暴露敏感信息（堆栈跟踪）

**建议修复方案**：

```tsx
// 1. 创建全局 Error Boundary
class ErrorBoundary extends React.Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    logger.error('React Error Boundary caught:', error, errorInfo);
    // 可以上报到错误追踪服务
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center min-h-screen gap-4">
          <h2 className="text-xl font-bold">Something went wrong</h2>
          <p className="text-muted-foreground">Please try refreshing the page.</p>
          <Button onClick={() => window.location.reload()}>Refresh Page</Button>
        </div>
      );
    }
    return this.props.children;
  }
}

// 2. 在 App.tsx 中包裹
<ErrorBoundary>
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <Routes>...</Routes>
    </BrowserRouter>
  </QueryClientProvider>
</ErrorBoundary>
```

建议在关键模块（Dashboard, News, Knowledge Graph）级别也添加独立的 Error Boundary。

---

### 4. API 错误消息暴露（中等）

**现状**：Axios 拦截器将后端错误消息直接通过 Toast 显示给用户。

```typescript
// client.ts
(error) => {
  const errorMsg = error.response?.data?.message || 'An error occurred';
  toast.error(errorMsg);  // 直接展示后端错误消息
}
```

**风险**：
- 后端可能返回包含技术细节的错误消息（SQL 错误、堆栈跟踪）
- 恶意用户可以通过构造请求获取系统内部信息
- 暴露后端技术栈信息（如 "Hibernate exception: ..."）

**建议修复方案**：

```typescript
// 方案 1：前端错误消息映射
const ERROR_MESSAGES: Record<number, string> = {
  400: '请求参数有误，请检查输入',
  401: '登录已过期，请重新登录',
  403: '您没有权限执行此操作',
  404: '请求的资源不存在',
  429: '操作过于频繁，请稍后再试',
  500: '服务器内部错误，请稍后再试',
  502: '服务暂时不可用，请稍后再试',
  503: '系统维护中，请稍后再试',
};

client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status;
    const userMessage = ERROR_MESSAGES[status] || '操作失败，请稍后再试';
    toast.error(userMessage);
    logger.error('API Error:', status, error.response?.data);
    return Promise.reject(error);
  }
);

// 方案 2：后端统一错误响应
// 后端应在生产环境隐藏技术细节，仅返回用户友好的错误消息
```

---

### 5. CORS 配置（中等）

**现状**：
- 开发环境通过 Vite 代理绕过 CORS
- 生产环境前端直接调用后端 API，依赖后端 CORS 配置

**风险**：
- 如果后端 CORS 配置不当（如 `Access-Control-Allow-Origin: *`），任何网站可以调用 API
- 携带凭证的请求需要精确的 Origin 配置

**建议**：

后端 CORS 配置建议：

```java
// 仅允许前端域名
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://yourdomain.com",
            "https://admin.yourdomain.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);  // 需要精确 Origin
        config.setMaxAge(3600L);
        // ...
    }
}
```

---

### 6. XSS 防护分析

**现状**：React 默认对 JSX 中的动态内容进行 HTML 转义，提供基础 XSS 防护。

**已有防护**：
- React JSX 自动转义：`<div>{userInput}</div>` 会转义 HTML 标签
- `dangerouslySetInnerHTML` 未在项目中使用

**潜在风险点**：
- Knowledge Graph 使用 D3.js 直接操作 DOM（`.text()`, `.attr()`）
  - `d3.select(...).text(label)` 是安全的（自动转义）
  - 但如果未来使用 `.html()` 则存在 XSS 风险
- Toast 通知显示 API 错误消息（见上文第 4 点）
- `window.open(url, '_blank')` 在 News 详情中未添加 `noopener` 以外的安全参数

**建议**：

```tsx
// Knowledge Graph 中禁止使用 .html()
// 安全的：
node.append('text').text(d => d.label);

// 危险的（禁止使用）：
node.append('text').html(d => d.label);  // 可能注入 HTML
```

---

### 7. 敏感信息处理

**现状**：
- 无敏感信息在前端代码中硬编码
- API Key 不在前端代码中（DashScope API Key 仅在后端使用）
- 环境变量仅包含 `VITE_API_URL`（API 地址，非敏感信息）

**已有防护**：
- API Key 在后端管理，前端通过 API 间接调用 AI 服务
- Mock 数据中不包含真实凭证

**建议**：
- 确保 `.env` 文件在 `.gitignore` 中
- 生产环境使用环境变量注入，不使用 `.env` 文件
- 前端构建产物中不应包含任何敏感信息

---

## 生产部署安全检查清单

### 部署前必须完成

| 优先级 | 项目 | 当前状态 | 建议 |
|--------|------|----------|------|
| P0 | 身份认证 | 未实现 | 实现 JWT 或 Session 认证 |
| P0 | 路由守卫 | 未实现 | 保护 Admin 模块路由 |
| P0 | API 鉴权 | 未实现 | 后端 API 添加认证中间件 |
| P0 | CSRF 保护 | 未实现 | 添加 CSRF Token 或 SameSite Cookie |
| P1 | Error Boundary | 未实现 | 添加全局和模块级 Error Boundary |
| P1 | 错误消息过滤 | 部分实现 | 使用前端错误消息映射 |
| P1 | CORS 配置 | 待确认 | 后端精确配置允许的 Origin |
| P1 | HTTPS | 待部署 | 生产环境必须启用 HTTPS |
| P2 | CSP 头 | 未设置 | 配置 Content-Security-Policy |
| P2 | Rate Limiting | 前端部分实现 | 后端 API 添加限流 |
| P2 | 日志脱敏 | 未实现 | 前端 logger 不应记录敏感数据 |

### Content-Security-Policy 建议

```nginx
# Nginx 配置
add_header Content-Security-Policy "
  default-src 'self';
  script-src 'self';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https:;
  font-src 'self';
  connect-src 'self' https://your-api-domain.com;
  frame-ancestors 'none';
" always;

# 其他安全头
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
```

### 依赖安全

```bash
# 定期检查依赖漏洞
npm audit

# 自动修复
npm audit fix

# 查看依赖许可证
npx license-checker --summary
```

### 前端 Rate Limiting

当前仅 System 模块有爬取触发冷却时间（60 秒）。建议扩展到所有修改操作：

```typescript
// 通用 Rate Limiter
const useRateLimit = (cooldownMs: number) => {
  const lastActionTime = useRef(0);

  const canExecute = () => {
    const now = Date.now();
    return now - lastActionTime.current >= cooldownMs;
  };

  const execute = (action: () => void) => {
    if (!canExecute()) {
      const remaining = Math.ceil(
        (cooldownMs - (Date.now() - lastActionTime.current)) / 1000
      );
      toast.error(`请等待 ${remaining} 秒后再试`);
      return false;
    }
    lastActionTime.current = Date.now();
    action();
    return true;
  };

  return { canExecute, execute };
};
```

---

## 安全架构建议（未来演进）

### 短期（1-2 周）

1. 添加全局 Error Boundary
2. 实现前端错误消息映射（隐藏后端技术细节）
3. 后端 CORS 精确配置
4. 添加 CSP 和安全响应头

### 中期（2-4 周）

1. 实现 JWT 认证流程（登录页、Token 管理、路由守卫）
2. Admin 模块添加权限控制（仅管理员可访问）
3. API 请求添加 CSRF Token
4. 实现前端 Token 自动刷新机制

### 长期（1-2 月）

1. 集成 OAuth 2.0 / OIDC（可选）
2. 添加操作审计日志
3. 实现前端请求签名（防篡改）
4. 集成前端错误追踪服务（Sentry 等）
