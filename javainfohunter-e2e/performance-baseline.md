# Performance Baseline Documentation

## Overview

This document defines the performance baselines for the JavaInfoHunter system. These baselines are used to detect performance regressions in CI/CD pipelines.

## Test Environment

**Baseline Configuration:**
- **Hardware:** GitHub Actions ubuntu-latest (2-core CPU, 7 GB RAM)
- **Java:** OpenJDK 21 (Temurin)
- **Database:** PostgreSQL 15 (Alpine)
- **Message Queue:** RabbitMQ 3.12 (Alpine)
- **Cache:** Redis 7 (Alpine)
- **Application:** JavaInfoHunter API

## Baseline Metrics

### 1. API Response Times

| Endpoint | P50 (ms) | P95 (ms) | P99 (ms) | Target |
|----------|----------|----------|----------|--------|
| GET /api/v1/rss-sources | 50 | 150 | 300 | P95 < 200 |
| POST /api/v1/rss-sources | 100 | 250 | 500 | P95 < 300 |
| GET /api/v1/rss-sources/{id} | 30 | 100 | 200 | P95 < 150 |
| PUT /api/v1/rss-sources/{id} | 100 | 250 | 500 | P95 < 300 |
| DELETE /api/v1/rss-sources/{id} | 50 | 150 | 300 | P95 < 200 |
| GET /api/v1/news | 100 | 300 | 600 | P95 < 400 |
| GET /api/v1/news/{id} | 30 | 100 | 200 | P95 < 150 |
| GET /api/v1/news/search | 200 | 500 | 1000 | P95 < 600 |

### 2. Throughput Metrics

| Scenario | Concurrent Users | Requests/sec | Success Rate | Target |
|----------|------------------|--------------|--------------|--------|
| RSS CRUD | 100 | > 500 | > 99% | > 500 req/s |
| News Query | 500 | > 1000 | > 99% | > 1000 req/s |
| Search | 200 | > 300 | > 98% | > 300 req/s |
| Mixed Load | 800 | > 800 | > 98% | > 800 req/s |

### 3. Virtual Thread Performance Test

**Test: VirtualThreadStressTest.java**

| Metric | Target | Baseline | Date |
|--------|--------|----------|------|
| 1,000 RSS Source Creations | < 30s | TBD | TBD |
| 10,000 News Queries | < 30s | TBD | TBD |
| 5,000 Search Operations | < 30s | TBD | TBD |
| Total Throughput | > 500 req/s | TBD | TBD |

### 4. JMeter Load Test

**Test: JavaInfoHunter.jmx**

| Thread Group | Threads | Loops | Total Requests | Duration (target) |
|--------------|---------|-------|----------------|-------------------|
| RSS CRUD | 100 | 10 | 1,000 | < 30s |
| News Query | 500 | 20 | 10,000 | < 60s |
| Search | 200 | 25 | 5,000 | < 45s |

## Regression Thresholds

A regression is flagged when:

| Metric | Degradation Threshold |
|--------|----------------------|
| P95 Latency | > 20% increase |
| P99 Latency | > 25% increase |
| Throughput | > 15% decrease |
| Error Rate | > 0.5% absolute increase |
| Success Rate | < 98% (from 99%+) |

## Performance Targets by Priority

### High Priority (Must Meet)
- P95 latency for all CRUD operations < 300ms
- Throughput for read operations > 500 req/s
- Success rate > 99%
- Error rate < 1%

### Medium Priority (Should Meet)
- P95 latency for search < 600ms
- Throughput for mixed load > 800 req/s
- P99 latency < 2x P95 latency

### Low Priority (Nice to Have)
- Cold start time < 5 seconds
- Memory footprint < 512MB per instance
- CPU utilization < 70% at target throughput

## Establishing Baseline

### Step 1: Prepare Test Environment

```bash
# Start test services
docker-compose -f docker-compose.test.yml up -d

# Verify services are healthy
docker-compose -f docker-compose.test.yml ps
```

### Step 2: Seed Test Data

```bash
# Run data seeding script
./mvnw spring-boot:run -pl javainfohunter-api \
  -Dspring-boot.run.arguments="--seedTestData=true"
```

### Step 3: Run Performance Tests

```bash
# JUnit performance tests
./mvnw test -pl javainfohunter-e2e -Dtest=VirtualThreadStressTest

# JMeter load tests
jmeter -n -t javainfohunter-e2e/src/test/resources/jmeter/JavaInfoHunter.jmx \
  -l baseline-results.jtl -e -o baseline-report/
```

### Step 4: Capture Baseline Metrics

```bash
# Extract metrics from JMeter results
jmeter-plugins/bin/JMeterPluginsCMD.sh \
  --generate-csv baseline-metrics.csv \
  --input-jtl baseline-results.jtl \
  --plugin-type AggregateReport
```

### Step 5: Store Baseline

