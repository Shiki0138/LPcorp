import { z } from 'zod';
import type { ClientFormData, FormValidationResult, AutoClassificationResult } from '@/types';

// Validation schemas
const phoneRegex = /^[\+]?[1-9][\d]{0,15}$/;
const urlRegex = /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)$/;

export const clientFormSchema = z.object({
  // Personal Information
  contactPerson: z.string()
    .min(2, 'Contact person name must be at least 2 characters')
    .max(100, 'Contact person name must be less than 100 characters')
    .regex(/^[a-zA-Z\s\-\.]+$/, 'Contact person name contains invalid characters'),
  
  email: z.string()
    .email('Please enter a valid email address')
    .max(255, 'Email must be less than 255 characters'),
  
  phone: z.string()
    .regex(phoneRegex, 'Please enter a valid phone number')
    .min(10, 'Phone number must be at least 10 digits')
    .max(16, 'Phone number must be less than 16 digits'),
  
  companyName: z.string()
    .max(200, 'Company name must be less than 200 characters')
    .optional()
    .or(z.literal('')),
  
  website: z.string()
    .regex(urlRegex, 'Please enter a valid URL')
    .max(255, 'Website URL must be less than 255 characters')
    .optional()
    .or(z.literal('')),
  
  address: z.string()
    .max(500, 'Address must be less than 500 characters')
    .optional()
    .or(z.literal('')),

  // Business Information
  industry: z.string()
    .min(1, 'Please select an industry')
    .max(100, 'Industry must be less than 100 characters'),
  
  businessType: z.enum(['STARTUP', 'SMALL_BUSINESS', 'MEDIUM_BUSINESS', 'ENTERPRISE', 'NON_PROFIT', 'GOVERNMENT', 'OTHER'], {
    errorMap: () => ({ message: 'Please select a valid business type' })
  }),
  
  projectBudget: z.string()
    .min(1, 'Please select a project budget range'),
  
  timeline: z.string()
    .min(1, 'Please select a project timeline'),

  // Project Requirements
  projectType: z.array(z.enum(['WEBSITE', 'MOBILE_APP', 'WEB_APP', 'ECOMMERCE', 'LANDING_PAGE', 'BRANDING', 'MARKETING', 'CONSULTATION', 'MAINTENANCE', 'OTHER']))
    .min(1, 'Please select at least one project type')
    .max(5, 'Please select no more than 5 project types'),
  
  description: z.string()
    .min(10, 'Project description must be at least 10 characters')
    .max(2000, 'Project description must be less than 2000 characters'),
  
  requirements: z.string()
    .max(3000, 'Requirements must be less than 3000 characters')
    .optional()
    .or(z.literal('')),
  
  additionalInfo: z.string()
    .max(1000, 'Additional information must be less than 1000 characters')
    .optional()
    .or(z.literal('')),
});

/**
 * Basic sanitization function
 */
export function sanitizeInput(input: string): string {
  // Basic sanitization - remove HTML tags and dangerous characters
  return input
    .replace(/<[^>]*>/g, '') // Remove HTML tags
    .replace(/[<>&"']/g, (match) => {
      const entities: { [key: string]: string } = {
        '<': '&lt;',
        '>': '&gt;',
        '&': '&amp;',
        '"': '&quot;',
        "'": '&#x27;'
      };
      return entities[match] || match;
    });
}

/**
 * Sanitizes input data to prevent XSS attacks
 */
export function sanitizeFormData(data: Record<string, unknown>): Record<string, unknown> {
  const sanitized: Record<string, unknown> = {};
  
  for (const [key, value] of Object.entries(data)) {
    if (typeof value === 'string') {
      sanitized[key] = sanitizeInput(value).trim();
    } else if (Array.isArray(value)) {
      sanitized[key] = value.map(item => 
        typeof item === 'string' ? sanitizeInput(item).trim() : item
      );
    } else {
      sanitized[key] = value;
    }
  }
  
  return sanitized;
}

/**
 * Validates and sanitizes client form data
 */
