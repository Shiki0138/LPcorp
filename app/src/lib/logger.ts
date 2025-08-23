/**
 * Advanced Logging System
 * Provides structured logging with multiple levels, transports, and monitoring integration
 */

import { redis } from './redis';
import { prisma } from './database';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error' | 'critical';

export interface LogEntry {
  level: LogLevel;
  message: string;
  timestamp: Date;
  component?: string;
  userId?: string;
  requestId?: string;
  sessionId?: string;
  metadata?: Record<string, any>;
  error?: {
    name: string;
    message: string;
    stack?: string;
  };
  performance?: {
    duration: number;
    memory: number;
    cpu: number;
  };
  context?: {
    url?: string;
    method?: string;
    userAgent?: string;
    ipAddress?: string;
  };
}

export interface LoggerConfig {
  level: LogLevel;
  enableConsole: boolean;
  enableFile: boolean;
  enableDatabase: boolean;
  enableRedis: boolean;
  enableExternal: boolean;
  batchSize: number;
  flushInterval: number; // milliseconds
  retentionDays: number;
  sensitiveFields: string[];
}

export class Logger {
  private static readonly DEFAULT_CONFIG: LoggerConfig = {
    level: (process.env.LOG_LEVEL as LogLevel) || 'info',
    enableConsole: true,
    enableFile: process.env.NODE_ENV === 'production',
    enableDatabase: true,
    enableRedis: true,
    enableExternal: process.env.NODE_ENV === 'production',
    batchSize: 100,
    flushInterval: 5000, // 5 seconds
    retentionDays: 30,
    sensitiveFields: ['password', 'token', 'apiKey', 'secret', 'authorization'],
  };

  private static config: LoggerConfig = { ...this.DEFAULT_CONFIG };
  private static logBuffer: LogEntry[] = [];
  private static flushTimer: NodeJS.Timeout | null = null;

  private static readonly LOG_LEVELS: Record<LogLevel, number> = {
    debug: 0,
    info: 1,
    warn: 2,
    error: 3,
    critical: 4,
  };

  /**
   * Initialize logger with custom configuration
   */
  static configure(config: Partial<LoggerConfig>): void {
    this.config = { ...this.DEFAULT_CONFIG, ...config };
    this.startBatchProcessor();
  }

  /**
   * Log debug message
   */
  static debug(message: string, metadata?: Record<string, any>): void {
    this.log('debug', message, metadata);
  }

  /**
   * Log info message
   */
  static info(message: string, metadata?: Record<string, any>): void {
    this.log('info', message, metadata);
  }

  /**
   * Log warning message
   */
  static warn(message: string, metadata?: Record<string, any>): void {
    this.log('warn', message, metadata);
  }

  /**
   * Log error message
   */
  static error(message: string, error?: Error, metadata?: Record<string, any>): void {
    const errorInfo = error ? {
      name: error.name,
      message: error.message,
      stack: error.stack,
    } : undefined;

    this.log('error', message, metadata, errorInfo);
  }

  /**
   * Log critical message
   */
  static critical(message: string, error?: Error, metadata?: Record<string, any>): void {
    const errorInfo = error ? {
      name: error.name,
      message: error.message,
      stack: error.stack,
    } : undefined;

    this.log('critical', message, metadata, errorInfo);
    
    // Critical logs should be processed immediately
    this.flush().catch(console.error);
  }

  /**
   * Log HTTP request
   */
  static logRequest(
    method: string,
    url: string,
    statusCode: number,
    duration: number,
    userId?: string,
    metadata?: Record<string, any>
  ): void {
    this.log('info', `${method} ${url} ${statusCode}`, {
      ...metadata,
      type: 'http_request',
      statusCode,
      duration,
      userId,
    });
  }

  /**
   * Log database query
   */
  static logQuery(
    query: string,
    duration: number,
    affectedRows?: number,
    metadata?: Record<string, any>
  ): void {
    this.log('debug', 'Database query executed', {
      ...metadata,
      type: 'database_query',
      query: this.sanitizeQuery(query),
      duration,
      affectedRows,
    });
  }

  /**
   * Log security event
   */
  static logSecurity(
    event: string,
    severity: 'low' | 'medium' | 'high' | 'critical',
    metadata?: Record<string, any>
  ): void {
    const level: LogLevel = severity === 'critical' ? 'critical' : 
                           severity === 'high' ? 'error' :
                           severity === 'medium' ? 'warn' : 'info';

    this.log(level, `Security event: ${event}`, {
      ...metadata,
      type: 'security_event',
      severity,
    });
  }

  /**
   * Log performance metric
   */
  static logPerformance(
    operation: string,
    duration: number,
    metadata?: Record<string, any>
  ): void {
    const performance = {
      duration,
      memory: process.memoryUsage().heapUsed,
      cpu: process.cpuUsage().user,
    };

    this.log('info', `Performance: ${operation}`, {
      ...metadata,
      type: 'performance',
    }, undefined, performance);
  }

