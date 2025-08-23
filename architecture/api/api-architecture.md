# RESTful + GraphQL Hybrid API Architecture

## API Strategy Overview

### Hybrid API Approach
- **GraphQL**: Complex queries, real-time subscriptions, frontend-focused
- **REST**: Simple CRUD operations, third-party integrations, mobile apps
- **WebSocket**: Real-time collaboration, live updates, notifications

### API Gateway Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 Global API Gateway                          │
│              (Kong Enterprise/AWS API Gateway)              │
│                                                             │
│  Rate Limiting │ Authentication │ Caching │ Analytics       │
│  Load Balancing │ Request Routing │ SSL Termination         │
└─────────────────────┬───────────────────────────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    ▼                 ▼                 ▼
┌──────────┐  ┌──────────────┐  ┌──────────────┐
│GraphQL   │  │   REST API   │  │ WebSocket    │
│Federation│  │   Gateway    │  │   Server     │
│Gateway   │  │   (Express)  │  │ (Socket.io)  │
└──────────┘  └──────────────┘  └──────────────┘
    │                 │                 │
    └─────────────────┼─────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                Microservices Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ LP Builder  │ │ User Mgmt   │ │ Analytics   │   ...     │
│  │ Service     │ │ Service     │ │ Service     │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

## GraphQL Federation Architecture

### Schema Federation Strategy

```graphql
# User Service Schema
type User @key(fields: "id") {
  id: ID!
  email: String!
  profile: UserProfile
  landingPages: [LandingPage!]!
}

type UserProfile {
  firstName: String
  lastName: String
  avatar: String
  subscription: SubscriptionTier
}

# LP Builder Service Schema
type LandingPage @key(fields: "id") {
  id: ID!
  title: String!
  slug: String!
  content: PageContent!
  template: Template
  status: PageStatus!
  owner: User! @provides(fields: "id email")
  analytics: PageAnalytics!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type PageContent {
  sections: [ContentSection!]!
  metadata: PageMetadata
  customCSS: String
  customJS: String
}

# Analytics Service Schema
type PageAnalytics @key(fields: "pageId") {
  pageId: ID!
  views: Int!
  uniqueVisitors: Int!
  conversionRate: Float
  performanceScore: Float
  metrics: AnalyticsMetrics!
}

# Federated Queries
query GetLandingPageWithAnalytics($pageId: ID!) {
  landingPage(id: $pageId) {
    id
    title
    content {
      sections {
        type
        data
      }
    }
    owner {
      email
      profile {
        firstName
        lastName
      }
    }
    analytics {
      views
      uniqueVisitors
      conversionRate
      performanceScore
    }
  }
}
```

### Apollo Gateway Configuration

```typescript
import { ApolloGateway } from '@apollo/gateway';
import { ApolloServer } from '@apollo/server';

const gateway = new ApolloGateway({
  supergraphSdl: process.env.APOLLO_SCHEMA_CONFIG_EMBEDDED,
  serviceList: [
    { name: 'users', url: 'http://user-service:4001/graphql' },
    { name: 'landing-pages', url: 'http://lp-service:4002/graphql' },
    { name: 'analytics', url: 'http://analytics-service:4003/graphql' },
    { name: 'files', url: 'http://file-service:4004/graphql' },
  ],
  introspectionHeaders: {
    'Apollo-Require-Preflight': 'true',
  },
});

const server = new ApolloServer({
  gateway,
  plugins: [
    // Performance monitoring
    ApolloServerPluginUsageReporting(),
    // Caching
    responseCachePlugin({
      sessionId: (requestContext) => requestContext.request.http?.headers.get('authorization') || null,
    }),
    // Rate limiting
    ApolloServerPluginRateLimiting({
      keyGenerator: (requestContext) => requestContext.request.http?.headers.get('x-user-id'),
      defaultRule: {
        windowMs: 60000,
        max: 1000,
      },
    }),
  ],
});
```

## REST API Specification

### Core REST Endpoints

```yaml
openapi: 3.0.3
info:
  title: LP Production System API
  version: 2.0.0
  description: Enterprise-grade Landing Page production system

servers:
  - url: https://api.lp-system.com/v2
    description: Production API
  - url: https://staging-api.lp-system.com/v2
    description: Staging API

paths:
  # Authentication Endpoints
  /auth/login:
    post:
      summary: User login
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                email:
                  type: string
                  format: email
                password:
                  type: string
                mfaCode:
                  type: string
      responses:
        '200':
          description: Login successful
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                  refreshToken:
                    type: string
                  user:
                    $ref: '#/components/schemas/User'

  # Landing Pages CRUD
  /landing-pages:
    get:
      summary: List user's landing pages
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
        - name: status
          in: query
          schema:
            type: string
            enum: [draft, published, archived]
      responses:
        '200':
          description: Landing pages list
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/LandingPage'
                  pagination:
                    $ref: '#/components/schemas/Pagination'

    post:
      summary: Create new landing page
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateLandingPageRequest'
      responses:
        '201':
          description: Landing page created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LandingPage'

  /landing-pages/{pageId}:
    get:
      summary: Get landing page by ID
      parameters:
        - name: pageId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Landing page details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LandingPage'

    put:
      summary: Update landing page
      parameters:
        - name: pageId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateLandingPageRequest'
      responses:
        '200':
          description: Landing page updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LandingPage'

  # Analytics Endpoints
  /analytics/pages/{pageId}/metrics:
    get:
      summary: Get page analytics
      parameters:
        - name: pageId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: period
          in: query
          schema:
            type: string
            enum: [day, week, month, year]
            default: week
      responses:
        '200':
          description: Analytics data
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageAnalytics'

  # File Upload Endpoints
  /files/upload:
    post:
      summary: Upload file to storage
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                folder:
                  type: string
                  enum: [images, videos, documents]
      responses:
        '201':
          description: File uploaded successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FileUploadResponse'

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    User:
      type: object
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
          format: email
        profile:
          $ref: '#/components/schemas/UserProfile'
        createdAt:
          type: string
          format: date-time

    LandingPage:
      type: object
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
        slug:
          type: string
        content:
          $ref: '#/components/schemas/PageContent'
        status:
          type: string
          enum: [draft, published, archived]
        templateId:
          type: string
          format: uuid
        seoMetadata:
          $ref: '#/components/schemas/SEOMetadata'
        publishedAt:
          type: string
          format: date-time
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

security:
  - BearerAuth: []
```

