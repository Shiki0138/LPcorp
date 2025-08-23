# Enterprise System Docker Infrastructure

This directory contains the Docker infrastructure for the enterprise microservices system.

## Directory Structure

```
infrastructure/
├── docker/
│   ├── java-base/          # Base Java image with JVM optimizations
│   └── node-base/          # Base Node.js image for frontend services
├── scripts/
│   ├── build-all.sh        # Build all Docker images
│   ├── push-to-registry.sh # Push images to registry
│   └── tag-release.sh      # Tag images for release
└── monitoring/             # Monitoring stack configurations
```

## Quick Start

### 1. Build All Images

```bash
# Build all images with default settings
./infrastructure/scripts/build-all.sh

# Build with custom registry
DOCKER_REGISTRY=myregistry.com ./infrastructure/scripts/build-all.sh

# Build with specific version
VERSION=1.0.0 ./infrastructure/scripts/build-all.sh

# Build sequentially (instead of parallel)
BUILD_PARALLEL=false ./infrastructure/scripts/build-all.sh
```

### 2. Run Local Development Stack

```bash
# Start all services
docker-compose up -d

# Start specific services
docker-compose up -d postgres redis kafka api-gateway

# View logs
docker-compose logs -f user-service

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### 3. Run Tests

```bash
# Run integration tests
docker-compose -f docker-compose.test.yml up --abort-on-container-exit

# Run specific test suite
docker-compose -f docker-compose.test.yml up integration-test-runner

# Clean up test environment
docker-compose -f docker-compose.test.yml down -v
```

### 4. Deploy Monitoring Stack

```bash
# Start monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Access monitoring UIs:
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000 (admin/admin)
# - Kibana: http://localhost:5601
# - Jaeger: http://localhost:16686
```

## Base Images

### Java Base Image

The Java base image includes:
- OpenJDK 17 (Eclipse Temurin)
- Alpine Linux for minimal size
- JVM optimizations for containers
- Non-root user (appuser:appgroup)
- Health check support
- OpenTelemetry Java agent

JVM optimizations included:
- Container-aware memory settings
- G1GC garbage collector
- String deduplication
- Optimized TLS settings

### Node Base Image

The Node base image includes:
- Node.js 18 LTS
- Alpine Linux for minimal size
- PM2 for process management
- Non-root user (appuser:appgroup)
- Health check support

## Service Dockerfiles

Each microservice has its own Dockerfile with:
- Multi-stage builds for smaller images
- Service-specific entry points
- Dependency waiting logic
- Health checks
- JMX ports for monitoring

## Environment Variables

### Common Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `SERVER_PORT` | Service port | Service-specific |
| `JAVA_OPTS_APPEND` | Additional JVM options | - |

### Database Connections

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `postgres` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | Service-specific |
| `DB_USER` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `MONGO_HOST` | MongoDB host | `mongodb` |
| `MONGO_PORT` | MongoDB port | `27017` |

### Message Queue Connections

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `kafka:9092` |
| `RABBITMQ_HOST` | RabbitMQ host | `rabbitmq` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `REDIS_HOST` | Redis host | `redis` |
| `REDIS_PORT` | Redis port | `6379` |

## Security Considerations

1. **Non-root Users**: All containers run as non-root users
2. **Network Isolation**: Services use Docker networks for isolation
3. **Secrets Management**: Use Docker secrets or external secret managers
4. **TLS/SSL**: Enable TLS for all external communications
5. **Image Scanning**: Run security scans on all images

## Performance Optimization

1. **Multi-stage Builds**: Reduces image size
2. **Layer Caching**: Optimized Dockerfile ordering
3. **JVM Tuning**: Container-aware JVM settings
4. **Resource Limits**: Set appropriate CPU/memory limits
5. **Health Checks**: Ensures service availability

## Monitoring and Observability

The monitoring stack includes:

- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **ELK Stack**: Log aggregation
- **Jaeger**: Distributed tracing
- **APM Server**: Application performance monitoring

### Metrics Endpoints

All services expose metrics at:
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/health` - Health check
- `/actuator/info` - Service information

## Deployment

### Local Development

```bash
# Use docker-compose.yml
docker-compose up -d
```

### Testing

```bash
# Use docker-compose.test.yml
docker-compose -f docker-compose.test.yml up
```

### Production

```bash
# Use environment-specific compose files
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Or use Kubernetes manifests
kubectl apply -f k8s/
```

## Troubleshooting

### Common Issues

1. **Container fails to start**
   ```bash
   # Check logs
   docker-compose logs service-name
   
   # Check health status
   docker-compose ps
   ```

2. **Database connection issues**
   ```bash
   # Verify database is running
   docker-compose ps postgres
   
   # Check connectivity
   docker-compose exec service-name nc -zv postgres 5432
   ```

3. **Memory issues**
   ```bash
   # Check container resources
   docker stats
   
   # Adjust JVM memory
   JAVA_OPTS_APPEND="-Xmx1g" docker-compose up service-name
   ```

### Debugging

```bash
# Execute shell in container
docker-compose exec service-name sh

# View JVM settings
docker-compose exec service-name java -XX:+PrintFlagsFinal -version

# Check network connectivity
docker-compose exec service-name ping other-service
```

## Best Practices

1. **Image Tagging**: Always tag images with version numbers
2. **Resource Limits**: Set CPU and memory limits in production
3. **Logging**: Use structured JSON logging
4. **Secrets**: Never hardcode secrets in images
5. **Updates**: Regularly update base images for security patches

## CI/CD Integration

### GitLab CI Example

```yaml
build:
  stage: build
  script:
    - ./infrastructure/scripts/build-all.sh
  tags:
    - docker

push:
  stage: deploy
  script:
    - ./infrastructure/scripts/push-to-registry.sh
  only:
    - main
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './infrastructure/scripts/build-all.sh'
            }
        }
        stage('Test') {
            steps {
                sh 'docker-compose -f docker-compose.test.yml up --abort-on-container-exit'
            }
        }
        stage('Push') {
            when { branch 'main' }
            steps {
                sh './infrastructure/scripts/push-to-registry.sh'
            }
        }
    }
}
```

## Maintenance

### Updating Base Images

1. Update Dockerfile with new base image version
2. Rebuild all dependent images
3. Test thoroughly before deploying

### Cleaning Up

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Complete cleanup
docker system prune -a --volumes
```

## Support

For issues and questions:
1. Check service logs: `docker-compose logs service-name`
2. Review health checks: `docker-compose ps`
3. Consult service documentation in `/docs`