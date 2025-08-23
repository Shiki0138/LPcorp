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

echo "Waiting for Kafka..."
while ! nc -z ${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092} 9092; do
  sleep 1
done
echo "Kafka is ready!"

# Service-specific JVM options
export JAVA_OPTS_APPEND="-Dspring.application.name=order-service \
    -Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus \
    -Dmanagement.metrics.export.prometheus.enabled=true \
    -javaagent:/app/libs/opentelemetry-javaagent.jar"

# Start the application
exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar /app/app.jar