# Base Java image with OpenJDK 17 Alpine
FROM eclipse-temurin:17-jre-alpine AS base

# Install required packages
RUN apk add --no-cache \
    curl \
    bash \
    openssl \
    ca-certificates \
    fontconfig \
    ttf-dejavu \
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set up directories
RUN mkdir -p /app/logs /app/config /app/libs && \
    chown -R appuser:appgroup /app

WORKDIR /app

# JVM optimization flags for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+AlwaysPreTouch \
    -XX:+DisableExplicitGC \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxGCPauseMillis=200 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3"

# Add health check script
COPY --chown=appuser:appgroup health-check.sh /app/
RUN chmod +x /app/health-check.sh

# Switch to non-root user
USER appuser

# Default health check (can be overridden)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD /app/health-check.sh || exit 1

# Base entrypoint
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar /app/app.jar"]