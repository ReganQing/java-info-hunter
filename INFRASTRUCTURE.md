# Infrastructure Setup Guide

This document explains how to set up and run the JavaInfoHunter infrastructure using Docker Compose.

## Prerequisites

- Docker 24.0+
- Docker Compose 2.20+
- Maven 3.8+
- Java 21+

## Quick Start

### 1. Start Infrastructure

```bash
# Start all services
docker-compose up -d

# Start with monitoring (Prometheus + Grafana)
docker-compose --profile monitoring up -d

# Verify services are running
docker-compose ps
```

### 2. Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432` | User: `postgres`, Password: `postgres` (default) |
| RabbitMQ Management | http://localhost:15672 | User: `admin`, Password: `admin` (default) |
| Redis | `localhost:6379` | Password: `redis123` (default) |
| Redis Commander | http://localhost:8082 | User: `admin`, Password: `admin` (default) |
| Prometheus | http://localhost:9090 | (monitoring profile only) |
| Grafana | http://localhost:3000 | User: `admin`, Password: `admin` (default) |

### 3. Configure Environment Variables

```bash
# Copy example environment file
cp .env.example .env

# Edit with your credentials
nano .env
```

### 4. Run Application

```bash
# Development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Staging profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging

# Production profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Services

### PostgreSQL 16 + pgvector

- **Port**: 5432
- **Database**: `javainfohunter`
- **Features**:
  - pgvector extension for vector operations
  - Automatic initialization script
  - Health checks

### RabbitMQ 3 Management

- **Ports**: 5672 (AMQP), 15672 (Management UI)
- **Features**:
  - Management UI for monitoring
  - Default vhost: `/`
  - Health checks

### Redis 7

- **Port**: 6379
- **Features**:
  - Persistence enabled (AOF)
  - Password protection
  - Health checks

### Redis Commander

- **Port**: 8082
- **Features**:
  - Web UI for Redis management
  - Authentication
  - Depends on Redis

### Prometheus (Optional)

- **Port**: 9090
- **Features**:
  - Metrics scraping from Spring Boot Actuator
  - 15s scrape interval
  - Requires monitoring profile

### Grafana (Optional)

- **Port**: 3000
- **Features**:
  - Metrics visualization
  - Pre-configured Prometheus datasource
  - Requires monitoring profile

## Environment Profiles

### Development (dev)

- DDL auto-update enabled
- SQL logging enabled
- Flyway disabled
- Detailed logging
- All actuator endpoints exposed

### Staging

- DDL validation only
- SQL logging disabled
- Flyway enabled
- Production-like logging
- Limited actuator endpoints
- Prometheus metrics enabled

### Production

- DDL validation only
- SQL logging disabled
- Flyway enabled
- Optimized connection pools
- File logging with rotation
- Limited actuator endpoints
- Prometheus metrics enabled
- Graceful shutdown enabled
- API documentation disabled

## Testing

### Run Unit Tests

```bash
./mvnw test
```

### Run Integration Tests

```bash
# Start infrastructure first
docker-compose up -d

# Run integration tests
./mvnw test -Drun.integration.tests=true

# Stop infrastructure
docker-compose down
```

### Test Coverage

```bash
./mvnw test jacoco:report
```

View report: `target/site/jacoco/index.html`

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs <service-name>

# Restart service
docker-compose restart <service-name>

# Rebuild and restart
docker-compose up -d --build <service-name>
```

### Port Conflicts

Edit ports in `docker-compose.yml`:

```yaml
services:
  postgres:
    ports:
      - "5433:5432"  # Change to 5433
```

### Database Connection Issues

1. Verify PostgreSQL is running:
   ```bash
   docker-compose ps postgres
   ```

2. Check connection:
   ```bash
   docker-compose exec postgres pg_isready -U postgres
   ```

3. View logs:
   ```bash
   docker-compose logs postgres
   ```

### Redis Connection Issues

1. Verify Redis is running:
   ```bash
   docker-compose ps redis
   ```

2. Test connection:
   ```bash
   docker-compose exec redis redis-cli ping
   ```

3. Check Redis Commander:
   - Open http://localhost:8082
   - Login with credentials from `.env`

### Clear All Data

```bash
# Stop and remove containers, volumes, and images
docker-compose down -v

# Start fresh
docker-compose up -d
```

## Maintenance

### Backup PostgreSQL

```bash
docker-compose exec postgres pg_dump -U postgres javainfohunter > backup.sql
```

### Restore PostgreSQL

```bash
cat backup.sql | docker-compose exec -T postgres psql -U postgres javainfohunter
```

### View RabbitMQ Queues

1. Open http://localhost:15672
2. Login with credentials
3. Navigate to Queues tab

### Monitor Redis Usage

1. Open http://localhost:8082
2. Login with credentials
3. Browse keys and inspect values

## Performance Tuning

### PostgreSQL

Edit connection pool in `application-{profile}.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Increase for high load
      minimum-idle: 20
```

### Redis

Edit pool configuration in `application-{profile}.yml`:

```yaml
spring:
  data:
    redis:
      jedis:
        pool:
          max-active: 50  # Increase for high load
          max-idle: 20
```

### RabbitMQ

Tune in `docker-compose.yml`:

```yaml
rabbitmq:
  environment:
    RABBITMQ_VM_MEMORY_HIGH_WATERMARK: 0.6  # 60% memory limit
```

## Security

### Change Default Passwords

Always change default passwords in `.env`:

```bash
# Generate strong passwords
POSTGRES_PASSWORD=$(openssl rand -base64 32)
RABBITMQ_PASSWORD=$(openssl rand -base64 32)
REDIS_PASSWORD=$(openssl rand -base64 32)
```

### Network Isolation

Services communicate via `javainfohunter-network` bridge network.

### TLS/SSL

For production, enable TLS for:
- PostgreSQL (port 5432)
- RabbitMQ (ports 5671, 15671)
- Redis (port 6380 with stunnel)

## Monitoring

### Prometheus Metrics

Access Spring Boot Actuator metrics:

```
http://localhost:8080/actuator/prometheus
```

### Grafana Dashboards

1. Open http://localhost:3000
2. Login with credentials
3. Import dashboards from `grafana/provisioning/dashboards/`

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Individual service health
docker-compose ps
```

## Cleanup

### Stop Services

```bash
docker-compose down
```

### Remove Volumes

```bash
docker-compose down -v
```

### Remove Everything

```bash
docker-compose down -v --remove-orphans
docker system prune -a
```

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [RabbitMQ Documentation](https://www.rabbitmq.com/docs)
- [Redis Documentation](https://redis.io/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
