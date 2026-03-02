# RabbitMQ 渐进式学习指南 - JavaInfoHunter 项目实践

## 文档说明

本文档通过 JavaInfoHunter 项目的实际场景，引导您从零开始掌握 RabbitMQ，最终实现生产级消息队列架构。

**学习路径：**
```
阶段1: 基础概念 → 阶段2: Work Queue → 阶段3: 发布订阅 → 阶段4: 路由模式 → 阶段5: 生产级架构
```

---

## 为什么选择渐进式学习？

### 直接学习生产级架构的问题

如果直接学习复杂的生产级架构（包含死信队列、重试、确认机制等），您会面临：

❌ **概念过载**：一次性理解太多概念（Exchange、Queue、Binding、ACK、Dead Letter...）
❌ **调试困难**：出问题时不知道是哪个环节出错
❌ **学习曲线陡峭**：容易放弃或理解不透彻

### 渐进式学习的优势

✅ **循序渐进**：每次只学习 1-2 个新概念
✅ **即时验证**：每个阶段都能运行并看到效果
✅ **理解深刻**：知道每个设计决策的原因
✅ **可调试**：问题范围小，容易定位

---

## 项目需求分析

### JavaInfoHunter 的消息队列场景

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│  爬取模块     │   →     │  消息队列     │   →     │  处理模块     │
│  Crawler     │  Message│   RabbitMQ   │  Message │  Processor   │
│              │  Queue  │              │  Queue  │  (Agent)      │
└──────────────┘         └──────────────┘         └──────────────┘
     ↑                                                   ↓
     │                                              ┌──────────────┐
   URL/源                                          │  PostgreSQL  │
   RSS Feed                                        │  + pgvector  │
                                                   └──────────────┘
```

**核心需求：**
1. **解耦**：爬虫和处理的速率不同，需要缓冲
2. **可靠性**：数据不能丢失（手动 ACK）
3. **扩展性**：可以增加多个消费者（虚拟线程）
4. **持久化**：服务重启后消息不丢失

---

## 阶段 1：基础概念理解 (30 分钟)

### 1.1 RabbitMQ 核心概念

```
Producer（生产者）
    ↓ 发送消息
Exchange（交换机）
    ↓ 根据路由规则
Queue（队列）
    ↓ 存储消息
Consumer（消费者）
    ↓ 处理消息
业务系统
```

### 1.2 最简示例：Hello World

**场景**：发送一条测试消息

**代码实现：**

```java
// 1. 配置类
@Configuration
public class RabbitMQConfig {

    // 定义队列
    @Bean
    public Queue helloQueue() {
        return QueueBuilder.durable("hello.queue")  // durable = 持久化
                .build();
    }
}

// 2. 生产者（发送消息）
@Service
public class ProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendHello(String message) {
        rabbitTemplate.convertAndSend("hello.queue", message);
        System.out.println("发送消息: " + message);
    }
}

// 3. 消费者（接收消息）
@Component
public class ConsumerService {

    @RabbitListener(queues = "hello.queue")
    public void receiveHello(String message) {
        System.out.println("接收消息: " + message);
    }
}
```

**测试代码：**

```java
@SpringBootTest
public class HelloWorldTest {

    @Autowired
    private ProducerService producerService;

    @Test
    public void testHelloWorld() throws InterruptedException {
        producerService.sendHello("Hello RabbitMQ!");
        Thread.sleep(1000);  // 等待消费者处理
    }
}
```

**运行结果：**
```
发送消息: Hello RabbitMQ!
接收消息: Hello RabbitMQ!
```

**关键点：**
- `QueueBuilder.durable()`：队列持久化，RabbitMQ 重启后不丢失
- `@RabbitListener`：自动监听队列，Spring 自动创建容器
- `convertAndSend()`：自动序列化对象为 JSON

---

## 阶段 2：Work Queue 模式 (1 小时)

### 2.1 应用场景：爬虫任务分发

**问题**：爬取速度慢，处理速度快，如何平衡？

**解决方案**：使用 Work Queue 模式，多个消费者竞争消费消息

```
┌──────────────┐
│  爬虫模块     │
│  Producer    │
└──────┬───────┘
       │ 发送 URL
       ↓
┌──────────────┐
│  url.queue   │  (队列)
└──────┬───────┘
       │
   ┌───┴────┐
   ↓        ↓
┌──────┐ ┌──────┐
│Worker1│ │Worker2│  (多个消费者)
│(快)   │ │(快)   │
└──────┘ └──────┘
   │        │
   └───┬────┘
       ↓
  并发处理
