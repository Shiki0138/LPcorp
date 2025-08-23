#!/bin/sh
set -e

# Wait for dependencies
echo "Waiting for PostgreSQL..."
while ! nc -z ${DB_HOST:-postgres} ${DB_PORT:-5432}; do
  sleep 1
done
echo "PostgreSQL is ready!"

echo "Waiting for Redis..."
while ! nc -z ${REDIS_HOST:-redis} ${REDIS_PORT:-6379}; do
  sleep 1
done
echo "Redis is ready!"

# Service-specific JVM options with enhanced security
export JAVA_OPTS_APPEND="-Dspring.application.name=auth-service \
    -Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus \
    -Dmanagement.metrics.export.prometheus.enabled=true \
    -Dauth.jwt.private-key-path=/app/keys/private.pem \
    -Dauth.jwt.public-key-path=/app/keys/public.pem \
    -Djava.security.properties=/app/config/java.security \
    -javaagent:/app/libs/opentelemetry-javaagent.jar"

# Start the application
exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar /app/app.jar