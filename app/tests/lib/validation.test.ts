import { validateClientForm, classifyProject, sanitizeInput } from '@/lib/validation';
import type { ClientFormData } from '@/types';

describe('Validation Library', () => {
  describe('sanitizeInput', () => {
    it('should remove HTML tags from string values', () => {
      const input = {
        name: '<script>alert("xss")</script>John Doe',
        email: 'john@test.com',
        description: '<b>Bold text</b> and <i>italic</i>',
      };

      const result = sanitizeInput(input);

      expect(result.name).toBe('John Doe');
      expect(result.email).toBe('john@test.com');
      expect(result.description).toBe('Bold text and italic');
    });

    it('should sanitize arrays of strings', () => {
      const input = {
        projectType: ['<script>WEBSITE</script>', 'MOBILE_APP'],
        tags: ['<img src=x onerror=alert(1)>tag1', 'tag2'],
      };

      const result = sanitizeInput(input);

      expect(result.projectType).toEqual(['WEBSITE', 'MOBILE_APP']);
      expect(result.tags).toEqual(['tag1', 'tag2']);
    });

    it('should preserve non-string values', () => {
      const input = {
        age: 25,
        active: true,
        metadata: { key: 'value' },
      };

      const result = sanitizeInput(input);

      expect(result.age).toBe(25);
      expect(result.active).toBe(true);
      expect(result.metadata).toEqual({ key: 'value' });
    });
  });

  describe('validateClientForm', () => {
    const validFormData: ClientFormData = {
      contactPerson: 'John Doe',
      email: 'john@example.com',
      phone: '+1234567890',
      companyName: 'Test Company',
      website: 'https://example.com',
      address: '123 Main St, City, State 12345',
      industry: 'Technology',
      businessType: 'STARTUP',
      projectBudget: '$10,000 - $25,000',
      timeline: '2-4 weeks',
      projectType: ['WEBSITE', 'MOBILE_APP'],
      description: 'This is a detailed project description with at least 10 characters.',
      requirements: 'Some specific requirements',
      additionalInfo: 'Additional information',
    };

    it('should validate correct form data', () => {
      const result = validateClientForm(validFormData);

      expect(result.isValid).toBe(true);
      expect(result.errors).toEqual({});
      expect(result.sanitizedData).toBeDefined();
    });

    it('should reject empty contact person', () => {
      const invalidData = { ...validFormData, contactPerson: '' };
      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(result.errors.contactPerson).toBeDefined();
    });

    it('should reject invalid email format', () => {
      const invalidData = { ...validFormData, email: 'invalid-email' };
      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(result.errors.email).toBeDefined();
    });

    it('should reject invalid phone format', () => {
      const invalidData = { ...validFormData, phone: 'invalid-phone' };
      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(result.errors.phone).toBeDefined();
    });

    it('should reject invalid website URL', () => {
      const invalidData = { ...validFormData, website: 'invalid-url' };
      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(result.errors.website).toBeDefined();
    });

    it('should reject empty project type array', () => {
      const invalidData = { ...validFormData, projectType: [] };
      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(result.errors.projectType).toBeDefined();
    });

    it('should reject short description', () => {
      const invalidData = { ...validFormData, description: 'Short' };
      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(result.errors.description).toBeDefined();
    });

    it('should handle missing required fields', () => {
      const invalidData = {
        contactPerson: '',
        email: '',
        phone: '',
        industry: '',
        businessType: undefined,
        projectBudget: '',
        timeline: '',
        projectType: [],
        description: '',
      } as any;

      const result = validateClientForm(invalidData);

      expect(result.isValid).toBe(false);
      expect(Object.keys(result.errors)).toContain('contactPerson');
      expect(Object.keys(result.errors)).toContain('email');
      expect(Object.keys(result.errors)).toContain('phone');
      expect(Object.keys(result.errors)).toContain('industry');
      expect(Object.keys(result.errors)).toContain('businessType');
      expect(Object.keys(result.errors)).toContain('projectBudget');
      expect(Object.keys(result.errors)).toContain('timeline');
      expect(Object.keys(result.errors)).toContain('projectType');
      expect(Object.keys(result.errors)).toContain('description');
    });

    it('should accept optional fields as empty strings', () => {
      const dataWithOptionals = {
        ...validFormData,
        companyName: '',
        website: '',
        address: '',
        requirements: '',
        additionalInfo: '',
      };

      const result = validateClientForm(dataWithOptionals);

      expect(result.isValid).toBe(true);
    });
  });

  describe('classifyProject', () => {
    const baseFormData: ClientFormData = {
      contactPerson: 'John Doe',
      email: 'john@example.com',
      phone: '+1234567890',
      industry: 'Technology',
      businessType: 'STARTUP',
      projectBudget: '$10,000 - $25,000',
      timeline: '2-4 weeks',
      projectType: ['WEBSITE'],
      description: 'A detailed project description.',
    };

    it('should classify startup as urgent priority', () => {
      const result = classifyProject(baseFormData);

      expect(result.priority).toBe('URGENT');
    });

    it('should classify enterprise as high priority', () => {
      const formData = { ...baseFormData, businessType: 'ENTERPRISE' as const };
      const result = classifyProject(formData);

      expect(result.priority).toBe('HIGH');
    });

    it('should classify urgent timeline as urgent priority', () => {
      const formData = { ...baseFormData, timeline: 'ASAP (urgent)' };
      const result = classifyProject(formData);

      expect(result.priority).toBe('URGENT');
    });

    it('should classify e-commerce projects correctly', () => {
      const formData = { ...baseFormData, projectType: ['ECOMMERCE'] };
      const result = classifyProject(formData);

      expect(result.category).toBe('E-Commerce');
      expect(result.complexity).toBe('HIGH');
      expect(result.recommendedTimeline).toBe('8-16 weeks');
    });

    it('should classify mobile app projects correctly', () => {
      const formData = { ...baseFormData, projectType: ['MOBILE_APP'] };
      const result = classifyProject(formData);

      expect(result.category).toBe('Application Development');
      expect(result.complexity).toBe('HIGH');
      expect(result.recommendedTimeline).toBe('12-24 weeks');
    });

    it('should classify website projects correctly', () => {
      const formData = { ...baseFormData, projectType: ['WEBSITE'] };
      const result = classifyProject(formData);

      expect(result.category).toBe('Web Development');
      expect(result.complexity).toBe('MEDIUM');
      expect(result.recommendedTimeline).toBe('4-8 weeks');
    });

    it('should classify landing page projects correctly', () => {
      const formData = { ...baseFormData, projectType: ['LANDING_PAGE'] };
      const result = classifyProject(formData);

      expect(result.category).toBe('Marketing');
      expect(result.complexity).toBe('LOW');
      expect(result.recommendedTimeline).toBe('1-3 weeks');
    });

    it('should classify branding projects correctly', () => {
      const formData = { ...baseFormData, projectType: ['BRANDING'] };
      const result = classifyProject(formData);

      expect(result.category).toBe('Design & Branding');
      expect(result.complexity).toBe('MEDIUM');
      expect(result.recommendedTimeline).toBe('3-6 weeks');
    });

    it('should handle multiple project types by using the first one', () => {
      const formData = {
        ...baseFormData,
        projectType: ['LANDING_PAGE', 'WEBSITE', 'ECOMMERCE'] as const,
      };
      const result = classifyProject(formData);

      expect(result.category).toBe('Marketing'); // Based on LANDING_PAGE
    });
  });
});