/**
 * Advanced Caching Service with Redis
 * Provides intelligent caching strategies for LP generation and analytics
 */

import { redis } from './redis';
import { prisma } from './database';

export interface CacheOptions {
  ttl?: number; // Time to live in seconds
  tags?: string[]; // Cache tags for invalidation
  serialize?: boolean; // Whether to JSON serialize
  compress?: boolean; // Whether to compress data
  namespace?: string; // Cache namespace
}

export interface CacheStats {
  hits: number;
  misses: number;
  hitRate: number;
  totalKeys: number;
  memoryUsage: number;
}

export class CacheService {
  private static readonly DEFAULT_TTL = 3600; // 1 hour
  private static readonly DEFAULT_NAMESPACE = 'app';
  private static readonly STATS_KEY = 'cache:stats';
  private static readonly TAG_PREFIX = 'tag:';

  /**
   * Get value from cache
   */
  static async get<T>(
    key: string,
    options: CacheOptions = {}
  ): Promise<T | null> {
    try {
      const fullKey = this.buildKey(key, options.namespace);
      const value = await redis.get(fullKey);
      
      if (value === null) {
        await this.incrementStat('misses');
        return null;
      }

      await this.incrementStat('hits');
      
      if (options.serialize !== false) {
        return JSON.parse(value);
      }
      
      return value as T;
    } catch (error) {
      console.error('Cache get error:', error);
      return null;
    }
  }

  /**
   * Set value in cache
   */
  static async set<T>(
    key: string,
    value: T,
    options: CacheOptions = {}
  ): Promise<boolean> {
    try {
      const fullKey = this.buildKey(key, options.namespace);
      const ttl = options.ttl || this.DEFAULT_TTL;
      
      let serializedValue: string;
      if (options.serialize !== false) {
        serializedValue = JSON.stringify(value);
      } else {
        serializedValue = value as string;
      }

      // Set the value with TTL
      await redis.setex(fullKey, ttl, serializedValue);

      // Handle cache tags for invalidation
      if (options.tags && options.tags.length > 0) {
        await this.setTags(fullKey, options.tags, ttl);
      }

      return true;
    } catch (error) {
      console.error('Cache set error:', error);
      return false;
    }
  }

  /**
   * Delete from cache
   */
  static async delete(
    key: string,
    namespace?: string
  ): Promise<boolean> {
    try {
      const fullKey = this.buildKey(key, namespace);
      const result = await redis.del(fullKey);
      return result > 0;
    } catch (error) {
      console.error('Cache delete error:', error);
      return false;
    }
  }

  /**
   * Get or set with callback (cache-aside pattern)
   */
  static async getOrSet<T>(
    key: string,
    callback: () => Promise<T>,
    options: CacheOptions = {}
  ): Promise<T> {
    // Try to get from cache first
    const cached = await this.get<T>(key, options);
    if (cached !== null) {
      return cached;
    }

    // Execute callback to get fresh data
    const value = await callback();
    
    // Store in cache
    await this.set(key, value, options);
    
    return value;
  }

  /**
   * Invalidate cache by tags
   */
  static async invalidateByTags(tags: string[]): Promise<number> {
    try {
      let invalidatedCount = 0;
      
      for (const tag of tags) {
        const tagKey = `${this.TAG_PREFIX}${tag}`;
        const keys = await redis.smembers(tagKey);
        
        if (keys.length > 0) {
          // Delete all keys associated with this tag
          await redis.del(...keys);
          invalidatedCount += keys.length;
          
          // Remove the tag set
          await redis.del(tagKey);
        }
      }
      
      return invalidatedCount;
    } catch (error) {
      console.error('Cache invalidation error:', error);
      return 0;
    }
  }

  /**
   * Invalidate cache by pattern
   */
  static async invalidateByPattern(pattern: string): Promise<number> {
    try {
      const keys = await redis.keys(pattern);
      if (keys.length > 0) {
        await redis.del(...keys);
        return keys.length;
      }
      return 0;
    } catch (error) {
      console.error('Cache pattern invalidation error:', error);
      return 0;
    }
  }

  /**
   * Get cache statistics
   */
  static async getStats(): Promise<CacheStats> {
    try {
      const pipeline = redis.pipeline();
      
      pipeline.hget(this.STATS_KEY, 'hits');
      pipeline.hget(this.STATS_KEY, 'misses');
      pipeline.dbsize();
      pipeline.memory('usage', this.STATS_KEY);
      
      const results = await pipeline.exec();
      
      if (!results) {
        throw new Error('Failed to get cache stats');
      }

      const hits = parseInt(results[0]?.[1] as string || '0');
      const misses = parseInt(results[1]?.[1] as string || '0');
      const totalKeys = results[2]?.[1] as number || 0;
      const memoryUsage = results[3]?.[1] as number || 0;
      
      const total = hits + misses;
      const hitRate = total > 0 ? (hits / total) * 100 : 0;

      return {
        hits,
        misses,
        hitRate,
        totalKeys,
        memoryUsage,
      };
    } catch (error) {
      console.error('Cache stats error:', error);
      return {
        hits: 0,
        misses: 0,
        hitRate: 0,
        totalKeys: 0,
        memoryUsage: 0,
      };
    }
  }

