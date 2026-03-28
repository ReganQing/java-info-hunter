# Spring AI 1.0.2 → 1.1.2 Upgrade Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade Spring AI from 1.0.2 to 1.1.2, Spring AI Alibaba to 1.1.2.0, and Spring Boot from 3.3.5 to 3.5.x, fixing all breaking API changes.

**Architecture:** Multi-module Maven project with 3 layers of BOM management (Spring Boot parent → Spring AI BOM → Spring AI Alibaba BOM). The upgrade requires Spring Boot 3.5.x as a prerequisite since Spring AI 1.1.2 is built against it. Key breaking changes: artifact renames (pgvector starter), split of `spring-ai-core` into sub-modules, removed `spring-ai-spring-boot-autoconfigure` monolith, and potential `DashScopeChatOptions` API changes.

**Tech Stack:** Spring Boot 3.5.12, Spring AI 1.1.2, Spring AI Alibaba 1.1.2.0, Java 21, DashScope (qwen-max)

---

## Version Matrix

| Component | Current | Target | Notes |
|-----------|---------|--------|-------|
| Spring Boot | 3.3.5 | 3.5.12 | Required by Spring AI 1.1.x |
| Spring AI BOM | 1.0.2 | 1.1.2 | Manages all spring-ai-* artifacts |
| Spring AI Alibaba BOM | 1.0.0.2 | 1.1.2.0 | Manages alibaba starter versions |
| pgvector starter | `spring-ai-pgvector-store-spring-boot-starter:1.0.0-M2` | `spring-ai-starter-vector-store-pgvector` (BOM managed) | **Artifact renamed** |
| DashScope starter | via Alibaba BOM | via Alibaba BOM | Version managed by BOM |

## Breaking Changes Summary

1. **Artifact Renames**: All Spring AI starters renamed (`spring-ai-{X}-spring-boot-starter` → `spring-ai-starter-model-{X}` or `spring-ai-starter-vector-store-{X}`)
2. **Module Split**: `spring-ai-core` split into `spring-ai-commons`, `spring-ai-model`, `spring-ai-client-chat`, `spring-ai-vector-store`
3. **Removed Artifacts**: `spring-ai-spring-boot-autoconfigure` no longer exists (per-component autoconfigure modules replace it)
4. **Exclusions Must Be Updated**: Old exclusion targets (`spring-ai-core`, `spring-autoconfigure`) may no longer exist or have different artifact IDs
5. **Spring Boot 3.5.x**: New baseline, minor property/config changes possible

## Files to Modify

| # | File | Change Type |
|---|------|-------------|
| 1 | `pom.xml` (root) | Version properties + BOM versions |
| 2 | `javainfohunter-ai-service/pom.xml` | Exclusions + artifact rename |
| 3 | `javainfohunter-api/pom.xml` | springdoc/knife4j version check |
| 4 | `javainfohunter-processor/src/main/resources/application.yml` | Verify retry config keys |
| 5 | `javainfohunter-ai-service/.../ToolCallAgent.java` | Verify API compatibility |
| 6 | `javainfohunter-api/.../ApiApplication.java` | Verify exclude class paths |

---

### Task 1: Update Root POM Versions

**Files:**
- Modify: `pom.xml`

**Step 1: Update version properties**

Change the `<properties>` block in the root `pom.xml`:

```xml
<!-- BEFORE -->
<spring-ai.version>1.0.2</spring-ai.version>
<spring-ai-alibaba.version>1.0.0-M2.1</spring-ai-alibaba.version>

<!-- AFTER -->
<spring-ai.version>1.1.2</spring-ai.version>
<spring-ai-alibaba.version>1.1.2.0</spring-ai-alibaba.version>
```

**Step 2: Update Spring Boot parent version**

```xml
<!-- BEFORE -->
<version>3.3.5</version>

<!-- AFTER -->
<version>3.5.12</version>
```

**Step 3: Update Spring AI Alibaba BOM version**

```xml
<!-- BEFORE -->
<version>1.0.0.2</version>

<!-- AFTER -->
<version>1.1.2.0</version>
```

**Step 4: Update hardcoded Spring Boot version references in dependencyManagement**

Remove hardcoded `<version>3.3.5</version>` from:
- `spring-boot-starter-data-redis`
- `spring-boot-starter-aop`
- `spring-boot-starter-actuator`

These are managed by the Spring Boot parent and don't need explicit versions.

**Step 5: Commit**

```bash
git add pom.xml
git commit -m "chore: upgrade Spring Boot 3.5.12, Spring AI 1.1.2, Spring AI Alibaba 1.1.2.0"
```

---

### Task 2: Update AI Service Module POM (Exclusions + pgvector)

**Files:**
- Modify: `javainfohunter-ai-service/pom.xml`

**Step 1: Rename pgvector starter artifact**

```xml
<!-- BEFORE -->
<artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
<version>1.0.0-M2</version>

<!-- AFTER -->
<artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
<!-- no version - managed by spring-ai-bom -->
```

**Step 2: Update exclusions in DashScope starter**

The old exclusions reference `spring-ai-core` and `spring-autoconfigure`. In Spring AI 1.1.x, these artifacts may have been split/renamed. Remove outdated exclusions that reference artifacts that no longer exist:

