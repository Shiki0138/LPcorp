# Multi-stage build for Payment Service
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
ENV SERVER_PORT=8004 \
    SERVICE_NAME=payment-service \
    SPRING_PROFILES_ACTIVE=prod

# Copy the JAR file
COPY --from=build --chown=appuser:appgroup /build/target/payment-service-*.jar /app/app.jar

# Copy service-specific config
COPY --chown=appuser:appgroup docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

# Expose service port
EXPOSE 8004

# JMX port for monitoring (optional)
EXPOSE 9004

# Override entrypoint for service-specific initialization
ENTRYPOINT ["/app/docker-entrypoint.sh"]