  /**
   * Warm up cache with frequently accessed data
   */
  static async warmUp(): Promise<void> {
    try {
      console.log('Starting cache warm-up...');
      
      // Warm up landing page templates
      const templates = await prisma.landingPageTemplate.findMany({
        where: { isActive: true },
        include: {
          _count: {
            select: { landingPages: true },
          },
        },
        orderBy: { usageCount: 'desc' },
        take: 50,
      });

      for (const template of templates) {
        await this.set(
          `template:${template.id}`,
          template,
          {
            ttl: 7200, // 2 hours
            tags: ['templates', 'popular'],
            namespace: 'lp',
          }
        );
      }

      // Warm up system configuration
      const configs = await prisma.systemConfig.findMany({
        where: { isEditable: true },
      });

      const configMap = configs.reduce((acc, config) => {
        acc[config.key] = config.value;
        return acc;
      }, {} as Record<string, string>);

      await this.set(
        'system:config',
        configMap,
        {
          ttl: 3600, // 1 hour
          tags: ['system', 'config'],
        }
      );

      // Warm up asset library categories
      const assetCategories = await prisma.assetLibrary.groupBy({
        by: ['category'],
        _count: {
          category: true,
        },
      });

      await this.set(
        'assets:categories',
        assetCategories,
        {
          ttl: 1800, // 30 minutes
          tags: ['assets', 'categories'],
        }
      );

      console.log('Cache warm-up completed');
    } catch (error) {
      console.error('Cache warm-up error:', error);
    }
  }

  /**
   * Clear all cache
   */
  static async clear(): Promise<boolean> {
    try {
      await redis.flushdb();
      await this.resetStats();
      return true;
    } catch (error) {
      console.error('Cache clear error:', error);
      return false;
    }
  }

  /**
   * Health check for cache service
   */
  static async healthCheck(): Promise<{
    status: 'healthy' | 'unhealthy';
    latency: number;
    error?: string;
  }> {
    const start = Date.now();
    
    try {
      // Test basic operations
      await redis.ping();
      await this.set('health:check', 'ok', { ttl: 10 });
      const result = await this.get('health:check');
      await this.delete('health:check');
      
      const latency = Date.now() - start;
      
      if (result === 'ok') {
        return { status: 'healthy', latency };
      } else {
        return {
          status: 'unhealthy',
          latency,
          error: 'Cache read/write test failed',
        };
      }
    } catch (error) {
      const latency = Date.now() - start;
      return {
        status: 'unhealthy',
        latency,
        error: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Build full cache key with namespace
   */
  private static buildKey(key: string, namespace?: string): string {
    const ns = namespace || this.DEFAULT_NAMESPACE;
    return `${ns}:${key}`;
  }

  /**
   * Set cache tags for invalidation
   */
  private static async setTags(
    cacheKey: string,
    tags: string[],
    ttl: number
  ): Promise<void> {
    try {
      const pipeline = redis.pipeline();
      
      for (const tag of tags) {
        const tagKey = `${this.TAG_PREFIX}${tag}`;
        pipeline.sadd(tagKey, cacheKey);
        pipeline.expire(tagKey, ttl);
      }
      
      await pipeline.exec();
    } catch (error) {
      console.error('Cache tag setting error:', error);
    }
  }

  /**
   * Increment cache statistics
   */
  private static async incrementStat(stat: 'hits' | 'misses'): Promise<void> {
    try {
      await redis.hincrby(this.STATS_KEY, stat, 1);
    } catch (error) {
      console.error('Cache stat increment error:', error);
    }
  }

  /**
   * Reset cache statistics
   */
  private static async resetStats(): Promise<void> {
    try {
      await redis.del(this.STATS_KEY);
    } catch (error) {
      console.error('Cache stat reset error:', error);
    }
  }
}

// Specialized cache utilities for common use cases

export class LPCacheService extends CacheService {
  /**
   * Cache landing page template
   */
  static async cacheTemplate(templateId: string, ttl = 3600): Promise<void> {
    const template = await prisma.landingPageTemplate.findUnique({
      where: { id: templateId },
      include: {
        _count: {
          select: { landingPages: true },
        },
      },
    });

    if (template) {
      await this.set(
        `template:${templateId}`,
        template,
        {
          ttl,
          tags: ['templates', `template:${templateId}`],
          namespace: 'lp',
        }
      );
    }
  }

  /**
   * Cache landing page analytics
   */
  static async cacheAnalytics(
    landingPageId: string,
    timeRange: string,
    data: any,
    ttl = 300
  ): Promise<void> {
    await this.set(
      `analytics:${landingPageId}:${timeRange}`,
      data,
      {
        ttl,
        tags: ['analytics', `page:${landingPageId}`],
        namespace: 'lp',
      }
    );
  }

  /**
   * Invalidate landing page cache
   */
  static async invalidatePage(landingPageId: string): Promise<void> {
    await this.invalidateByTags([`page:${landingPageId}`]);
  }

  /**
   * Invalidate template cache
   */
  static async invalidateTemplate(templateId: string): Promise<void> {
    await this.invalidateByTags([`template:${templateId}`]);
  }
}

export default CacheService;