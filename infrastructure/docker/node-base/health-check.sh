#!/bin/sh
# Health check script for Node.js applications

# Default health check endpoint
HEALTH_ENDPOINT="${HEALTH_ENDPOINT:-http://localhost:${PORT:-3000}/health}"

# Check if the application is healthy
response=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_ENDPOINT")

if [ "$response" = "200" ]; then
    exit 0
else
    exit 1
fi