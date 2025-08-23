#!/bin/bash

# Script to scaffold a new microservice
# Creates a complete microservice structure with all necessary files

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to convert string to different cases
to_camel_case() {
    echo "$1" | sed -r 's/(^|-)([a-z])/\U\2/g'
}

to_snake_case() {
    echo "$1" | sed -r 's/([a-z])([A-Z])/\1_\2/g' | tr '[:upper:]' '[:lower:]' | tr '-' '_'
}

to_package_name() {
    echo "$1" | tr '[:upper:]' '[:lower:]' | tr '-' '.'
}

# Function to validate service name
validate_service_name() {
    local name=$1
    if [[ ! "$name" =~ ^[a-z][a-z0-9-]*$ ]]; then
        print_error "Service name must start with lowercase letter and contain only lowercase letters, numbers, and hyphens"
        return 1
    fi
    return 0
}

# Function to create service structure
create_service_structure() {
    local service_name=$1
    local port=$2
    local package_name=$(to_package_name "$service_name")
    local class_name=$(to_camel_case "$service_name")
    
    print_info "Creating service structure for $service_name..."
    
    # Create directory structure
    mkdir -p "$service_name/src/main/java/com/microservices/$package_name"/{controller,service,repository,model,dto,config,exception}
    mkdir -p "$service_name/src/main/resources"/{static,templates}
    mkdir -p "$service_name/src/test/java/com/microservices/$package_name"/{controller,service,repository}
    mkdir -p "$service_name/src/test/resources"
    
    print_success "Directory structure created"
}

# Function to create pom.xml
create_pom_xml() {
    local service_name=$1
    local class_name=$(to_camel_case "$service_name")
    
    cat > "$service_name/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.microservices</groupId>
        <artifactId>microservices-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    
    <artifactId>$service_name</artifactId>
    <name>$class_name</name>
    <description>$class_name for Microservices Platform</description>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
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
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-sleuth</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-sleuth-zipkin</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Messaging -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        
        <!-- Cache -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>1.7.0</version>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
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
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>\${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>1.5.5.Final</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF
    
    print_success "Created pom.xml"
}

# Function to create application class
create_application_class() {
    local service_name=$1
    local package_name=$(to_package_name "$service_name")
    local class_name=$(to_camel_case "$service_name")
    
    cat > "$service_name/src/main/java/com/microservices/$package_name/${class_name}Application.java" << EOF
package com.microservices.$package_name;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ${class_name}Application {

    public static void main(String[] args) {
        SpringApplication.run(${class_name}Application.class, args);
    }
}
EOF
    
    print_success "Created application class"
}

# Function to create application properties
create_application_properties() {
    local service_name=$1
    local port=$2
    
    cat > "$service_name/src/main/resources/application.yml" << EOF
spring:
  application:
    name: $service_name
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:dev}
  
  # Database Configuration
  datasource:
    url: jdbc:mysql://\${DB_HOST:localhost}:\${DB_PORT:3306}/\${DB_NAME:${service_name}_db}?useSSL=false&serverTimezone=UTC
    username: \${DB_USER:root}
    password: \${DB_PASSWORD:rootpassword}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  
  # Redis Configuration
  redis:
    host: \${REDIS_HOST:localhost}
    port: \${REDIS_PORT:6379}
    password: \${REDIS_PASSWORD:redispassword}
  
  # RabbitMQ Configuration
  rabbitmq:
    host: \${RABBITMQ_HOST:localhost}
    port: \${RABBITMQ_PORT:5672}
    username: \${RABBITMQ_USER:admin}
    password: \${RABBITMQ_PASSWORD:adminpassword}
  
  # Security
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://\${AUTH_SERVICE_HOST:localhost}:\${AUTH_SERVICE_PORT:8081}

# Server Configuration
server:
  port: $port
  error:
    include-message: always
    include-binding-errors: always

# Eureka Configuration
eureka:
  client:
    service-url:
      defaultZone: http://\${EUREKA_HOST:localhost}:\${EUREKA_PORT:8761}/eureka/
  instance:
    prefer-ip-address: true
    instance-id: \${spring.application.name}:\${spring.application.instance_id:\${random.value}}

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    health:
      show-details: always

# OpenAPI Documentation
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

# Logging
logging:
  level:
    com.microservices: DEBUG
    org.springframework.cloud: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/$service_name.log

# Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      default:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        wait-duration-in-open-state: 5s
        failure-rate-threshold: 50
        event-consumer-buffer-size: 10
  retry:
    instances:
      default:
        max-attempts: 3
        wait-duration: 1s
  bulkhead:
    instances:
      default:
        max-concurrent-calls: 10
  thread-pool-bulkhead:
    instances:
      default:
        max-thread-pool-size: 4
        core-thread-pool-size: 2
        queue-capacity: 2

# Custom Properties
app:
  version: 1.0.0
  description: $service_name for Microservices Platform
EOF
    
    # Create dev profile
    cat > "$service_name/src/main/resources/application-dev.yml" << EOF
spring:
  jpa:
    show-sql: true
  
  # H2 Database for development
  datasource:
    url: jdbc:h2:mem:${service_name}_db
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true
      path: /h2-console

logging:
  level:
    com.microservices: DEBUG
    org.springframework: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
EOF
    
    # Create test profile
    cat > "$service_name/src/main/resources/application-test.yml" << EOF
spring:
  datasource:
    url: jdbc:h2:mem:test_db
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop

eureka:
  client:
    enabled: false

logging:
  level:
    root: WARN
    com.microservices: DEBUG
EOF
    
    print_success "Created application properties"
}

