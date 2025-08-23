# API Gateway

## Overview
The API Gateway is the single entry point for all client requests to the microservices architecture. It provides routing, load balancing, circuit breaking, rate limiting, and security features.

## Features
- Dynamic routing with Spring Cloud Gateway
- Service discovery with Eureka
- Load balancing across service instances
- Circuit breaker pattern with Resilience4j
- Rate limiting with Redis
- Request/Response logging
- CORS configuration
- Authentication and authorization (to be implemented)
- Request/Response transformation
- Metrics and monitoring

## Architecture

### Routes Configuration
The gateway routes requests to the following services:
- `/api/v1/users/**` → User Service (port 8081)
- `/api/v1/orders/**` → Order Service (port 8082)
- `/api/v1/inventory/**` → Inventory Service (port 8083)
- `/api/v1/payments/**` → Payment Service (port 8084)
- `/api/v1/notifications/**` → Notification Service (port 8085)
- `/api/v1/auth/**` → Auth Service (port 8086)
- `/api/v1/analytics/**` → Analytics Service (port 8087)
- `/api/v1/reports/**` → Reporting Service (port 8088)

## Configuration

### Environment Variables
```bash
# Server
SERVER_PORT=8080

# Eureka
EUREKA_SERVER=http://localhost:8761/eureka/

# Redis (for rate limiting)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

## Running Locally

### Prerequisites
- Java 17
- Maven 3.8+
- Redis (for rate limiting)
- Eureka Server running
- At least one microservice running

### Steps
1. Start Eureka Server
2. Start Redis:
   ```bash
   docker run -d -p 6379:6379 redis:alpine
   ```
3. Run the gateway:
   ```bash
   mvn spring-boot:run
   ```

## Rate Limiting

The gateway implements rate limiting using Redis. Default limits:
- Replenish Rate: 10 requests/second
- Burst Capacity: 20 requests

Rate limiting can be configured per route or globally.

## Circuit Breaker

Resilience4j circuit breaker configuration:
- Sliding Window Size: 10
- Failure Rate Threshold: 50%
- Wait Duration in Open State: 5 seconds
- Minimum Number of Calls: 5

## Security

### CORS Configuration
- Allowed Origins: Configurable per environment
- Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed Headers: All headers
- Allow Credentials: true

### Authentication (To be implemented)
- JWT token validation
- OAuth2 integration
- API key authentication

## Monitoring

### Health Check
- `GET /actuator/health`

### Metrics
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

### Gateway Routes
- `GET /actuator/gateway/routes`
- `GET /actuator/gateway/globalfilters`
- `GET /actuator/gateway/routefilters`

## Custom Filters

### Global Filters
1. **LoggingGlobalFilter**: Logs all incoming requests and outgoing responses
2. **AuthenticationFilter**: Validates JWT tokens (to be implemented)
3. **RequestIdFilter**: Adds unique request ID to headers (to be implemented)

### Route Filters
1. **CircuitBreaker**: Implements circuit breaker pattern
2. **RequestRateLimiter**: Rate limiting per user/IP/API key
3. **RewritePath**: Rewrites request paths
4. **AddResponseHeader**: Adds custom response headers

## Performance Tuning

### JVM Options
```bash
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0
-XX:+UseG1GC
-XX:+UseStringDeduplication
```

### Netty Configuration
- Use native transport when available
- Configure connection pool size
- Tune worker threads

## Troubleshooting

### Common Issues

1. **Service not found**
   - Ensure service is registered with Eureka
   - Check service name matches route configuration

2. **Rate limit exceeded**
   - Check Redis connection
   - Verify rate limit configuration

3. **Circuit breaker open**
   - Check downstream service health
   - Review circuit breaker thresholds

### Debug Logging
Enable debug logging:
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: DEBUG
```

## Building

### Build JAR:
```bash
mvn clean package
```

### Build Docker image:
```bash
docker build -t api-gateway:latest .
```

## Deployment

### Docker Compose
```yaml
version: '3.8'
services:
  api-gateway:
    image: api-gateway:latest
    ports:
      - "8080:8080"
    environment:
      - EUREKA_SERVER=http://eureka:8761/eureka/
      - REDIS_HOST=redis
    depends_on:
      - eureka
      - redis
```

### Kubernetes
See `/infrastructure/kubernetes/api-gateway-deployment.yaml`