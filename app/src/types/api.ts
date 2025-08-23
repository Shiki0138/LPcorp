/**
 * API Type Definitions
 * Comprehensive type definitions for all API endpoints and responses
 */

import type { 
  ProjectType, 
  ProjectStatus, 
  Priority, 
  FileType, 
  AccessLevel,
  LPCategory,
  LPStatus,
  EventType,
  AssetCategory,
  UserRole,
  UserStatus
} from '@prisma/client';

// =====================================================
// Common Types
// =====================================================

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
  details?: Array<{
    field: string;
    message: string;
  }>;
}

export interface PaginationMeta {
  page: number;
  limit: number;
  totalCount: number;
  totalPages: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
}

export interface PaginatedResponse<T> extends ApiResponse<T> {
  data: T & {
    pagination: PaginationMeta;
  };
}

// =====================================================
// User & Authentication Types
// =====================================================

export interface User {
  id: string;
  email: string;
  name?: string;
  image?: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Session {
  user: {
    id: string;
    email: string;
    name?: string;
    image?: string;
    role: UserRole;
  };
  expires: string;
}

// =====================================================
// Project Types
// =====================================================

export interface ProjectBase {
  id: string;
  name: string;
  description: string;
  type: ProjectType;
  status: ProjectStatus;
  priority: Priority;
  progress: number;
  budget?: number;
  estimatedCost?: number;
  actualCost?: number;
  startDate?: string;
  endDate?: string;
  deadline?: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Project extends ProjectBase {
  client: {
    id: string;
    name?: string;
    email: string;
  };
  admin?: {
    id: string;
    name?: string;
    email: string;
  };
  landingPage?: LandingPageSummary;
  fileCount: number;
  landingPageCount: number;
  healthScore: number;
  isOverdue: boolean;
  daysUntilDeadline?: number;
}

export interface CreateProjectRequest {
  name: string;
  description: string;
  type: ProjectType;
  priority?: Priority;
  budget?: number;
  estimatedCost?: number;
  deadline?: string;
  tags?: string[];
  metadata?: Record<string, any>;
  landingPageConfig?: LandingPageConfig;
}

export interface CreateProjectResponse {
  project: ProjectBase & {
    client: {
      id: string;
      name?: string;
      email: string;
    };
  };
  landingPage?: {
    id: string;
    title: string;
    slug: string;
    status: LPStatus;
    url: string;
  };
}

export interface ProjectListFilters {
  search?: string;
  status?: ProjectStatus;
  type?: ProjectType;
  priority?: Priority;
  sortBy?: 'createdAt' | 'updatedAt' | 'name' | 'priority' | 'deadline' | 'progress';
  sortOrder?: 'asc' | 'desc';
  includeArchived?: boolean;
  tags?: string;
  page?: number;
  limit?: number;
}

export interface ProjectStats {
  total: number;
  byStatus: Array<{
    status: ProjectStatus;
    count: number;
    percentage: number;
  }>;
  byType: Array<{
    type: ProjectType;
    count: number;
    percentage: number;
  }>;
  byPriority: Array<{
    priority: Priority;
    count: number;
    percentage: number;
  }>;
  recentActivity: number;
}

export interface ProjectListResponse {
  projects: Project[];
  pagination: PaginationMeta;
  filters: ProjectListFilters;
  stats: ProjectStats;
}

// =====================================================
// Landing Page Types
// =====================================================

export interface LandingPageConfig {
  templateId?: string;
  category?: LPCategory;
  targetAudience?: string;
  valueProposition?: string;
  conversionGoal?: string;
  colorScheme?: {
    primary: string;
    secondary: string;
    accent: string;
  };
  typography?: {
    headingFont: string;
    bodyFont: string;
  };
  heroSection?: {
    headline: string;
    subheadline?: string;
    ctaText: string;
    backgroundImage?: string;
  };
}

export interface LandingPageSummary {
  id: string;
  title: string;
  slug: string;
  status: LPStatus;
  views: number;
  conversions: number;
  conversionRate: number;
}

export interface LandingPageTemplate {
  id: string;
  name: string;
  description?: string;
  category: LPCategory;
  industry: string[];
  isPremium: boolean;
  rating?: number;
  usageCount: number;
}

export interface LandingPageSection {
  id: string;
  type: string;
  content: Record<string, any>;
  order?: number;
  visible?: boolean;
}

export interface LandingPage {
  id: string;
  projectId: string;
  templateId?: string;
  title: string;
  slug: string;
  metaDescription?: string;
  keywords: string[];
  heroSection: Record<string, any>;
  contentSections: {
    sections: LandingPageSection[];
  };
  ctaElements: Record<string, any>;
  formElements: Record<string, any>;
  colorScheme: Record<string, any>;
  typography: Record<string, any>;
  layout: Record<string, any>;
  targetAudience?: string;
  valueProposition?: string;
  conversionGoal?: string;
  views: number;
  uniqueViews: number;
  conversions: number;
  conversionRate: number;
  bounceRate: number;
  avgSessionTime: number;
  status: LPStatus;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// =====================================================
// File & Asset Types
// =====================================================

export interface FileUploadOptions {
  projectId?: string;
  category?: string;
  accessLevel?: AccessLevel;
  processImages?: boolean;
  generateThumbnails?: boolean;
  optimizeImages?: boolean;
  quality?: number;
  tags?: string;
}

export interface ProcessedFile {
  id: string;
  filename: string;
  originalName: string;
  mimeType: string;
  size: number;
  path: string;
  url: string;
  fileType: FileType;
  width?: number;
  height?: number;
  format?: string;
  thumbnailPath?: string;
  thumbnailUrl?: string;
  optimized: boolean;
  processed: boolean;
}

export interface FileUploadResult {
  originalName: string;
  file?: ProcessedFile;
  warnings?: string[];
  error?: string;
}

export interface AssetLibraryEntry {
  id: string;
  name: string;
  category: AssetCategory;
  url?: string;
  thumbnailUrl?: string;
}

export interface UploadResponse {
  uploaded: Array<{
    originalName: string;
    file: ProcessedFile;
    warnings?: string[];
  }>;
  failed: Array<{
    originalName: string;
    error: string;
  }>;
  assetLibraryEntries: AssetLibraryEntry[];
}

export interface UploadStats {
  totalFiles: number;
  successfulUploads: number;
  failedUploads: number;
  totalSize: number;
  averageFileSize: number;
}

export interface UploadLimits {
  maxFileSize: number;
  maxTotalSize: number;
  maxFiles: number;
  allowedTypes: string[];
}

export interface StorageUsage {
  totalSize: number;
  fileCount: number;
  remainingSize: number;
}

export interface UploadConfigResponse {
  limits: UploadLimits;
  usage: StorageUsage;
  supportedCategories: AssetCategory[];
  accessLevels: AccessLevel[];
}

// =====================================================
// Analytics Types
// =====================================================

export interface AnalyticsEvent {
  landingPageId?: string | undefined;
  eventType: EventType;
  eventName: string;
  eventData?: Record<string, any> | undefined;
  sessionId: string;
  userId?: string | undefined;
  referrer?: string | undefined;
  conversionValue?: number | undefined;
  conversionType?: string | undefined;
  funnelStep?: number | undefined;
}

export interface AnalyticsEventBatch {
  events: AnalyticsEvent[];
}

export interface AnalyticsMetrics {
  views: number;
  uniqueViews: number;
  conversions: number;
  conversionRate: number;
  bounceRate: number;
  avgSessionTime: number;
  topSources: Array<{
    source: string;
    count: number;
  }>;
  deviceBreakdown: Record<string, number>;
  geographicData: Record<string, number>;
  timeBasedData: Array<{
    timestamp: string;
    value: number;
  }>;
}

export interface RealTimeAnalytics {
  currentVisitors: number;
  recentEvents: Array<{
    type: string;
    timestamp: number;
    count: number;
  }>;
  conversionRate: number;
}

export interface AnalyticsResponse {
  type: 'realtime' | 'historical';
  landingPageId: string;
  timeRange?: 'hour' | 'day' | 'week' | 'month' | 'year';
  analytics: AnalyticsMetrics | RealTimeAnalytics;
  timestamp: string;
  generatedAt?: string;
}

export interface TrackEventResponse {
  eventType: EventType;
  eventName: string;
  landingPageId?: string;
}

export interface TrackBatchResponse {
  eventsTracked: number;
  eventTypes: EventType[];
}

// =====================================================
// Error Types
// =====================================================

export interface ValidationError {
  field: string;
  message: string;
}

export interface ApiError {
  error: string;
  message?: string;
  details?: ValidationError[];
  statusCode?: number;
}

// =====================================================
// Cache & Performance Types
// =====================================================

export interface CacheStats {
  hits: number;
  misses: number;
  hitRate: number;
  totalKeys: number;
  memoryUsage: number;
}

export interface SystemHealthCheck {
  status: 'healthy' | 'unhealthy';
  latency: number;
  error?: string;
  timestamp: string;
  services: {
    database: boolean;
    redis: boolean;
    fileStorage: boolean;
  };
}

// =====================================================
// Utility Types
// =====================================================

export type TimeRange = 'hour' | 'day' | 'week' | 'month' | 'year';

export type SortOrder = 'asc' | 'desc';

export type RequestMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

// =====================================================
// API Endpoint Types
// =====================================================

export interface ApiEndpoint {
  method: RequestMethod;
  path: string;
  description: string;
  authentication: boolean;
  requestBody?: any;
  responseBody?: any;
  queryParams?: Record<string, any>;
}

// =====================================================
// Form & Validation Types
// =====================================================

export interface FormField {
  name: string;
  type: 'text' | 'email' | 'tel' | 'number' | 'select' | 'textarea' | 'checkbox' | 'radio' | 'file';
  label: string;
  placeholder?: string;
  required?: boolean;
  validation?: {
    pattern?: string;
    min?: number;
    max?: number;
    minLength?: number;
    maxLength?: number;
  };
  options?: Array<{
    value: string;
    label: string;
  }>;
}

export interface FormConfiguration {
  fields: FormField[];
  submitText: string;
  successMessage: string;
  errorMessage?: string;
  redirectUrl?: string;
  emailNotification?: boolean;
}

// =====================================================
// Export all types
// =====================================================

export type {
  ProjectType,
  ProjectStatus,
  Priority,
  FileType,
  AccessLevel,
  LPCategory,
  LPStatus,
  EventType,
  AssetCategory,
  UserRole,
  UserStatus,
};