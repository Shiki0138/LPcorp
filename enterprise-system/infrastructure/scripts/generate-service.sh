#!/bin/bash

# Function to convert kebab-case to CamelCase
to_camel_case() {
    echo "$1" | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2)}1' | sed 's/ //g'
}

# Function to convert kebab-case to snake_case
to_snake_case() {
    echo "$1" | sed 's/-/_/g'
}

generate_service() {
    SERVICE_NAME=$1
    SERVICE_DESC=$2
    SERVICE_PORT=$3
    DB_NAME=$4
    
    SERVICE_PATH="services/$SERVICE_NAME"
    PACKAGE_NAME=$(to_snake_case $SERVICE_NAME)
    CLASS_NAME=$(to_camel_case $SERVICE_NAME)
    
    echo "Generating $SERVICE_NAME..."
    
    # Create pom.xml
    cat > "$SERVICE_PATH/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>enterprise-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>$SERVICE_NAME</artifactId>
    <name>${CLASS_NAME}</name>
    <description>$SERVICE_DESC</description>

    <dependencies>
        <!-- Common Library -->
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>common-lib</artifactId>
            <version>\${project.version}</version>
        </dependency>
        
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Spring Cloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        
        <!-- Cache -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- Messaging -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
EOF

    # Create Application class
    cat > "$SERVICE_PATH/src/main/java/com/enterprise/$PACKAGE_NAME/${CLASS_NAME}Application.java" << EOF
package com.enterprise.$PACKAGE_NAME;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {"com.enterprise.$PACKAGE_NAME", "com.enterprise.common"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class ${CLASS_NAME}Application {
    
    public static void main(String[] args) {
        SpringApplication.run(${CLASS_NAME}Application.class, args);
    }
}
EOF

    # Create application.yml
    cat > "$SERVICE_PATH/src/main/resources/application.yml" << EOF
spring:
  application:
    name: $SERVICE_NAME
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:dev}
    
server:
  port: \${SERVER_PORT:$SERVICE_PORT}
  
---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://\${DB_HOST:localhost}:\${DB_PORT:5432}/\${DB_NAME:$DB_NAME}
    username: \${DB_USER:postgres}
    password: \${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true
  redis:
    host: \${REDIS_HOST:localhost}
    port: \${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: \${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: $SERVICE_NAME-group
  rabbitmq:
    host: \${RABBITMQ_HOST:localhost}
    port: \${RABBITMQ_PORT:5672}
    username: \${RABBITMQ_USER:guest}
    password: \${RABBITMQ_PASSWORD:guest}
    
eureka:
  client:
    service-url:
      defaultZone: \${EUREKA_SERVER:http://localhost:8761/eureka/}
  instance:
    prefer-ip-address: true
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: \${spring.application.name}
      
logging:
  level:
    com.enterprise.$PACKAGE_NAME: DEBUG
    org.springframework.web: INFO
    
---
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
    
---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:postgresql://\${DB_HOST}:\${DB_PORT}/\${DB_NAME}
    username: \${DB_USER}
    password: \${DB_PASSWORD}
    hikari:
      maximum-pool-size: 30
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
logging:
  level:
    com.enterprise.$PACKAGE_NAME: INFO
    org.springframework.web: WARN
EOF

    # Create Dockerfile
    cat > "$SERVICE_PATH/Dockerfile" << EOF
# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom
COPY ../../pom.xml /app/parent-pom.xml

# Copy common-lib
COPY ../../shared/common-lib /app/shared/common-lib

# Copy service pom and source
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \\
    CMD wget --no-verbose --tries=1 --spider http://localhost:$SERVICE_PORT/actuator/health || exit 1

# Expose port
EXPOSE $SERVICE_PORT

# JVM options
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar app.jar"]
EOF

    # Create README
    cat > "$SERVICE_PATH/README.md" << EOF
# $CLASS_NAME

## Overview
$SERVICE_DESC

## API Endpoints
See Swagger UI at http://localhost:$SERVICE_PORT/swagger-ui.html

## Configuration

### Environment Variables
\`\`\`bash
# Server
SERVER_PORT=$SERVICE_PORT

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=$DB_NAME
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
\`\`\`

## Running Locally

### Prerequisites
- Java 17
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- Apache Kafka
- RabbitMQ

### Run
\`\`\`bash
mvn spring-boot:run
\`\`\`

## Building

### Build JAR:
\`\`\`bash
mvn clean package
\`\`\`

### Build Docker image:
\`\`\`bash
docker build -t $SERVICE_NAME:latest .
\`\`\`

## Monitoring
- Health: http://localhost:$SERVICE_PORT/actuator/health
- Metrics: http://localhost:$SERVICE_PORT/actuator/metrics
- API Docs: http://localhost:$SERVICE_PORT/v3/api-docs
EOF

    echo "Generated $SERVICE_NAME successfully!"
}

# Make the script executable
chmod +x "$0"