export function validateClientForm(data: unknown): FormValidationResult {
  try {
    // First sanitize the input
    const sanitized = sanitizeFormData(data as Record<string, unknown>);
    
    // Then validate with schema
    const validated = clientFormSchema.parse(sanitized);
    
    return {
      isValid: true,
      errors: {},
      sanitizedData: validated as ClientFormData,
    };
  } catch (error) {
    if (error instanceof z.ZodError) {
      const errors: Record<string, string> = {};
      
      error.errors.forEach((err) => {
        const path = err.path.join('.');
        errors[path] = err.message;
      });
      
      return {
        isValid: false,
        errors,
        sanitizedData: sanitizeFormData(data as Record<string, unknown>) as Partial<ClientFormData>,
      };
    }
    
    throw error;
  }
}

/**
 * Automatically classifies project priority and category based on form data
 */
export function classifyProject(data: ClientFormData): AutoClassificationResult {
  let priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' = 'MEDIUM';
  let category = 'General';
  let complexity: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM';
  let recommendedTimeline = '4-8 weeks';
  
  // Priority classification based on business type and timeline
  if (data.businessType === 'ENTERPRISE') {
    priority = 'HIGH';
  } else if (data.businessType === 'STARTUP') {
    priority = 'URGENT';
  }
  
  if (data.timeline.toLowerCase().includes('urgent') || data.timeline.toLowerCase().includes('asap')) {
    priority = 'URGENT';
  } else if (data.timeline.toLowerCase().includes('week')) {
    priority = 'HIGH';
  }
  
  // Category classification based on project types
  if (data.projectType.includes('ECOMMERCE')) {
    category = 'E-Commerce';
    complexity = 'HIGH';
    recommendedTimeline = '8-16 weeks';
  } else if (data.projectType.includes('MOBILE_APP') || data.projectType.includes('WEB_APP')) {
    category = 'Application Development';
    complexity = 'HIGH';
    recommendedTimeline = '12-24 weeks';
  } else if (data.projectType.includes('WEBSITE')) {
    category = 'Web Development';
    complexity = 'MEDIUM';
    recommendedTimeline = '4-8 weeks';
  } else if (data.projectType.includes('LANDING_PAGE')) {
    category = 'Marketing';
    complexity = 'LOW';
    recommendedTimeline = '1-3 weeks';
  } else if (data.projectType.includes('BRANDING')) {
    category = 'Design & Branding';
    complexity = 'MEDIUM';
    recommendedTimeline = '3-6 weeks';
  }
  
  // Budget estimation based on project complexity and type
  let estimatedBudget: number | undefined;
  const budgetRange = data.projectBudget.toLowerCase();
  
  if (budgetRange.includes('10000') || budgetRange.includes('10k')) {
    estimatedBudget = 10000;
  } else if (budgetRange.includes('25000') || budgetRange.includes('25k')) {
    estimatedBudget = 25000;
  } else if (budgetRange.includes('50000') || budgetRange.includes('50k')) {
    estimatedBudget = 50000;
  }
  
  return {
    priority,
    category,
    estimatedBudget,
    complexity,
    recommendedTimeline,
  };
}

// File validation schemas
export const fileUploadSchema = z.object({
  filename: z.string().min(1).max(255),
  mimeType: z.string().min(1),
  size: z.number().min(1).max(10 * 1024 * 1024), // 10MB max
});

export const imageUploadSchema = fileUploadSchema.extend({
  mimeType: z.enum(['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/svg+xml']),
});

/**
 * Validates file upload data
 */
export function validateFileUpload(file: { filename: string; mimeType: string; size: number }) {
  try {
    return fileUploadSchema.parse(file);
  } catch (error) {
    if (error instanceof z.ZodError) {
      throw new Error(error.errors[0]?.message || 'Invalid file');
    }
    throw error;
  }
}

/**
 * Validates image upload data
 */
export function validateImageUpload(file: { filename: string; mimeType: string; size: number }) {
  try {
    return imageUploadSchema.parse(file);
  } catch (error) {
    if (error instanceof z.ZodError) {
      throw new Error(error.errors[0]?.message || 'Invalid image file');
    }
    throw error;
  }
}