# Function to create sample controller
create_sample_controller() {
    local service_name=$1
    local package_name=$(to_package_name "$service_name")
    local class_name=$(to_camel_case "$service_name")
    
    cat > "$service_name/src/main/java/com/microservices/$package_name/controller/${class_name}Controller.java" << EOF
package com.microservices.$package_name.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/${service_name}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "$class_name", description = "$class_name API")
public class ${class_name}Controller {

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "$service_name"
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "Service information")
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "$service_name",
            "version", "1.0.0",
            "description", "$class_name for Microservices Platform"
        ));
    }
}
EOF
    
    print_success "Created sample controller"
}

# Function to create configuration classes
create_config_classes() {
    local service_name=$1
    local package_name=$(to_package_name "$service_name")
    
    # Security Configuration
    cat > "$service_name/src/main/java/com/microservices/$package_name/config/SecurityConfig.java" << EOF
package com.microservices.$package_name.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/${service_name}/health", "/api/v1/${service_name}/info").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt();
        
        return http.build();
    }
}
EOF
    
    # OpenAPI Configuration
    cat > "$service_name/src/main/java/com/microservices/$package_name/config/OpenApiConfig.java" << EOF
package com.microservices.$package_name.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("$service_name API")
                .version("1.0.0")
                .description("API documentation for $service_name")
                .contact(new Contact()
                    .name("Development Team")
                    .email("dev@microservices.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
EOF
    
    print_success "Created configuration classes"
}

# Function to create test classes
create_test_classes() {
    local service_name=$1
    local package_name=$(to_package_name "$service_name")
    local class_name=$(to_camel_case "$service_name")
    
    # Application test
    cat > "$service_name/src/test/java/com/microservices/$package_name/${class_name}ApplicationTests.java" << EOF
package com.microservices.$package_name;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ${class_name}ApplicationTests {

    @Test
    void contextLoads() {
    }
}
EOF
    
    # Controller test
    cat > "$service_name/src/test/java/com/microservices/$package_name/controller/${class_name}ControllerTest.java" << EOF
package com.microservices.$package_name.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ${class_name}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/${service_name}/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("$service_name"));
    }

    @Test
    void info_ShouldReturnServiceInfo() throws Exception {
        mockMvc.perform(get("/api/v1/${service_name}/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("$service_name"))
            .andExpect(jsonPath("$.version").exists());
    }
}
EOF
    
    print_success "Created test classes"
}

# Function to create Dockerfile
create_dockerfile() {
    local service_name=$1
    
    cat > "$service_name/Dockerfile" << EOF
FROM openjdk:17-jdk-slim AS build

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Build application
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

# Create non-root user
RUN groupadd -g 1000 spring && useradd -u 1000 -g spring spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

USER spring

# Expose port
EXPOSE $port

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:$port/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    
    print_success "Created Dockerfile"
}

