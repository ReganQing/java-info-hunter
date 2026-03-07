# Test Coverage Guide

## Overview

This guide explains the test coverage strategy for the JavaInfoHunter E2E test suite and how to interpret coverage reports.

## Coverage Goals

### Minimum Coverage Thresholds

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| **Instruction Coverage** | 80% | N/A | ⏳ Pending |
| **Branch Coverage** | 80% | N/A | ⏳ Pending |
| **Line Coverage** | 80% | N/A | ⏳ Pending |
| **Method Coverage** | 80% | N/A | ⏳ Pending |
| **Class Coverage** | 80% | N/A | ⏳ Pending |

## Coverage Analysis by Module

### javainfohunter-ai-service

**Critical Components to Test:**

| Component | Priority | Coverage Target |
|-----------|----------|-----------------|
| Agent Coordinators | HIGH | 90%+ |
| Tool Registry | HIGH | 90%+ |
| Agent Manager | HIGH | 90%+ |
| Chat Service | MEDIUM | 80%+ |
| Embedding Service | MEDIUM | 80%+ |

**Gaps to Address:**

- [ ] Agent execution flow tests
- [ ] Tool invocation tests
- [ ] Error handling tests
- [ ] Concurrent execution tests

### javainfohunter-crawler

**Critical Components to Test:**

| Component | Priority | Coverage Target |
|-----------|----------|-----------------|
| RSS Feed Parser | HIGH | 90%+ |
| Content Extractor | HIGH | 90%+ |
| Crawl Scheduler | HIGH | 85%+ |
| URL Normalizer | MEDIUM | 80%+ |

**Gaps to Address:**

- [ ] Malformed RSS feed handling
- [ ] Network timeout handling
- [ ] Duplicate content detection
- [ ] Rate limiting tests

### javainfohunter-processor

**Critical Components to Test:**

| Component | Priority | Coverage Target |
|-----------|----------|-----------------|
| Content Analyzer | HIGH | 90%+ |
| Sentiment Analyzer | HIGH | 85%+ |
| Topic Extractor | HIGH | 85%+ |
| Summary Generator | MEDIUM | 80%+ |

**Gaps to Address:**

- [ ] AI service integration tests
- [ ] Error recovery tests
- [ ] Processing pipeline tests
- [ ] Performance benchmarks

### javainfohunter-api

**Critical Components to Test:**

| Component | Priority | Coverage Target |
|-----------|----------|-----------------|
| Controllers | HIGH | 90%+ |
| Services | HIGH | 90%+ |
| DTOs | MEDIUM | 80%+ |
| Validation | HIGH | 85%+ |

**Gaps to Address:**

- [ ] Input validation tests
- [ ] Error response tests
- [ ] Pagination tests
- [ ] Filter tests

## Coverage Reports

### Generate Coverage Report

```bash
# Generate report
./mvnw.cmd test jacoco:report -pl javainfohunter-e2e

# View HTML report
start javainfohunter-e2e/target/site/jacoco/index.html
```

### Report Sections

1. **Overview** - Overall coverage metrics
2. **Packages** - Coverage by package
3. **Classes** - Coverage by class
4. **Source** - Line-by-line coverage

### Interpreting Coverage Reports

**Color Coding:**
- 🟢 Green: Covered (80%+)
- 🟡 Yellow: Partially covered (50-79%)
- 🔴 Red: Not covered (<50%)

**Key Metrics:**

- **Instruction Coverage**: Percentage of bytecode instructions executed
- **Branch Coverage**: Percentage of if/switch branches taken
- **Line Coverage**: Percentage of source lines executed
- **Method Coverage**: Percentage of methods called
- **Class Coverage**: Percentage of classes instantiated

## Coverage Improvement Strategy

### Phase 1: Baseline (Current)

**Status:** Tests written but many disabled

**Action Items:**
- [x] Write test infrastructure (ApiTestHelper)
- [x] Create test data fixtures (test-rss-feed.xml)
- [x] Write E2E test cases
- [ ] Enable tests as modules complete
- [ ] Generate baseline coverage report

### Phase 2: Core Coverage (Target: 60%)

**Focus:** Critical paths and happy paths

**Action Items:**
- [ ] Complete RSS source CRUD coverage
- [ ] Complete news query coverage
- [ ] Complete basic search coverage
- [ ] Add error handling tests

### Phase 3: Edge Cases (Target: 80%)

**Focus:** Error paths and edge cases

**Action Items:**
- [ ] Test all error responses
- [ ] Test validation failures
- [ ] Test boundary conditions
- [ ] Test concurrent operations

### Phase 4: Advanced Scenarios (Target: 90%+)

**Focus:** Complex scenarios and integration

