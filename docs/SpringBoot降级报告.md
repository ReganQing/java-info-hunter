# Spring Boot 降级报告（4.0.3 → 3.3.5）

## 📊 执行总结

**执行时间：** 2026-03-01
**任务：** 降级 Spring Boot 以解决 Spring AI Alibaba 兼容性问题
**状态：** ⚠️ 版本降级成功，但测试仍受阻于 Bean 冲突

---

## ✅ 已完成的工作

### 1. Spring Boot 版本降级

**修改文件：** `pom.xml`

```xml
<!-- 修改前 -->
<version>4.0.3</version>

<!-- 修改后 -->
<version>3.3.5</version>
```

**状态：** ✅ 成功

### 2. 项目重新构建

```bash
mvnw.cmd clean
mvnw.cmd compile -DskipTests
```

**结果：** ✅ 编译成功（36 个源文件，27 个警告）

**警告类型：** Lombok @Builder 初始化表达式警告（不影响功能）

### 3. 测试配置优化

**修改文件：**
- `TestApplication.java` - 添加自动配置排除
- `application-test.yml` - 添加 Bean 覆盖和自动配置排除

**添加的配置：**
```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  autoconfigure:
    exclude:
      - com.alibaba.cloud.ai.autoconfigure.dashscope.*
      - org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration
```

---

## ❌ 遇到的问题

### 问题 1：Bean 定义冲突

**错误信息：**
```
Cannot register bean definition with name 'chatClientBuilderConfigurer'
since there is already [Root bean: class [null]]
```

**根本原因：**
- Spring AI 和 Spring AI Alibaba 都提供了 `ChatClientAutoConfiguration`
- 两个自动配置类尝试注册相同名称的 Bean
- Spring Boot 3.3.5 的 Bean 定义覆盖机制与 4.0.3 有差异

**影响的类：**
- `org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration`
- `org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration`

### 问题 2：自动配置排除不完整

**尝试的方案：**
1. ✅ 排除数据库自动配置
2. ✅ 排除 Spring AI Alibaba 自动配置
3. ✅ 添加 `allow-bean-definition-overriding: true`
4. ❌ Bean 冲突仍然存在

---

## 📋 兼容性分析

### Spring AI Alibaba 依赖关系

```
spring-ai-alibaba-starter-dashscope
├── spring-ai-core (Spring AI 官方)
│   └── spring-autoconfigure (自动配置)
│       ├── ChatClientAutoConfiguration
│       └── EmbeddingModelAutoConfiguration
└── spring-ai-alibaba-autoconfigure
    ├── DashScopeChatAutoConfiguration
    └── DashScopeEmbeddingAutoConfiguration
```

### 冲突点

| 自动配置类 | 提供者 | 冲突原因 |
|-------------|--------|----------|
| `ChatClientAutoConfiguration` | Spring AI | 两个包都有同名类 |
| `DashScopeAutoConfiguration` | Spring AI Alibaba | 依赖 Spring AI ChatClient |

---

## 💡 推荐解决方案

### 方案 A：排除 Spring AI 依赖（推荐⭐⭐⭐⭐⭐）

**步骤：**
1. 在测试 pom.xml 中排除 Spring AI 核心
2. 只保留 Spring AI Alibaba 和 RabbitMQ 依赖

**修改：**
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <exclusions>
        <!-- 排除 Spring AI 核心避免冲突 -->
        <exclusion>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-core</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-autoconfigure</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**优点：**
- 彻底解决 Bean 冲突
- 保持 Spring AI Alibaba 完整功能
- RabbitMQ 测试不受影响

**缺点：**
- 需要修改 pom.xml

### 方案 B：创建独立的 RabbitMQ 学习模块（推荐⭐⭐⭐⭐）

**步骤：**
1. 创建新模块 `rabbitmq-learning`
2. 只添加 RabbitMQ 依赖
3. 不依赖 AI 服务模块

**项目结构：**
```
JavaInfoHunter/
├── rabbitmq-learning/         # 新增独立模块
│   ├── pom.xml                # 只有 RabbitMQ 依赖
│   └── src/test/java/...      # 测试代码
├── javainfohunter-ai-service/  # 保持不变
└── pom.xml
```

