'use client';

import React, { useState, useCallback } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { clientFormSchema } from '@/lib/validation';
import type { ClientFormData } from '@/types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Loader2, CheckCircle, AlertCircle, Upload } from 'lucide-react';

interface ClientFormProps {
  onSubmit?: (data: ClientFormData) => void;
  onSuccess?: (result: any) => void;
  className?: string;
}

const industryOptions = [
  'Technology',
  'Healthcare',
  'Finance',
  'Education',
  'Retail',
  'Manufacturing',
  'Real Estate',
  'Food & Beverage',
  'Entertainment',
  'Non-Profit',
  'Government',
  'Other',
];

const budgetRanges = [
  'Under $5,000',
  '$5,000 - $10,000',
  '$10,000 - $25,000',
  '$25,000 - $50,000',
  '$50,000 - $100,000',
  'Over $100,000',
];

const timelineOptions = [
  'ASAP (Rush Job)',
  '1-2 weeks',
  '2-4 weeks',
  '1-3 months',
  '3-6 months',
  '6+ months',
  'Flexible',
];

const projectTypeOptions = [
  { value: 'WEBSITE', label: 'Website Design & Development' },
  { value: 'MOBILE_APP', label: 'Mobile Application' },
  { value: 'WEB_APP', label: 'Web Application' },
  { value: 'ECOMMERCE', label: 'E-Commerce Platform' },
  { value: 'LANDING_PAGE', label: 'Landing Page' },
  { value: 'BRANDING', label: 'Branding & Logo Design' },
  { value: 'MARKETING', label: 'Digital Marketing' },
  { value: 'CONSULTATION', label: 'Technical Consultation' },
  { value: 'MAINTENANCE', label: 'Website Maintenance' },
  { value: 'OTHER', label: 'Other (Please specify)' },
];

export default function ClientForm({ onSubmit, onSuccess, className }: ClientFormProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitStatus, setSubmitStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors, isValid, isDirty },
    reset,
  } = useForm<ClientFormData>({
    resolver: zodResolver(clientFormSchema),
    mode: 'onChange',
    defaultValues: {
      projectType: [],
    },
  });

  const watchedProjectType = watch('projectType');
  const watchedBusinessType = watch('businessType');

  const onFormSubmit = useCallback(async (data: ClientFormData) => {
    setIsSubmitting(true);
    setSubmitStatus('idle');
    setErrorMessage('');
    setSuccessMessage('');

    try {
      if (onSubmit) {
        await onSubmit(data);
      } else {
        // Default API submission
        const response = await fetch('/api/client/submit', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(data),
        });

        const result = await response.json();

        if (!response.ok) {
          throw new Error(result.message || 'Submission failed');
        }

        setSubmitStatus('success');
        setSuccessMessage(result.message || 'Your project request has been submitted successfully!');
        
        if (onSuccess) {
          onSuccess(result);
        }

        // Reset form after successful submission
        reset();
      }
    } catch (error) {
      setSubmitStatus('error');
      setErrorMessage(error instanceof Error ? error.message : 'An unexpected error occurred');
    } finally {
      setIsSubmitting(false);
    }
  }, [onSubmit, onSuccess, reset]);

  const getEstimatedComplexity = () => {
    if (watchedProjectType?.includes('MOBILE_APP') || watchedProjectType?.includes('WEB_APP')) {
      return 'High';
    }
    if (watchedProjectType?.includes('ECOMMERCE')) {
      return 'High';
    }
    if (watchedProjectType?.includes('WEBSITE') || watchedProjectType?.includes('BRANDING')) {
      return 'Medium';
    }
    if (watchedProjectType?.includes('LANDING_PAGE')) {
      return 'Low';
    }
    return 'Medium';
  };

  const getPriorityIndicator = () => {
    if (watchedBusinessType === 'ENTERPRISE') {
      return 'High';
    }
    if (watchedBusinessType === 'STARTUP') {
      return 'Urgent';
    }
    return 'Medium';
  };

  return (
    <div className={`max-w-4xl mx-auto space-y-8 ${className}`}>
      {/* Status Messages */}
      {submitStatus === 'success' && (
        <Alert className="border-green-200 bg-green-50">
          <CheckCircle className="h-4 w-4 text-green-600" />
          <AlertDescription className="text-green-800">
            {successMessage}
          </AlertDescription>
        </Alert>
      )}

      {submitStatus === 'error' && (
        <Alert className="border-red-200 bg-red-50">
          <AlertCircle className="h-4 w-4 text-red-600" />
          <AlertDescription className="text-red-800">
            {errorMessage}
          </AlertDescription>
        </Alert>
      )}

      <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-8">
        {/* Personal Information */}
        <Card>
          <CardHeader>
            <CardTitle>Contact Information</CardTitle>
            <CardDescription>
              Please provide your contact details so we can reach you about your project.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="contactPerson">
                  Contact Person *
                </Label>
                <Input
                  id="contactPerson"
                  placeholder="John Doe"
                  {...register('contactPerson')}
                  className={errors.contactPerson ? 'border-red-500' : ''}
                />
                {errors.contactPerson && (
                  <p className="text-sm text-red-600">{errors.contactPerson.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">
                  Email Address *
                </Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="john@company.com"
                  {...register('email')}
                  className={errors.email ? 'border-red-500' : ''}
                />
                {errors.email && (
                  <p className="text-sm text-red-600">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="phone">
                  Phone Number *
                </Label>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="+1 (555) 123-4567"
                  {...register('phone')}
                  className={errors.phone ? 'border-red-500' : ''}
                />
                {errors.phone && (
                  <p className="text-sm text-red-600">{errors.phone.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="companyName">
                  Company Name
                </Label>
                <Input
                  id="companyName"
                  placeholder="Your Company Inc."
                  {...register('companyName')}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="website">
                  Website
                </Label>
                <Input
                  id="website"
                  type="url"
                  placeholder="https://www.yourcompany.com"
                  {...register('website')}
                  className={errors.website ? 'border-red-500' : ''}
                />
                {errors.website && (
                  <p className="text-sm text-red-600">{errors.website.message}</p>
                )}
              </div>

              <div className="space-y-2 md:col-span-2">
                <Label htmlFor="address">
                  Business Address
                </Label>
                <Input
                  id="address"
                  placeholder="123 Business St, City, State 12345"
                  {...register('address')}
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Business Information */}
        <Card>
          <CardHeader>
            <CardTitle>Business Information</CardTitle>
            <CardDescription>
              Tell us about your business to help us understand your needs better.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="industry">
                  Industry *
                </Label>
                <Controller
                  name="industry"
                  control={control}
                  render={({ field }) => (
                    <Select onValueChange={field.onChange} value={field.value}>
                      <SelectTrigger className={errors.industry ? 'border-red-500' : ''}>
                        <SelectValue placeholder="Select your industry" />
                      </SelectTrigger>
                      <SelectContent>
                        {industryOptions.map((industry) => (
                          <SelectItem key={industry} value={industry}>
                            {industry}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.industry && (
                  <p className="text-sm text-red-600">{errors.industry.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="businessType">
                  Business Type *
                </Label>
                <Controller
                  name="businessType"
                  control={control}
                  render={({ field }) => (
                    <Select onValueChange={field.onChange} value={field.value}>
                      <SelectTrigger className={errors.businessType ? 'border-red-500' : ''}>
                        <SelectValue placeholder="Select business type" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="STARTUP">Startup</SelectItem>
                        <SelectItem value="SMALL_BUSINESS">Small Business</SelectItem>
                        <SelectItem value="MEDIUM_BUSINESS">Medium Business</SelectItem>
                        <SelectItem value="ENTERPRISE">Enterprise</SelectItem>
                        <SelectItem value="NON_PROFIT">Non-Profit</SelectItem>
                        <SelectItem value="GOVERNMENT">Government</SelectItem>
                        <SelectItem value="OTHER">Other</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.businessType && (
                  <p className="text-sm text-red-600">{errors.businessType.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="projectBudget">
                  Project Budget *
                </Label>
                <Controller
                  name="projectBudget"
                  control={control}
                  render={({ field }) => (
                    <Select onValueChange={field.onChange} value={field.value}>
                      <SelectTrigger className={errors.projectBudget ? 'border-red-500' : ''}>
                        <SelectValue placeholder="Select budget range" />
                      </SelectTrigger>
                      <SelectContent>
                        {budgetRanges.map((range) => (
                          <SelectItem key={range} value={range}>
                            {range}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.projectBudget && (
                  <p className="text-sm text-red-600">{errors.projectBudget.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="timeline">
                  Preferred Timeline *
                </Label>
                <Controller
                  name="timeline"
                  control={control}
                  render={({ field }) => (
                    <Select onValueChange={field.onChange} value={field.value}>
                      <SelectTrigger className={errors.timeline ? 'border-red-500' : ''}>
                        <SelectValue placeholder="Select timeline" />
                      </SelectTrigger>
                      <SelectContent>
                        {timelineOptions.map((timeline) => (
                          <SelectItem key={timeline} value={timeline}>
                            {timeline}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.timeline && (
                  <p className="text-sm text-red-600">{errors.timeline.message}</p>
                )}
              </div>
            </div>

            {/* Intelligence Indicators */}
            {(watchedBusinessType || watchedProjectType?.length) && (
              <div className="bg-blue-50 p-4 rounded-lg">
                <h4 className="font-medium text-blue-900 mb-2">Project Analysis</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-blue-700">Estimated Complexity: </span>
                    <span className="font-medium">{getEstimatedComplexity()}</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Priority Level: </span>
                    <span className="font-medium">{getPriorityIndicator()}</span>
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Project Requirements */}
        <Card>
          <CardHeader>
            <CardTitle>Project Requirements</CardTitle>
            <CardDescription>
              Describe your project in detail so we can provide the best solution.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-4">
              <Label>
                Project Type * (Select all that apply)
              </Label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {projectTypeOptions.map((option) => (
                  <Controller
                    key={option.value}
                    name="projectType"
                    control={control}
                    render={({ field }) => (
                      <div className="flex items-center space-x-2">
                        <Checkbox
                          id={option.value}
                          checked={field.value?.includes(option.value) || false}
                          onCheckedChange={(checked) => {
                            const currentValue = field.value || [];
                            if (checked) {
                              field.onChange([...currentValue, option.value]);
                            } else {
                              field.onChange(
                                currentValue.filter((val) => val !== option.value)
                              );
                            }
                          }}
                        />
                        <Label
                          htmlFor={option.value}
                          className="text-sm font-normal cursor-pointer"
                        >
                          {option.label}
                        </Label>
                      </div>
                    )}
                  />
                ))}
              </div>
              {errors.projectType && (
                <p className="text-sm text-red-600">{errors.projectType.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">
                Project Description *
              </Label>
              <Textarea
                id="description"
                placeholder="Please describe your project in detail. What are your goals, target audience, key features needed, etc."
                rows={4}
                {...register('description')}
                className={errors.description ? 'border-red-500' : ''}
              />
              {errors.description && (
                <p className="text-sm text-red-600">{errors.description.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="requirements">
                Specific Requirements
              </Label>
              <Textarea
                id="requirements"
                placeholder="Any specific technical requirements, integrations, or features you need..."
                rows={3}
                {...register('requirements')}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="additionalInfo">
                Additional Information
              </Label>
              <Textarea
                id="additionalInfo"
                placeholder="Any other information that might help us understand your needs..."
                rows={2}
                {...register('additionalInfo')}
              />
            </div>
          </CardContent>
        </Card>

        {/* Submit Button */}
        <div className="flex justify-end space-x-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => reset()}
            disabled={isSubmitting || !isDirty}
          >
            Reset Form
          </Button>
          <Button
            type="submit"
            disabled={isSubmitting || !isValid}
            className="min-w-[150px]"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Submitting...
              </>
            ) : (
              'Submit Request'
            )}
          </Button>
        </div>
      </form>
    </div>
  );
}