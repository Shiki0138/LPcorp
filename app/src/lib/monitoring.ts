/**
 * System Monitoring and Performance Tracking
 * Comprehensive monitoring for system health, performance metrics, and alerting
 */

import { redis } from './redis';
import { prisma } from './database';
import { Logger } from './logger';
import { CacheService } from './cache';
import { AnalyticsService } from './analytics';

export interface SystemMetrics {
  timestamp: Date;
  cpu: {
    usage: number;
    loadAverage: number[];
  };
  memory: {
    used: number;
    total: number;
    percentage: number;
    heapUsed: number;
    heapTotal: number;
  };
  database: {
    connections: number;
    avgQueryTime: number;
    slowQueries: number;
    errorRate: number;
  };
  redis: {
    connections: number;
    memoryUsage: number;
    hitRate: number;
    commandsPerSecond: number;
  };
  api: {
    requestsPerMinute: number;
    avgResponseTime: number;
    errorRate: number;
    activeConnections: number;
  };
  application: {
    uptime: number;
    version: string;
    environment: string;
    buildId: string;
  };
}

export interface PerformanceAlert {
  id: string;
  type: 'cpu' | 'memory' | 'database' | 'redis' | 'api' | 'disk' | 'custom';
  severity: 'low' | 'medium' | 'high' | 'critical';
  message: string;
  value: number;
  threshold: number;
  timestamp: Date;
  resolved: boolean;
  resolvedAt?: Date;
}

export interface HealthStatus {
  status: 'healthy' | 'degraded' | 'unhealthy';
  components: {
    database: boolean;
    redis: boolean;
    fileSystem: boolean;
    external: boolean;
  };
  uptime: number;
  responseTime: number;
  errorRate: number;
  lastCheck: Date;
}

export class MonitoringService {
  private static readonly METRICS_RETENTION_DAYS = 30;
  private static readonly ALERT_THRESHOLDS = {
    cpu: { warning: 70, critical: 90 },
    memory: { warning: 80, critical: 95 },
    dbQueryTime: { warning: 1000, critical: 5000 }, // milliseconds
    redisMemory: { warning: 500 * 1024 * 1024, critical: 1024 * 1024 * 1024 }, // bytes
    apiResponseTime: { warning: 2000, critical: 5000 }, // milliseconds
    errorRate: { warning: 5, critical: 10 }, // percentage
  };

  private static metricsCollectionInterval: NodeJS.Timeout | null = null;
  private static alertCheckInterval: NodeJS.Timeout | null = null;
  private static startTime = Date.now();

  /**
   * Start monitoring service
   */
  static start(): void {
    Logger.info('Starting monitoring service');

    // Collect metrics every 30 seconds
    this.metricsCollectionInterval = setInterval(async () => {
      try {
        await this.collectMetrics();
      } catch (error) {
        Logger.error('Metrics collection error', error);
      }
    }, 30000);

    // Check alerts every minute
    this.alertCheckInterval = setInterval(async () => {
      try {
        await this.checkAlerts();
      } catch (error) {
        Logger.error('Alert checking error', error);
      }
    }, 60000);

    // Initial metrics collection
    this.collectMetrics().catch(error => 
      Logger.error('Initial metrics collection error', error)
    );
  }

  /**
   * Stop monitoring service
   */
  static stop(): void {
    Logger.info('Stopping monitoring service');

    if (this.metricsCollectionInterval) {
      clearInterval(this.metricsCollectionInterval);
      this.metricsCollectionInterval = null;
    }

    if (this.alertCheckInterval) {
      clearInterval(this.alertCheckInterval);
      this.alertCheckInterval = null;
    }
  }

  /**
   * Collect comprehensive system metrics
   */
  static async collectMetrics(): Promise<SystemMetrics> {
    try {
      const timestamp = new Date();
      
      // Collect CPU and memory metrics
      const memoryUsage = process.memoryUsage();
      const cpuUsage = process.cpuUsage();
      
      // Database metrics
      const dbMetrics = await this.getDatabaseMetrics();
      
      // Redis metrics
      const redisMetrics = await this.getRedisMetrics();
      
      // API metrics
      const apiMetrics = await this.getApiMetrics();

      const metrics: SystemMetrics = {
        timestamp,
        cpu: {
          usage: cpuUsage.user / 1000000, // Convert to milliseconds
          loadAverage: process.platform !== 'win32' ? require('os').loadavg() : [0, 0, 0],
        },
        memory: {
          used: memoryUsage.heapUsed,
          total: memoryUsage.heapTotal,
          percentage: (memoryUsage.heapUsed / memoryUsage.heapTotal) * 100,
          heapUsed: memoryUsage.heapUsed,
          heapTotal: memoryUsage.heapTotal,
        },
        database: dbMetrics,
        redis: redisMetrics,
        api: apiMetrics,
        application: {
          uptime: Date.now() - this.startTime,
          version: process.env.npm_package_version || '1.0.0',
          environment: process.env.NODE_ENV || 'development',
          buildId: process.env.BUILD_ID || 'local',
        },
      };

      // Store metrics in Redis with TTL
      await this.storeMetrics(metrics);
      
      // Log performance metrics
      Logger.logPerformance('metrics_collection', Date.now() - timestamp.getTime(), {
        component: 'monitoring',
      });

      return metrics;
    } catch (error) {
      Logger.error('Failed to collect metrics', error);
      throw error;
    }
  }

