# Multi-stage build for API Gateway
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# Copy source code
WORKDIR /build
COPY pom.xml .
COPY src ./src

# Download dependencies and build
RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

# Runtime stage
FROM infrastructure/docker/java-base:latest

# Service-specific configuration
ENV SERVER_PORT=8080 \
    SERVICE_NAME=api-gateway \
    SPRING_PROFILES_ACTIVE=prod

# Copy the JAR file
COPY --from=build --chown=appuser:appgroup /build/target/api-gateway-*.jar /app/app.jar

# Copy service-specific config
COPY --chown=appuser:appgroup docker-entrypoint.sh /app/
COPY --chown=appuser:appgroup routes/ /app/routes/
RUN chmod +x /app/docker-entrypoint.sh

# Expose service port
EXPOSE 8080

# JMX port for monitoring (optional)
EXPOSE 9080

# Override entrypoint for service-specific initialization
ENTRYPOINT ["/app/docker-entrypoint.sh"]