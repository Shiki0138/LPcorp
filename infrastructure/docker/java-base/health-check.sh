#!/bin/sh
# Health check script for Java applications

# Default health check endpoint
HEALTH_ENDPOINT="${HEALTH_ENDPOINT:-http://localhost:${SERVER_PORT:-8080}/actuator/health}"

# Check if the application is healthy
response=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_ENDPOINT")

if [ "$response" = "200" ]; then
    exit 0
else
    exit 1
fi