  /**
   * Get current health status
   */
  static async getHealthStatus(): Promise<HealthStatus> {
    try {
      const startTime = Date.now();
      
      // Check component health in parallel
      const [dbHealth, redisHealth, fsHealth, externalHealth] = await Promise.allSettled([
        this.checkDatabaseHealth(),
        this.checkRedisHealth(),
        this.checkFileSystemHealth(),
        this.checkExternalServicesHealth(),
      ]);

      const components = {
        database: dbHealth.status === 'fulfilled' && dbHealth.value,
        redis: redisHealth.status === 'fulfilled' && redisHealth.value,
        fileSystem: fsHealth.status === 'fulfilled' && fsHealth.value,
        external: externalHealth.status === 'fulfilled' && externalHealth.value,
      };

      const healthyComponents = Object.values(components).filter(Boolean).length;
      const totalComponents = Object.keys(components).length;
      
      let status: HealthStatus['status'] = 'healthy';
      if (healthyComponents === 0) {
        status = 'unhealthy';
      } else if (healthyComponents < totalComponents) {
        status = 'degraded';
      }

      // Get error rate from recent metrics
      const errorRate = await this.getRecentErrorRate();
      
      const responseTime = Date.now() - startTime;

      return {
        status,
        components,
        uptime: Date.now() - this.startTime,
        responseTime,
        errorRate,
        lastCheck: new Date(),
      };
    } catch (error) {
      Logger.error('Health status check failed', error);
      return {
        status: 'unhealthy',
        components: {
          database: false,
          redis: false,
          fileSystem: false,
          external: false,
        },
        uptime: Date.now() - this.startTime,
        responseTime: 0,
        errorRate: 100,
        lastCheck: new Date(),
      };
    }
  }

