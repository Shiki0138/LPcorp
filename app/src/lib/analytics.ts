/**
 * Analytics Service for Landing Page Tracking
 * Provides comprehensive analytics capabilities for LP performance monitoring
 */

import { prisma } from './database';
import { redis } from './redis';
import type { EventType } from '@prisma/client';

export interface AnalyticsEventData {
  landingPageId?: string;
  eventType: EventType;
  eventName: string;
  eventData?: Record<string, any>;
  sessionId: string;
  userId?: string;
  ipAddress?: string;
  userAgent?: string;
  referrer?: string;
  country?: string;
  region?: string;
  city?: string;
  deviceType?: string;
  browser?: string;
  os?: string;
  screenSize?: string;
  conversionValue?: number;
  conversionType?: string;
  funnelStep?: number;
}

export interface AnalyticsMetrics {
  views: number;
  uniqueViews: number;
  conversions: number;
  conversionRate: number;
  bounceRate: number;
  avgSessionTime: number;
  topSources: Array<{ source: string; count: number }>;
  deviceBreakdown: Record<string, number>;
  geographicData: Record<string, number>;
  timeBasedData: Array<{ timestamp: string; value: number }>;
}

export class AnalyticsService {
  private static readonly CACHE_TTL = 300; // 5 minutes
  private static readonly BATCH_SIZE = 100;
  private static readonly REAL_TIME_KEY_PREFIX = 'analytics:rt:';

  /**
   * Track a single analytics event
   */
  static async trackEvent(data: AnalyticsEventData): Promise<void> {
    try {
      // Store in database
      await prisma.analyticsEvent.create({
        data: {
          landingPageId: data.landingPageId,
          eventType: data.eventType,
          eventName: data.eventName,
          eventData: data.eventData || null,
          sessionId: data.sessionId,
          userId: data.userId,
          ipAddress: data.ipAddress,
          userAgent: data.userAgent,
          referrer: data.referrer,
          country: data.country,
          region: data.region,
          city: data.city,
          deviceType: data.deviceType,
          browser: data.browser,
          os: data.os,
          screenSize: data.screenSize,
          conversionValue: data.conversionValue,
          conversionType: data.conversionType,
          funnelStep: data.funnelStep,
        },
      });

      // Update real-time counters in Redis
      if (data.landingPageId) {
        await this.updateRealTimeMetrics(data.landingPageId, data);
      }

      // Update landing page metrics if applicable
      if (data.landingPageId && data.eventType === 'PAGE_VIEW') {
        await this.incrementPageViews(data.landingPageId, data.sessionId);
      }

      if (data.landingPageId && data.eventType === 'CONVERSION') {
        await this.incrementConversions(data.landingPageId);
      }
    } catch (error) {
      console.error('Failed to track analytics event:', error);
      throw new Error('Analytics tracking failed');
    }
  }

  /**
   * Track multiple events in batch
   */
  static async trackEventsBatch(events: AnalyticsEventData[]): Promise<void> {
    try {
      // Process in batches
      for (let i = 0; i < events.length; i += this.BATCH_SIZE) {
        const batch = events.slice(i, i + this.BATCH_SIZE);
        
        // Insert batch into database
        await prisma.analyticsEvent.createMany({
          data: batch.map(event => ({
            landingPageId: event.landingPageId,
            eventType: event.eventType,
            eventName: event.eventName,
            eventData: event.eventData || null,
            sessionId: event.sessionId,
            userId: event.userId,
            ipAddress: event.ipAddress,
            userAgent: event.userAgent,
            referrer: event.referrer,
            country: event.country,
            region: event.region,
            city: event.city,
            deviceType: event.deviceType,
            browser: event.browser,
            os: event.os,
            screenSize: event.screenSize,
            conversionValue: event.conversionValue,
            conversionType: event.conversionType,
            funnelStep: event.funnelStep,
          })),
        });

        // Update real-time metrics for each event
        for (const event of batch) {
          if (event.landingPageId) {
            await this.updateRealTimeMetrics(event.landingPageId, event);
          }
        }
      }
    } catch (error) {
      console.error('Failed to track analytics events batch:', error);
      throw new Error('Batch analytics tracking failed');
    }
  }

