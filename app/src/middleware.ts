/**
 * Next.js Middleware for Security and Request Processing
 * Implements rate limiting, security headers, authentication checks, and request validation
 */

import { NextRequest, NextResponse } from 'next/server';
import { getToken } from 'next-auth/jwt';
import { SecurityService } from '@/lib/security';

// Route configurations
const PUBLIC_ROUTES = [
  '/api/auth',
  '/auth',
  '/lp', // Landing pages should be publicly accessible
  '/_next',
  '/favicon.ico',
  '/robots.txt',
  '/sitemap.xml',
];

const API_ROUTES = ['/api'];
const ADMIN_ROUTES = ['/admin', '/api/admin'];
const PROTECTED_ROUTES = ['/dashboard', '/projects', '/analytics'];

// Rate limit configurations for different route types
const RATE_LIMITS = {
  api: { window: 900, maxRequests: 100 }, // 100 requests per 15 minutes for API
  auth: { window: 300, maxRequests: 5 },  // 5 requests per 5 minutes for auth
  upload: { window: 3600, maxRequests: 20 }, // 20 requests per hour for uploads
  default: { window: 900, maxRequests: 200 }, // 200 requests per 15 minutes for general
};

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const startTime = Date.now();

  try {
    // Skip middleware for static assets and Next.js internals
    if (
      pathname.startsWith('/_next') ||
      pathname.startsWith('/static') ||
      pathname.includes('.') && !pathname.startsWith('/api')
    ) {
      return NextResponse.next();
    }

    // Create response with security headers
    const response = NextResponse.next();
    
    // Apply security headers to all responses
    const securityHeaders = SecurityService.getCSPHeaders();
    Object.entries(securityHeaders).forEach(([key, value]) => {
      response.headers.set(key, value);
    });

    // Add performance and debugging headers
    response.headers.set('X-Request-ID', crypto.randomUUID());
    response.headers.set('X-Timestamp', Date.now().toString());

    // Determine rate limit configuration
    let rateLimitConfig = RATE_LIMITS.default;
    if (pathname.startsWith('/api/auth')) {
      rateLimitConfig = RATE_LIMITS.auth;
    } else if (pathname.startsWith('/api/assets/upload') || pathname.startsWith('/api/upload')) {
      rateLimitConfig = RATE_LIMITS.upload;
    } else if (pathname.startsWith('/api')) {
      rateLimitConfig = RATE_LIMITS.api;
    }

    // Apply rate limiting
    const rateLimitResult = await SecurityService.checkRateLimit(
      request,
      undefined,
      rateLimitConfig
    );

    // Set rate limit headers
    response.headers.set('X-RateLimit-Limit', rateLimitConfig.maxRequests.toString());
    response.headers.set('X-RateLimit-Remaining', rateLimitResult.remaining.toString());
    response.headers.set('X-RateLimit-Reset', rateLimitResult.resetTime.toString());

    if (!rateLimitResult.allowed) {
      response.headers.set('Retry-After', (rateLimitResult.retryAfter || 900).toString());
      return new NextResponse(
        JSON.stringify({
          error: 'Rate limit exceeded',
          retryAfter: rateLimitResult.retryAfter,
        }),
        {
          status: 429,
          headers: {
            'Content-Type': 'application/json',
            ...Object.fromEntries(response.headers.entries()),
          },
        }
      );
    }

    // Check if route is public
    const isPublicRoute = PUBLIC_ROUTES.some(route => pathname.startsWith(route));
    
    if (isPublicRoute) {
      // Add processing time header
      response.headers.set('X-Response-Time', `${Date.now() - startTime}ms`);
      return response;
    }

    // Get authentication token
    const token = await getToken({ 
      req: request, 
      secret: process.env.NEXTAUTH_SECRET 
    });

    // Check authentication for protected routes
    const isProtectedRoute = PROTECTED_ROUTES.some(route => pathname.startsWith(route));
    const isApiRoute = API_ROUTES.some(route => pathname.startsWith(route));
    const isAdminRoute = ADMIN_ROUTES.some(route => pathname.startsWith(route));

    if (isProtectedRoute || (isApiRoute && !isPublicRoute)) {
      if (!token) {
        if (isApiRoute) {
          return new NextResponse(
            JSON.stringify({ error: 'Authentication required' }),
            {
              status: 401,
              headers: {
                'Content-Type': 'application/json',
                ...Object.fromEntries(response.headers.entries()),
              },
            }
          );
        } else {
          // Redirect to login for web routes
          const loginUrl = new URL('/auth/signin', request.url);
          loginUrl.searchParams.set('callbackUrl', pathname);
          return NextResponse.redirect(loginUrl);
        }
      }

      // Check user status
      if (token.status !== 'ACTIVE') {
        if (isApiRoute) {
          return new NextResponse(
            JSON.stringify({ error: 'Account inactive' }),
            {
              status: 403,
              headers: {
                'Content-Type': 'application/json',
                ...Object.fromEntries(response.headers.entries()),
              },
            }
          );
        } else {
          return NextResponse.redirect(new URL('/auth/account-inactive', request.url));
        }
      }
    }

    // Check admin access
    if (isAdminRoute) {
      if (!token || (token.role !== 'ADMIN' && token.role !== 'SUPER_ADMIN')) {
        if (isApiRoute) {
          return new NextResponse(
            JSON.stringify({ error: 'Admin access required' }),
            {
              status: 403,
              headers: {
                'Content-Type': 'application/json',
                ...Object.fromEntries(response.headers.entries()),
              },
            }
          );
        } else {
          return NextResponse.redirect(new URL('/auth/unauthorized', request.url));
        }
      }
    }

    // Validate CSRF token for state-changing operations
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)) {
      const contentType = request.headers.get('content-type') || '';
      
      // Skip CSRF check for multipart/form-data (file uploads) and auth endpoints
      if (
        !pathname.startsWith('/api/auth') &&
        !contentType.includes('multipart/form-data')
      ) {
        const csrfToken = request.headers.get('x-csrf-token');
        const sessionId = token?.sub || 'anonymous';
        
        if (!csrfToken || !SecurityService.validateCSRFToken(csrfToken, sessionId)) {
          return new NextResponse(
            JSON.stringify({ error: 'Invalid CSRF token' }),
            {
              status: 403,
              headers: {
                'Content-Type': 'application/json',
                ...Object.fromEntries(response.headers.entries()),
              },
            }
          );
        }
      }
    }

    // Request size validation for API routes
    if (isApiRoute && ['POST', 'PUT', 'PATCH'].includes(request.method)) {
      const contentLength = request.headers.get('content-length');
      const maxSize = 10 * 1024 * 1024; // 10MB
      
      if (contentLength && parseInt(contentLength) > maxSize) {
        return new NextResponse(
          JSON.stringify({ error: 'Request too large' }),
          {
            status: 413,
            headers: {
              'Content-Type': 'application/json',
              ...Object.fromEntries(response.headers.entries()),
            },
          }
        );
      }
    }

    // Add user context to request headers for API routes
    if (isApiRoute && token) {
      response.headers.set('X-User-ID', token.sub || '');
      response.headers.set('X-User-Role', token.role || '');
    }

    // Log request for monitoring (async, don't wait)
    logRequest(request, response, token, startTime).catch(console.error);

    // Add processing time header
    response.headers.set('X-Response-Time', `${Date.now() - startTime}ms`);

    return response;

  } catch (error) {
    console.error('Middleware error:', error);
    
    // Return a generic error response
    return new NextResponse(
      JSON.stringify({ error: 'Internal server error' }),
      {
        status: 500,
        headers: {
          'Content-Type': 'application/json',
          'X-Response-Time': `${Date.now() - startTime}ms`,
        },
      }
    );
  }
}

/**
 * Log request for monitoring and analytics
 */
async function logRequest(
  request: NextRequest,
  response: NextResponse,
  token: any,
  startTime: number
): Promise<void> {
  try {
    const { pathname } = request.nextUrl;
    const method = request.method;
    const userAgent = request.headers.get('user-agent') || 'unknown';
    const referer = request.headers.get('referer') || 'direct';
    const ip = request.headers.get('x-forwarded-for')?.split(',')[0] ||
               request.headers.get('x-real-ip') ||
               'unknown';
    
    const logData = {
      timestamp: new Date().toISOString(),
      method,
      pathname,
      ip,
      userAgent,
      referer,
      userId: token?.sub,
      userRole: token?.role,
      responseTime: Date.now() - startTime,
      requestId: response.headers.get('X-Request-ID'),
    };

    // For now, just log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.log('Request:', logData);
    }

    // In production, you might want to send this to a logging service
    // like DataDog, CloudWatch, or a custom analytics endpoint
  } catch (error) {
    console.error('Request logging error:', error);
  }
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
};