import { Prisma } from '@prisma/client';

// Re-export all API types for unified access
export * from './api';

// Enhanced Prisma types with includes
export type UserWithRelations = Prisma.UserGetPayload<{
  include: {
    clientProfile: true;
    projects: true;
    landingPages: true;
  };
}>;

export type ClientProfileWithUser = Prisma.ClientProfileGetPayload<{
  include: {
    user: true;
  };
}>;

export type ProjectWithRelations = Prisma.ProjectGetPayload<{
  include: {
    client: true;
    admin: true;
    files: true;
    landingPages: true;
  };
}>;

export type ProjectFileWithRelations = Prisma.ProjectFileGetPayload<{
  include: {
    project: true;
    parentFile: true;
    childFiles: true;
  };
}>;

export type LandingPageWithRelations = Prisma.LandingPageGetPayload<{
  include: {
    project: true;
    template: true;
    analyticsEvents: true;
  };
}>;

export type LandingPageTemplateWithRelations = Prisma.LandingPageTemplateGetPayload<{
  include: {
    landingPages: true;
    _count: {
      select: {
        landingPages: true;
      };
    };
  };
}>;

export type AssetLibraryWithCollections = Prisma.AssetLibraryGetPayload<{
  include: {
    collections: true;
  };
}>;

// Form types
export interface ClientFormData {
  // Personal Information
  contactPerson: string;
  email: string;
  phone: string;
  companyName?: string;
  website?: string;
  address?: string;

  // Business Information
  industry: string;
  businessType: 'STARTUP' | 'SMALL_BUSINESS' | 'MEDIUM_BUSINESS' | 'ENTERPRISE' | 'NON_PROFIT' | 'GOVERNMENT' | 'OTHER';
  projectBudget: string;
  timeline: string;

  // Project Requirements
  projectType: ('WEBSITE' | 'MOBILE_APP' | 'WEB_APP' | 'ECOMMERCE' | 'LANDING_PAGE' | 'BRANDING' | 'MARKETING' | 'CONSULTATION' | 'MAINTENANCE' | 'OTHER')[];
  description: string;
  requirements?: string;
  additionalInfo?: string;
}

export interface FormValidationResult {
  isValid: boolean;
  errors: Record<string, string>;
  sanitizedData: Partial<ClientFormData>;
}

export interface AutoClassificationResult {
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  category: string;
  estimatedBudget?: number;
  complexity: 'LOW' | 'MEDIUM' | 'HIGH';
  recommendedTimeline: string;
}

// File processing types
export interface ImageProcessingOptions {
  width?: number;
  height?: number;
  quality?: number;
  format?: 'jpeg' | 'png' | 'webp';
  createThumbnail?: boolean;
  thumbnailWidth?: number;
  thumbnailHeight?: number;
}

export interface ProcessedImage {
  originalPath: string;
  processedPath: string;
  thumbnailPath?: string;
  width: number;
  height: number;
  size: number;
  format: string;
  metadata: Record<string, unknown>;
}

export interface FileUploadResult {
  success: boolean;
  file?: {
    id: string;
    filename: string;
    originalName: string;
    path: string;
    url?: string;
    size: number;
    mimeType: string;
    processed: boolean;
  };
  error?: string;
}

// API Response types
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

export interface PaginatedResponse<T> extends ApiResponse<T[]> {
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
    hasNext: boolean;
    hasPrev: boolean;
  };
}

// Enhanced Dashboard types
export interface DashboardStats {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  pendingProjects: number;
  totalClients: number;
  thisMonthProjects: number;
  thisMonthRevenue: number;
  avgProjectValue: number;
  // LP-specific stats
  totalLandingPages: number;
  publishedPages: number;
  draftPages: number;
  totalPageViews: number;
  totalConversions: number;
  avgConversionRate: number;
  // Performance metrics
  cacheHitRate: number;
  avgResponseTime: number;
  systemHealth: 'healthy' | 'warning' | 'critical';
}

export interface ProjectProgress {
  projectId: string;
  name: string;
  progress: number;
  status: string;
  client: string;
  dueDate?: string | undefined;
}

// Authentication types
export interface AuthUser {
  id: string;
  email: string;
  name?: string;
  role: 'CLIENT' | 'ADMIN' | 'SUPER_ADMIN';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
}

// Configuration types
export interface SystemConfigItem {
  id: string;
  key: string;
  value: string;
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | 'TEXT';
  description?: string;
  isEditable: boolean;
}

// Email types
export interface EmailData {
  to: string;
  subject: string;
  html: string;
  text?: string;
  templateId?: string;
  variables?: Record<string, string>;
}