```xml
<!-- BEFORE: spring-ai-alibaba-starter-dashscope exclusions -->
<exclusions>
    <exclusion>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
    </exclusion>
    <exclusion>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-autoconfigure</artifactId>
    </exclusion>
</exclusions>

<!-- AFTER: Remove exclusions entirely - let BOM manage versions properly -->
<!-- The Alibaba BOM 1.1.2.0 is built against Spring AI 1.1.2, so no version conflicts -->
```

**Step 3: Update exclusions in pgvector starter**

```xml
<!-- BEFORE -->
<exclusions>
    <exclusion>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-spring-boot-autoconfigure</artifactId>
    </exclusion>
    <exclusion>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
    </exclusion>
</exclusions>

<!-- AFTER: Remove exclusions - old artifacts no longer exist -->
```

**Step 4: Build to verify dependency resolution**

Run: `./mvnw.cmd clean compile -pl javainfohunter-ai-service -am -DskipTests`
Expected: BUILD SUCCESS (dependency resolution works)

**Step 5: Commit**

```bash
git add javainfohunter-ai-service/pom.xml
git commit -m "fix: update AI service dependencies for Spring AI 1.1.2 artifact renames"
```

---

### Task 3: Fix Compilation Errors from API Changes

**Files:**
- Modify: `javainfohunter-ai-service/src/main/java/com/ron/javainfohunter/ai/agent/core/ToolCallAgent.java`
- Modify: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/ApiApplication.java`
- Possibly: Other files with Spring AI imports

**Step 1: Full build to find all compilation errors**

Run: `./mvnw.cmd clean compile -DskipTests`
Expected: May have compilation errors from API changes

**Step 2: Fix each compilation error**

Common fixes needed:
- `ToolCallingManager.builder().build()` → May need updated import path
- `DashScopeChatOptions.builder().withInternalToolExecutionEnabled(false).build()` → Verify API
- `ToolExecutionResult` → Verify import and API
- `DashScopeChatAutoConfiguration` / `DashScopeAgentAutoConfiguration` in ApiApplication → Verify class paths in 1.1.2.0

**Step 3: Build again to verify all fixes**

Run: `./mvnw.cmd clean compile -DskipTests`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -A
git commit -m "fix: resolve Spring AI 1.1.2 API breaking changes"
```

---

### Task 4: Fix Test Compilation and Run Tests

**Files:**
- Possibly: Test files referencing old Spring AI APIs
- Verify: `application.yml` retry config compatibility

**Step 1: Run full test suite**

Run: `./mvnw.cmd clean test`
Expected: Some tests may fail due to API changes

**Step 2: Fix test compilation errors**

Same pattern as Task 3 - fix import paths and API usage in test files.

**Step 3: Run tests again**

Run: `./mvnw.cmd test`
Expected: All tests pass (unit tests; integration tests that need infrastructure will be skipped)

**Step 4: Commit**

```bash
git add -A
git commit -m "fix: update test code for Spring AI 1.1.2 compatibility"
```

---

### Task 5: Verify Configuration Compatibility

**Files:**
- Verify: `javainfohunter-processor/src/main/resources/application.yml` (retry config)
- Verify: `javainfohunter-ai-service/src/main/resources/application.yml`
- Verify: All `application-dev.yml` files

**Step 1: Check retry config keys**

The `spring.ai.retry.*` properties should still work in Spring AI 1.1.2 (they're in `spring-ai-autoconfigure-retry` module). Verify:
- `spring.ai.retry.max-attempts`
- `spring.ai.retry.on-client-errors`
- `spring.ai.retry.exclude-on-http-codes`

**Step 2: Check DashScope config keys**

Verify these still work in Spring AI Alibaba 1.1.2.0:
- `spring.ai.dashscope.api-key`
- `spring.ai.dashscope.chat.options.model`
- `spring.ai.dashscope.chat.options.temperature`

**Step 3: Full package build**

Run: `./mvnw.cmd clean package -DskipTests`
Expected: BUILD SUCCESS for all modules

**Step 4: Commit any config fixes**

```bash
git add -A
git commit -m "fix: update configuration for Spring AI 1.1.2 compatibility"
```

---

### Task 6: Final Verification

**Step 1: Full build with tests**

Run: `./mvnw.cmd clean verify`
Expected: BUILD SUCCESS

**Step 2: Check dependency tree for conflicts**

Run: `./mvnw.cmd dependency:tree | grep spring-ai`
Expected: All spring-ai artifacts at version 1.1.2, alibaba artifacts at 1.1.2.0

**Step 3: Smoke test - start processor service**

Run: `./mvnw.cmd spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=dev`
Expected: Service starts without errors (check logs for DashScope connection)

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Spring Boot 3.5.x breaking changes for JPA/Flyway/RabbitMQ | Boot 3.3→3.5 is minor-version jump, mostly additive. Test thoroughly. |
| `DashScopeChatOptions` API changed | Verify at compile time, fix builder calls |
| `ToolCallingManager` / `ToolExecutionResult` API changed | Verify at compile time, check Spring AI 1.1 upgrade notes |
| Dependency conflicts between BOMs | BOM ordering matters: Spring AI Alibaba BOM first, then Spring AI BOM |
| springdoc/knife4j compatibility with Boot 3.5 | Check versions, may need upgrade |
