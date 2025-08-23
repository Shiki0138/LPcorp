# Base Node.js image for frontend/admin services
FROM node:18-alpine AS base

# Install required packages
RUN apk add --no-cache \
    curl \
    bash \
    openssl \
    ca-certificates \
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set up directories
RUN mkdir -p /app/dist /app/public /app/logs && \
    chown -R appuser:appgroup /app

WORKDIR /app

# Node.js optimization for production
ENV NODE_ENV=production \
    NODE_OPTIONS="--max-old-space-size=2048 --enable-source-maps"

# Add health check script
COPY --chown=appuser:appgroup health-check.sh /app/
RUN chmod +x /app/health-check.sh

# Install global packages
RUN npm install -g pm2@latest

# Switch to non-root user
USER appuser

# Default health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD /app/health-check.sh || exit 1

# Default command using PM2 for process management
CMD ["pm2-runtime", "start", "ecosystem.config.js"]