export interface EmailTemplate {
  id: string;
  name: string;
  subject: string;
  htmlBody: string;
  textBody?: string;
  type: 'WELCOME' | 'CONFIRMATION' | 'NOTIFICATION' | 'REMINDER' | 'MARKETING' | 'SYSTEM';
  variables: string[];
  isActive: boolean;
}

// Enhanced Error types
export class AppError extends Error {
  constructor(
    message: string,
    public statusCode: number = 500,
    public code?: string,
    public details?: unknown
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export class ValidationError extends AppError {
  constructor(message: string, public validationErrors: Record<string, string>) {
    super(message, 400, 'VALIDATION_ERROR');
    this.name = 'ValidationError';
  }
}

export class RateLimitError extends AppError {
  constructor(message: string = 'Rate limit exceeded') {
    super(message, 429, 'RATE_LIMIT');
    this.name = 'RateLimitError';
  }
}

export class FileUploadError extends AppError {
  constructor(message: string, public uploadDetails?: Record<string, any>) {
    super(message, 400, 'FILE_UPLOAD_ERROR');
    this.name = 'FileUploadError';
  }
}

export class CacheError extends AppError {
  constructor(message: string) {
    super(message, 500, 'CACHE_ERROR');
    this.name = 'CacheError';
  }
}

export class DatabaseError extends AppError {
  constructor(message: string, public originalError?: Error) {
    super(message, 500, 'DATABASE_ERROR');
    this.name = 'DatabaseError';
  }
}

export class ExternalServiceError extends AppError {
  constructor(service: string, message: string) {
    super(`${service}: ${message}`, 502, 'EXTERNAL_SERVICE_ERROR');
    this.name = 'ExternalServiceError';
  }
}

// Additional UI and component types
export interface ComponentProps {
  className?: string;
  children?: React.ReactNode;
}

export interface ModalProps extends ComponentProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
}

export interface TableColumn<T = any> {
  key: keyof T | string;
  label: string;
  sortable?: boolean;
  width?: string;
  render?: (value: any, row: T) => React.ReactNode;
}

export interface FilterOption {
  label: string;
  value: string | number;
  count?: number;
}

export interface SearchConfig {
  placeholder: string;
  fields: string[];
  suggestions?: string[];
  minLength?: number;
}

export interface BreadcrumbItem {
  label: string;
  href?: string;
  icon?: string;
}

export interface ToastNotification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message?: string;
  duration?: number;
  actions?: Array<{
    label: string;
    onClick: () => void;
  }>;
}

// Landing Page Builder types
export interface LPSection {
  id: string;
  type: 'hero' | 'features' | 'testimonials' | 'pricing' | 'cta' | 'form' | 'custom';
  name: string;
  content: Record<string, any>;
  styles: Record<string, any>;
  order: number;
  visible: boolean;
}

export interface LPTheme {
  id: string;
  name: string;
  colors: {
    primary: string;
    secondary: string;
    accent: string;
    background: string;
    text: string;
  };
  fonts: {
    heading: string;
    body: string;
  };
  components: Record<string, any>;
}

export interface BuilderState {
  selectedSection?: string;
  selectedElement?: string;
  previewMode: boolean;
  device: 'desktop' | 'tablet' | 'mobile';
  zoom: number;
}

// Analytics and reporting types
export interface AnalyticsDateRange {
  start: Date;
  end: Date;
  preset?: 'today' | 'yesterday' | 'last7days' | 'last30days' | 'thisMonth' | 'lastMonth' | 'custom';
}

export interface MetricCard {
  title: string;
  value: number | string;
  change?: {
    value: number;
    period: string;
    type: 'increase' | 'decrease';
  };
  icon?: string;
  format?: 'number' | 'currency' | 'percentage';
}

export interface ChartData {
  labels: string[];
  datasets: Array<{
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string;
    tension?: number;
  }>;
}

// Utility types
export type Optional<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;
export type DeepPartial<T> = T extends object ? {
  [P in keyof T]?: DeepPartial<T[P]>;
} : T;
export type Prettify<T> = { [K in keyof T]: T[K] } & {};
export type NonNullable<T> = T extends null | undefined ? never : T;
export type KeysOfType<T, U> = {
  [K in keyof T]: T[K] extends U ? K : never;
}[keyof T];

// Form and validation types
export interface FormState<T = any> {
  values: T;
  errors: Partial<Record<keyof T, string>>;
  touched: Partial<Record<keyof T, boolean>>;
  isSubmitting: boolean;
  isValid: boolean;
}

export interface ValidationRule {
  required?: boolean;
  min?: number;
  max?: number;
  pattern?: RegExp;
  custom?: (value: any) => string | null;
}

export type ValidationRules<T> = Partial<Record<keyof T, ValidationRule>>;