```

### 2.2 代码实现

**配置类：**

```java
@Configuration
public class WorkQueueConfig {

    @Bean
    public Queue urlQueue() {
        return QueueBuilder.durable("url.queue")
                .build();
    }
}
```

**生产者：模拟爬虫调度器**

```java
@Service
public class CrawlerProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 模拟：从数据库读取待爬取的 URL
    public void sendUrlsToCrawl() {
        List<String> urls = List.of(
                "https://example.com/news/1",
                "https://example.com/news/2",
                "https://example.com/news/3",
                "https://example.com/news/4",
                "https://example.com/news/5"
        );

        for (String url : urls) {
            rabbitTemplate.convertAndSend("url.queue", url);
            System.out.println("发送 URL 到队列: " + url);
        }
    }
}
```

**消费者：爬虫工作线程**

```java
@Component
public class CrawlerWorker {

    @RabbitListener(queues = "url.queue")
    public void crawlUrl(String url) {
        System.out.println(Thread.currentThread().getName() + " 开始爬取: " + url);

        // 模拟爬取过程
        try {
            Thread.sleep(1000);  // 爬取耗时
            String content = "爬取的内容: " + url;
            System.out.println(Thread.currentThread().getName() + " 爬取完成: " + url);

            // TODO: 发送到下一个队列（原始内容队列）
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**配置多个消费者：**

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# 设置消费者并发数（相当于创建多个 Worker）
rabbitmq:
  listener:
    simple:
      concurrency: 3  # 最小并发数
      max-concurrency: 10  # 最大并发数
```

**测试：**

```java
@SpringBootTest
public class WorkQueueTest {

    @Autowired
    private CrawlerProducer crawlerProducer;

    @Test
    public void testWorkQueue() throws InterruptedException {
        crawlerProducer.sendUrlsToCrawl();
        Thread.sleep(10000);  // 等待所有消费者处理完成
    }
}
```

**预期输出：**
```
发送 URL 到队列: https://example.com/news/1
发送 URL 到队列: https://example.com/news/2
...
org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#0-1 开始爬取: https://example.com/news/1
org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#0-2 开始爬取: https://example.com/news/2
org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#0-3 开始爬取: https://example.com/news/3
```

**关键点：**
- ✅ 多个消费者自动竞争消费，实现负载均衡
- RabbitMQ 默认使用 **轮询分发**（Round-Robin）
- ✅ 可以通过调整 `concurrency` 控制消费者数量

---

## 阶段 3：发布订阅模式 (1.5 小时)

### 3.1 应用场景：一对多广播

**问题**：爬取的原始内容需要同时发送给：
- 分析 Agent
- 摘要 Agent
- 分类 Agent
- 存储服务

**解决方案**：使用 Fanout Exchange 进行广播

```
              ┌──────────────┐
              │ Fanout       │
              │ Exchange     │
              └──────┬───────┘
                     │
        ┌────────────┼────────────┐
        ↓            ↓            ↓
   ┌─────────┐  ┌─────────┐  ┌─────────┐
   │analysis │  │summary  │  │classify │
   │.queue   │  │.queue   │  │.queue   │
   └────┬────┘  └────┬────┘  └────┬────┘
        ↓            ↓            ↓
   AnalysisAgent SummaryAgent ClassifyAgent
```

### 3.2 代码实现

**配置类：**

```java
@Configuration
public class FanoutConfig {

    // 1. 定义 Fanout Exchange
    @Bean
    public FanoutExchange rawContentExchange() {
        return new FanoutExchange("raw.content.exchange");
    }

    // 2. 定义多个队列
    @Bean
    public Queue analysisQueue() {
        return QueueBuilder.durable("analysis.queue").build();
    }

    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable("summary.queue").build();
    }

    @Bean
    public Queue classifyQueue() {
        return QueueBuilder.durable("classify.queue").build();
    }

    // 3. 绑定队列到 Exchange（Fanout 模式不需要 routing key）
    @Bean
    public Binding analysisBinding() {
        return BindingBuilder.bind(analysisQueue())
                .to(rawContentExchange());
    }

    @Bean
    public Binding summaryBinding() {
        return BindingBuilder.bind(summaryQueue())
                .to(rawContentExchange());
    }

    @Bean
    public Binding classifyBinding() {
        return BindingBuilder.bind(classifyQueue())
                .to(rawContentExchange());
    }
}
```

**生产者：发送原始内容到 Exchange**

```java
@Service
public class RawContentProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void broadcastRawContent(RawContent content) {
        // 发送到 Exchange，而不是直接发送到队列
        rabbitTemplate.convertAndSend("raw.content.exchange", "", content);
        System.out.println("广播原始内容: " + content.getTitle());
    }
}

// 数据传输对象（DTO）
@Data
@AllArgsConstructor
public class RawContent {
    private String url;
    private String title;
    private String content;
    private LocalDateTime publishTime;
}
```

**消费者：多个 Agent 并行处理**

```java
@Component
public class AnalysisAgent {