  /**
   * Get comprehensive analytics for a landing page
   */
  static async getLandingPageAnalytics(
    landingPageId: string,
    timeRange: 'hour' | 'day' | 'week' | 'month' | 'year' = 'day'
  ): Promise<AnalyticsMetrics> {
    const cacheKey = `analytics:${landingPageId}:${timeRange}`;
    
    try {
      // Try to get from cache first
      const cached = await redis.get(cacheKey);
      if (cached) {
        return JSON.parse(cached);
      }

      // Calculate date range
      const now = new Date();
      const startDate = this.getStartDate(now, timeRange);

      // Get landing page metrics
      const landingPage = await prisma.landingPage.findUnique({
        where: { id: landingPageId },
        select: {
          views: true,
          uniqueViews: true,
          conversions: true,
          conversionRate: true,
          bounceRate: true,
          avgSessionTime: true,
        },
      });

      if (!landingPage) {
        throw new Error('Landing page not found');
      }

      // Get detailed analytics from events
      const events = await prisma.analyticsEvent.findMany({
        where: {
          landingPageId,
          timestamp: {
            gte: startDate,
            lte: now,
          },
        },
        select: {
          eventType: true,
          eventName: true,
          timestamp: true,
          referrer: true,
          deviceType: true,
          country: true,
          sessionId: true,
          conversionValue: true,
        },
      });

      // Process analytics data
      const metrics: AnalyticsMetrics = {
        views: landingPage.views,
        uniqueViews: landingPage.uniqueViews,
        conversions: landingPage.conversions,
        conversionRate: landingPage.conversionRate,
        bounceRate: landingPage.bounceRate,
        avgSessionTime: landingPage.avgSessionTime,
        topSources: this.calculateTopSources(events),
        deviceBreakdown: this.calculateDeviceBreakdown(events),
        geographicData: this.calculateGeographicData(events),
        timeBasedData: this.calculateTimeBasedData(events, timeRange),
      };

      // Cache the results
      await redis.setex(cacheKey, this.CACHE_TTL, JSON.stringify(metrics));

      return metrics;
    } catch (error) {
      console.error('Failed to get landing page analytics:', error);
      throw new Error('Analytics retrieval failed');
    }
  }

  /**
   * Get real-time analytics data
   */
  static async getRealTimeAnalytics(landingPageId: string): Promise<{
    currentVisitors: number;
    recentEvents: Array<{ type: string; timestamp: number; count: number }>;
    conversionRate: number;
  }> {
    try {
      const pipeline = redis.pipeline();
      
      // Get current visitors (unique sessions in last 30 minutes)
      pipeline.scard(`${this.REAL_TIME_KEY_PREFIX}visitors:${landingPageId}`);
      
      // Get recent events (last hour)
      pipeline.hgetall(`${this.REAL_TIME_KEY_PREFIX}events:${landingPageId}`);
      
      // Get conversion rate
      pipeline.hget(`${this.REAL_TIME_KEY_PREFIX}conversions:${landingPageId}`, 'rate');
      
      const results = await pipeline.exec();
      
      if (!results) {
        throw new Error('Redis pipeline failed');
      }

      const currentVisitors = results[0]?.[1] as number || 0;
      const eventsData = results[1]?.[1] as Record<string, string> || {};
      const conversionRate = parseFloat(results[2]?.[1] as string || '0');

      const recentEvents = Object.entries(eventsData).map(([type, data]) => {
        const parsed = JSON.parse(data);
        return {
          type,
          timestamp: parsed.timestamp,
          count: parsed.count,
        };
      });

      return {
        currentVisitors,
        recentEvents,
        conversionRate,
      };
    } catch (error) {
      console.error('Failed to get real-time analytics:', error);
      throw new Error('Real-time analytics retrieval failed');
    }
  }

  /**
   * Update real-time metrics in Redis
   */
  private static async updateRealTimeMetrics(
    landingPageId: string,
    data: AnalyticsEventData
  ): Promise<void> {
    try {
      const pipeline = redis.pipeline();
      const now = Date.now();

      // Track active visitors (expire after 30 minutes)
      if (data.sessionId) {
        pipeline.sadd(`${this.REAL_TIME_KEY_PREFIX}visitors:${landingPageId}`, data.sessionId);
        pipeline.expire(`${this.REAL_TIME_KEY_PREFIX}visitors:${landingPageId}`, 1800);
      }

      // Track recent events
      const eventKey = `${this.REAL_TIME_KEY_PREFIX}events:${landingPageId}`;
      const eventData = JSON.stringify({ timestamp: now, count: 1 });
      pipeline.hset(eventKey, data.eventType, eventData);
      pipeline.expire(eventKey, 3600); // 1 hour

      // Track conversions
      if (data.eventType === 'CONVERSION') {
        const conversionKey = `${this.REAL_TIME_KEY_PREFIX}conversions:${landingPageId}`;
        pipeline.hincrby(conversionKey, 'count', 1);
        pipeline.expire(conversionKey, 3600);
      }

      await pipeline.exec();
    } catch (error) {
      console.error('Failed to update real-time metrics:', error);
    }
  }