```bash
# Create baseline directory
mkdir -p javainfohunter-e2e/baselines/$(date +%Y%m%d)

# Copy results
cp baseline-results.jtl javainfohunter-e2e/baselines/$(date +%Y%m%d)/
cp baseline-metrics.csv javainfohunter-e2e/baselines/$(date +%Y%m%d)/
cp baseline-report/* javainfohunter-e2e/baselines/$(date +%Y%m%d)/
```

## Monitoring Performance Trends

### Weekly Performance Report

Generate weekly performance trend report:

```bash
# Run all performance tests
./mvnw test -pl javainfohunter-e2e -Dtest=VirtualThreadStressTest
jmeter -n -t javainfohunter-e2e/src/test/resources/jmeter/JavaInfoHunter.jmx \
  -l weekly-results.jtl -e -o weekly-report/

# Compare with baseline
diff baseline-metrics.csv weekly-metrics.csv
```

### CI/CD Integration

Performance tests run automatically:
- On every push to main branch
- Daily at 2 AM UTC
- On PRs to main (comparison only, no blocking)

## Performance Optimization Checklist

### Database Optimization
- [ ] Connection pool size tuned (target: 20-50 connections)
- [ ] Indexes created on frequently queried columns
- [ ] Query plans analyzed with EXPLAIN
- [ ] N+1 query issues resolved
- [ ] Prepared statements used

### Caching Strategy
- [ ] Redis cache configured for hot data
- [ ] Cache TTL set appropriately (5-15 minutes)
- [ ] Cache invalidation strategy defined
- [ ] Cache hit rate monitored (> 80% target)

### API Optimization
- [ ] Pagination implemented for list endpoints
- [ ] Field selection/projection available
- [ ] Compression enabled for large payloads
- [ ] Rate limiting configured

### Virtual Thread Optimization
- [ ] Virtual threads enabled for I/O operations
- [ ] Thread pool sizes configured
- [ ] Blocking operations minimized
- [ ] Synchronized blocks avoided

## Performance Testing Best Practices

### 1. Test Realistic Scenarios
- Use production-like data volumes
- Simulate realistic user behavior
- Include think time between requests

### 2. Test Incrementally
- Start with single user, increase gradually
- Identify breaking point
- Test at 150% of expected peak load

### 3. Measure the Right Metrics
- Focus on user-perceived latency (P95, P99)
- Monitor error rates, not just throughput
- Track resource utilization

### 4. Isolate Test Environment
- Use dedicated test infrastructure
- Avoid shared resources
- Ensure consistent test conditions

### 5. Automate Regression Detection
- Compare against baseline on every PR
- Alert on significant regressions
- Track trends over time

## Troubleshooting Performance Issues

### High Latency

**Symptoms:** P95/P99 latency exceeds targets

**Investigation:**
1. Check database query performance
2. Review thread dumps for blocked threads
3. Analyze GC logs for pause times
4. Check network latency

**Solutions:**
- Add database indexes
- Tune connection pool
- Optimize queries
- Increase cache hit rate

### Low Throughput

**Symptoms:** Requests/sec below target

**Investigation:**
1. Check CPU utilization
2. Monitor thread pool exhaustion
3. Review database connection pool
4. Check for lock contention

**Solutions:**
- Scale horizontally (more instances)
- Tune thread pools
- Optimize critical path
- Use async processing

### High Error Rate

**Symptoms:** Error rate > 1%

**Investigation:**
1. Check application logs
2. Monitor database connections
3. Review external service health
4. Check resource limits

**Solutions:**
- Fix NPEs and unhandled exceptions
- Increase connection pool
- Add retry logic
- Implement circuit breakers

## Resources

- [JavaInfoHunter JMeter Test Plan](./src/test/resources/jmeter/JavaInfoHunter.jmx)
- [Virtual Thread Performance Test](./src/test/java/com/ron/javainfohunter/e2e/performance/VirtualThreadStressTest.java)
- [Spring Boot Performance Tuning](https://spring.io/guides/gs/performance/)
- [JMeter Performance Testing](https://jmeter.apache.org/usermanual/index.html)

## Appendix: Performance Test Templates

### JMeter Test Configuration Template

```xml
<!-- Thread Group Configuration -->
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">${__P(users,100)}</stringProp>
  <stringProp name="ThreadGroup.ramp_time">${__P(ramp,10)}</stringProp>
  <stringProp name="ThreadGroup.duration">${__P(duration,60)}</stringProp>
</ThreadGroup>
```

### Performance Test Java Template

```java
@Test
@DisplayName("Should meet throughput target")
void shouldMeetThroughputTarget() {
    int targetThroughput = 500; // requests per second
    int testDuration = 30; // seconds

    // Run test
    PerformanceMetrics metrics = runLoadTest(targetThroughput, testDuration);

    // Assert
    assertThat(metrics.getThroughput())
        .isGreaterThan(targetThroughput);
    assertThat(metrics.getP95Latency())
        .isLessThan(Duration.ofMillis(300));
}
```

---

**Document Version:** 1.0
**Last Updated:** 2026-03-07
**Next Review:** 2026-04-07
