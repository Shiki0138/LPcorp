# Enterprise System - Microservices Architecture

## Overview

This is a comprehensive enterprise system built with microservices architecture using Spring Boot, following the technology decisions outlined in the architecture documentation. The system demonstrates best practices for building scalable, maintainable, and production-ready enterprise applications.

## Architecture

### Technology Stack
- **Programming Language**: Java 17 with Spring Boot 3.2
- **API Framework**: Spring Boot REST + GraphQL
- **Databases**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Message Queue**: Apache Kafka + RabbitMQ
- **Container Orchestration**: Kubernetes
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Monitoring**: Prometheus + Grafana, ELK Stack, Jaeger

### Microservices

1. **API Gateway** (Port 8080)
   - Central entry point for all API requests
   - Route management and load balancing
   - Rate limiting and circuit breaking

2. **User Service** (Port 8081)
   - User registration and management
   - Authentication and authorization
   - Profile management

3. **Order Service** (Port 8082)
   - Order lifecycle management
   - Order processing and tracking
   - Integration with inventory and payment

4. **Inventory Service** (Port 8083)
   - Stock management
   - Product catalog
   - Inventory tracking

5. **Payment Service** (Port 8084)
   - Payment processing
   - Transaction management
   - Payment method integration

6. **Notification Service** (Port 8085)
   - Email notifications
   - SMS notifications
   - Push notifications

7. **Auth Service** (Port 8086)
   - JWT token generation
   - OAuth2 integration
   - Security management

8. **Analytics Service** (Port 8087)
   - Business analytics
   - Real-time metrics
   - Data aggregation

9. **Reporting Service** (Port 8088)
   - Report generation
   - Data export
   - Scheduled reports

## Project Structure

```
enterprise-system/
├── services/                 # Microservices
│   ├── user-service/
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── auth-service/
│   ├── analytics-service/
│   └── reporting-service/
├── api-gateway/             # API Gateway
├── infrastructure/          # Infrastructure as Code
│   ├── docker/             # Docker configurations
│   ├── kubernetes/         # K8s manifests
│   ├── terraform/          # Terraform modules
│   └── scripts/            # Utility scripts
├── shared/                  # Shared libraries
│   ├── common-lib/         # Common utilities
│   ├── proto/              # Protocol buffers
│   └── schemas/            # Shared schemas
├── monitoring/              # Monitoring configs
├── tests/                   # Integration tests
│   ├── integration/
│   ├── performance/
│   └── e2e/
└── docs/                    # Documentation
```

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker and Docker Compose
- Kubernetes (optional for production deployment)

### Running with Docker Compose

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd enterprise-system
   ```

2. Build all services:
   ```bash
   mvn clean package -DskipTests
   ```

3. Start the infrastructure:
   ```bash
   docker-compose up -d postgres redis kafka rabbitmq
   ```

4. Start the services:
   ```bash
   docker-compose up -d
   ```

5. Access the services:
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - RabbitMQ Management: http://localhost:15672
   - Grafana: http://localhost:3000
   - Prometheus: http://localhost:9090
   - Kibana: http://localhost:5601
   - Jaeger: http://localhost:16686

### Running Individual Services

```bash
# Start a specific service
cd services/user-service
mvn spring-boot:run
```

## Development

### Building the Project

```bash
# Build all modules
mvn clean install

# Build specific service
cd services/user-service
mvn clean package

# Build Docker images
docker-compose build
```

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run specific service tests
cd services/user-service
mvn test
```

### Code Quality

```bash
# Run SonarQube analysis
mvn sonar:sonar \
  -Dsonar.projectKey=enterprise-system \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>

# Check code coverage
mvn jacoco:report
```

## API Documentation

Each service provides Swagger UI for API documentation:

- User Service: http://localhost:8081/swagger-ui.html
- Order Service: http://localhost:8082/swagger-ui.html
- Inventory Service: http://localhost:8083/swagger-ui.html
- Payment Service: http://localhost:8084/swagger-ui.html
- And so on...

## Configuration

### Environment Variables

Common environment variables used across services:

```bash
# Spring Profiles
SPRING_PROFILES_ACTIVE=dev

# Database
DB_HOST=localhost
DB_PORT=5432
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

### Spring Profiles

- `dev`: Development environment with debug logging
- `test`: Test environment with H2 database
- `prod`: Production environment with optimized settings

## Deployment

### Kubernetes Deployment

1. Create namespace:
   ```bash
   kubectl apply -f infrastructure/kubernetes/namespace.yaml
   ```

2. Apply configurations:
   ```bash
   kubectl apply -f infrastructure/kubernetes/
   ```

3. Deploy services:
   ```bash
   kubectl apply -f infrastructure/kubernetes/*-deployment.yaml
   ```

### Production Deployment with Terraform

```bash
cd infrastructure/terraform
terraform init
terraform plan -var="environment=production"
terraform apply -var="environment=production"
```

## Monitoring and Observability

### Metrics
- Prometheus metrics available at `/actuator/prometheus`
- Custom business metrics in each service
- JVM metrics and application metrics

### Logging
- Centralized logging with ELK stack
- Structured JSON logging
- Correlation IDs for request tracing

### Tracing
- Distributed tracing with Jaeger
- OpenTelemetry integration
- Request flow visualization

### Alerts
- Prometheus AlertManager for metric-based alerts
- Custom alert rules for each service
- Integration with notification channels

## Security

### Authentication & Authorization
- JWT-based authentication
- OAuth2 integration support
- Role-based access control (RBAC)
- API key authentication for service-to-service

### Security Best Practices
- HTTPS/TLS encryption
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- CSRF protection
- Rate limiting
- Circuit breakers

## Performance

### Optimization Strategies
- Redis caching for frequently accessed data
- Database connection pooling
- Asynchronous processing with Kafka
- Reactive programming where applicable
- CDN for static assets
- Database query optimization

### Scalability
- Horizontal scaling with Kubernetes
- Auto-scaling based on metrics
- Load balancing across instances
- Database read replicas
- Caching strategies

## Troubleshooting

### Common Issues

1. **Service Discovery Issues**
   - Ensure Eureka server is running
   - Check service registration

2. **Database Connection Issues**
   - Verify database credentials
   - Check network connectivity

3. **Message Queue Issues**
   - Ensure Kafka/RabbitMQ are running
   - Check topic/queue configuration

### Debug Mode

Enable debug logging:
```yaml
logging:
  level:
    com.enterprise: DEBUG
    org.springframework: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request

### Coding Standards
- Follow Java coding conventions
- Write unit tests for new features
- Update documentation
- Ensure code coverage >80%

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

- Documentation: See `/docs` directory
- Issues: GitHub Issues
- Wiki: Project Wiki

## Roadmap

### Phase 1 (Current)
- ✅ Core microservices implementation
- ✅ Basic authentication and authorization
- ✅ Database integration
- ✅ Message queue integration
- ✅ Basic monitoring

### Phase 2 (Q2 2025)
- 🔲 GraphQL API implementation
- 🔲 Advanced caching strategies
- 🔲 Enhanced security features
- 🔲 Performance optimization

### Phase 3 (Q3 2025)
- 🔲 Machine learning integration
- 🔲 Advanced analytics
- 🔲 Multi-region deployment
- 🔲 Advanced monitoring and alerting

### Phase 4 (Q4 2025)
- 🔲 Mobile application support
- 🔲 WebSocket support
- 🔲 Advanced reporting features
- 🔲 Blockchain integration

---

**Built with ❤️ using Spring Boot and modern cloud-native technologies**