### API Performance Standards

```typescript
// Rate Limiting Configuration
const rateLimitConfig = {
  // Per user limits
  userLimits: {
    free: { requests: 100, window: '1h' },
    pro: { requests: 1000, window: '1h' },
    enterprise: { requests: 10000, window: '1h' }
  },
  
  // Per endpoint limits
  endpointLimits: {
    '/auth/login': { requests: 10, window: '15m' },
    '/landing-pages': { requests: 100, window: '1h' },
    '/files/upload': { requests: 50, window: '1h' }
  }
};

// Response Time SLAs
const performanceTargets = {
  p50: 200, // ms
  p95: 500, // ms
  p99: 1000, // ms
  availability: 99.99 // %
};
```

## WebSocket Real-Time Architecture

### Socket.io Implementation

```typescript
import { Server } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { Redis } from 'ioredis';

const pubClient = new Redis(process.env.REDIS_URL);
const subClient = pubClient.duplicate();

const io = new Server(server, {
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(','),
    credentials: true
  },
  adapter: createAdapter(pubClient, subClient)
});

// Authentication middleware
io.use(async (socket, next) => {
  const token = socket.handshake.auth.token;
  try {
    const user = await verifyJWT(token);
    socket.user = user;
    next();
  } catch (error) {
    next(new Error('Authentication failed'));
  }
});

// Landing page collaboration
io.of('/collaboration').on('connection', (socket) => {
  socket.on('join-page', async (pageId) => {
    // Verify user has access to page
    const hasAccess = await checkPageAccess(socket.user.id, pageId);
    if (!hasAccess) {
      socket.emit('error', 'Access denied');
      return;
    }
    
    socket.join(pageId);
    socket.to(pageId).emit('user-joined', {
      userId: socket.user.id,
      userName: socket.user.profile.firstName
    });
  });

  socket.on('page-update', (data) => {
    socket.to(data.pageId).emit('page-updated', {
      userId: socket.user.id,
      changes: data.changes,
      timestamp: Date.now()
    });
  });
});

// Analytics real-time updates
io.of('/analytics').on('connection', (socket) => {
  socket.on('subscribe-page-metrics', (pageId) => {
    socket.join(`metrics:${pageId}`);
  });
});
```

## API Security Implementation

### Authentication & Authorization

```typescript
// JWT Configuration
const jwtConfig = {
  accessToken: {
    secret: process.env.JWT_ACCESS_SECRET,
    expiresIn: '15m',
    algorithm: 'RS256'
  },
  refreshToken: {
    secret: process.env.JWT_REFRESH_SECRET,
    expiresIn: '7d',
    algorithm: 'HS256'
  }
};

// RBAC Implementation
class RBACMiddleware {
  static requireRole(roles: string[]) {
    return (req: Request, res: Response, next: NextFunction) => {
      const userRole = req.user?.role;
      if (!userRole || !roles.includes(userRole)) {
        return res.status(403).json({ error: 'Insufficient permissions' });
      }
      next();
    };
  }

  static requirePermission(resource: string, action: string) {
    return async (req: Request, res: Response, next: NextFunction) => {
      const hasPermission = await checkPermission(
        req.user.id, 
        resource, 
        action, 
        req.params
      );
      
      if (!hasPermission) {
        return res.status(403).json({ error: 'Access denied' });
      }
      next();
    };
  }
}
```

## Monitoring & Observability

### API Metrics Collection

```typescript
// Prometheus metrics
import client from 'prom-client';

const httpRequestDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'HTTP request duration in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.001, 0.005, 0.015, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1, 5]
});

const httpRequestsTotal = new client.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code']
});

// Middleware for metrics collection
export const metricsMiddleware = (req: Request, res: Response, next: NextFunction) => {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    
    httpRequestDuration
      .labels(req.method, req.route?.path || req.path, res.statusCode.toString())
      .observe(duration);
      
    httpRequestsTotal
      .labels(req.method, req.route?.path || req.path, res.statusCode.toString())
      .inc();
  });
  
  next();
};
```

This hybrid API architecture provides maximum flexibility, performance, and security for the LP production system.