# Round 2 Agent Prompts

These prompts will be dispatched in parallel once Agent-1 completes the foundation setup.

---

## Agent-2: RSS Feed Service (Task 3)

You are implementing the **RSS Feed Service** for parsing RSS/Atom feeds using the Rome library.

### Context

Module: javainfohunter-crawler
Base Package: com.ron.javainfohunter.crawler
Dependency: Rome 1.19.0

### Your Tasks

1. **Create DTOs** in `dto/` package:
   - `FeedItemDto.java` - Represents a single RSS item (title, link, pubDate, description, content, author)
   - `FeedInfoDto.java` - Represents feed metadata (title, description, link, language)

2. **Create Service Interface**:
   - `RssFeedService.java` interface with methods:
     - `FeedInfoDto fetchFeedInfo(String url)` - Get feed metadata
     - `List<FeedItemDto> fetchFeedItems(String url)` - Get all items from feed
     - `List<FeedItemDto> fetchFeedItemsSince(String url, Instant since)` - Get items after timestamp

3. **Create Service Implementation**:
   - `RssFeedServiceImpl.java` using Rome's SyndFeedInput and SyndFeed
   - Handle both RSS and Atom formats
   - Add proper error handling for invalid URLs, timeouts
   - Use Java 21 virtual threads for concurrent feed fetching if beneficial

### Requirements

- Follow project conventions (Lombok, logging, thread safety)
- Handle HTTP errors gracefully (404, timeout, malformed XML)
- Add proper logging at INFO level
- Consider caching feed metadata

### Testing

Create `RssFeedServiceTest.java` with:
- Mock HTTP responses
- Test both RSS and Atom formats
- Test error scenarios

---

## Agent-3: Crawler Service (Task 4)

You are implementing the **Crawler Service** that orchestrates the crawling logic.

### Context

Module: javainfohunter-crawler
Base Package: com.ron.javainfohunter.crawler
Dependencies: RssFeedService (from Agent-2), RawContentRepository

### Your Tasks

1. **Create Service**:
   - `CrawlerService.java` with methods:
     - `CrawlResult crawlSource(RssSource source)` - Crawl a single RSS source
     - `List<CrawlResult> crawlSources(List<RssSource> sources)` - Batch crawl
     - `void markAsCrawled(Long sourceId, List<String> contentHashes)` - Update last_crawled_at and track items

2. **Create Models**:
   - `CrawlResult.java` - Contains sourceId, itemCount, success/failure, errors
   - `CrawlException.java` - Custom exception for crawl failures

3. **Implement Logic**:
   - Use RssFeedService to fetch items
   - Detect duplicate content using SHA-256 hash of title + content
   - Only return new items (not in database)
   - Handle retry logic with exponential backoff
   - Update rss_sources.last_crawled_at timestamp

### Requirements

- Use content hashing for deduplication
- Implement retry logic (3 attempts with exponential backoff)
- Log crawl statistics (items found, new items, duplicates)
- Handle network failures gracefully
- Consider using virtual threads for parallel crawling

### Testing

Create `CrawlerServiceTest.java` with:
- Mock RssFeedService and Repository
- Test duplicate detection
- Test retry logic
- Test error handling

---

## Agent-4: MQ Producer (Task 5)

You are implementing the **RabbitMQ Producer** to publish crawled content to message queues.

### Context

Module: javainfohunter-crawler
Base Package: com.ron.javainfohunter.crawler
Exchange: crawler.exchange
Queue: raw_content.queue
Routing Key: raw_content

### Your Tasks

1. **Create Message Model**:
   - `RawContentMessage.java` - DTO for MQ messages
     - fields: sourceId, sourceName, title, link, content, publishedAt, crawledAt

2. **Create Producer Service**:
   - `RawContentProducer.java` with methods:
     - `void publishRawContent(RawContentMessage message)` - Publish single item
     - `void publishRawContentBatch(List<RawContentMessage> messages)` - Publish batch

3. **Implement Publishing Logic**:
   - Use RabbitTemplate with confirm callback
   - Implement publisher confirms (wait for broker ACK)
   - Handle publishing failures with retry
   - Add proper logging (published count, failures)
   - Serialize messages as JSON

### Requirements

- Use publisher confirms for reliability
- Implement retry on publishing failures
- Add message correlation IDs for tracing
- Log publish statistics
- Handle connection failures gracefully

### Testing

Create `RawContentProducerTest.java` with:
- Use embedded RabbitMQ or mock RabbitTemplate
- Test message serialization
- Test publisher confirm callback
- Test retry logic

---

## Agent-5: Scheduler (Task 6)

You are implementing the **Scheduler** that polls the database for RSS sources to crawl.

### Context

Module: javainfohunter-crawler
Base Package: com.ron.javainfohunter.crawler
Dependencies: RssSourceRepository, CrawlerService, RawContentProducer

### Your Tasks

1. **Create Repository** (if not exists):
   - `RssSourceRepository.java` - Spring Data JPA repository
   - Add custom query: `findByIsActiveTrueAndLastCrawledAtBeforeOrLastCrawledAtIsNull`

2. **Create Scheduler**:
   - `CrawlerScheduler.java` - @Component with @Scheduled
   - Method: `@Scheduled(fixedRate = 3600000)` - Run every hour
   - Logic:
     - Query for active sources due for crawling
     - Group sources by crawl_interval (if supported)
     - Call CrawlerService for each source
     - Publish results via RawContentProducer
     - Update last_crawled_at timestamp

3. **Create Configuration**:
   - `SchedulerProperties.java` - Configuration properties
     - enabled: boolean
     - cron: expression (optional, override fixedRate)
     - batch-size: max concurrent crawls

### Requirements

- Use virtual threads for concurrent crawling of multiple sources
- Respect crawl_interval per source (if in database)
- Add proper logging (sources found, crawled, published)
- Handle scheduler errors without stopping the entire process
- Make scheduling configurable via properties

### Testing

Create `CrawlerSchedulerTest.java` with:
- Mock all dependencies
- Test scheduling logic
- Test error handling
- Test concurrent crawling

---

## Coordination Notes

**All Round 2 agents work on different packages and files:**
- Agent-2: dto/, service/feed/
- Agent-3: service/crawler/, exception/
- Agent-4: producer/, messaging/
- Agent-5: scheduler/, repository/

**No file conflicts expected** - each agent has exclusive domain.

**After Round 2 completes:**
1. Review all implementations
2. Check for integration issues
3. Start Agent-6 for error handling (Task 7)