  /**
   * Log business event
   */
  static logBusiness(
    event: string,
    userId?: string,
    metadata?: Record<string, any>
  ): void {
    this.log('info', `Business event: ${event}`, {
      ...metadata,
      type: 'business_event',
      userId,
    });
  }

  /**
   * Core logging method
   */
  private static log(
    level: LogLevel,
    message: string,
    metadata?: Record<string, any>,
    error?: { name: string; message: string; stack?: string },
    performance?: { duration: number; memory: number; cpu: number },
    context?: { url?: string; method?: string; userAgent?: string; ipAddress?: string }
  ): void {
    // Check if log level meets threshold
    if (this.LOG_LEVELS[level] < this.LOG_LEVELS[this.config.level]) {
      return;
    }

    const logEntry: LogEntry = {
      level,
      message,
      timestamp: new Date(),
      component: metadata?.component,
      userId: metadata?.userId,
      requestId: metadata?.requestId,
      sessionId: metadata?.sessionId,
      metadata: this.sanitizeMetadata(metadata),
      error,
      performance,
      context,
    };

    // Console logging (immediate)
    if (this.config.enableConsole) {
      this.logToConsole(logEntry);
    }

    // Buffer for batch processing
    this.logBuffer.push(logEntry);

    // Force flush on critical logs or when buffer is full
    if (level === 'critical' || this.logBuffer.length >= this.config.batchSize) {
      this.flush().catch(console.error);
    }
  }

  /**
   * Console logging with formatting
   */
  private static logToConsole(entry: LogEntry): void {
    const colors = {
      debug: '\x1b[36m',   // Cyan
      info: '\x1b[32m',    // Green
      warn: '\x1b[33m',    // Yellow
      error: '\x1b[31m',   // Red
      critical: '\x1b[35m', // Magenta
    };

    const reset = '\x1b[0m';
    const timestamp = entry.timestamp.toISOString();
    const color = colors[entry.level];
    
    const logMessage = `${color}[${timestamp}] ${entry.level.toUpperCase()}${reset}: ${entry.message}`;
    
    if (entry.level === 'error' || entry.level === 'critical') {
      console.error(logMessage);
      if (entry.error?.stack) {
        console.error(entry.error.stack);
      }
    } else if (entry.level === 'warn') {
      console.warn(logMessage);
    } else {
      console.log(logMessage);
    }

    if (entry.metadata && Object.keys(entry.metadata).length > 0) {
      console.log('  Metadata:', JSON.stringify(entry.metadata, null, 2));
    }
  }

  /**
   * Flush log buffer to all configured transports
   */
  private static async flush(): Promise<void> {
    if (this.logBuffer.length === 0) return;

    const logsToFlush = [...this.logBuffer];
    this.logBuffer = [];

    try {
      const promises: Promise<void>[] = [];

      if (this.config.enableRedis) {
        promises.push(this.flushToRedis(logsToFlush));
      }

      if (this.config.enableDatabase) {
        promises.push(this.flushToDatabase(logsToFlush));
      }

      if (this.config.enableExternal) {
        promises.push(this.flushToExternal(logsToFlush));
      }

      await Promise.allSettled(promises);
    } catch (error) {
      console.error('Log flush error:', error);
      // Re-add logs to buffer for retry
      this.logBuffer.unshift(...logsToFlush);
    }
  }

  /**
   * Flush logs to Redis for real-time access
   */
  private static async flushToRedis(logs: LogEntry[]): Promise<void> {
    try {
      const pipeline = redis.pipeline();
      
      for (const log of logs) {
        const key = `logs:${log.level}:${Date.now()}:${Math.random()}`;
        pipeline.setex(key, 86400, JSON.stringify(log)); // 24 hours retention
        
        // Add to level-specific sorted set for efficient querying
        pipeline.zadd(`logs:${log.level}:index`, log.timestamp.getTime(), key);
      }

      await pipeline.exec();
    } catch (error) {
      console.error('Redis log flush error:', error);
      throw error;
    }
  }

  /**
   * Flush logs to database for long-term storage
   */
  private static async flushToDatabase(logs: LogEntry[]): Promise<void> {
    try {
      // Only store warn, error, and critical logs in database to save space
      const importantLogs = logs.filter(log => 
        ['warn', 'error', 'critical'].includes(log.level)
      );

      if (importantLogs.length === 0) return;

      await prisma.auditLog.createMany({
        data: importantLogs.map(log => ({
          userId: log.userId,
          action: log.level,
          resource: 'system_log',
          resourceId: log.requestId,
          oldValues: null,
          newValues: {
            level: log.level,
            message: log.message,
            component: log.component,
            metadata: log.metadata,
            error: log.error,
            performance: log.performance,
            context: log.context,
          },
          ipAddress: log.context?.ipAddress,
          userAgent: log.context?.userAgent,
          timestamp: log.timestamp,
        })),
      });
    } catch (error) {
      console.error('Database log flush error:', error);
      throw error;
    }
  }

