# Comprehensive Audit Fixes - Implementation Progress

> Plan: [2026-04-19-comprehensive-audit-fixes.md](./2026-04-19-comprehensive-audit-fixes.md)

---

## Phase 1: CRITICAL Fixes (4 tasks)

### Task 1: Fix Agent State Reset Between run() Calls
- **Status:** DONE
- **Commit:** `fcee317` fix: reset agent state in run() finally block for reusability
- **Files changed:**
  - `javainfohunter-ai-service/.../agent/core/BaseAgent.java` - reset agentState, currentStep, messages in finally
  - `javainfohunter-ai-service/.../agent/core/BaseAgentTest.java` - added re-invocability tests, updated existing assertions
- **Tests:** BaseAgentTest 8/8 pass
- **Adaptation:** Existing tests updated from asserting FINISHED to IDLE (correct new behavior)

### Task 2: Fix Agent State Transition Race Condition
- **Status:** DONE
- **Commit:** `5fcb552` fix: synchronize agent state transitions via transitionTo() method
- **Files changed:**
  - `javainfohunter-ai-service/.../agent/core/BaseAgent.java` - added synchronized `transitionTo()` method
  - `javainfohunter-ai-service/.../agent/core/ReActAgent.java` - replaced setAgentState() with transitionTo()
  - `javainfohunter-ai-service/.../agent/core/ToolCallAgent.java` - replaced setAgentState() with transitionTo()
  - `javainfohunter-ai-service/.../agent/specialized/CoordinatorAgent.java` - replaced setAgentState() with transitionTo()
  - `javainfohunter-ai-service/.../agent/core/BaseAgentConcurrencyTest.java` - new concurrency tests
- **Tests:** BaseAgentConcurrencyTest 2/2 pass
- **Adaptation:** Test adapted - synchronized block + finally reset already serializes; transitionTo() added for architectural correctness

### Task 3: Fix JWT Refresh Token Hash Collision
- **Status:** DONE
- **Commit:** `dbaa79e` fix: replace insecure 32-bit token hash with SHA-256
- **Files changed:**
  - `javainfohunter-api/.../security/JwtService.java` - replaced Integer.toHexString(hashCode()) with SHA-256
  - `javainfohunter-api/.../security/JwtServiceTest.java` - new test verifying 64-char SHA-256 hash, no collisions
  - `javainfohunter-ai-service/.../db/migration/V4__clear_refresh_tokens_for_sha256_upgrade.sql` - clear existing tokens
- **Tests:** JwtServiceTest 2/2 pass

### Task 4: Fix ExecutorService Resource Leak in Processor
- **Status:** DONE
- **Commit:** `56b267f` fix: add @PreDestroy shutdown for virtual thread executor in ContentRoutingService
- **Files changed:**
  - `javainfohunter-processor/.../service/impl/ContentRoutingServiceImpl.java` - added @PreDestroy shutdown() with 10s graceful timeout
  - `javainfohunter-processor/.../service/impl/ContentRoutingServiceImplTest.java` - new test verifying executor shutdown
- **Tests:** ContentRoutingServiceImplTest 1/1 pass

---

## Phase 2: HIGH Priority Fixes (15 tasks)

| Task | Description | Status | Commit |
|------|-------------|--------|--------|
| 5 | Fix Importance Score Key Mismatch | PENDING | - |
| 6 | Fix AuthService Exception Types | PENDING | - |
| 7 | Fix Shared Mutable Agents in TaskCoordinator | PENDING | - |
| 8 | Fix @Data on JPA Entities | PENDING | - |
| 9 | Fix AgentServiceImpl OOM with findAll() | PENDING | - |
| 10 | Secure CrawlController Endpoints | PENDING | - |
| 11 | Fix ContentPublisher correlationId Reuse | PENDING | - |
| 12 | Fix HTTP Redirect Following in RssFeedCrawler | PENDING | - |
| 13 | Fix AiServiceAutoConfiguration matchIfMissing Mismatch | PENDING | - |
| 14 | Fix Tool Auto-Discovery Not Working | PENDING | - |
| 15 | Replace Polling with CountDownLatch in ContentRoutingService | PENDING | - |
| 16 | Fix @Transactional Self-Invocation in TransactionalStoreService | PENDING | - |
| 17 | Fix Double Virtual Thread Wrapping in Processor | PENDING | - |
| 18 | Add Memory Management for Result Maps | PENDING | - |
| 19 | Add Missing Agent ID Validation in Chain/Parallel | PENDING | - |

---

## Phase 3: MEDIUM Priority Fixes (16 tasks)

| Task | Description | Status | Commit |
|------|-------------|--------|--------|
| 20 | Fix executeParallel Timeout | PENDING | - |
| 21 | Fix N+1 Query in NewsServiceImpl.searchNews() | PENDING | - |
| 22 | Fix Rate Limiting TOCTOU Race Condition | PENDING | - |
| 23 | Fix Duplicate Error Classification in Crawler | PENDING | - |
| 24 | Consolidate CrawlCoordinator and CrawlOrchestrator | PENDING | - |
| 25 | Fix CrawlScheduler Loading All Active Sources | PENDING | - |
| 26 | Fix RabbitMQ Consumer Config Override | PENDING | - |
| 27 | Fix RssSourceServiceImpl Manual Memory Pagination | PENDING | - |
| 28 | Fix Actuator Dev Exposure | PENDING | - |
| 29 | Fix ChatService/EmbeddingService Null Checks | PENDING | - |
| 30 | Fix Status Update Race Condition in TransactionalStoreService | PENDING | - |
| 31 | Remove Duplicate getNewsByCategory Endpoint | PENDING | - |
| 32 | Fix Hardcoded Language in TransactionalStoreService | PENDING | - |
| 33 | Fix CrawlErrorHandler ErrorType Mapping Gap | PENDING | - |
| 34 | Fix RabbitMQ ClassMapper Missing Types | PENDING | - |
| 35 | Remove Dead AsyncConfig Bean | PENDING | - |

---

## Summary

| Phase | Total | Done | Pending |
|-------|-------|------|---------|
| Phase 1 (CRITICAL) | 4 | 4 | 0 |
| Phase 2 (HIGH) | 15 | 0 | 15 |
| Phase 3 (MEDIUM) | 16 | 0 | 16 |
| **Total** | **35** | **4** | **31** |