# Function to create README
create_readme() {
    local service_name=$1
    local port=$2
    local class_name=$(to_camel_case "$service_name")
    
    cat > "$service_name/README.md" << EOF
# $class_name

## Overview
$class_name is a microservice in the Microservices Platform.

## Features
- RESTful API
- Service discovery with Eureka
- Configuration management with Config Server
- Circuit breaker with Resilience4j
- API documentation with OpenAPI/Swagger
- Security with OAuth2/JWT
- Distributed tracing with Sleuth and Zipkin
- Metrics with Micrometer and Prometheus

## Technology Stack
- Java 17
- Spring Boot 3.x
- Spring Cloud
- MySQL/H2 Database
- Redis for caching
- RabbitMQ for messaging

## Running Locally

### Prerequisites
- Java 17
- Maven 3.6+
- Docker (for dependencies)

### Start Dependencies
\`\`\`bash
docker-compose up -d mysql redis rabbitmq
\`\`\`

### Run Application
\`\`\`bash
mvn spring-boot:run
\`\`\`

The service will start on port $port.

## API Documentation
Once the service is running, access the API documentation at:
- Swagger UI: http://localhost:$port/swagger-ui.html
- OpenAPI Spec: http://localhost:$port/v3/api-docs

## Configuration
The service can be configured using environment variables or application properties.

Key configuration properties:
- \`DB_HOST\`: Database host (default: localhost)
- \`DB_PORT\`: Database port (default: 3306)
- \`REDIS_HOST\`: Redis host (default: localhost)
- \`RABBITMQ_HOST\`: RabbitMQ host (default: localhost)

## Building
\`\`\`bash
mvn clean package
\`\`\`

## Testing
\`\`\`bash
mvn test
\`\`\`

## Docker
Build Docker image:
\`\`\`bash
docker build -t $service_name:latest .
\`\`\`

Run with Docker:
\`\`\`bash
docker run -p $port:$port $service_name:latest
\`\`\`

## Health Check
- Health endpoint: http://localhost:$port/actuator/health
- Metrics: http://localhost:$port/actuator/metrics
- Prometheus: http://localhost:$port/actuator/prometheus
EOF
    
    print_success "Created README"
}

# Function to update parent pom.xml
update_parent_pom() {
    local service_name=$1
    
    if [ -f "pom.xml" ]; then
        print_info "Updating parent pom.xml..."
        
        # Check if modules section exists
        if grep -q "<modules>" pom.xml; then
            # Add new module before closing </modules> tag
            sed -i.bak "/<\/modules>/i\\
        <module>$service_name</module>" pom.xml
            rm pom.xml.bak
        else
            print_warning "Parent pom.xml doesn't have modules section. Please add $service_name manually."
        fi
    fi
}

# Main function
main() {
    echo "=========================================="
    echo "Microservice Generator"
    echo "=========================================="
    echo
    
    # Get service name
    if [ -z "$1" ]; then
        read -p "Enter service name (e.g., payment-service): " service_name
    else
        service_name=$1
    fi
    
    # Validate service name
    if ! validate_service_name "$service_name"; then
        exit 1
    fi
    
    # Check if service already exists
    if [ -d "$service_name" ]; then
        print_error "Service $service_name already exists!"
        exit 1
    fi
    
    # Get port number
    if [ -z "$2" ]; then
        read -p "Enter port number (e.g., 8086): " port
    else
        port=$2
    fi
    
    # Validate port
    if ! [[ "$port" =~ ^[0-9]+$ ]] || [ "$port" -lt 1024 ] || [ "$port" -gt 65535 ]; then
        print_error "Invalid port number. Must be between 1024 and 65535."
        exit 1
    fi
    
    print_info "Creating microservice: $service_name on port $port"
    
    # Create service
    create_service_structure "$service_name" "$port"
    create_pom_xml "$service_name"
    create_application_class "$service_name"
    create_application_properties "$service_name" "$port"
    create_sample_controller "$service_name"
    create_config_classes "$service_name"
    create_test_classes "$service_name"
    create_dockerfile "$service_name" "$port"
    create_readme "$service_name" "$port"
    
    # Update parent pom if exists
    update_parent_pom "$service_name"
    
    # Create git ignore
    cp .gitignore "$service_name/.gitignore" 2>/dev/null || true
    
    # Success message
    echo
    print_success "Microservice $service_name created successfully!"
    echo
    echo "Next steps:"
    echo "1. cd $service_name"
    echo "2. mvn clean install"
    echo "3. mvn spring-boot:run"
    echo
    echo "Access points:"
    echo "- API: http://localhost:$port/api/v1/$service_name"
    echo "- Swagger UI: http://localhost:$port/swagger-ui.html"
    echo "- Health: http://localhost:$port/actuator/health"
}

# Run main function
main "$@"