  /**
   * Flush logs to external monitoring service
   */
  private static async flushToExternal(logs: LogEntry[]): Promise<void> {
    try {
      // Implementation for external services like DataDog, CloudWatch, etc.
      // This is a placeholder for actual implementation
      
      if (process.env.EXTERNAL_LOG_ENDPOINT) {
        const response = await fetch(process.env.EXTERNAL_LOG_ENDPOINT, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${process.env.EXTERNAL_LOG_TOKEN}`,
          },
          body: JSON.stringify({ logs }),
        });

        if (!response.ok) {
          throw new Error(`External logging failed: ${response.statusText}`);
        }
      }
    } catch (error) {
      console.error('External log flush error:', error);
      throw error;
    }
  }

  /**
   * Start batch processor timer
   */
  private static startBatchProcessor(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
    }

    this.flushTimer = setInterval(() => {
      this.flush().catch(console.error);
    }, this.config.flushInterval);
  }

  /**
   * Sanitize metadata to remove sensitive information
   */
  private static sanitizeMetadata(metadata?: Record<string, any>): Record<string, any> | undefined {
    if (!metadata) return undefined;

    const sanitized = { ...metadata };
    
    for (const field of this.config.sensitiveFields) {
      if (sanitized[field]) {
        sanitized[field] = '[REDACTED]';
      }
    }

    return sanitized;
  }

  /**
   * Sanitize SQL query to remove sensitive data
   */
  private static sanitizeQuery(query: string): string {
    // Basic SQL sanitization - replace potential sensitive values
    return query
      .replace(/('.*?')/g, "'[REDACTED]'")
      .replace(/(\$\d+)/g, '$[REDACTED]')
      .substring(0, 500); // Limit query length
  }

  /**
   * Get logs from Redis with filtering
   */
  static async getLogs(
    level?: LogLevel,
    limit = 100,
    offset = 0
  ): Promise<LogEntry[]> {
    try {
      const pattern = level ? `logs:${level}:*` : 'logs:*';
      const keys = await redis.keys(pattern);
      
      // Sort by timestamp (newest first)
      const sortedKeys = keys
        .sort((a, b) => {
          const timestampA = parseInt(a.split(':')[2] || '0');
          const timestampB = parseInt(b.split(':')[2] || '0');
          return timestampB - timestampA;
        })
        .slice(offset, offset + limit);

      const logs: LogEntry[] = [];
      
      for (const key of sortedKeys) {
        const logData = await redis.get(key);
        if (logData) {
          logs.push(JSON.parse(logData));
        }
      }

      return logs;
    } catch (error) {
      console.error('Error retrieving logs:', error);
      return [];
    }
  }

  /**
   * Clean up old logs
   */
  static async cleanup(): Promise<void> {
    try {
      const cutoffTime = Date.now() - (this.config.retentionDays * 24 * 60 * 60 * 1000);
      
      // Clean Redis logs
      const keys = await redis.keys('logs:*');
      const pipeline = redis.pipeline();
      
      for (const key of keys) {
        const timestamp = parseInt(key.split(':')[2] || '0');
        if (timestamp < cutoffTime) {
          pipeline.del(key);
        }
      }
      
      await pipeline.exec();

      // Clean database logs
      await prisma.auditLog.deleteMany({
        where: {
          timestamp: {
            lt: new Date(cutoffTime),
          },
          resource: 'system_log',
        },
      });

      this.info('Log cleanup completed', { 
        cutoffTime: new Date(cutoffTime).toISOString(),
        retentionDays: this.config.retentionDays 
      });
    } catch (error) {
      this.error('Log cleanup failed', error);
    }
  }

  /**
   * Get system health metrics
   */
  static async getHealthMetrics(): Promise<{
    logBufferSize: number;
    errorRate: number;
    avgResponseTime: number;
    criticalErrors: number;
  }> {
    try {
      const now = Date.now();
      const oneHourAgo = now - (60 * 60 * 1000);

      // Get error logs from last hour
      const errorKeys = await redis.zrangebyscore(
        'logs:error:index',
        oneHourAgo,
        now
      );

      const criticalKeys = await redis.zrangebyscore(
        'logs:critical:index',
        oneHourAgo,
        now
      );

      return {
        logBufferSize: this.logBuffer.length,
        errorRate: errorKeys.length,
        avgResponseTime: 0, // Would be calculated from performance logs
        criticalErrors: criticalKeys.length,
      };
    } catch (error) {
      console.error('Error getting health metrics:', error);
      return {
        logBufferSize: this.logBuffer.length,
        errorRate: 0,
        avgResponseTime: 0,
        criticalErrors: 0,
      };
    }
  }

  /**
   * Graceful shutdown
   */
  static async shutdown(): Promise<void> {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }

    // Flush remaining logs
    await this.flush();
    
    this.info('Logger shutdown completed');
  }
}

// Initialize logger with default configuration
Logger.configure({});

// Graceful shutdown on process termination
process.on('SIGTERM', () => {
  Logger.shutdown().catch(console.error);
});

process.on('SIGINT', () => {
  Logger.shutdown().catch(console.error);
});

export default Logger;