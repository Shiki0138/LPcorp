import nodemailer from 'nodemailer';
import type { EmailData, EmailTemplate } from '@/types';
import { prisma } from './database';
import { redis } from './redis';

// Email configuration
const emailConfig = {
  host: process.env.EMAIL_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.EMAIL_PORT || '587', 10),
  secure: process.env.EMAIL_PORT === '465',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
};

// Create reusable transporter
let transporter: nodemailer.Transporter | null = null;

function getTransporter(): nodemailer.Transporter {
  if (!transporter) {
    transporter = nodemailer.createTransporter(emailConfig);
  }
  return transporter;
}

/**
 * Sends email using the configured transporter
 */
export async function sendEmail(emailData: EmailData): Promise<boolean> {
  try {
    const transporter = getTransporter();
    
    // Verify transporter configuration
    await transporter.verify();
    
    const mailOptions = {
      from: process.env.EMAIL_FROM || emailData.to,
      to: emailData.to,
      subject: emailData.subject,
      html: emailData.html,
      text: emailData.text,
    };
    
    const result = await transporter.sendMail(mailOptions);
    
    // Log email sent
    await logEmail({
      templateId: emailData.templateId,
      toEmail: emailData.to,
      fromEmail: mailOptions.from,
      subject: emailData.subject,
      htmlBody: emailData.html,
      textBody: emailData.text,
      status: 'SENT',
      sentAt: new Date(),
      metadata: {
        messageId: result.messageId,
        variables: emailData.variables,
      },
    });
    
    return true;
  } catch (error) {
    console.error('Email sending failed:', error);
    
    // Log email failure
    await logEmail({
      templateId: emailData.templateId,
      toEmail: emailData.to,
      fromEmail: process.env.EMAIL_FROM || '',
      subject: emailData.subject,
      htmlBody: emailData.html,
      textBody: emailData.text,
      status: 'FAILED',
      errorMessage: error instanceof Error ? error.message : 'Unknown error',
    });
    
    return false;
  }
}

/**
 * Logs email activity to database
 */
async function logEmail(logData: {
  templateId?: string;
  toEmail: string;
  fromEmail: string;
  subject: string;
  htmlBody?: string;
  textBody?: string;
  status: 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'BOUNCED' | 'OPENED' | 'CLICKED';
  sentAt?: Date;
  deliveredAt?: Date;
  openedAt?: Date;
  clickedAt?: Date;
  errorMessage?: string;
  metadata?: any;
}): Promise<void> {
  try {
    await prisma.emailLog.create({
      data: logData,
    });
  } catch (error) {
    console.error('Failed to log email:', error);
  }
}

/**
 * Gets email template by name with caching
 */
export async function getEmailTemplate(templateName: string): Promise<EmailTemplate | null> {
  const cacheKey = `email-template:${templateName}`;
  
  try {
    // Check cache first
    const cached = await redis.get(cacheKey);
    if (cached) {
      return JSON.parse(cached) as EmailTemplate;
    }
    
    // Get from database
    const template = await prisma.emailTemplate.findFirst({
      where: {
        name: templateName,
        isActive: true,
      },
    });
    
    if (template) {
      // Cache for 1 hour
      await redis.setex(cacheKey, 3600, JSON.stringify(template));
      return template;
    }
    
    return null;
  } catch (error) {
    console.error('Failed to get email template:', error);
    return null;
  }
}

/**
 * Renders email template with variables
 */
export function renderEmailTemplate(
  template: EmailTemplate,
  variables: Record<string, string>
): { subject: string; html: string; text?: string } {
  let subject = template.subject;
  let html = template.htmlBody;
  let text = template.textBody;
  
  // Replace variables in template
  for (const [key, value] of Object.entries(variables)) {
    const placeholder = `{{${key}}}`;
    subject = subject.replace(new RegExp(placeholder, 'g'), value);
    html = html.replace(new RegExp(placeholder, 'g'), value);
    if (text) {
      text = text.replace(new RegExp(placeholder, 'g'), value);
    }
  }
  
  return { subject, html, text };
}

/**
 * Sends templated email
 */
export async function sendTemplatedEmail(
  templateName: string,
  to: string,
  variables: Record<string, string>
): Promise<boolean> {
  try {
    const template = await getEmailTemplate(templateName);
    if (!template) {
      throw new Error(`Email template '${templateName}' not found`);
    }
    
    const rendered = renderEmailTemplate(template, variables);
    
    return await sendEmail({
      to,
      subject: rendered.subject,
      html: rendered.html,
      text: rendered.text,
      templateId: template.id,
      variables,
    });
  } catch (error) {
    console.error('Failed to send templated email:', error);
    return false;
  }
}

/**
 * Sends client confirmation email
 */
export async function sendClientConfirmationEmail(
  clientEmail: string,
  clientName: string,
  projectDetails: {
    type: string;
    description: string;
    timeline: string;
    budget: string;
  }
): Promise<boolean> {
  return await sendTemplatedEmail('CLIENT_CONFIRMATION', clientEmail, {
    clientName,
    projectType: projectDetails.type,
    projectDescription: projectDetails.description,
    timeline: projectDetails.timeline,
    budget: projectDetails.budget,
    supportEmail: process.env.EMAIL_FROM || 'support@company.com',
    companyName: 'Your Company',
  });
}

/**
 * Sends admin notification email
 */
export async function sendAdminNotificationEmail(
  adminEmail: string,
  clientDetails: {
    name: string;
    email: string;
    company?: string;
    priority: string;
  },
  projectDetails: {
    type: string;
    description: string;
    timeline: string;
    budget: string;
  }
): Promise<boolean> {
  return await sendTemplatedEmail('ADMIN_NOTIFICATION', adminEmail, {
    clientName: clientDetails.name,
    clientEmail: clientDetails.email,
    companyName: clientDetails.company || 'N/A',
    priority: clientDetails.priority,
    projectType: projectDetails.type,
    projectDescription: projectDetails.description,
    timeline: projectDetails.timeline,
    budget: projectDetails.budget,
    adminDashboardUrl: `${process.env.NEXTAUTH_URL}/admin/dashboard`,
  });
}

/**
 * Creates default email templates
 */
export async function createDefaultEmailTemplates(): Promise<void> {
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
}