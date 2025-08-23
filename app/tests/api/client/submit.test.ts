import { NextRequest } from 'next/server';
import { POST } from '@/app/api/client/submit/route';
import { prisma } from '@/lib/database';
import { redis } from '@/lib/redis';

// Mock dependencies
jest.mock('@/lib/database');
jest.mock('@/lib/redis');
jest.mock('next-auth/next');
jest.mock('@/lib/validation');
jest.mock('@/lib/email');

const mockPrisma = prisma as jest.Mocked<typeof prisma>;
const mockRedis = redis as jest.Mocked<typeof redis>;

// Mock validation functions
const mockValidateClientForm = require('@/lib/validation').validateClientForm as jest.Mock;
const mockClassifyProject = require('@/lib/validation').classifyProject as jest.Mock;
const mockSendClientConfirmationEmail = require('@/lib/email').sendClientConfirmationEmail as jest.Mock;
const mockSendAdminNotificationEmail = require('@/lib/email').sendAdminNotificationEmail as jest.Mock;

// Mock next-auth
const mockGetServerSession = require('next-auth/next').getServerSession as jest.Mock;

describe('/api/client/submit', () => {
  const validFormData = {
    contactPerson: 'John Doe',
    email: 'john@example.com',
    phone: '+1234567890',
    companyName: 'Test Company',
    website: 'https://example.com',
    address: '123 Main St',
    industry: 'Technology',
    businessType: 'STARTUP',
    projectBudget: '$10,000 - $25,000',
    timeline: '2-4 weeks',
    projectType: ['WEBSITE'],
    description: 'This is a detailed project description.',
    requirements: 'Some requirements',
    additionalInfo: 'Additional info',
  };

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Setup default mocks
    mockValidateClientForm.mockReturnValue({
      isValid: true,
      errors: {},
      sanitizedData: validFormData,
    });
    
    mockClassifyProject.mockReturnValue({
      priority: 'HIGH',
      category: 'Web Development',
      complexity: 'MEDIUM',
      recommendedTimeline: '4-8 weeks',
      estimatedBudget: 15000,
    });
    
    mockRedis.incr.mockResolvedValue(1);
    mockRedis.expire.mockResolvedValue(1);
    mockRedis.setex.mockResolvedValue('OK');
    
    mockSendClientConfirmationEmail.mockResolvedValue(true);
    mockSendAdminNotificationEmail.mockResolvedValue(true);
  });

  describe('POST', () => {
    it('should successfully submit a valid form', async () => {
      const mockUser = { id: 'user-123', email: 'john@example.com' };
      const mockClientProfile = { id: 'profile-123', userId: 'user-123' };
      const mockProject = { id: 'project-123', clientId: 'user-123' };

      mockGetServerSession.mockResolvedValue({ user: mockUser });
      
      // Mock Prisma transaction
      mockPrisma.$transaction.mockResolvedValue({
        clientProfile: mockClientProfile,
        project: mockProject,
      });

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 'Content-Type': 'application/json' },
      });

      const response = await POST(request);
      const data = await response.json();

      expect(response.status).toBe(200);
      expect(data.success).toBe(true);
      expect(data.data.projectId).toBe('project-123');
      expect(mockValidateClientForm).toHaveBeenCalledWith(validFormData);
      expect(mockClassifyProject).toHaveBeenCalledWith(validFormData);
    });

    it('should reject invalid form data', async () => {
      mockValidateClientForm.mockReturnValue({
        isValid: false,
        errors: {
          email: 'Invalid email format',
          contactPerson: 'Name is required',
        },
        sanitizedData: {},
      });

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify({ email: 'invalid', contactPerson: '' }),
        headers: { 'Content-Type': 'application/json' },
      });

      const response = await POST(request);
      const data = await response.json();

      expect(response.status).toBe(400);
      expect(data.success).toBe(false);
      expect(data.error).toBe('Validation failed');
      expect(data.data).toEqual({
        email: 'Invalid email format',
        contactPerson: 'Name is required',
      });
    });

    it('should handle rate limiting', async () => {
      mockRedis.incr.mockResolvedValue(6); // Exceeds limit of 5

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 
          'Content-Type': 'application/json',
          'x-forwarded-for': '192.168.1.1',
        },
      });

      const response = await POST(request);
      const data = await response.json();

      expect(response.status).toBe(429);
      expect(data.success).toBe(false);
      expect(data.error).toBe('Rate limit exceeded');
    });

    it('should create new user for unauthenticated submissions', async () => {
      mockGetServerSession.mockResolvedValue(null);
      
      mockPrisma.user.findUnique.mockResolvedValue(null);
      mockPrisma.user.create.mockResolvedValue({
        id: 'new-user-123',
        email: 'john@example.com',
        name: 'John Doe',
      });
      
      mockPrisma.$transaction.mockResolvedValue({
        clientProfile: { id: 'profile-123' },
        project: { id: 'project-123' },
      });

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 'Content-Type': 'application/json' },
      });

      const response = await POST(request);
      const data = await response.json();

      expect(response.status).toBe(200);
      expect(data.success).toBe(true);
      expect(mockPrisma.user.create).toHaveBeenCalledWith({
        data: {
          email: 'john@example.com',
          name: 'John Doe',
          role: 'CLIENT',
          status: 'ACTIVE',
        },
      });
    });

    it('should use existing user for unauthenticated submissions', async () => {
      const existingUser = { id: 'existing-123', email: 'john@example.com' };
      
      mockGetServerSession.mockResolvedValue(null);
      mockPrisma.user.findUnique.mockResolvedValue(existingUser);
      mockPrisma.$transaction.mockResolvedValue({
        clientProfile: { id: 'profile-123' },
        project: { id: 'project-123' },
      });

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 'Content-Type': 'application/json' },
      });

      const response = await POST(request);

      expect(response.status).toBe(200);
      expect(mockPrisma.user.create).not.toHaveBeenCalled();
    });

    it('should handle database errors', async () => {
      mockGetServerSession.mockResolvedValue({ user: { id: 'user-123' } });
      mockPrisma.$transaction.mockRejectedValue(new Error('Database error'));

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 'Content-Type': 'application/json' },
      });

      const response = await POST(request);
      const data = await response.json();

      expect(response.status).toBe(500);
      expect(data.success).toBe(false);
      expect(data.error).toBe('Internal server error');
    });

    it('should send confirmation emails asynchronously', async () => {
      const mockUser = { id: 'user-123' };
      mockGetServerSession.mockResolvedValue({ user: mockUser });
      mockPrisma.$transaction.mockResolvedValue({
        clientProfile: { id: 'profile-123' },
        project: { id: 'project-123' },
      });

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 'Content-Type': 'application/json' },
      });

      await POST(request);

      // Note: Since emails are sent asynchronously with Promise.all(...).catch(),
      // we need to wait a bit for them to be called
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(mockSendClientConfirmationEmail).toHaveBeenCalledWith(
        'john@example.com',
        'John Doe',
        expect.objectContaining({
          type: 'WEBSITE',
          description: 'This is a detailed project description.',
          timeline: '2-4 weeks',
          budget: '$10,000 - $25,000',
        })
      );

      expect(mockSendAdminNotificationEmail).toHaveBeenCalled();
    });

    it('should cache submission data', async () => {
      const mockUser = { id: 'user-123' };
      const mockProject = { id: 'project-123' };
      
      mockGetServerSession.mockResolvedValue({ user: mockUser });
      mockPrisma.$transaction.mockResolvedValue({
        clientProfile: { id: 'profile-123' },
        project: mockProject,
      });

      const request = new NextRequest('http://localhost:3000/api/client/submit', {
        method: 'POST',
        body: JSON.stringify(validFormData),
        headers: { 'Content-Type': 'application/json' },
      });

      await POST(request);

      expect(mockRedis.setex).toHaveBeenCalledWith(
        'submission:project-123',
        86400,
        expect.stringContaining('"projectId":"project-123"')
      );
    });
  });
});