  /**
   * Increment page views for a landing page
   */
  private static async incrementPageViews(
    landingPageId: string,
    sessionId: string
  ): Promise<void> {
    try {
      // Check if this session has already been counted
      const sessionKey = `session:${landingPageId}:${sessionId}`;
      const isUnique = await redis.setnx(sessionKey, '1');
      
      if (isUnique) {
        // Set expiration for session tracking (24 hours)
        await redis.expire(sessionKey, 86400);
        
        // Increment both total and unique views
        await prisma.landingPage.update({
          where: { id: landingPageId },
          data: {
            views: { increment: 1 },
            uniqueViews: { increment: 1 },
          },
        });
      } else {
        // Only increment total views
        await prisma.landingPage.update({
          where: { id: landingPageId },
          data: {
            views: { increment: 1 },
          },
        });
      }
    } catch (error) {
      console.error('Failed to increment page views:', error);
    }
  }

  /**
   * Increment conversions for a landing page
   */
  private static async incrementConversions(landingPageId: string): Promise<void> {
    try {
      const landingPage = await prisma.landingPage.findUnique({
        where: { id: landingPageId },
        select: { conversions: true, views: true },
      });

      if (!landingPage) return;

      const newConversions = landingPage.conversions + 1;
      const conversionRate = landingPage.views > 0 
        ? (newConversions / landingPage.views) * 100 
        : 0;

      await prisma.landingPage.update({
        where: { id: landingPageId },
        data: {
          conversions: newConversions,
          conversionRate,
        },
      });
    } catch (error) {
      console.error('Failed to increment conversions:', error);
    }
  }

  /**
   * Helper methods for data processing
   */
  private static getStartDate(now: Date, timeRange: string): Date {
    const date = new Date(now);
    switch (timeRange) {
      case 'hour':
        date.setHours(date.getHours() - 1);
        break;
      case 'day':
        date.setDate(date.getDate() - 1);
        break;
      case 'week':
        date.setDate(date.getDate() - 7);
        break;
      case 'month':
        date.setMonth(date.getMonth() - 1);
        break;
      case 'year':
        date.setFullYear(date.getFullYear() - 1);
        break;
    }
    return date;
  }

  private static calculateTopSources(events: any[]): Array<{ source: string; count: number }> {
    const sources = events.reduce((acc, event) => {
      if (event.referrer) {
        const source = new URL(event.referrer).hostname;
        acc[source] = (acc[source] || 0) + 1;
      }
      return acc;
    }, {} as Record<string, number>);

    return Object.entries(sources)
      .map(([source, count]) => ({ source, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10);
  }

  private static calculateDeviceBreakdown(events: any[]): Record<string, number> {
    return events.reduce((acc, event) => {
      if (event.deviceType) {
        acc[event.deviceType] = (acc[event.deviceType] || 0) + 1;
      }
      return acc;
    }, {} as Record<string, number>);
  }

  private static calculateGeographicData(events: any[]): Record<string, number> {
    return events.reduce((acc, event) => {
      if (event.country) {
        acc[event.country] = (acc[event.country] || 0) + 1;
      }
      return acc;
    }, {} as Record<string, number>);
  }

  private static calculateTimeBasedData(
    events: any[],
    timeRange: string
  ): Array<{ timestamp: string; value: number }> {
    const groupBy = timeRange === 'hour' ? 'minute' : 
                   timeRange === 'day' ? 'hour' : 'day';
    
    const grouped = events.reduce((acc, event) => {
      const date = new Date(event.timestamp);
      let key: string;
      
      if (groupBy === 'minute') {
        key = `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
      } else if (groupBy === 'hour') {
        key = `${date.getHours()}:00`;
      } else {
        key = date.toISOString().split('T')[0];
      }
      
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return Object.entries(grouped)
      .map(([timestamp, value]) => ({ timestamp, value }))
      .sort((a, b) => a.timestamp.localeCompare(b.timestamp));
  }
}

export default AnalyticsService;