    @RabbitListener(queues = "analysis.queue")
    public void analyze(RawContent content) {
        System.out.println("分析 Agent 收到: " + content.getTitle());
        // TODO: 调用 AI 进行情感分析、关键词提取
    }
}

@Component
public class SummaryAgent {

    @RabbitListener(queues = "summary.queue")
    public void summarize(RawContent content) {
        System.out.println("摘要 Agent 收到: " + content.getTitle());
        // TODO: 调用 AI 生成摘要
    }
}

@Component
public class ClassifyAgent {

    @RabbitListener(queues = "classify.queue")
    public void classify(RawContent content) {
        System.out.println("分类 Agent 收到: " + content.getTitle());
        // TODO: 调用 AI 进行分类
    }
}
```

**测试：**

```java
@SpringBootTest
public class FanoutTest {

    @Autowired
    private RawContentProducer producer;

    @Test
    public void testFanout() throws InterruptedException {
        RawContent content = new RawContent(
                "https://example.com/news/1",
                "AI 技术突破：GPT-5 发布",
                "今日，OpenAI 发布了 GPT-5...",
                LocalDateTime.now()
        );

        producer.broadcastRawContent(content);
        Thread.sleep(2000);
    }
}
```

**预期输出：**
```
广播原始内容: AI 技术突破：GPT-5 发布
分析 Agent 收到: AI 技术突破：GPT-5 发布
摘要 Agent 收到: AI 技术突破：GPT-5 发布
分类 Agent 收到: AI 技术突破：GPT-5 发布
```

**关键点：**
- ✅ 一条消息被多个消费者同时处理
- ✅ 消费者之间互不影响
- ✅ 适合 **并行处理** 场景（Parallel Pattern）

---

## 阶段 4：路由模式 (2 小时)

### 4.1 应用场景：按类别分发

**问题**：不同类别的新闻需要不同的处理流程
- 科技新闻 → 需要深度分析
- 财经新闻 → 需要情感分析
- 体育新闻 → 只需简单分类

**解决方案**：使用 Topic Exchange 根据路由键分发

```
                              ┌──────────────┐
                              │ Topic        │
                              │ Exchange     │
                              └──────┬───────┘
                                     │
            ┌────────────────────────┼────────────────────────┐
            │ routing key: tech.*    │ routing key: finance.* │
            ↓                        ↓
       ┌─────────┐            ┌─────────┐
       │  tech   │            │ finance │
       │ .queue  │            │ .queue  │
       └────┬────┘            └────┬────┘
            ↓                      ↓
       DeepAnalysis          SentimentAnalysis
```

### 4.2 代码实现

**配置类：**

```java
@Configuration
public class TopicConfig {

    // 1. 定义 Topic Exchange
    @Bean
    public TopicExchange newsExchange() {
        return new TopicExchange("news.exchange");
    }

    // 2. 定义队列
    @Bean
    public Queue techQueue() {
        return QueueBuilder.durable("tech.queue").build();
    }

    @Bean
    public Queue financeQueue() {
        return QueueBuilder.durable("finance.queue").build();
    }

    @Bean
    public Queue allNewsQueue() {
        return QueueBuilder.durable("all.news.queue").build();
    }

    // 3. 绑定队列到 Exchange，指定路由键模式
    @Bean
    public Binding techBinding() {
        // 匹配: tech.*
        return BindingBuilder.bind(techQueue())
                .to(newsExchange())
                .with("tech.*");
    }

    @Bean
    public Binding financeBinding() {
        // 匹配: finance.*
        return BindingBuilder.bind(financeQueue())
                .to(newsExchange())
                .with("finance.*");
    }

    @Bean
    public Binding allNewsBinding() {
        // 匹配: *.* (所有新闻)
        return BindingBuilder.bind(allNewsQueue())
                .to(newsExchange())
                .with("*.*");
    }
}
```

**生产者：发送带路由键的消息**

```java
@Service
public class NewsProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendNews(News news) {
        // 构造路由键：category.subcategory
        String routingKey = news.getCategory() + "." + news.getSubcategory();

