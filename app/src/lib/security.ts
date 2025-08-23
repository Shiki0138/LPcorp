/**
 * Advanced Security Service
 * Comprehensive security features including rate limiting, input validation, and access control
 */

import { NextRequest } from 'next/server';
import { redis } from './redis';
import { prisma } from './database';
import bcrypt from 'bcryptjs';
import { z } from 'zod';
import crypto from 'crypto';

export interface SecurityConfig {
  rateLimit: {
    window: number; // Time window in seconds
    maxRequests: number; // Max requests per window
    skipSuccessfulRequests?: boolean;
    skipFailedRequests?: boolean;
  };
  authentication: {
    maxLoginAttempts: number;
    lockoutDuration: number; // in seconds
    sessionTimeout: number; // in seconds
    passwordMinLength: number;
    requireSpecialChars: boolean;
    requireNumbers: boolean;
    requireUppercase: boolean;
  };
  fileUpload: {
    maxFileSize: number;
    allowedMimeTypes: string[];
    scanForMalware: boolean;
    quarantineDirectory: string;
  };
  apiSecurity: {
    requireHttps: boolean;
    enableCors: boolean;
    corsOrigins: string[];
    enableCSRF: boolean;
    maxRequestSize: number;
  };
}

export interface RateLimitResult {
  allowed: boolean;
  remaining: number;
  resetTime: number;
  retryAfter?: number;
}

export interface SecurityAuditEvent {
  id: string;
  type: 'login_attempt' | 'rate_limit_exceeded' | 'suspicious_activity' | 'file_upload' | 'api_access';
  severity: 'low' | 'medium' | 'high' | 'critical';
  userId?: string;
  ipAddress: string;
  userAgent: string;
  details: Record<string, any>;
  timestamp: Date;
}

