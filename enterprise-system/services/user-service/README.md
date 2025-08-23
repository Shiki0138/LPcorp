# User Service

## Overview
The User Service is a microservice responsible for managing user accounts, authentication, and authorization in the enterprise system.

## Features
- User registration and management
- Email and phone verification
- Role-based access control
- User status management
- Event-driven notifications
- Redis caching for performance
- PostgreSQL for data persistence

## API Endpoints

### User Management
- `POST /api/v1/users` - Create a new user
- `GET /api/v1/users/{id}` - Get user by ID
- `GET /api/v1/users/username/{username}` - Get user by username
- `GET /api/v1/users` - Get all users (paginated)
- `PUT /api/v1/users/{id}` - Update user information
- `DELETE /api/v1/users/{id}` - Delete user
- `PATCH /api/v1/users/{id}/status` - Update user status
- `POST /api/v1/users/{id}/verify-email` - Verify user email

## Configuration

### Environment Variables
```bash
# Server
SERVER_PORT=8081

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=userdb
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

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

### Steps
1. Start dependencies:
   ```bash
   docker-compose up -d postgres redis kafka
   ```

2. Run database migrations:
   ```bash
   mvn flyway:migrate
   ```

3. Run the service:
   ```bash
   mvn spring-boot:run
   ```

## Testing

### Run unit tests:
```bash
mvn test
```

### Run integration tests:
```bash
mvn verify
```

## Building

### Build JAR:
```bash
mvn clean package
```

### Build Docker image:
```bash
docker build -t user-service:latest .
```

## Monitoring

### Health Check
- `GET /actuator/health`

### Metrics
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

### API Documentation
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8081/v3/api-docs`

## Events Published
- `user.created` - When a new user is created
- `user.updated` - When user information is updated
- `user.deleted` - When a user is deleted
- `user.status.changed` - When user status changes
- `user.email.verified` - When email is verified

## Security
- BCrypt password hashing
- JWT token authentication (to be implemented)
- Role-based access control
- Input validation and sanitization

## Performance
- Redis caching for frequently accessed data
- Database connection pooling with HikariCP
- Asynchronous event publishing
- Optimized JPA queries

## Dependencies
- Spring Boot 3.2
- Spring Security
- Spring Data JPA
- Spring Cloud Netflix Eureka
- PostgreSQL Driver
- Redis
- Apache Kafka
- Flyway
- MapStruct
- Lombok