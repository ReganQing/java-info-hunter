# Dashboard Live Data Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace all hardcoded data in the frontend dashboard with live data from backend APIs, adding new backend endpoints where needed.

**Architecture:** Add a `/api/v1/news/stats` endpoint to expose category statistics. Use existing `/api/v1/admin/status` services map for System Health. Use existing `/api/v1/agents/executions` for Recent Activity. Remove all hardcoded arrays from the dashboard component.

**Tech Stack:** Spring Boot JPA (backend), React Query v5 (frontend)

**Branch:** `feature/dashboard-live-data`

---

## Phase A: Backend - New Stats Endpoint

### Task 1: Create NewsStatsResponse DTO

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/dto/response/NewsStatsResponse.java`

**Step 1:** Create DTO:

```java
@Data
@Builder
public class NewsStatsResponse {
    private List<CategoryStats> categoryStats;
    private List<SentimentStats> sentimentStats;
    private long totalPublished;

    @Data
    @Builder
    public static class CategoryStats {
        private String category;
        private long count;
    }

    @Data
    @Builder
    public static class SentimentStats {
        private String sentiment;
        private long count;
        private Double avgScore;
    }
}
```

**Step 2:** Commit
```bash
git commit -m "feat: add NewsStatsResponse DTO with category and sentiment stats"
```

### Task 2: Add stats method to NewsService

**Files:**
- Modify: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/service/NewsService.java`
- Modify: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/service/impl/NewsServiceImpl.java`

**Step 1:** Add method to NewsService interface:
```java
NewsStatsResponse getNewsStats();
```

**Step 2:** Implement in NewsServiceImpl:
```java
@Override
public NewsStatsResponse getNewsStats() {
    List<Object[]> categoryData = newsRepository.getStatisticsByCategory();
    List<Object[]> sentimentData = newsRepository.getStatisticsBySentiment();

    List<CategoryStats> categories = categoryData.stream()
        .map(row -> CategoryStats.builder()
            .category((String) row[0])
            .count((Long) row[1])
            .build())
        .toList();

    List<SentimentStats> sentiments = sentimentData.stream()
        .map(row -> SentimentStats.builder()
            .sentiment(((News.Sentiment) row[0]).name())
            .count((Long) row[1])
            .avgScore(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
            .build())
        .toList();

    long totalPublished = categories.stream()
        .mapToLong(CategoryStats::getCount)
        .sum();

    return NewsStatsResponse.builder()
        .categoryStats(categories)
        .sentimentStats(sentiments)
        .totalPublished(totalPublished)
        .build();
}
```

**Step 3:** Commit
```bash
git commit -m "feat: add getNewsStats service method using repository statistics queries"
```

### Task 3: Add stats endpoint to NewsController

**Files:**
- Modify: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/controller/NewsController.java`

**Step 1:** Add endpoint:

```java
@GetMapping("/stats")
@Operation(summary = "Get news statistics", description = "Get category and sentiment statistics for published news")
public ResponseEntity<ApiResponse<NewsStatsResponse>> getNewsStats() {
    log.debug("Getting news statistics");
    NewsStatsResponse response = newsService.getNewsStats();
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**Step 2:** Commit
```bash
git commit -m "feat: add GET /api/v1/news/stats endpoint for dashboard"
```

---

## Phase B: Frontend - Replace Hardcoded Data

### Task 4: Add stats types and API endpoint

**Files:**
- Modify: `src/shared/api/types.ts` (frontend)
- Modify: `src/shared/api/endpoints.ts`

**Step 1:** Add to types.ts:
```typescript
export interface NewsStatsResponse {
  categoryStats: Array<{ category: string; count: number }>;
  sentimentStats: Array<{ sentiment: string; count: number; avgScore: number }>;
  totalPublished: number;
}
```

**Step 2:** Add to newsApi in endpoints.ts:
```typescript
getStats: (): ApiData<ApiResponse<NewsStatsResponse>> =>
  apiClient.get('/news/stats'),
```

**Step 3:** Commit
```bash
git commit -m "feat: add news stats API endpoint and types to frontend"
```

### Task 5: Replace hardcoded topCategories with live data

**Files:**
- Modify: `src/modules/research/dashboard/index.tsx`

**Step 1:** Add useQuery for stats:
```typescript
const { data: statsData } = useQuery({
  queryKey: ['news-stats'],
  queryFn: () => newsApi.getStats(),
  staleTime: 60000, // 1 minute
});
```

**Step 2:** Replace hardcoded `topCategories` with:
```typescript
const CATEGORY_COLORS = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-red-500', 'bg-yellow-500', 'bg-indigo-500'];
const topCategories = (statsData?.data?.categoryStats || [])
  .sort((a, b) => b.count - a.count)
  .slice(0, 6)
  .map((cat, i) => ({
    name: cat.category,
    count: cat.count,
    color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
  }));
```

**Step 3:** Commit
```bash
git commit -m "feat: replace hardcoded topCategories with live category stats"
```

### Task 6: Replace hardcoded System Health with live data

**Files:**
- Modify: `src/modules/research/dashboard/index.tsx`

**Step 1:** Replace the hardcoded System Health card with dynamic data from `statsData?.data?.services`:

```tsx
const services = statsData?.data?.services || {};
const healthItems = [
  { name: 'RSS Sources', status: services.rssSources?.status || 'UNKNOWN' },
  { name: 'News Processing', status: services.newsProcessing?.status || 'UNKNOWN' },
  { name: 'Agent System', status: services.agentSystem?.status || 'UNKNOWN' },
];
```

**Step 2:** Use status-to-color mapping function:
```typescript
const statusColor = (status: string) => {
  switch (status) {
    case 'UP': return { dot: 'bg-green-500', text: 'text-green-600', label: 'Healthy' };
    case 'DOWN': return { dot: 'bg-red-500', text: 'text-red-600', label: 'Down' };
    case 'DEGRADED': return { dot: 'bg-yellow-500', text: 'text-yellow-600', label: 'Degraded' };
    default: return { dot: 'bg-gray-500', text: 'text-gray-600', label: 'Unknown' };
  }
};
```

**Step 3:** Commit
```bash
git commit -m "feat: replace hardcoded System Health with live service status"
```

### Task 7: Replace hardcoded Recent Activity with agent executions

**Files:**
- Modify: `src/modules/research/dashboard/index.tsx`
- Modify: `src/shared/api/endpoints.ts`

**Step 1:** Add useQuery for recent agent executions:
```typescript
const { data: activityData } = useQuery({
  queryKey: ['recent-activity'],
  queryFn: () => agentsApi.getExecutions(0, 5),
  staleTime: 30000,
});
```

**Step 2:** Replace hardcoded `recentActivity` with mapped execution data:
```typescript
const recentActivity = (activityData?.data?.content || []).map((exec) => ({
  id: exec.id,
  action: `${exec.agentType} ${exec.status}`,
  source: exec.agentId,
  time: exec.startTime ? formatDistanceToNow(new Date(exec.startTime), { addSuffix: true }) : 'N/A',
}));
```

Add `formatDistanceToNow` from `date-fns`.

**Step 3:** Commit
```bash
git commit -m "feat: replace hardcoded Recent Activity with live agent executions"
```

### Task 8: Remove hardcoded StatCard trends

**Files:**
- Modify: `src/modules/research/dashboard/index.tsx`

**Step 1:** Remove `trend={12}` from "Total Articles" StatCard
**Step 2:** Remove `trend={-5}` from "Pending Processing" StatCard
**Step 3:** These will just show the current value without a trend indicator until historical comparison is implemented.

**Step 4:** Commit
```bash
git commit -m "fix: remove hardcoded trend values from dashboard StatCards"
```

---

## Phase C: Verification

### Task 9: Build verification

**Step 1:** Run backend build: `mvnw.cmd clean package`
**Step 2:** Run frontend build: `npm run build`
**Step 3:** Verify new `/api/v1/news/stats` endpoint returns expected data structure
**Step 4:** Merge branch to main