export class SecurityService {
  private static readonly DEFAULT_CONFIG: SecurityConfig = {
    rateLimit: {
      window: 900, // 15 minutes
      maxRequests: 100,
      skipSuccessfulRequests: false,
      skipFailedRequests: false,
    },
    authentication: {
      maxLoginAttempts: 5,
      lockoutDuration: 1800, // 30 minutes
      sessionTimeout: 86400, // 24 hours
      passwordMinLength: 8,
      requireSpecialChars: true,
      requireNumbers: true,
      requireUppercase: true,
    },
    fileUpload: {
      maxFileSize: 50 * 1024 * 1024, // 50MB
      allowedMimeTypes: [
        'image/jpeg',
        'image/png',
        'image/webp',
        'image/gif',
        'application/pdf',
        'text/plain',
      ],
      scanForMalware: true,
      quarantineDirectory: './quarantine',
    },
    apiSecurity: {
      requireHttps: process.env.NODE_ENV === 'production',
      enableCors: true,
      corsOrigins: [process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000'],
      enableCSRF: true,
      maxRequestSize: 10 * 1024 * 1024, // 10MB
    },
  };

  /**
   * Rate limiting implementation
   */
  static async checkRateLimit(
    request: NextRequest,
    identifier?: string,
    config?: Partial<SecurityConfig['rateLimit']>
  ): Promise<RateLimitResult> {
    try {
      const rateLimitConfig = { ...this.DEFAULT_CONFIG.rateLimit, ...config };
      const clientIdentifier = identifier || this.getClientIdentifier(request);
      const key = `rate_limit:${clientIdentifier}`;
      const now = Date.now();
      const windowStart = now - (rateLimitConfig.window * 1000);

      // Use Redis sorted set for sliding window rate limiting
      const pipeline = redis.pipeline();
      
      // Remove old entries
      pipeline.zremrangebyscore(key, 0, windowStart);
      
      // Add current request
      pipeline.zadd(key, now, `${now}-${Math.random()}`);
      
      // Count current requests
      pipeline.zcard(key);
      
      // Set expiration
      pipeline.expire(key, rateLimitConfig.window);
      
      const results = await pipeline.exec();
      
      if (!results) {
        throw new Error('Redis pipeline failed');
      }

      const requestCount = results[2]?.[1] as number || 0;
      const allowed = requestCount <= rateLimitConfig.maxRequests;
      
      if (!allowed) {
        // Log rate limit exceeded
        await this.logSecurityEvent({
          type: 'rate_limit_exceeded',
          severity: 'medium',
          ipAddress: this.getClientIP(request),
          userAgent: request.headers.get('user-agent') || 'unknown',
          details: {
            identifier: clientIdentifier,
            requestCount,
            limit: rateLimitConfig.maxRequests,
            window: rateLimitConfig.window,
          },
        });
      }

      return {
        allowed,
        remaining: Math.max(0, rateLimitConfig.maxRequests - requestCount),
        resetTime: windowStart + (rateLimitConfig.window * 1000),
        retryAfter: allowed ? undefined : rateLimitConfig.window,
      };
    } catch (error) {
      console.error('Rate limiting error:', error);
      // Fail open for availability
      return {
        allowed: true,
        remaining: this.DEFAULT_CONFIG.rateLimit.maxRequests,
        resetTime: Date.now() + (this.DEFAULT_CONFIG.rateLimit.window * 1000),
      };
    }
  }

  /**
   * Input validation and sanitization
   */
  static validateInput<T>(
    input: unknown,
    schema: z.ZodSchema<T>,
    options: {
      stripUnknown?: boolean;
      sanitizeHtml?: boolean;
      maxDepth?: number;
    } = {}
  ): { success: boolean; data?: T; errors?: z.ZodError } {
    try {
      // Basic depth check to prevent deeply nested objects
      if (options.maxDepth && this.getObjectDepth(input) > options.maxDepth) {
        throw new Error('Input object too deeply nested');
      }

      // Sanitize HTML content if requested
      let sanitizedInput = input;
      if (options.sanitizeHtml) {
        sanitizedInput = this.sanitizeHtml(input);
      }

      const result = schema.safeParse(sanitizedInput);
      
      if (result.success) {
        return { success: true, data: result.data };
      } else {
        return { success: false, errors: result.error };
      }
    } catch (error) {
      console.error('Input validation error:', error);
      return { success: false };
    }
  }

  /**
   * Password security utilities
   */
  static async hashPassword(password: string): Promise<string> {
    const saltRounds = 12;
    return bcrypt.hash(password, saltRounds);
  }

  static async verifyPassword(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }

  static validatePasswordStrength(password: string): {
    isValid: boolean;
    score: number;
    feedback: string[];
  } {
    const config = this.DEFAULT_CONFIG.authentication;
    const feedback: string[] = [];
    let score = 0;

    // Length check
    if (password.length < config.passwordMinLength) {
      feedback.push(`Password must be at least ${config.passwordMinLength} characters long`);
    } else {
      score += 20;
    }

    // Uppercase check
    if (config.requireUppercase && !/[A-Z]/.test(password)) {
      feedback.push('Password must contain at least one uppercase letter');
    } else if (/[A-Z]/.test(password)) {
      score += 20;
    }

    // Numbers check
    if (config.requireNumbers && !/\d/.test(password)) {
      feedback.push('Password must contain at least one number');
    } else if (/\d/.test(password)) {
      score += 20;
    }

    // Special characters check
    if (config.requireSpecialChars && !/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      feedback.push('Password must contain at least one special character');
    } else if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      score += 20;
    }

    // Additional length bonus
    if (password.length >= 12) score += 10;
    if (password.length >= 16) score += 10;

    // Diversity check
    const uniqueChars = new Set(password.toLowerCase()).size;
    if (uniqueChars >= password.length * 0.7) {
      score += 10;
    }

    return {
      isValid: feedback.length === 0 && score >= 60,
      score: Math.min(100, score),
      feedback,
    };
  }

  /**
   * User account lockout management
   */
  static async checkAccountLockout(email: string): Promise<{
    isLocked: boolean;
    attemptsRemaining: number;
    lockoutExpiry?: Date;
  }> {
    try {
      const key = `lockout:${email}`;
      const data = await redis.hmget(key, 'attempts', 'lockoutExpiry');
      
      const attempts = parseInt(data[0] || '0');
      const lockoutExpiry = data[1] ? new Date(data[1]) : null;
      
      const config = this.DEFAULT_CONFIG.authentication;
      const isLocked = lockoutExpiry && lockoutExpiry > new Date();
      
      return {
        isLocked: isLocked || false,
        attemptsRemaining: Math.max(0, config.maxLoginAttempts - attempts),
        lockoutExpiry: lockoutExpiry || undefined,
      };
    } catch (error) {
      console.error('Account lockout check error:', error);
      return { isLocked: false, attemptsRemaining: 5 };
    }
  }

