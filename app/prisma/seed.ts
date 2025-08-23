import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  console.log('Seeding database...');

  // Create admin user
  const hashedPassword = await bcrypt.hash('admin123', 12);
  
  const admin = await prisma.user.upsert({
    where: { email: 'admin@company.com' },
    update: {},
    create: {
      email: 'admin@company.com',
      name: 'System Administrator',
      role: 'SUPER_ADMIN',
      status: 'ACTIVE',
    } as any,
  });

  console.log('Created admin user:', admin.email);

  // Create email templates
  const templates = [
    {
      name: 'CLIENT_CONFIRMATION',
      subject: 'Thank you for your project inquiry - {{companyName}}',
      type: 'CONFIRMATION' as const,
      htmlBody: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2>Thank you for your project inquiry!</h2>
          <p>Dear {{clientName}},</p>
          <p>We have received your project request and are excited to work with you. Here are the details we received:</p>
          
          <div style="background-color: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0;">
            <h3>Project Details</h3>
            <ul>
              <li><strong>Project Type:</strong> {{projectType}}</li>
              <li><strong>Timeline:</strong> {{timeline}}</li>
              <li><strong>Budget:</strong> {{budget}}</li>
            </ul>
            <p><strong>Description:</strong></p>
            <p>{{projectDescription}}</p>
          </div>
          
          <p>Our team will review your request and get back to you within 24 hours with next steps.</p>
          
          <p>If you have any questions, please don't hesitate to contact us at {{supportEmail}}.</p>
          
          <p>Best regards,<br>The {{companyName}} Team</p>
        </div>
      `,
      textBody: `
Thank you for your project inquiry!

Dear {{clientName}},

We have received your project request and are excited to work with you.

Project Details:
- Project Type: {{projectType}}
- Timeline: {{timeline}}
- Budget: {{budget}}

Description: {{projectDescription}}

Our team will review your request and get back to you within 24 hours with next steps.

If you have any questions, please contact us at {{supportEmail}}.

Best regards,
The {{companyName}} Team
      `,
      variables: ['clientName', 'projectType', 'timeline', 'budget', 'projectDescription', 'supportEmail', 'companyName'],
    },
    {
      name: 'ADMIN_NOTIFICATION',
      subject: 'New Project Inquiry - Priority: {{priority}}',
      type: 'NOTIFICATION' as const,
      htmlBody: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2>New Project Inquiry Received</h2>
          
          <div style="background-color: #f0f8ff; padding: 20px; border-radius: 8px; margin: 20px 0;">
            <h3>Client Information</h3>
            <ul>
              <li><strong>Name:</strong> {{clientName}}</li>
              <li><strong>Email:</strong> {{clientEmail}}</li>
              <li><strong>Company:</strong> {{companyName}}</li>
              <li><strong>Priority:</strong> <span style="color: red;">{{priority}}</span></li>
            </ul>
          </div>
          
          <div style="background-color: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0;">
            <h3>Project Details</h3>
            <ul>
              <li><strong>Type:</strong> {{projectType}}</li>
              <li><strong>Timeline:</strong> {{timeline}}</li>
              <li><strong>Budget:</strong> {{budget}}</li>
            </ul>
            <p><strong>Description:</strong></p>
            <p>{{projectDescription}}</p>
          </div>
          
          <div style="text-align: center; margin: 30px 0;">
            <a href="{{adminDashboardUrl}}" style="background-color: #007cba; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px;">
              View in Admin Dashboard
            </a>
          </div>
        </div>
      `,
      textBody: `
New Project Inquiry Received

Client Information:
- Name: {{clientName}}
- Email: {{clientEmail}}
- Company: {{companyName}}
- Priority: {{priority}}

Project Details:
- Type: {{projectType}}
- Timeline: {{timeline}}
- Budget: {{budget}}

Description: {{projectDescription}}

View in Admin Dashboard: {{adminDashboardUrl}}
      `,
      variables: ['clientName', 'clientEmail', 'companyName', 'priority', 'projectType', 'timeline', 'budget', 'projectDescription', 'adminDashboardUrl'],
    },
  ];

  for (const template of templates) {
    await prisma.emailTemplate.upsert({
      where: { name: template.name },
      update: template,
      create: template,
    });
  }

  console.log('Created email templates');

  // Create system configuration
  const configs = [
    {
      key: 'company_name',
      value: 'Your Company',
      type: 'STRING' as const,
      description: 'Company name displayed in emails and forms',
    },
    {
      key: 'support_email',
      value: 'support@company.com',
      type: 'STRING' as const,
      description: 'Support email address',
    },
    {
      key: 'max_file_size',
      value: '10485760',
      type: 'NUMBER' as const,
      description: 'Maximum file upload size in bytes (10MB)',
    },
    {
      key: 'allowed_file_types',
      value: JSON.stringify(['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'application/pdf']),
      type: 'JSON' as const,
      description: 'Allowed file MIME types for uploads',
    },
    {
      key: 'email_enabled',
      value: 'true',
      type: 'BOOLEAN' as const,
      description: 'Enable/disable email notifications',
    },
  ];

  for (const config of configs) {
    await prisma.systemConfig.upsert({
      where: { key: config.key },
      update: config,
      create: config,
    });
  }

  console.log('Created system configuration');

  // Create sample client for testing
  const testClient = await prisma.user.upsert({
    where: { email: 'test@client.com' },
    update: {},
    create: {
      email: 'test@client.com',
      name: 'Test Client',
      role: 'CLIENT',
      status: 'ACTIVE',
    },
  });

  // Create sample client profile
  await prisma.clientProfile.upsert({
    where: { userId: testClient.id },
    update: {},
    create: {
      userId: testClient.id,
      contactPerson: 'Test Client',
      phone: '+1234567890',
      companyName: 'Test Company Inc.',
      website: 'https://testcompany.com',
      address: '123 Test Street, Test City, TC 12345',
      industry: 'Technology',
      businessType: 'STARTUP',
      projectBudget: '$10,000 - $25,000',
      timeline: '2-4 weeks',
      projectType: ['WEBSITE', 'MOBILE_APP'],
      description: 'This is a sample project description for testing purposes.',
      requirements: 'Sample requirements for the project.',
      priority: 'HIGH',
      category: 'Web Development',
    },
  });

  // Create sample project
  await prisma.project.upsert({
    where: { id: 'sample-project-1' },
    update: {},
    create: {
      id: 'sample-project-1',
      clientId: testClient.id,
      adminId: admin.id,
      name: 'Sample Website Project',
      description: 'A sample project for testing the system functionality.',
      type: 'WEBSITE',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      progress: 35,
      estimatedCost: 15000,
      tags: ['website', 'responsive', 'modern'],
      metadata: {
        complexity: 'MEDIUM',
        category: 'Web Development',
        submissionDate: new Date(),
      },
    },
  });

  console.log('Seeding completed successfully!');
}

main()
  .catch((e) => {
    console.error('Error seeding database:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });