# CI/CD Setup Guide

## Overview

This document explains the CI/CD pipeline setup for the JavaInfoHunter project using GitHub Actions.

## Architecture

```
GitHub Actions CI/CD Pipeline
|
+-- ci.yml (Main Pipeline)
|   +-- Build
|   +-- Unit Tests (per module)
|   +-- Code Quality Checks
|   +-- Security Scanning
|
+-- e2e-tests.yml (E2E Tests)
|   +-- Smoke Tests (all events)
|   +-- Full E2E Tests (push/manual)
|   +-- API Contract Tests
|
+-- coverage.yml (Coverage Reporting)
|   +-- Coverage Analysis
|   +-- Trend Tracking
|   +-- Badge Generation
|
+-- performance.yml (Performance Tests)
|   +-- JUnit Performance Tests
|   +-- JMeter Load Tests (scheduled)
|   +-- Regression Detection
```

## Workflows

### 1. CI Pipeline (ci.yml)

**Triggers:**
- Push to main, develop, feature/*, fix/*
- Pull requests to main/develop

**Jobs:**

| Job | Purpose | Timeout |
|-----|---------|----------|
| Build | Compile and package | 15 min |
| Unit Tests | Run unit tests per module | 20 min |
| Code Quality | Checkstyle, SpotBugs, PMD | 15 min |
| Security Scan | OWASP, Trivy | 15 min |
| Test Summary | Aggregate test results | - |

**Quality Checks:**
```bash
# Run locally before pushing
./mvnw clean compile
./mvnw test
./mvnw checkstyle:check
./mvnw spotbugs:check
./mvnw pmd:check
./mvnw org.owasp:dependency-check-maven:check
```

### 2. E2E Tests (e2e-tests.yml)

**Triggers:**
- Push to main/develop
- Pull requests to main/develop
- Manual workflow dispatch

**Jobs:**

| Job | When Runs | Services |
|-----|-----------|----------|
| Smoke Tests | All events | PostgreSQL, RabbitMQ, Redis |
| Full E2E | Push/manual | PostgreSQL, RabbitMQ, Redis |
| API Tests | All events | PostgreSQL, RabbitMQ, Redis |

**Environment Variables:**
```yaml
DB_USERNAME: postgres
DB_PASSWORD: postgres
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/javainfohunter_test
SPRING_RABBITMQ_HOST: localhost
SPRING_REDIS_HOST: localhost
```

**Local E2E Test Execution:**
```bash
# Using Docker services
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15
docker run -d -p 5672:5672 rabbitmq:3-management
docker run -d -p 6379:6379 redis:7

# Run E2E tests
./mvnw test -pl javainfohunter-e2e -Dtestcontainers.enable=false
```

### 3. Coverage Reporting (coverage.yml)

**Triggers:**
- Push to main/develop
- Pull requests to main/develop

**Features:**

1. **Coverage Aggregation**
   - Merges reports from all modules
   - Generates aggregate HTML report

2. **Threshold Enforcement**
   - 80% minimum coverage
   - Fails build if below threshold

3. **Codecov Integration**
   - Uploads coverage data
   - Generates trend reports

4. **PR Comments**
   - Comments coverage on PRs
   - Shows changed files coverage

5. **Badge Generation**
   - Creates coverage badge
   - Updates on main branch pushes

**Coverage Goals:**

| Metric | Target |
|--------|--------|
| Instruction Coverage | 80% |
| Branch Coverage | 80% |
| Line Coverage | 80% |
| Method Coverage | 80% |
| Class Coverage | 80% |

**Local Coverage Report:**
```bash
# Generate coverage report
./mvnw test jacoco:report

# View HTML report
open javainfohunter-ai-service/target/site/jacoco/index.html
```

### 4. Performance Tests (performance.yml)

**Triggers:**
- Push to main/develop
- Pull requests to main
- Schedule: Daily at 2 AM UTC
- Manual with custom parameters

**Jobs:**

| Job | Purpose | Schedule |
|-----|---------|----------|
| Performance Tests | JUnit virtual thread stress | All events |
| JMeter Load Tests | Full load testing | Daily/manual |
| Regression Check | Compare with baseline | PRs only |
| Metrics Update | Aggregate all metrics | After tests |

**Performance Baselines:**

| Metric | Target |
|--------|--------|
| Throughput | > 100 req/s |
| P95 Latency | < 1000ms |
| P99 Latency | < 2000ms |
| Error Rate | < 1% |

**Local Performance Testing:**
```bash
# Run JUnit performance tests
./mvnw test -pl javainfohunter-e2e -Dtest=VirtualThreadStressTest

# Run JMeter tests (requires running API server)
jmeter -n -t javainfohunter-e2e/src/test/resources/jmeter/JavaInfoHunter.jmx \
  -l results.jtl -e -o report/
```

## Setup Instructions

### 1. Repository Secrets

No secrets required for basic CI/CD. Optional secrets:

| Secret | Purpose | Required |
|--------|---------|----------|
| `CODECOV_TOKEN` | Codecov upload | No |
| `DASHSCOPE_API_KEY` | AI service tests | No |

### 2. GitHub Actions Permissions

Workflows require:
- `contents: read` (default)
- `contents: write` (for badge generation)
- `pull-requests: write` (for PR comments)
- `actions: read` (for downloading artifacts)

### 3. Branch Protection Rules

Recommended settings for main branch:

```yaml
Required status checks:
  - Build (ci.yml)
  - Unit Tests (ci.yml)
  - Smoke Tests (e2e-tests.yml)
  - Code Quality (ci.yml)

Require branches to be up to date before merging
Require status checks to pass before merging

Include administrators
```

## Local Development Simulation

### Simulate CI Build Locally

```bash
#!/bin/bash
# ci-local.sh - Simulate CI pipeline locally

echo "=== Step 1: Build ==="
./mvnw clean compile
./mvnw package -DskipTests

echo "=== Step 2: Unit Tests ==="
./mvnw test

echo "=== Step 3: Code Quality ==="
./mvnw checkstyle:check || true
./mvnw spotbugs:check || true

echo "=== Step 4: Coverage ==="
./mvnw jacoco:report

echo "=== CI Build Complete ==="
```

### Simulate E2E Tests Locally

```bash
#!/bin/bash
# e2e-local.sh - Simulate E2E pipeline locally

# Start services
docker-compose -f docker-compose.test.yml up -d

# Wait for services
sleep 10

# Run tests
./mvnw test -pl javainfohunter-e2e -Dtestcontainers.enable=false

# Cleanup
docker-compose -f docker-compose.test.yml down
```

## Troubleshooting

### Issue: CI fails locally but passes in GitHub

**Cause:** Environment differences (Java version, Maven cache)

**Solution:**
```bash
# Match CI environment
export JAVA_VERSION=21
./mvnw clean install -U  # -U updates snapshots
```

### Issue: E2E tests timeout in CI

**Cause:** Service startup takes too long

**Solution:**
- Increase service health check timeouts
- Add pre-warmed service containers
- Use Testcontainers instead of service containers

### Issue: Coverage not uploading to Codecov

**Cause:** Missing token or network issues

**Solution:**
```yaml
# Add CODECOV_TOKEN to repository secrets
# Or use public mode (no token required)
```

### Issue: Performance tests flaky

**Cause:** Resource contention in CI

**Solution:**
- Increase timeout thresholds
- Run on dedicated runners
- Use scheduled runs instead of per-commit

## Monitoring and Alerts

### CI/CD Metrics to Track

1. **Build Duration**
   - Target: < 15 minutes
   - Alert if: > 30 minutes

2. **Test Pass Rate**
   - Target: > 95%
   - Alert if: < 80%

3. **Coverage Trend**
   - Target: Increasing
   - Alert if: Decreasing by > 5%

4. **Flaky Tests**
   - Target: 0
   - Alert if: > 2 per week

### Setting Up Alerts

```yaml
# .github/workflows/notify.yml
name: CI Notifications

on:
  workflow_run:
    workflows: [CI, E2E Tests, Coverage]
    types: [completed]
    status: [failure]

jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - name: Send Slack notification
        uses: slackapi/slack-github-action@v1
        with:
          webhook: ${{ secrets.SLACK_WEBHOOK }}
          payload: |
            {
              "text": "CI failed for ${{ github.event.repository.name }}"
            }
```

## Best Practices

### 1. Fast Feedback

- Run quick checks first (compile, unit tests)
- Run slow checks later (E2E, performance)
- Use parallel jobs where possible

### 2. Reliable Tests

- Avoid external dependencies in unit tests
- Use deterministic test data
- Clean up resources in @AfterEach

### 3. Coverage Gates

- Set realistic thresholds (start at 70%, aim for 80%)
- Focus on new code coverage in PRs
- Use diff coverage for PR comments

### 4. Performance Baselines

- Establish baseline on main branch
- Compare PR against baseline
- Only alert on significant regressions (> 10%)

### 5. Security

- Scan dependencies regularly
- Fix CVEs within 7 days for high severity
- Keep base images updated

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [JMeter User Manual](https://jmeter.apache.org/usermanual/index.html)
- [Codecov Documentation](https://docs.codecov.com/)

## Next Steps

- [ ] Set up branch protection rules
- [ ] Configure Codecov token (optional)
- [ ] Set up Slack notifications
- [ ] Create Docker Compose for local testing
- [ ] Configure performance alerts
- [ ] Set up coverage badge in README