  static async recordLoginAttempt(
    email: string,
    success: boolean,
    ipAddress: string,
    userAgent: string
  ): Promise<void> {
    try {
      const key = `lockout:${email}`;
      const config = this.DEFAULT_CONFIG.authentication;

      if (success) {
        // Clear lockout on successful login
        await redis.del(key);
      } else {
        // Increment failed attempts
        const attempts = await redis.hincrby(key, 'attempts', 1);
        
        if (attempts >= config.maxLoginAttempts) {
          // Lock account
          const lockoutExpiry = new Date(Date.now() + (config.lockoutDuration * 1000));
          await redis.hset(key, 'lockoutExpiry', lockoutExpiry.toISOString());
          await redis.expire(key, config.lockoutDuration);
          
          // Log security event
          await this.logSecurityEvent({
            type: 'login_attempt',
            severity: 'high',
            ipAddress,
            userAgent,
            details: {
              email,
              action: 'account_locked',
              attempts,
              lockoutExpiry,
            },
          });
        }
      }

      // Always log login attempts
      await this.logSecurityEvent({
        type: 'login_attempt',
        severity: success ? 'low' : 'medium',
        ipAddress,
        userAgent,
        details: {
          email,
          success,
          attempts: success ? 0 : await redis.hget(key, 'attempts'),
        },
      });
    } catch (error) {
      console.error('Login attempt recording error:', error);
    }
  }

  /**
   * File upload security
   */
  static async validateFileUpload(
    file: Buffer,
    filename: string,
    mimeType: string
  ): Promise<{
    isValid: boolean;
    errors: string[];
    sanitizedFilename: string;
  }> {
    const errors: string[] = [];
    const config = this.DEFAULT_CONFIG.fileUpload;

    // File size check
    if (file.length > config.maxFileSize) {
      errors.push(`File size exceeds maximum of ${config.maxFileSize} bytes`);
    }

    // MIME type check
    if (!config.allowedMimeTypes.includes(mimeType)) {
      errors.push(`File type ${mimeType} is not allowed`);
    }

    // Filename sanitization
    const sanitizedFilename = this.sanitizeFilename(filename);

    // Magic number validation (basic file signature check)
    if (!this.validateFileSignature(file, mimeType)) {
      errors.push('File signature does not match declared MIME type');
    }

    // Malware scanning (basic implementation)
    if (config.scanForMalware && this.containsSuspiciousPatterns(file)) {
      errors.push('File contains suspicious patterns');
    }

    return {
      isValid: errors.length === 0,
      errors,
      sanitizedFilename,
    };
  }

  /**
   * CSRF protection
   */
  static generateCSRFToken(sessionId: string): string {
    return crypto
      .createHmac('sha256', process.env.CSRF_SECRET || 'fallback-secret')
      .update(sessionId)
      .digest('hex');
  }

  static validateCSRFToken(token: string, sessionId: string): boolean {
    const expectedToken = this.generateCSRFToken(sessionId);
    return crypto.timingSafeEqual(
      Buffer.from(token, 'hex'),
      Buffer.from(expectedToken, 'hex')
    );
  }

  /**
   * Content Security Policy headers
   */
  static getCSPHeaders(): Record<string, string> {
    const csp = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://vercel.live",
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
      "font-src 'self' https://fonts.gstatic.com",
      "img-src 'self' data: https: blob:",
      "media-src 'self'",
      "object-src 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'",
      "upgrade-insecure-requests",
    ].join('; ');