        rabbitTemplate.convertAndSend("news.exchange", routingKey, news);
        System.out.println("发送新闻到 " + routingKey + ": " + news.getTitle());
    }
}

@Data
@AllArgsConstructor
public class News {
    private String category;      // tech, finance, sports
    private String subcategory;   // ai, blockchain, stock
    private String title;
    private String content;
}
```

**消费者：不同的处理逻辑**

```java
@Component
public class TechNewsProcessor {

    @RabbitListener(queues = "tech.queue")
    public void processTechNews(News news) {
        System.out.println("【科技新闻】深度分析: " + news.getTitle());
        // TODO: 深度分析、技术解读
    }
}

@Component
public class FinanceNewsProcessor {

    @RabbitListener(queues = "finance.queue")
    public void processFinanceNews(News news) {
        System.out.println("【财经新闻】情感分析: " + news.getTitle());
        // TODO: 情感分析、市场影响
    }
}

@Component
public class AllNewsArchiver {

    @RabbitListener(queues = "all.news.queue")
    public void archiveNews(News news) {
        System.out.println("【归档】保存到数据库: " + news.getTitle());
        // TODO: 保存到 PostgreSQL
    }
}
```

**测试：**

```java
@SpringBootTest
public class TopicTest {

    @Autowired
    private NewsProducer producer;

    @Test
    public void testTopicRouting() throws InterruptedException {
        List<News> newsList = List.of(
                new News("tech", "ai", "GPT-5 发布", "..."),
                new News("tech", "blockchain", "比特币突破10万", "..."),
                new News("finance", "stock", "股市大涨", "..."),
                new News("sports", "nba", "湖人夺冠", "...")
        );

        for (News news : newsList) {
            producer.sendNews(news);
        }

        Thread.sleep(2000);
    }
}
```

**预期输出：**
```
发送新闻到 tech.ai: GPT-5 发布
发送新闻到 tech.blockchain: 比特币突破10万
发送新闻到 finance.stock: 股市大涨
发送新闻到 sports.nba: 湖人夺冠

【科技新闻】深度分析: GPT-5 发布
【归档】保存到数据库: GPT-5 发布

【科技新闻】深度分析: 比特币突破10万
【归档】保存到数据库: 比特币突破10万

【财经新闻】情感分析: 股市大涨
【归档】保存到数据库: 股市大涨

【归档】保存到数据库: 湖人夺冠  (只匹配 *.*)
```

**关键点：**
- ✅ 使用路由键模式匹配（`*` 匹配一个词，`#` 匹配零个或多个词）
- ✅ 一个消息可以同时匹配多个队列
- ✅ 适合 **按类别分发** 场景

---

## 阶段 5：生产级架构 (3 小时)

### 5.1 可靠性保障机制

**问题**：如何保证消息不丢失？

**解决方案：三重保障**

```
1. 消息持久化：MessageDeliveryMode.PERSISTENT
2. 队列持久化：durable = true
3. 手动 ACK：处理成功后才确认
```

### 5.2 完整配置

**生产者配置：**

```java
@Configuration
public class ProducerConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 1. 开启强制回调（消息无法路由时触发）
        template.setMandatory(true);

        // 2. 开启确认回调（消息到达 Exchange）
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("✅ 消息成功到达 Exchange");
            } else {
                System.err.println("❌ 消息未到达 Exchange: " + cause);
            }
        });

        // 3. 开启返回回调（消息无法路由到队列）
        template.setReturnsCallback(returned -> {
            System.err.println("❌ 消息无法路由: " + returned.getMessage());
        });

        return template;
    }
}
```

**消费者配置：**

```java
@Configuration
public class ConsumerConfig {

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // 1. 开启手动 ACK
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 2. 设置重试机制
        factory.setRetryTemplate(new RetryTemplate() {{
            // 重试 3 次，间隔 2 秒
            setRetryPolicy(new SimpleRetryPolicy(3));
            setBackOffPolicy(new FixedBackOffPolicy(2000L));
        }});

        return factory;
    }
}
```

**生产级消费者代码：**