**Action Items:**
- [ ] Test failure recovery
- [ ] Test performance under load
- [ ] Test security scenarios
- [ ] Test data consistency

## Coverage Gaps Analysis

### High Priority Gaps

1. **API Controller Error Handling**
   - Missing: 404, 400, 500 response tests
   - Impact: Low coverage on error paths
   - Estimate: +15% coverage

2. **Database Repository Methods**
   - Missing: Custom query methods
   - Impact: Low coverage on data access
   - Estimate: +20% coverage

3. **Service Layer Business Logic**
   - Missing: Validation and transformation logic
   - Impact: Low coverage on business rules
   - Estimate: +25% coverage

### Medium Priority Gaps

4. **Integration Points**
   - Missing: External service integration
   - Impact: Low coverage on integrations
   - Estimate: +10% coverage

5. **Async Processing**
   - Missing: Message-driven tests
   - Impact: Low coverage on async flows
   - Estimate: +15% coverage

## Coverage Trend Tracking

### Baseline Measurement (To Be Established)

```bash
# Generate baseline report
./mvnw.cmd clean test jacoco:report -pl javainfohunter-e2e

# Save baseline
cp javainfohunter-e2e/target/site/jacoco/jacoco.csv \
   javainfohunter-e2e/coverage-baseline.csv
```

### Track Coverage Changes

```bash
# Compare with baseline
./mvnw.cmd test jacoco:report -pl javainfohunter-e2e

# View differences
diff javainfohunter-e2e/coverage-baseline.csv \
     javainfohunter-e2e/target/site/jacoco/jacoco.csv
```

### Coverage Trend Goals

| Milestone | Target | Date |
|-----------|--------|------|
| Baseline | 0% | 2026-03-07 |
| Phase 1 Complete | 40% | TBD |
| Phase 2 Complete | 60% | TBD |
| Phase 3 Complete | 80% | TBD |
| Phase 4 Complete | 90%+ | TBD |

## Test Coverage Best Practices

### 1. Write Tests Before Code (TDD)

```java
// RED: Write failing test first
@Test
void shouldCreateRssSource() {
    // Test implementation
}

// GREEN: Write minimal code to pass
public RssSourceResponse createSource(Request request) {
    // Implementation
}

// REFACTOR: Improve code
public RssSourceResponse createSource(Request request) {
    // Refactored implementation
}
```

### 2. Focus on Behavior, Not Implementation

**Good:** Test what the system does
```java
@Test
void shouldReturn404ForNonExistentSource() {
    given().get("/api/v1/rss-sources/99999")
           .then().statusCode(404);
}
```

**Bad:** Test internal implementation
```java
@Test
void shouldCallRepositoryFindById() {
    // Don't test which methods are called internally
}
```

### 3. Test Edge Cases

```java
@Test
void shouldHandleEmptyName() {
    // Test boundary condition
}

@Test
void shouldHandleNullCategory() {
    // Test null input
}

@Test
void shouldHandleSpecialCharacters() {
    // Test special characters
}
```

### 4. Test Error Paths

```java
@Test
void shouldReturn400ForInvalidUrl() {
    // Test validation error
}

@Test
void shouldReturn409ForDuplicateSource() {
    // Test conflict error
}
```

### 5. Test Concurrent Scenarios

```java
@Test
void shouldHandleConcurrentCreations() {
    // Test thread safety
}
```

## Coverage Tools

### JaCoCo (Primary Tool)

**Configuration:** Already set up in pom.xml

**Generate Report:**
```bash
./mvnw.cmd jacoco:report -pl javainfohunter-e2e
```

**View Report:**
```bash
start javainfohunter-e2e/target/site/jacoco/index.html
```

### IDE Integration

**IntelliJ IDEA:**
1. Run tests with coverage
2. View coverage in IDE
3. Generate HTML report

**VS Code:**
1. Install Coverage Gutters extension
2. Run tests
3. View coverage in editor

## Coverage Enforcement

### Maven Enforcer Plugin (Future)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce-coverage</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requireCoverage>
                        <minimumBranch>0.80</minimumBranch>
                        <minimumLine>0.80</minimumLine>
                    </requireCoverage>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Next Steps

1. **Complete API Module** - Enable currently disabled tests
2. **Generate Baseline Report** - Establish coverage baseline
3. **Identify Gaps** - Use JaCoCo report to find uncovered code
4. **Write Missing Tests** - Focus on high-priority gaps
5. **Achieve 80% Coverage** - Target for Phase 3-6 completion
6. **Set Up CI/CD** - Automated coverage checks in pipeline

## Resources

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html)
- [Testing Best Practices](https://testing.googleblog.com/)
