# CI/CD Quick Reference Card

## Quick Commands

### Local Testing (Simulate CI)

```bash
# Full CI pipeline simulation
./mvnw clean compile
./mvnw test
./mvnw jacoco:report

# E2E tests with Docker
docker-compose -f docker-compose.test.yml up -d
./mvnw test -pl javainfohunter-e2e -Dtestcontainers.enable=false
docker-compose -f docker-compose.test.yml down

# Performance tests
./mvnw test -pl javainfohunter-e2e -Dtest=VirtualThreadStressTest

# JMeter load test (requires running API)
jmeter -n -t javainfohunter-e2e/src/test/resources/jmeter/JavaInfoHunter.jmx \
  -l results.jtl -e -o report/
```

### GitHub Actions Status

```bash
# List workflow runs
gh run list --workflow=ci.yml
gh run list --workflow=e2e-tests.yml
gh run list --workflow=coverage.yml

# View specific run
gh run view [run-id]

# Watch logs in real-time
gh run watch

# Re-run failed workflow
gh run rerun [run-id]
```

## Workflows at a Glance

### ci.yml (Main Pipeline)
```
Build -> Unit Tests -> Code Quality -> Security Scan -> Status Check
```
- Runs on: All pushes and PRs
- Duration: ~10-15 min
- Required for: PR merge

### e2e-tests.yml (E2E Tests)
```
Smoke Tests (all) -> Full E2E (push/main) -> API Tests (all)
```
- Runs on: Push to main/develop, PRs
- Duration: ~15-20 min
- Services: PostgreSQL, RabbitMQ, Redis

### coverage.yml (Coverage)
```
Test All Modules -> Merge Reports -> Check Thresholds (80%) -> Upload
```
- Runs on: Push to main/develop, PRs
- Duration: ~20 min
- Coverage target: 80%

### performance.yml (Performance)
```
JUnit Tests -> JMeter Tests (daily) -> Regression Check
```
- Runs on: Push to main, PRs to main, daily schedule
- Duration: ~30-45 min
- Regression threshold: 20% degradation

## Environment Variables

### CI Environment
```yaml
JAVA_VERSION: 21
DB_USERNAME: postgres
DB_PASSWORD: postgres
```

### Local Test Environment
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DASHSCOPE_API_KEY=your-key-here
```

## Coverage Commands

```bash
# Generate coverage
./mvnw test jacoco:report

# Check specific module
./mvnw test jacoco:report -pl javainfohunter-ai-service

# Aggregate coverage
./mvnw jacoco:merge-aggregate
./mvnw jacoco:report-aggregate

# View HTML report
open javainfohunter-ai-service/target/site/jacoco/index.html
```

## Troubleshooting

### CI Failures

| Error | Cause | Fix |
|-------|-------|-----|
| Build failed | Compilation error | Fix locally first |
| Test failed | Broken test | Run locally to debug |
| Timeout | Slow test | Increase timeout or optimize |
| OOM | Memory limit | Check for leaks |

### E2E Test Failures

| Error | Cause | Fix |
|-------|-------|-----|
| Connection refused | Service not running | Start Docker services |
| Context load failed | Missing bean | Check module dependencies |
| Timeout | Test data missing | Seed test data |

### Coverage Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Below threshold | New code untested | Add tests for new code |
| Report empty | JaCoCo agent not running | Re-run with jacoco:prepare-agent |
| Merge failed | Conflicting reports | Clean and rebuild |

## CI/CD Best Practices

### Before Pushing
```bash
# 1. Pull latest changes
git pull

# 2. Build locally
./mvnw clean install

# 3. Run tests
./mvnw test

# 4. Check coverage
./mvnw jacoco:check
```

### Writing PR-Friendly Tests
- Keep tests fast (< 30s each)
- Avoid external dependencies
- Use deterministic data
- Clean up resources

### Performance Tests
- Run nightly, not per commit
- Compare against baseline
- Alert on regressions > 20%
- Track trends over time

## Quick Reference Files

| File | Purpose |
|------|---------|
| `.github/workflows/ci.yml` | Main CI pipeline |
| `.github/workflows/e2e-tests.yml` | E2E test pipeline |
| `.github/workflows/coverage.yml` | Coverage reporting |
| `.github/workflows/performance.yml` | Performance tests |
| `docker-compose.test.yml` | Local test services |
| `prometheus-test.yml` | Local monitoring |
| `docs/ci-cd-setup.md` | Full CI/CD guide |
| `javainfohunter-e2e/performance-baseline.md` | Performance targets |

## Common Workflows

### Creating a PR
```bash
# 1. Create feature branch
git checkout -b feature/my-feature

# 2. Make changes and test
./mvnw clean test

# 3. Commit and push
git add .
git commit -m "feat: my feature"
git push -u origin feature/my-feature

# 4. Create PR via GitHub
gh pr create --title "feat: my feature" --body "Description"
```

### Updating After Review
```bash
# 1. Make requested changes
./mvnw test  # Verify tests pass

# 2. Commit to branch
git add .
git commit -m "fix: address review comments"
git push

# 3. PR updates automatically
```

### Emergency Rollback
```bash
# 1. Revert commit
git revert HEAD

# 2. Push revert
git push

# 3. Monitor CI/CD for green status
gh run watch
```

---

**Last Updated:** 2026-03-07
**For detailed documentation:** See [CI/CD Setup Guide](./ci-cd-setup.md)