```java
@Component
public class RobustConsumer {

    @RabbitListener(queues = "processed.queue")
    public void processMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        try {
            // 1. 解析消息
            String content = new String(message.getBody());

            // 2. 业务处理
            System.out.println("开始处理: " + content);
            processBusinessLogic(content);

            // 3. 手动 ACK（确认成功）
            channel.basicAck(deliveryTag, false);  // false = 不批量确认
            System.out.println("✅ 处理成功，已确认");

        } catch (BusinessException e) {
            // 业务异常：拒绝消息，不重新入队
            try {
                channel.basicNack(deliveryTag, false, false);
                System.err.println("❌ 业务异常，丢弃消息: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            // 系统异常：拒绝消息，重新入队
            try {
                channel.basicNack(deliveryTag, false, true);  // true = 重新入队
                System.err.println("⚠️ 系统异常，消息重新入队: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void processBusinessLogic(String content) {
        // 模拟处理
        if (content.contains("error")) {
            throw new BusinessException("模拟业务异常");
        }
        if (content.contains("fail")) {
            throw new RuntimeException("模拟系统异常");
        }
    }
}

// 自定义业务异常
class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

### 5.3 死信队列（DLQ）

**问题**：重试多次仍失败的消息如何处理？

**解决方案**：死信队列 + 告警机制

```java
@Configuration
public class DeadLetterConfig {

    // 1. 定义死信交换机和队列
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dead.letter.exchange");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter");
    }

    // 2. 定义业务队列（指定死信交换机）
    @Bean
    public Queue businessQueue() {
        return QueueBuilder.durable("business.queue")
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }
}

// 死信队列消费者（记录失败消息）
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = "dead.letter.queue")
    public void handleDeadLetter(Message message) {
        String content = new String(message.getBody());
        System.err.println("💀 消息进入死信队列: " + content);

        // TODO: 发送告警邮件、记录到数据库
    }
}
```

### 5.4 消息持久化配置

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    # 开启发布确认
    publisher-confirm-type: correlated
    # 开启返回回调
    publisher-returns: true

# 监听器配置
rabbitmq:
  listener:
    simple:
      acknowledge-mode: manual  # 手动 ACK
      retry:
        enabled: true
        max-attempts: 3
        initial-interval: 2000  # 重试间隔
```

---

## 5.5 生产级测试

```java
@SpringBootTest
public class ProductionTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testReliability() throws InterruptedException {
        List<String> messages = List.of(
                "正常消息",
                "包含error的业务异常",
                "包含fail的系统异常"
        );

        for (String msg : messages) {
            // 发送持久化消息
            rabbitTemplate.convertAndSend(
                    "business.exchange",
                    "business.key",
                    msg,
                    message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }
            );
        }

        Thread.sleep(10000);  // 等待处理完成
    }
}
```

**预期输出：**
```
✅ 消息成功到达 Exchange
开始处理: 正常消息
✅ 处理成功，已确认

✅ 消息成功到达 Exchange
开始处理: 包含error的业务异常
❌ 业务异常，丢弃消息: 模拟业务异常
💀 消息进入死信队列: 包含error的业务异常

✅ 消息成功到达 Exchange
开始处理: 包含fail的系统异常
⚠️ 系统异常，消息重新入队: 模拟系统异常
开始处理: 包含fail的系统异常
⚠️ 系统异常，消息重新入队: 模拟系统异常
... (重试 3 次)
💀 消息进入死信队列: 包含fail的系统异常
```

---

## 总结：从简单到复杂的演进路径

### 阶段对比表

| 阶段 | 模式 | Exchange | 路由键 | 适用场景 | 复杂度 |
|------|------|----------|--------|----------|--------|
| 1 | 基础 | Default | 队列名 | 测试、简单任务 | ⭐ |
| 2 | Work Queue | Default | 队列名 | 负载均衡 | ⭐⭐ |
| 3 | Fanout | Fanout | 无 | 广播、并行处理 | ⭐⭐⭐ |
| 4 | Topic | Topic | 模式匹配 | 按类别分发 | ⭐⭐⭐⭐ |
| 5 | 生产级 | 任意 | 任意 | 生产环境 | ⭐⭐⭐⭐⭐ |

### 学习建议

1. **逐个阶段实践**：不要跳过任何阶段
2. **运行代码**：每个示例都要实际运行
3. **观察 RabbitMQ 管理界面**：http://localhost: 15672
4. **故意制造错误**：测试异常处理机制
5. **阅读日志**：理解每个步骤的执行流程

### 下一步

完成以上学习后，您可以：
1. 设计 JavaInfoHunter 的完整消息架构
2. 结合 Agent 编排模式实现端到端流程
3. 添加监控和告警机制
4. 性能优化和压测

---

**文档版本:** v1.0
**最后更新:** 2026-03-01
**作者:** JavaInfoHunter Team
