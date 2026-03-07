# Test Execution Guide

## Overview

This guide explains how to execute the E2E tests for the JavaInfoHunter project.

## Prerequisites

### Required Software

- **Java 21+** - Required for virtual thread support
- **Maven 3.9+** - Build tool
- **PostgreSQL 15+** - Database (or use Testcontainers)
- **RabbitMQ 3.12+** - Message queue (or use Testcontainers)
- **Redis 7+** - Cache (or use Testcontainers)

### Environment Variables

```bash
# Database
export DB_USERNAME=postgres
export DB_PASSWORD=your-password

# AI Service (optional for E2E tests)
export DASHSCOPE_API_KEY=your-api-key
```

## Test Execution Modes

### Mode 1: Using External Services (Recommended for Windows)

Use this mode when Testcontainers is not available or on Windows.

```bash
# Set system property to disable Testcontainers
./mvnw.cmd test -pl javainfohunter-e2e -Dtestcontainers.enable=false

# Or use Maven properties
./mvnw.cmd test -pl javainfohunter-e2e -Dtestcontainers.enable=false
```

**External Services Configuration:**

- PostgreSQL: `localhost:5432`
- RabbitMQ: `localhost:5672`
- Redis: `localhost:6379`

You can override defaults with system properties:

```bash
./mvnw.cmd test -pl javainfohunter-e2e \
    -Dtestcontainers.enable=false \
    -Ddb.url=jdbc:postgresql://localhost:5432/javainfohunter \
    -Ddb.user=postgres \
    -Ddb.password=postgres \
    -Drabbitmq.host=localhost \
    -Drabbitmq.port=5672 \
    -Drabbitmq.user=admin \
    -Drabbitmq.password=admin \
    -Dredis.host=localhost \
    -Dredis.port=6379
```

### Mode 2: Using Testcontainers (Linux/MacOS)

Testcontainers will automatically start dependencies in Docker containers.

```bash
# No special configuration needed
./mvnw test -pl javainfohunter-e2e
```

**Requirements:**
- Docker installed and running
- Sufficient memory (4GB+ recommended)

## Running Specific Tests

### Run All E2E Tests

```bash
./mvnw.cmd test -pl javainfohunter-e2e
```

### Run Specific Test Class

```bash
# Crawl Process E2E Test
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=CrawlProcessApiE2ETest

# API Endpoints E2E Test
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=ApiEndpointsE2ETest

# Performance Test
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=VirtualThreadStressTest
```

### Run Specific Test Method

```bash
# Single test method
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=CrawlProcessApiE2ETest#test01_CreateRssSource

# Multiple test methods
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=CrawlProcessApiE2ETest#test01_CreateRssSource+test02_TriggerManualCrawl
```

### Run Tests by Tag/Display Name

```bash
# Run all RSS source tests
./mvnw.cmd test -pl javainfohunter-e2E -Dtest="ApiEndpointsE2ETest" -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition
```

## Test Categories

### 1. Integration Tests (Phase 1-2)

Test database and messaging infrastructure:

```bash
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=PostgreSQLIntegrationTest
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=RabbitMQIntegrationTest
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=RedisIntegrationTest
```

### 2. E2E Tests (Phase 3)

Test complete system flows:

```bash
# Crawl to API flow
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=CrawlProcessApiE2ETest

# API endpoints
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=ApiEndpointsE2ETest
```

### 3. Performance Tests (Phase 4)

Test system under load:

```bash
# Virtual thread stress test
./mvnw.cmd test -pl javainfohunter-e2e -Dtest=VirtualThreadStressTest
```

## Coverage Reports

### Generate JaCoCo Coverage Report

```bash
# Generate report after running tests
./mvnw.cmd test jacoco:report -pl javainfohunter-e2e

# View report
# HTML report: javainfohunter-e2e/target/site/jacoco/index.html
```

### Coverage Thresholds

Current targets (to be achieved):

- **Instruction Coverage**: 80%
- **Branch Coverage**: 80%
- **Line Coverage**: 80%
- **Method Coverage**: 80%
- **Class Coverage**: 80%

## Test Execution Tips

### Skip Slow Tests

```bash
# Skip performance tests
./mvnw.cmd test -pl javainfohunter-e2e -Dtest='!VirtualThreadStressTest'
```

### Run with Debug Output

```bash
# Enable debug logging
./mvnw.cmd test -pl javainfohunter-e2e -X

# Enable test debug output
./mvnw.cmd test -pl javainfohunter-e2e -Dorg.slf4j.simpleLogger.log.com.ron.javainfohunter=debug
```

### Parallel Execution

```bash
# Run tests in parallel (requires surefire configuration)
./mvnw.cmd test -pl javainfohunter-e2e -DforkCount=2 -DreuseForks=false
```

## Troubleshooting

### Issue: Spring Context Fails to Load

**Cause:** API module beans not available or incomplete.

**Solution:**
1. Ensure API module is built: `./mvnw.cmd install -pl javainfohunter-api`
2. Check that all required services are running
3. Review test logs for specific bean creation failures

### Issue: Connection Refused

**Cause:** External services not running.

**Solution:**
```bash
# Start PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15

# Start RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# Start Redis
docker run -d -p 6379:6379 redis:7
```

### Issue: Tests Time Out

**Cause:** Tests waiting for data that doesn't exist.

**Solution:**
1. Seed test data in database
2. Disable waiting tests with @Disabled
3. Increase timeout in @Test annotation

### Issue: Port Already in Use

**Cause:** API server already running on port 8080.

**Solution:**
```bash
# Change test port
-Dlocal.server.port=8081
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432

      rabbitmq:
        image: rabbitmq:3-management
        ports:
          - 5672:5672

      redis:
        image: redis:7
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run E2E Tests
        run: ./mvnw test -pl javainfohunter-e2e
        env:
          DB_USERNAME: postgres
          DB_PASSWORD: postgres

      - name: Upload Coverage Report
        uses: codecov/codecov-action@v3
```

## Best Practices

1. **Use external services mode on Windows** - Testcontainers has compatibility issues
2. **Seed test data** - Use @BeforeEach to set up test data
3. **Clean up after tests** - Use @AfterAll to remove test data
4. **Use appropriate timeouts** - Don't let tests hang indefinitely
5. **Document disabled tests** - Always add TODO comments with @Disabled
6. **Run tests locally first** - Before pushing to CI/CD

## Next Steps

- [ ] Complete API module implementation
- [ ] Enable all @Disabled tests
- [ ] Achieve 80%+ code coverage
- [ ] Set up CI/CD pipeline
- [ ] Add performance benchmarks
