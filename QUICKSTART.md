# JavaInfoHunter Infrastructure Quick Start

## Setup Infrastructure (2 minutes)

```bash
# 1. Copy environment variables
cp .env.example .env

# 2. Start infrastructure
docker-compose up -d

# 3. Verify services
docker-compose ps
```

## Access Services

- **PostgreSQL**: `localhost:5432` (user: `postgres`, pass: `postgres`)
- **RabbitMQ Management**: http://localhost:15672 (user: `admin`, pass: `admin`)
- **Redis Commander**: http://localhost:8082 (user: `admin`, pass: `admin`)

## Run Application

```bash
# Development mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Access API: http://localhost:8080
# Access Swagger: http://localhost:8080/swagger-ui.html
# Access Actuator: http://localhost:8080/actuator/health
```

## Run Tests

```bash
# Unit tests (no infrastructure needed)
./mvnw test

# Integration tests (requires Docker Compose)
./mvnw test -Drun.integration.tests=true
```

## Stop Infrastructure

```bash
docker-compose down
```

## Full Documentation

See [INFRASTRUCTURE.md](INFRASTRUCTURE.md) for complete guide.

## Implementation Details

See [P1_TASK4_SUMMARY.md](P1_TASK4_SUMMARY.md) for implementation summary.
