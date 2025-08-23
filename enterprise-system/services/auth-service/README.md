# AuthService

## Overview
Authentication and authorization service

## API Endpoints
See Swagger UI at http://localhost:8086/swagger-ui.html

## Configuration

### Environment Variables
```bash
# Server
SERVER_PORT=8086

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=authdb
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672

# Eureka
EUREKA_SERVER=http://localhost:8761/eureka/
```

## Running Locally

### Prerequisites
- Java 17
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- Apache Kafka
- RabbitMQ

### Run
```bash
mvn spring-boot:run
```

## Building

### Build JAR:
```bash
mvn clean package
```

### Build Docker image:
```bash
docker build -t auth-serviceatest .
```

## Monitoring
- Health: http://localhost:8086/actuator/health
- Metrics: http://localhost:8086/actuator/metrics
- API Docs: http://localhost:8086/v3/api-docs