    return {
      'Content-Security-Policy': csp,
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'X-XSS-Protection': '1; mode=block',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
    };
  }

  /**
   * Security audit logging
   */
  private static async logSecurityEvent(
    event: Omit<SecurityAuditEvent, 'id' | 'timestamp'>
  ): Promise<void> {
    try {
      const auditEvent: SecurityAuditEvent = {
        id: crypto.randomUUID(),
        timestamp: new Date(),
        ...event,
      };

      // Store in Redis for quick access
      const key = `security_events:${Date.now()}:${auditEvent.id}`;
      await redis.setex(key, 86400, JSON.stringify(auditEvent)); // 24 hours

      // Store in database for long-term analysis
      await prisma.auditLog.create({
        data: {
          userId: event.userId,
          action: event.type,
          resource: 'security',
          resourceId: auditEvent.id,
          oldValues: null,
          newValues: {
            severity: event.severity,
            details: event.details,
          },
          ipAddress: event.ipAddress,
          userAgent: event.userAgent,
        },
      });

      // Alert on critical events
      if (event.severity === 'critical') {
        await this.alertSecurityTeam(auditEvent);
      }
    } catch (error) {
      console.error('Security event logging error:', error);
    }
  }

  /**
   * Helper methods
   */
  private static getClientIdentifier(request: NextRequest): string {
    const ip = this.getClientIP(request);
    const userAgent = request.headers.get('user-agent') || 'unknown';
    return crypto.createHash('sha256').update(`${ip}:${userAgent}`).digest('hex');
  }

  private static getClientIP(request: NextRequest): string {
    return (
      request.headers.get('x-forwarded-for')?.split(',')[0] ||
      request.headers.get('x-real-ip') ||
      request.headers.get('cf-connecting-ip') ||
      'unknown'
    );
  }

  private static getObjectDepth(obj: any, depth = 0): number {
    if (depth > 10) return depth; // Prevent infinite recursion
    if (obj === null || typeof obj !== 'object') return depth;
    
    const depths = Object.values(obj).map(value => 
      this.getObjectDepth(value, depth + 1)
    );
    
    return Math.max(depth, ...depths);
  }

  private static sanitizeHtml(input: any): any {
    if (typeof input === 'string') {
      return input
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;')
        .replace(/\//g, '&#x2F;');
    }
    
    if (Array.isArray(input)) {
      return input.map(item => this.sanitizeHtml(item));
    }
    
    if (input && typeof input === 'object') {
      const sanitized: any = {};
      for (const [key, value] of Object.entries(input)) {
        sanitized[key] = this.sanitizeHtml(value);
      }
      return sanitized;
    }
    
    return input;
  }

  private static sanitizeFilename(filename: string): string {
    return filename
      .replace(/[^a-zA-Z0-9._-]/g, '_')
      .replace(/_{2,}/g, '_')
      .substring(0, 255);
  }

  private static validateFileSignature(buffer: Buffer, mimeType: string): boolean {
    if (buffer.length < 4) return false;

    const signatures: Record<string, number[][]> = {
      'image/jpeg': [[0xFF, 0xD8, 0xFF]],
      'image/png': [[0x89, 0x50, 0x4E, 0x47]],
      'image/gif': [[0x47, 0x49, 0x46, 0x38]],
      'application/pdf': [[0x25, 0x50, 0x44, 0x46]],
    };

    const expectedSignatures = signatures[mimeType];
    if (!expectedSignatures) return true; // Unknown type, allow

    return expectedSignatures.some(signature =>
      signature.every((byte, index) => buffer[index] === byte)
    );
  }

  private static containsSuspiciousPatterns(buffer: Buffer): boolean {
    const suspiciousPatterns = [
      /eval\s*\(/gi,
      /exec\s*\(/gi,
      /<script/gi,
      /javascript:/gi,
      /vbscript:/gi,
      /data:text\/html/gi,
    ];

    const content = buffer.toString('utf8', 0, Math.min(buffer.length, 1024));
    return suspiciousPatterns.some(pattern => pattern.test(content));
  }

  private static async alertSecurityTeam(event: SecurityAuditEvent): Promise<void> {
    // Implementation would depend on your alerting system
    // Could be email, Slack, webhook, etc.
    console.error('CRITICAL SECURITY EVENT:', event);
  }
}

export default SecurityService;