**优点：**
- 完全隔离测试环境
- 避免版本冲突
- 专注于 RabbitMQ 学习
- 可以复用现有测试代码

**缺点：**
- 需要创建新模块

### 方案 C：使用 Spring Boot 2.7.x（推荐⭐⭐⭐）

**步骤：**
降级到 Spring Boot 2.7.x（使用 `javax.*` 而非 `jakarta.*`）

**优点：**
- Spring AI Alibaba 完全兼容
- 社区支持最稳定

**缺点：**
- Jakarta EE 8 → Java EE 8
- 大规模代码重构
- 不推荐

### 方案 D：等待 Spring AI Alibaba 更新（推荐⭐）

**步骤：**
等待 Spring AI Alibaba 发布支持 Spring Boot 3.3.x 的版本

**优点：**
- 无需修改代码
- 官方支持

**缺点：**
- 时间不确定

---

## 📊 测试执行记录

### 测试运行历史

| 尝试次数 | 配置修改 | 结果 | 错误信息 |
|---------|---------|------|----------|
| 1 | 初始配置 | ❌ | RestClientAutoConfiguration not present |
| 2 | Spring Boot 3.3.5 | ❌ | Bean definition override |
| 3 | 添加 `spring.main.allow-bean-definition-overriding` | ❌ | Bean 冲突 |
| 4 | 添加 `autoconfigure.exclude` | ❌ | DashScopeSpeechSynthesisModel API key |
| 5 | 排除所有 Spring AI Alibaba 配置 | ❌ | 仍有 Bean 冲突 |
| 6 | 使用 `@ComponentScan` 过滤器 | ❌ | 编译错误 |

### 根本原因

**Spring AI Alibaba 1.0.0-M2.1 与 Spring Boot 3.3.5 仍有兼容性问题**

即使降级到 Spring Boot 3.3.5，Spring AI Alibaba 的自动配置机制仍然会与 Spring AI 官方库冲突。

---

## 📈 完成度评估

| 阶段 | 完成度 | 说明 |
|------|--------|------|
| **Spring Boot 降级** | 100% | ✅ 4.0.3 → 3.3.5 |
| **项目编译** | 100% | ✅ 无编译错误 |
| **配置优化** | 90% | ⚠️ Bean 冲突仍存在 |
| **测试运行** | 0% | ❌ 被自动配置阻塞 |

**总体完成度：70%**

---

## 🎯 最终建议

### 短期解决方案（1-2 小时）

**推荐：方案 A 或 B**

**方案 A（快速）- 排除 Spring AI 依赖：**
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**方案 B（推荐）- 创建独立模块：**
```bash
# 使用 Spring Initializr 创建新项目
# 选择：Spring Boot 3.3.5 + RabbitMQ + Test
# 复制现有测试代码到新模块
```

### 长期解决方案（1-2 周）

1. **关注 Spring AI Alibaba 更新**
   - 查看新版本发布
   - 测试与 Spring Boot 3.3.x 兼容性

2. **升级到稳定版本**
   - 等待 Spring Boot 3.4 或 4.0 稳定
   - Spring AI Alibaba 完全支持

3. **社区反馈**
   - 向 Spring AI Alibaba 提交 Issue
   - 参与社区讨论

---

## 📚 相关文档

- [Spring Boot 3.3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.3-Release-Notes)
- [Spring AI Alibaba GitHub](https://github.com/alibaba/spring-ai-alibaba)
- [Spring AI Compatibility](https://docs.spring.io/spring-ai/reference/)

---

## ✅ 完成清单

- [x] Spring Boot 版本降级到 3.3.5
- [x] 项目编译成功
- [x] 测试配置优化
- [x] Bean 冲突识别
- [x] 解决方案文档化
- [ ] RabbitMQ 测试成功运行（需采用推荐方案）

---

**状态：⚠️ 版本降级完成，Bean 冲突问题待解决（建议采用方案 A 或 B）**

**下一步行动：**
1. 选择推荐方案之一
2. 实施解决方案
3. 运行 RabbitMQ 测试
4. 继续渐进式学习
