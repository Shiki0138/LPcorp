#!/bin/sh
set -e

# Wait for dependencies
echo "Waiting for Redis..."
while ! nc -z ${REDIS_HOST:-redis} ${REDIS_PORT:-6379}; do
  sleep 1
done
echo "Redis is ready!"

echo "Waiting for Auth Service..."
while ! nc -z ${AUTH_SERVICE_HOST:-auth-service} ${AUTH_SERVICE_PORT:-8010}; do
  sleep 1
done
echo "Auth Service is ready!"

# Service-specific JVM options with rate limiting and caching
export JAVA_OPTS_APPEND="-Dspring.application.name=api-gateway \
    -Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus,gateway \
    -Dmanagement.metrics.export.prometheus.enabled=true \
    -Dspring.cloud.gateway.redis-rate-limiter.enabled=true \
    -Dspring.cloud.gateway.filter.request-rate-limiter.enabled=true \
    -javaagent:/app/libs/opentelemetry-javaagent.jar"

# Start the application
exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar /app/app.jar