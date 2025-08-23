# Multi-stage build for Auth Service
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
ENV SERVER_PORT=8010 \
    SERVICE_NAME=auth-service \
    SPRING_PROFILES_ACTIVE=prod

# Copy the JAR file
COPY --from=build --chown=appuser:appgroup /build/target/auth-service-*.jar /app/app.jar

# Copy service-specific config and keys
COPY --chown=appuser:appgroup docker-entrypoint.sh /app/
COPY --chown=appuser:appgroup keys/ /app/keys/
RUN chmod +x /app/docker-entrypoint.sh && \
    chmod -R 600 /app/keys/

# Expose service port
EXPOSE 8010

# JMX port for monitoring (optional)
EXPOSE 9010

# Override entrypoint for service-specific initialization
ENTRYPOINT ["/app/docker-entrypoint.sh"]