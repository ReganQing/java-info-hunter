# Production Integration Tests Guide

This document describes how to run the production-level integration tests for JavaInfoHunter modules.

## Overview

The production integration tests verify end-to-end functionality of all modules:

| Module | Test Class | Coverage |
|--------|------------|----------|
| **AI Service** | `AIServiceProductionIntegrationTest` | ChatService, EmbeddingService, Agent orchestration, database persistence |
| **Crawler** | `CrawlerProductionIntegrationTest` | RSS feed crawling, content extraction, deduplication, RabbitMQ publishing |
| **Processor** | `ProcessorProductionIntegrationTest` | Message consumption, AI agent processing, result aggregation, transactions |
| **API** | `ApiProductionIntegrationTest` | REST endpoints, validation, error handling, response structure |

## Prerequisites

### Required Environment Variables

All tests require the following environment variables:

```bash
# Database credentials (PostgreSQL in Testcontainers)
export DB_USERNAME=test
export DB_PASSWORD=test

# RabbitMQ credentials (RabbitMQ in Testcontainers)
export RABBITMQ_USERNAME=test
export RABBITMQ_PASSWORD=test

# Alibaba DashScope API Key (for AI tests)
export DASHSCOPE_API_KEY=your-dashscope-api-key-here
```

### Docker Requirement

All tests use **Testcontainers** which require Docker to be running:

```bash
# Verify Docker is running
docker ps
```

## Running the Tests

### Run All Production Tests

```bash
# From project root
mvnw.cmd test -Drun.production.tests=true
```

### Run Specific Module Tests

```bash
# AI Service tests only
mvnw.cmd test -Dtest=AIServiceProductionIntegrationTest -Drun.production.tests=true -pl javainfohunter-ai-service

# Crawler tests only
mvnw.cmd test -Dtest=CrawlerProductionIntegrationTest -Drun.production.tests=true -pl javainfohunter-crawler

# Processor tests only
mvnw.cmd test -Dtest=ProcessorProductionIntegrationTest -Drun.production.tests=true -pl javainfohunter-processor

# API tests only
mvnw.cmd test -Dtest=ApiProductionIntegrationTest -Drun.production.tests=true -pl javainfohunter-api
```

### Alternative: Pass API Key as System Property

```bash
mvnw.cmd test -Drun.production.tests=true -Ddashscope.api.key=your-key-here
```

## Test Configuration

### Testcontainers Configuration

Tests use the following container images:

| Service | Image | Purpose |
|---------|-------|---------|
| PostgreSQL | `postgres:16-alpine` | Database persistence |
| RabbitMQ | `rabbitmq:3.12-alpine` | Message queue |
| Redis | `redis:7-alpine` | Caching (API tests) |

### Test Profiles

All tests run with the `test` profile. Configuration is provided via `@DynamicPropertySource` to override Testcontainers connection details.

## What Each Test Verifies

### AI Service Tests (`AIServiceProductionIntegrationTest`)

1. **ChatService Integration**
   - Message sending and response handling
   - Empty message handling
   - Timeout handling

2. **EmbeddingService Integration**
   - Non-zero vector generation
   - Consistency for same input
   - Different vectors for different inputs
   - Cosine similarity calculation

3. **Agent Orchestration**
   - Chain pattern execution
   - Parallel pattern execution
   - Master-Worker pattern execution
   - Missing agent error handling

4. **AgentManager**
   - Agent registration/retrieval
   - Agent listing
   - Agent unregistration
   - Agent ID validation

5. **Database Persistence**
   - Agent execution record storage
   - Query by agent ID
   - Query by status

### Crawler Tests (`CrawlerProductionIntegrationTest`)

1. **RSS Feed Crawling**
   - Real RSS feed crawling (NYTimes, Wired, BBC, etc.)
   - Invalid URL handling
   - Content extraction

2. **Content Deduplication**
   - Duplicate detection via SHA-256 hash
   - Consistent hash generation
   - Batch hash lookup

3. **Message Publishing**
   - Single message publishing to RabbitMQ
   - Batch message publishing
   - Failure handling

4. **CrawlCoordinator**
   - Single source synchronous crawling
   - All active sources crawling
   - Statistics retrieval

5. **Error Handling & Retry**
   - Failed crawl counter increment
   - Inactive status after max retries
   - Success rate calculation

### Processor Tests (`ProcessorProductionIntegrationTest`)

1. **Content Routing**
   - Message routing to enabled agents
   - Null message handling
   - Result retrieval by hash
   - Timeout handling

2. **Agent Processing**
   - Summary agent processing
   - Analysis agent (sentiment) processing
   - Classification agent processing
   - Parallel execution

3. **Result Aggregation**
   - Multi-agent result aggregation
   - News entity creation from results

4. **Database Persistence**
   - News entity storage
   - News-to-RawContent linking
   - Agent execution records
   - Processing status updates

5. **Message Consumption**
   - JSON deserialization
   - Optional field handling

6. **Transaction Tests**
   - Rollback on failure

### API Tests (`ApiProductionIntegrationTest`)

1. **Health Check**
   - Actuator endpoint health status

2. **RSS Source API**
   - Create RSS source (with validation)
   - Get sources (pagination, filters)
   - Get source by ID
   - Update source
   - Delete source
   - Trigger manual crawl

3. **News API**
   - Get news list (pagination, filters)
   - Filter by category/sentiment
   - Get news by ID
   - Search news
   - Get trending news
   - Get similar news
   - Get news by category path

4. **Agent API**
   - Get agent statistics
   - Get execution history

5. **Admin API**
   - Get system status

6. **Response Structure**
   - Consistent API response format
   - Error details inclusion

7. **Validation**
   - Page size limits
   - Negative page rejection
   - URL format validation

## Continuous Integration

These tests are designed to run in CI/CD pipelines. Example GitHub Actions configuration:

```yaml
name: Production Integration Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:

jobs:
  integration-tests:
    runs-on: ubuntu-latest

    services:
      docker:
        image: docker:24-dind

    env:
      DB_USERNAME: test
      DB_PASSWORD: test
      RABBITMQ_USERNAME: test
      RABBITMQ_PASSWORD: test
      DASHSCOPE_API_KEY: ${{ secrets.DASHSCOPE_API_KEY }}

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run production integration tests
        run: mvnw.cmd test -Drun.production.tests=true
```

## Troubleshooting

### Docker Not Available

```
Error: Cannot connect to Docker daemon
```

**Solution**: Start Docker Desktop or Docker daemon.

### API Key Not Set

```
Error: DASHSCOPE_API_KEY not configured
```

**Solution**: Set the environment variable or system property.

### Timeout Errors

Tests may timeout if:
- Network is slow (affecting real RSS feed crawling)
- AI API is slow
- Testcontainers startup is slow

**Solution**: Increase timeout in test configuration.

## Notes

1. **Test Isolation**: Each test cleans up its data in `@AfterEach`.
2. **Parallel Execution**: Tests can be run in parallel for faster execution.
3. **Real Services**: Some tests use real public RSS feeds - network access required.
4. **Mocked AI**: AI services can be mocked by setting test API key to "sk-test-key".