  /**
   * Get performance metrics for a time range
   */
  static async getMetrics(
    startTime: Date,
    endTime: Date,
    granularity: 'minute' | 'hour' | 'day' = 'hour'
  ): Promise<SystemMetrics[]> {
    try {
      const cacheKey = `metrics:${granularity}:${startTime.getTime()}:${endTime.getTime()}`;
      
      // Try to get from cache first
      const cached = await CacheService.get<SystemMetrics[]>(cacheKey);
      if (cached) {
        return cached;
      }

      // Get raw metrics from Redis
      const pattern = 'metrics:*';
      const keys = await redis.keys(pattern);
      
      const metrics: SystemMetrics[] = [];
      
      for (const key of keys) {
        const data = await redis.get(key);
        if (data) {
          const metric = JSON.parse(data) as SystemMetrics;
          const timestamp = new Date(metric.timestamp);
          
          if (timestamp >= startTime && timestamp <= endTime) {
            metrics.push(metric);
          }
        }
      }

      // Sort by timestamp
      metrics.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());

      // Aggregate based on granularity
      const aggregatedMetrics = this.aggregateMetrics(metrics, granularity);

      // Cache the results
      await CacheService.set(cacheKey, aggregatedMetrics, {
        ttl: granularity === 'minute' ? 300 : granularity === 'hour' ? 3600 : 86400,
        tags: ['metrics', 'performance'],
      });

      return aggregatedMetrics;
    } catch (error) {
      Logger.error('Failed to get metrics', error);
      return [];
    }
  }

  /**
   * Check for performance alerts
   */
  static async checkAlerts(): Promise<PerformanceAlert[]> {
    try {
      const alerts: PerformanceAlert[] = [];
      const latestMetrics = await this.getLatestMetrics();
      
      if (!latestMetrics) {
        return alerts;
      }

      // CPU usage alert
      if (latestMetrics.cpu.usage > this.ALERT_THRESHOLDS.cpu.critical) {
        alerts.push(this.createAlert(
          'cpu',
          'critical',
          'Critical CPU usage detected',
          latestMetrics.cpu.usage,
          this.ALERT_THRESHOLDS.cpu.critical
        ));
      } else if (latestMetrics.cpu.usage > this.ALERT_THRESHOLDS.cpu.warning) {
        alerts.push(this.createAlert(
          'cpu',
          'medium',
          'High CPU usage detected',
          latestMetrics.cpu.usage,
          this.ALERT_THRESHOLDS.cpu.warning
        ));
      }

      // Memory usage alert
      if (latestMetrics.memory.percentage > this.ALERT_THRESHOLDS.memory.critical) {
        alerts.push(this.createAlert(
          'memory',
          'critical',
          'Critical memory usage detected',
          latestMetrics.memory.percentage,
          this.ALERT_THRESHOLDS.memory.critical
        ));
      } else if (latestMetrics.memory.percentage > this.ALERT_THRESHOLDS.memory.warning) {
        alerts.push(this.createAlert(
          'memory',
          'medium',
          'High memory usage detected',
          latestMetrics.memory.percentage,
          this.ALERT_THRESHOLDS.memory.warning
        ));
      }

      // Database performance alerts
      if (latestMetrics.database.avgQueryTime > this.ALERT_THRESHOLDS.dbQueryTime.critical) {
        alerts.push(this.createAlert(
          'database',
          'critical',
          'Critical database query performance',
          latestMetrics.database.avgQueryTime,
          this.ALERT_THRESHOLDS.dbQueryTime.critical
        ));
      } else if (latestMetrics.database.avgQueryTime > this.ALERT_THRESHOLDS.dbQueryTime.warning) {
        alerts.push(this.createAlert(
          'database',
          'medium',
          'Slow database queries detected',
          latestMetrics.database.avgQueryTime,
          this.ALERT_THRESHOLDS.dbQueryTime.warning
        ));
      }

      // Redis memory alert
      if (latestMetrics.redis.memoryUsage > this.ALERT_THRESHOLDS.redisMemory.critical) {
        alerts.push(this.createAlert(
          'redis',
          'critical',
          'Critical Redis memory usage',
          latestMetrics.redis.memoryUsage,
          this.ALERT_THRESHOLDS.redisMemory.critical
        ));
      } else if (latestMetrics.redis.memoryUsage > this.ALERT_THRESHOLDS.redisMemory.warning) {
        alerts.push(this.createAlert(
          'redis',
          'medium',
          'High Redis memory usage',
          latestMetrics.redis.memoryUsage,
          this.ALERT_THRESHOLDS.redisMemory.warning
        ));
      }

      // API performance alerts
      if (latestMetrics.api.avgResponseTime > this.ALERT_THRESHOLDS.apiResponseTime.critical) {
        alerts.push(this.createAlert(
          'api',
          'critical',
          'Critical API response time',
          latestMetrics.api.avgResponseTime,
          this.ALERT_THRESHOLDS.apiResponseTime.critical
        ));
      } else if (latestMetrics.api.avgResponseTime > this.ALERT_THRESHOLDS.apiResponseTime.warning) {
        alerts.push(this.createAlert(
          'api',
          'medium',
          'Slow API response time',
          latestMetrics.api.avgResponseTime,
          this.ALERT_THRESHOLDS.apiResponseTime.warning
        ));
      }

      // Store alerts
      if (alerts.length > 0) {
        await this.storeAlerts(alerts);
        
        // Send notifications for critical alerts
        const criticalAlerts = alerts.filter(alert => alert.severity === 'critical');
        if (criticalAlerts.length > 0) {
          await this.sendAlertNotifications(criticalAlerts);
        }
      }

      return alerts;
    } catch (error) {
      Logger.error('Alert checking failed', error);
      return [];
    }
  }

  /**
   * Private helper methods
   */
  private static async getDatabaseMetrics() {
    try {
      // Get database connection info (implementation depends on your setup)
      const startTime = Date.now();
      
      // Test query performance
      await prisma.$queryRaw`SELECT 1`;
      const queryTime = Date.now() - startTime;

      // Get slow query count from logs (implementation specific)
      const slowQueries = 0; // Would be implemented based on your logging

      return {
        connections: 1, // Would get actual connection pool info
        avgQueryTime: queryTime,
        slowQueries,
        errorRate: 0, // Would calculate from error logs
      };
    } catch (error) {
      Logger.error('Database metrics collection failed', error);
      return {
        connections: 0,
        avgQueryTime: 0,
        slowQueries: 0,
        errorRate: 100,
      };
    }
  }

  private static async getRedisMetrics() {
    try {
      const info = await redis.info('memory');
      const memory = info.match(/used_memory:(\d+)/);
      const memoryUsage = memory ? parseInt(memory[1]) : 0;

      const cacheStats = await CacheService.getStats();

      return {
        connections: 1, // Would get actual connection info
        memoryUsage,
        hitRate: cacheStats.hitRate,
        commandsPerSecond: 0, // Would calculate from Redis stats
      };
    } catch (error) {
      Logger.error('Redis metrics collection failed', error);
      return {
        connections: 0,
        memoryUsage: 0,
        hitRate: 0,
        commandsPerSecond: 0,
      };
    }
  }

  private static async getApiMetrics() {
    try {
      // Get API metrics from recent logs or metrics store
      const now = Date.now();
      const oneMinuteAgo = now - 60000;

      // These would be collected from request logs
      return {
        requestsPerMinute: 0, // Would calculate from request logs
        avgResponseTime: 0, // Would calculate from response time logs
        errorRate: 0, // Would calculate from error logs
        activeConnections: 0, // Would get from server stats
      };
    } catch (error) {
      Logger.error('API metrics collection failed', error);
      return {
        requestsPerMinute: 0,
        avgResponseTime: 0,
        errorRate: 0,
        activeConnections: 0,
      };
    }
  }

  private static async storeMetrics(metrics: SystemMetrics): Promise<void> {
    try {
      const key = `metrics:${metrics.timestamp.getTime()}`;
      await redis.setex(key, 86400 * this.METRICS_RETENTION_DAYS, JSON.stringify(metrics));
    } catch (error) {
      Logger.error('Failed to store metrics', error);
    }
  }

  private static async getLatestMetrics(): Promise<SystemMetrics | null> {
    try {
      const keys = await redis.keys('metrics:*');
      if (keys.length === 0) return null;

      // Get the most recent key
      const latestKey = keys.sort().pop();
      if (!latestKey) return null;

      const data = await redis.get(latestKey);
      return data ? JSON.parse(data) : null;
    } catch (error) {
      Logger.error('Failed to get latest metrics', error);
      return null;
    }
  }

  private static createAlert(
    type: PerformanceAlert['type'],
    severity: PerformanceAlert['severity'],
    message: string,
    value: number,
    threshold: number
  ): PerformanceAlert {
    return {
      id: `${type}_${Date.now()}_${Math.random()}`,
      type,
      severity,
      message,
      value,
      threshold,
      timestamp: new Date(),
      resolved: false,
    };
  }

  private static async storeAlerts(alerts: PerformanceAlert[]): Promise<void> {
    try {
      const pipeline = redis.pipeline();
      
      for (const alert of alerts) {
        const key = `alert:${alert.id}`;
        pipeline.setex(key, 86400 * 7, JSON.stringify(alert)); // 7 days
      }
      
      await pipeline.exec();
      
      // Log alerts
      for (const alert of alerts) {
        Logger.warn(`Performance alert: ${alert.message}`, {
          component: 'monitoring',
          alertType: alert.type,
          severity: alert.severity,
          value: alert.value,
          threshold: alert.threshold,
        });
      }
    } catch (error) {
      Logger.error('Failed to store alerts', error);
    }
  }

  private static async sendAlertNotifications(alerts: PerformanceAlert[]): Promise<void> {
    try {
      // Implementation for sending notifications (email, Slack, webhook, etc.)
      Logger.critical(`Critical performance alerts detected: ${alerts.length} alerts`, undefined, {
        component: 'monitoring',
        alerts: alerts.map(a => ({ type: a.type, message: a.message, value: a.value })),
      });
    } catch (error) {
      Logger.error('Failed to send alert notifications', error);
    }
  }

  private static aggregateMetrics(
    metrics: SystemMetrics[],
    granularity: 'minute' | 'hour' | 'day'
  ): SystemMetrics[] {
    // Simple aggregation - in a real implementation, you'd want more sophisticated aggregation
    // For now, just return the metrics as-is
    return metrics;
  }

  private static async checkDatabaseHealth(): Promise<boolean> {
    try {
      await prisma.$queryRaw`SELECT 1`;
      return true;
    } catch {
      return false;
    }
  }

  private static async checkRedisHealth(): Promise<boolean> {
    try {
      await redis.ping();
      return true;
    } catch {
      return false;
    }
  }

  private static async checkFileSystemHealth(): Promise<boolean> {
    try {
      const fs = await import('fs/promises');
      await fs.access('./');
      return true;
    } catch {
      return false;
    }
  }

  private static async checkExternalServicesHealth(): Promise<boolean> {
    // Check external services like email providers, CDNs, etc.
    return true; // Placeholder
  }

  private static async getRecentErrorRate(): Promise<number> {
    try {
      // Calculate error rate from recent logs
      const healthMetrics = await Logger.getHealthMetrics();
      return healthMetrics.errorRate;
    } catch {
      return 0;
    }
  }
}

export default MonitoringService;