# JavaInfoHunter AI Service 使用示例

本文档展示如何使用 javainfohunter-ai-service 进行 Agent 编排。

## 前置条件

1. 添加依赖到主项目 `pom.xml`:
```xml
<dependency>
    <groupId>com.ron</groupId>
    <artifactId>javainfohunter-ai-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

2. 配置 `application.yml`:
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}

javainfohunter:
  ai:
    enabled: true
    agent:
      max-steps: 10
```

3. 设置环境变量:
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

## 预置 Agent 使用示例

项目已内置 4 个预置 Agent，自动注册到 AgentManager：

### 1. CrawlerAgent - 网页爬取
```java
@Autowired
private AgentManager agentManager;

public String crawlWebpage(String html) {
    BaseAgent crawler = agentManager.getAgent("crawler-agent").orElseThrow();
    String result = crawler.run(html);
    return result;
}
```

### 2. AnalysisAgent - 内容分析
```java
public String analyzeContent(String content) {
    BaseAgent analyst = agentManager.getAgent("analysis-agent").orElseThrow();
    String result = analyst.run(content);
    return result;
}
```

### 3. SummaryAgent - 摘要生成
```java
public String summarizeArticle(String longText) {
    BaseAgent summarizer = agentManager.getAgent("summary-agent").orElseThrow();
    String result = summarizer.run(longText);
    return result;
}
```

### 4. ClassificationAgent - 内容分类
```java
public String classifyContent(String content) {
    BaseAgent classifier = agentManager.getAgent("classification-agent").orElseThrow();
    String result = classifier.run(content);
    return result;
}
```

## Agent 编排模式示例

### Chain 模式（链式执行）

输出作为下一个的输入：
```java
@Autowired
private TaskCoordinator taskCoordinator;

public void processNewsChain(String html) {
    CoordinationResult result = taskCoordinator.executeChain(
        "处理这个网页内容",
        List.of("crawler-agent", "analysis-agent", "summary-agent")
    );

    if (result.isSuccess()) {
        log.info("处理成功: {}", result.getFinalOutput());
    }
}
```

**流程**: HTML → 爬取 → 分析 → 摘要

### Parallel 模式（并行执行）

多个 Agent 同时处理：
```java
public void parallelAnalysis(String content) {
    CoordinationResult result = taskCoordinator.executeParallel(
        "多维度分析这个内容",
        List.of("analysis-agent", "summary-agent", "classification-agent")
    );

    if (result.isSuccess()) {
        result.getAgentOutputs().forEach((agentId, output) -> {
            log.info("{} 的输出: {}", agentId, output);
        });
    }
}
```

**优势**: 利用虚拟线程并发，速度快 3 倍

### Master-Worker 模式（主从协作）

```java
public void masterWorkerExample(String task) {
    CoordinationResult result = taskCoordinator.executeMasterWorker(
        "生成综合报告",
        "coordinator-agent",  // Master
        List.of("crawler-agent", "analysis-agent", "summary-agent")  // Workers
    );
}
```

## 完整工作流示例

### 场景：新闻内容处理管道

```java
@Service
public class NewsProcessingService {

    @Autowired
    private TaskCoordinator taskCoordinator;

    @Autowired
    private AgentService agentService;

    /**
     * 完整的新闻处理流程
     */
    public NewsReport processNews(String url, String html) {
        // Step 1: 并行爬取和初步分析
        CoordinationResult crawlResult = taskCoordinator.executeParallel(
            "爬取和分析: " + url,
            List.of("crawler-agent", "classification-agent")
        );

        if (!crawlResult.isSuccess()) {
            throw new RuntimeException("爬取失败: " + crawlResult.getErrorMessage());
        }

        String cleanContent = crawlResult.getAgentOutputs().get("crawler-agent");
        String classification = crawlResult.getAgentOutputs().get("classification-agent");

        // Step 2: 链式分析和摘要
        CoordinationResult analysisResult = taskCoordinator.executeChain(
            "深度分析并生成摘要",
            List.of("analysis-agent", "summary-agent")
        );

        // 组装结果
        return NewsReport.builder()
            .url(url)
            .content(cleanContent)
            .classification(classification)
            .summary(analysisResult.getFinalOutput())
            .build();
    }
}
```

