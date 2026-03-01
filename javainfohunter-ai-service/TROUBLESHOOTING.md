# 故障排查指南 (TROUBLESHOOTING.md)

本文档列出 javainfohunter-ai-service 模块的常见问题及其解决方案。

## 目录
1. [依赖相关问题](#依赖相关问题)
2. [配置问题](#配置问题)
3. [Agent 执行问题](#agent-执行问题)
4. [工具调用问题](#工具调用问题)
5. [性能问题](#性能问题)

---

## 依赖相关问题

### 1.1 NoSuchBeanDefinitionException: No qualifying bean of type 'ChatModel'

**错误信息：**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.ai.chat.model.ChatModel'
```

**原因：** Spring AI DashScope 依赖未正确添加或配置。

**解决方案：**

1. 确认 pom.xml 中有 Spring AI Alibaba 依赖：
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0-M2.1</version>
</dependency>
```

2. 添加 Spring Milestone 仓库（见父 pom.xml 或 [README.md](README.md)）

3. 设置环境变量：
```bash
# Windows
set DASHSCOPE_API_KEY=your-api-key-here

# Linux/MacOS
export DASHSCOPE_API_KEY=your-api-key-here
```

4. 验证依赖是否正确安装：
```bash
mvnw.cmd dependency:tree -pl javainfohunter-ai-service
```

---

### 1.2 依赖解析失败：spring-ai-alibaba-starter

**错误信息：**
```
Could not resolve artifact: com.alibaba.cloud.ai:spring-ai-alibaba-starter:1.0.0-M2.1
```

**原因：** 未配置 Spring Milestone 仓库。

**解决方案：** 在 pom.xml 或 `~/.m2/settings.xml` 中添加：
```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

---

### 1.3 Lombok 注解不生效

**错误信息：**
```
Symbol not found: log, @Data, @Builder
```

**原因：** IDE 未启用 Lombok 插件或注解处理器未配置。

**解决方案：**

1. 确认 pom.xml 中有 Lombok 依赖：
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.36</version>
    <scope>provided</scope>
</dependency>
```

2. IDE 配置：
   - **IntelliJ IDEA**: File → Settings → Plugins → 搜索 "Lombok" → 安装并启用
   - **Eclipse**: Help → Eclipse Marketplace → 搜索 "Lombok" → 安装

3. 启用注解处理：
   - **IntelliJ IDEA**: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing

---

## 配置问题

### 2.1 API Key 未配置

**错误信息：**
```
DashScope Api Key must be provided
```

**原因：** 未设置 DashScope API Key。

**解决方案：** 设置环境变量或在 application.yml 中配置：

**方式一：环境变量（推荐）**
```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="sk-your-api-key-here"

# Windows CMD
set DASHSCOPE_API_KEY=sk-your-api-key-here

# Linux/MacOS
export DASHSCOPE_API_KEY=sk-your-api-key-here
```

**方式二：application.yml**
```yaml
spring:
  ai:
    dashscope:
      api-key: sk-your-api-key-here
```

**方式三：启动参数**
```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.ai.dashscope.api-key=sk-your-api-key-here"
```

---

### 2.2 版本不兼容

**症状：**
- NoSuchMethodError
- ClassNotFoundException
- Bean 创建失败

**原因：** Spring Boot 或 Java 版本不兼容。

**解决方案：** 确认使用正确的版本：

1. **Spring Boot 版本**：必须是 4.0.3
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
</parent>
```

2. **Java 版本**：必须是 Java 21+
```bash
java -version
# 应显示：java version "21.x.x"
```

3. **Maven 编译器配置**：
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

---

### 2.3 ChatClient Bean 未创建

**错误信息：**
```
Field chatClient in com.ron.javainfohunter.ai.service.ChatService required a bean of type 'ChatClient' that could not be found.
```

**原因：** `AiServiceAutoConfiguration` 未被扫描或配置。

**解决方案：**

1. 确认自动配置类在 classpath 中：
```bash
mvnw.cmd clean compile -pl javainfohunter-ai-service
```

2. 检查 `spring.factories` 或 `spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件：
```text
com.ron.javainfohunter.ai.autoconfigure.AiServiceAutoConfiguration
```

3. 手动导入配置类（如果自动配置失败）：
```java
@SpringBootApplication
@Import(AiServiceAutoConfiguration.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## Agent 执行问题

### 3.1 Agent 陷入无限循环

**症状：** Agent 执行超过预期时间，无响应。

**原因：** Agent 未在 max-steps 限制内完成任务。

**解决方案：** 配置最大步数限制：

**方式一：配置文件**
```yaml
javainfohunter:
  ai:
    enabled: true
    agent:
      max-steps: 10  # 根据任务复杂度调整
```

**方式二：代码设置**
```java
@Autowired
private ToolCallAgent agent;

public void configureAgent() {
    agent.setMaxSteps(10);
}
```

**方式三：运行时动态设置**
```java
CoordinationResult result = taskCoordinator.executeChain(
    "任务描述",
    List.of("agent1", "agent2"),
    Map.of("maxSteps", 5)  // 覆盖默认值
);
```

---

### 3.2 Agent 状态异常

**错误信息：**
```
IllegalStateException: Agent is already running
```

**原因：** Agent 被多个线程同时调用。

**解决方案：** 使用 `TaskCoordinator` 的 Parallel 模式替代手动并发：
```java
// ❌ 错误：手动并发
CompletableFuture.runAsync(() -> agent1.execute(input));
CompletableFuture.runAsync(() -> agent1.execute(input));  // 同一个 agent

// ✅ 正确：使用 TaskCoordinator
taskCoordinator.executeParallel("任务", List.of("agent1", "agent2"));
```

---

### 3.3 Master-Worker 模式不工作

**错误信息：**
```
CoordinationResult.failure("Master-Worker pattern not implemented yet")
```

**原因：** Master-Worker 模式尚未实现。

**解决方案：** 使用 Chain 或 Parallel 模式替代：

```java
// ❌ 当前不支持
taskCoordinator.executeMasterWorker("任务", "master", List.of("worker1", "worker2"));

// ✅ 使用 Parallel 模式
CoordinationResult result = taskCoordinator.executeParallel(
    "任务",
    List.of("worker1", "worker2", "worker3")
);

// ✅ 或使用 Chain 模式
CoordinationResult result = taskCoordinator.executeChain(
    "任务",
    List.of("agent1", "agent2", "agent3")
);
```

---

### 3.4 Agent 返回结果为空

**症状：** Agent 执行完成但结果为 null 或空字符串。

**可能原因：**

1. **Prompt 未正确设置**
```java
// ❌ 错误
agent.setSystemPrompt(null);

// ✅ 正确
agent.setSystemPrompt("你是一个专业的网页内容分析助手");
```

2. **输入消息为空**
```java
// ❌ 错误
agent.execute("");

// ✅ 正确
agent.execute("请分析这个网页的内容");
```

3. **AI 模型响应失败**

**解决方案：** 启用 DEBUG 日志查看详细错误：
```yaml
logging:
  level:
    com.ron.javainfohunter.ai: DEBUG
    org.springframework.ai: DEBUG
```

---

## 工具调用问题

### 4.1 工具回调未触发

**症状：** Agent 执行但工具方法未被调用。

**原因：** 工具未正确注册或传递给 Agent。

**解决方案：**

1. 确认工具类使用了 `@Tool` 注解：
```java
@Component
public class MyTool {
    @Tool("描述工具功能")
    public String myMethod(@ToolParam("参数描述") String param) {
        return "result";
    }
}
```

2. 确认工具已注册到 ToolRegistry：
```java
@Autowired
private ToolRegistry toolRegistry;

@PostConstruct
public void registerTools() {
    toolRegistry.registerTools(myTool);  // 自动扫描 @Tool 注解
}
```

3. 确认工具已设置到 Agent：
```java
@Autowired
private ToolCallAgent agent;

@Autowired
private ToolRegistry toolRegistry;

@PostConstruct
public void setupAgent() {
    agent.setAvailableTools(
        toolRegistry.getAllTools().toArray(new ToolCallback[0])
    );
}
```

4. 使用 AgentService 自动管理工具：
```java
@Autowired
private AgentService agentService;

// AgentService 会自动设置工具
CoordinationResult result = agentService.orchestrate("task-id", "parallel");
```

---

### 4.2 工具参数解析错误

**错误信息：**
```
Tool call parameter binding failed: Cannot convert String to Integer
```

**原因：** `@ToolParam` 注解使用不当或参数类型不匹配。

**解决方案：** 确保使用 `@ToolParam` 描述参数：
```java
@Tool("搜索网页")
public String searchWeb(
    @ToolParam("搜索关键词") String keyword,
    @ToolParam("结果数量，默认10") int count  // 必须是基本类型包装类或基本类型
) {
    // 实现
}
```

**注意事项：**
- 使用 `@ToolParam` 描述每个参数的用途
- 参数类型应该是基本类型（int, long, boolean）或 String
- 复杂对象需要序列化为 String

---

### 4.3 工具执行超时

**症状：** 工具调用后长时间无响应。

**原因：** 工具内部执行耗时操作（如网络请求、文件IO）未设置超时。

**解决方案：**

1. 为工具方法添加超时控制：
```java
@Tool("搜索网页")
public String searchWeb(@ToolParam("关键词") String keyword) {
    return CompletableFuture.supplyAsync(() -> {
        // 执行搜索
        return searchService.search(keyword);
    }, executor)
    .orTimeout(30, TimeUnit.SECONDS)  // 30秒超时
    .join();
}
```

2. 使用虚拟线程（Java 21+）：
```java
@Tool("搜索网页")
public String searchWeb(@ToolParam("关键词") String keyword) {
    return Thread.ofVirtual().start(() -> {
        return searchService.search(keyword);
    }).join(30, TimeUnit.SECONDS);
}
```

---

### 4.4 工具返回结果格式错误

**症状：** Agent 无法正确解析工具返回的结果。

**原因：** 工具返回的结果不是 String 或格式不正确。

**解决方案：** 确保工具返回 String 类型：
```java
// ❌ 错误：返回复杂对象
@Tool("获取用户信息")
public User getUserInfo(@ToolParam("用户ID") String userId) {
    return userRepository.findById(userId);  // User 对象
}

// ✅ 正确：返回 JSON 字符串
@Tool("获取用户信息")
public String getUserInfo(@ToolParam("用户ID") String userId) {
    User user = userRepository.findById(userId);
    return "{\"name\":\"" + user.getName() + "\",\"age\":" + user.getAge() + "}";
}
```

---

## 性能问题

### 5.1 并发执行性能未提升

**症状：** Parallel 模式与 Chain 模式速度相当。

**原因：** 未使用 Java 21 虚拟线程。

**解决方案：**

1. 确认使用 Java 21+：
```bash
java -version  # 应显示 21
```

2. 在 Maven 中配置：
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

3. 确认 TaskCoordinator 使用了虚拟线程：
```java
// TaskCoordinatorImpl.java
private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

4. 性能测试对比：
```java
// Chain 模式：顺序执行
long start1 = System.currentTimeMillis();
taskCoordinator.executeChain("任务", agents);
long chainTime = System.currentTimeMillis() - start1;

// Parallel 模式：并行执行（应该快 3 倍左右）
long start2 = System.currentTimeMillis();
taskCoordinator.executeParallel("任务", agents);
long parallelTime = System.currentTimeMillis() - start2;

System.out.println("性能提升: " + (chainTime / (double) parallelTime) + "x");
```

---

### 5.2 内存占用过高

**症状：** 长时间运行后内存持续增长。

**可能原因：**
1. Agent 消息历史未清理
2. 未关闭的 ChatClient 连接
3. 工具回调资源泄漏

**解决方案：**

1. **限制消息历史大小**：
```java
@Autowired
private ToolCallAgent agent;

@PostConstruct
public void configureAgent() {
    agent.setMaxMessages(100);  // 保留最近 100 条消息
}
```

2. **定期清理 Agent 状态**：
```java
@Scheduled(fixedRate = 3600000)  // 每小时
public void cleanupAgents() {
    agentManager.getAllAgents().forEach((id, agent) -> {
        if (agent.getMessageHistory().size() > 100) {
            agent.reset();  // 重置 Agent 状态
        }
    });
}
```

3. **使用 @PreDestroy 清理资源**：
```java
@Component
public class AgentCleanup {

    @Autowired
    private AgentManager agentManager;

    @PreDestroy
    public void cleanup() {
        agentManager.getAllAgents().forEach((id, agent) -> {
            agent.reset();
        });
    }
}
```

4. **监控内存使用**：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  metrics:
    enable:
      jvm: true
```

---

### 5.3 AI 调用响应慢

**症状：** Agent 执行时间过长（> 10秒）。

**可能原因：**
1. 网络延迟
2. Prompt 过长
3. AI 模型性能限制

**解决方案：**

1. **优化 Prompt**：
```java
// ❌ 错误：Prompt 过长
agent.setSystemPrompt("""
    你是一个专业的助手...
    （此处省略 5000 字的详细说明）
""");

// ✅ 正确：简洁明确的 Prompt
agent.setSystemPrompt("你是网页内容分析专家，提取关键信息并以 JSON 格式返回。");
```

2. **启用流式输出**（如果支持）：
```java
ChatClient.CallResponseSpec response = chatClient.prompt()
    .user(input)
    .call()
    .stream();  // 流式响应
```

3. **使用缓存**（对于重复查询）：
```java
@Cacheable("agent-results")
public String executeAgent(String agentId, String input) {
    return agentManager.getAgent(agentId).execute(input);
}
```

---

## 其他问题

### 获取调试信息

启用 DEBUG 级别日志：
```yaml
logging:
  level:
    com.ron.javainfohunter.ai: DEBUG
    org.springframework.ai: DEBUG
    com.alibaba.cloud.ai: DEBUG
```

查看 Agent 执行日志：
```bash
# Windows
mvnw.cmd spring-boot:run | grep "Agent\|Tool"

# Linux/MacOS
./mvnw spring-boot:run | grep "Agent\|Tool"
```

---

### 单元测试失败

**常见错误：**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'ChatClient'
```

**解决方案：** 使用 `@SpringBootTest` 加载完整上下文：
```java
@SpringBootTest
class AgentServiceTest {

    @Autowired
    private AgentService agentService;

    @Test
    void testAgent() {
        // 测试代码
    }
}
```

如果不需要集成测试，可以 mock ChatClient：
```java
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private ChatClient chatClient;

    @Test
    void testAgent() {
        // 测试代码
    }
}
```

---

### 获取帮助

如果以上方案无法解决问题：

1. **查看日志**（见上文 "获取调试信息"）

2. **查看源码示例**：
   - `AgentAutoConfig.java` - Agent 注册示例
   - `CrawlerAgent.java` - Agent 实现示例
   - `HtmlParserTool.java` - 工具实现示例
   - `TaskCoordinatorImpl.java` - 协调器实现示例

3. **查看文档**：
   - [README.md](README.md) - 快速开始
   - [USAGE.md](USAGE.md) - 详细使用示例
   - [../docs/迁移完成总结.md](../docs/迁移完成总结.md) - 实现细节

4. **检查已知问题**：
   - Spring AI Alibaba 版本限制：仅支持 1.0.0-M2.1
   - Master-Worker 模式：尚未实现
   - 虚拟线程：需要 Java 21+

---

**最后更新：2026-03-01**

**文档版本：** 1.0.0