## 自定义 Agent 示例

### 创建自己的 Agent

```java
@Component
public class MyCustomAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的内容审核专家。
        你的任务是检查内容是否包含违规信息。
        """;

    @Autowired
    private ChatClient chatClient;

    public MyCustomAgent() {
        super(new ToolCallback[0]);
        setName("MyCustomAgent");
        setSystemPrompt(SYSTEM_PROMPT);
    }

    @Override
    public void cleanup() {
        // 清理资源
    }
}
```

### 注册自定义 Agent

```java
@Configuration
public class MyAgentConfig {

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private MyCustomAgent myCustomAgent;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolRegistry toolRegistry;

    @PostConstruct
    public void registerMyAgent() {
        myCustomAgent.setChatClient(chatClient);
        myCustomAgent.setAvailableTools(toolRegistry.getAllTools());
        agentManager.registerAgent("my-custom-agent", myCustomAgent);
    }
}
```

## 自定义工具示例

### 使用 @Tool 注解创建工具

```java
@Component
public class MyTools {

    @Tool(description = "发送 HTTP 请求并获取响应")
    public String httpRequest(
        @ToolParam("URL 地址") String url,
        @ToolParam("请求方法 (GET/POST)") String method
    ) {
        // 实现逻辑
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    @Tool(description = "查询数据库中的用户信息")
    public String queryUser(@ToolParam("用户 ID") String userId) {
        // 实现数据库查询
        return "User{id='123', name='张三'}";
    }
}
```

工具会自动注册到 ToolRegistry，供所有 Agent 使用。

## REST API 示例

创建 Controller 暴露 Agent 能力：

```java
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeContent(
        @RequestBody String content
    ) {
        // 并行分析
        CoordinationResult result = agentService.executeParallel(
            "分析这个内容",
            List.of("analysis-agent", "summary-agent", "classification-agent")
        );

        Map<String, Object> response = Map.of(
            "success", result.isSuccess(),
            "outputs", result.getAgentOutputs(),
            "final", result.getFinalOutput()
        );

        return ResponseEntity.ok(response);
    }
}
```

## 性能优化建议

### 1. 使用虚拟线程（默认启用）

Spring Boot 4.0+ 自动启用虚拟线程，适合高并发 Agent 调用。

### 2. 控制 Agent 步数

```yaml
javainfohunter:
  ai:
    agent:
      max-steps: 10  # 限制最大步数
```

### 3. 并行处理优先

优先使用 `executeParallel()` 而非 `executeChain()`，充分利用并发。

### 4. 缓存 Agent 结果

对于相同输入，可以缓存 Agent 的执行结果。

## 错误处理

```java
try {
    CoordinationResult result = taskCoordinator.executeChain(...);

    if (!result.isSuccess()) {
        log.error("Agent 执行失败: {}", result.getErrorMessage());

        // 获取成功的部分结果
        result.getAgentOutputs().forEach((agentId, output) -> {
            log.info("{} 成功: {}", agentId, output);
        });
    }
} catch (Exception e) {
    log.error("Agent 编排异常", e);
}
```

## 监控和日志

启用 DEBUG 日志查看 Agent 执行详情：

```yaml
logging:
  level:
    com.ron.javainfohunter.ai: DEBUG
```

日志输出示例：
```
INFO  - CrawlerAgent starting execution - Prompt: 解析这个HTML
DEBUG - CrawlerAgent executing step 1/10
INFO  - CrawlerAgent 的思考：我需要使用 html_parser_tool 解析HTML
INFO  - CrawlerAgent 选择了 1 个工具
INFO  - CrawlerAgent 的 act 结果：工具 html_parser_tool 完成了任务
INFO  - CrawlerAgent execution completed - Duration: 1234ms, Steps: 2
```

## 总结

✅ **预置 Agent**: 开箱即用，无需配置
✅ **三种协作模式**: Chain、Parallel、Master-Worker
✅ **声明式工具**: @Tool 注解自动注册
✅ **Spring Boot 集成**: 自动配置，开箱即用
✅ **虚拟线程**: 高性能并发执行

开始使用 Agent 编排模式，让 AI 为你工作！🚀
