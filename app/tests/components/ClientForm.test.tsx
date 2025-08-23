import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ClientForm from '@/components/forms/ClientForm';

// Mock the validation module
jest.mock('@/lib/validation', () => ({
  clientFormSchema: jest.fn(),
  validateClientForm: jest.fn(),
  classifyProject: jest.fn(),
  sanitizeInput: jest.fn(),
}));

// Mock fetch
global.fetch = jest.fn();

describe('ClientForm', () => {
  const mockOnSubmit = jest.fn();
  const mockOnSuccess = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        success: true,
        message: 'Form submitted successfully',
        data: { projectId: '123' },
      }),
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render all required form fields', () => {
    render(<ClientForm />);

    // Personal Information
    expect(screen.getByLabelText(/contact person/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone number/i)).toBeInTheDocument();

    // Business Information
    expect(screen.getByLabelText(/industry/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/business type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/project budget/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/preferred timeline/i)).toBeInTheDocument();

    // Project Requirements
    expect(screen.getByLabelText(/project type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/project description/i)).toBeInTheDocument();

    // Submit button
    expect(screen.getByRole('button', { name: /submit request/i })).toBeInTheDocument();
  });

  it('should render optional fields', () => {
    render(<ClientForm />);

    expect(screen.getByLabelText(/company name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/website/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/business address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/specific requirements/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/additional information/i)).toBeInTheDocument();
  });

  it('should show validation errors for invalid inputs', async () => {
    render(<ClientForm />);
    const user = userEvent.setup();

    // Try to submit without filling required fields
    const submitButton = screen.getByRole('button', { name: /submit request/i });
    
    // Submit button should be disabled initially
    expect(submitButton).toBeDisabled();

    // Fill in some invalid data
    const emailInput = screen.getByLabelText(/email address/i);
    await user.type(emailInput, 'invalid-email');

    const phoneInput = screen.getByLabelText(/phone number/i);
    await user.type(phoneInput, 'invalid-phone');

    // The form should show validation errors (via react-hook-form)
    // We can't easily test the specific error messages without mocking the validation
    // but we can verify the inputs have the error styling
    expect(emailInput).toHaveClass('border-red-500');
    expect(phoneInput).toHaveClass('border-red-500');
  });

  it('should handle successful form submission', async () => {
    render(<ClientForm onSuccess={mockOnSuccess} />);
    const user = userEvent.setup();

    // Fill in valid form data
    await user.type(screen.getByLabelText(/contact person/i), 'John Doe');
    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/phone number/i), '+1234567890');
    
    // Select dropdowns
    await user.click(screen.getByLabelText(/industry/i));
    await user.click(screen.getByText('Technology'));
    
    await user.click(screen.getByLabelText(/business type/i));
    await user.click(screen.getByText('Startup'));
    
    await user.click(screen.getByLabelText(/project budget/i));
    await user.click(screen.getByText('$10,000 - $25,000'));
    
    await user.click(screen.getByLabelText(/preferred timeline/i));
    await user.click(screen.getByText('2-4 weeks'));

    // Select project types
    const websiteCheckbox = screen.getByRole('checkbox', { name: /website design/i });
    await user.click(websiteCheckbox);

    // Fill description
    await user.type(
      screen.getByLabelText(/project description/i),
      'This is a detailed project description with more than 10 characters.'
    );

    // Submit form
    const submitButton = screen.getByRole('button', { name: /submit request/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockOnSuccess).toHaveBeenCalledWith({
        success: true,
        message: 'Form submitted successfully',
        data: { projectId: '123' },
      });
    });
  });

  it('should handle form submission errors', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: false,
      json: () => Promise.resolve({
        success: false,
        message: 'Submission failed',
        error: 'Validation failed',
      }),
    });

    render(<ClientForm />);
    const user = userEvent.setup();

    // Fill minimal valid data
    await user.type(screen.getByLabelText(/contact person/i), 'John Doe');
    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/phone number/i), '+1234567890');
    await user.type(screen.getByLabelText(/project description/i), 'Valid description');

    const submitButton = screen.getByRole('button', { name: /submit request/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/submission failed/i)).toBeInTheDocument();
    });
  });

  it('should show loading state during submission', async () => {
    // Mock a delayed response
    (global.fetch as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({
        ok: true,
        json: () => Promise.resolve({ success: true }),
      }), 100))
    );

    render(<ClientForm />);
    const user = userEvent.setup();

    // Fill minimal form data
    await user.type(screen.getByLabelText(/contact person/i), 'John Doe');
    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/phone number/i), '+1234567890');
    await user.type(screen.getByLabelText(/project description/i), 'Valid description');

    const submitButton = screen.getByRole('button', { name: /submit request/i });
    await user.click(submitButton);

    // Should show loading state
    expect(screen.getByText(/submitting/i)).toBeInTheDocument();
    expect(submitButton).toBeDisabled();
  });

  it('should reset form when reset button is clicked', async () => {
    render(<ClientForm />);
    const user = userEvent.setup();

    // Fill some form data
    const nameInput = screen.getByLabelText(/contact person/i);
    await user.type(nameInput, 'John Doe');
    expect(nameInput).toHaveValue('John Doe');

    // Click reset button
    const resetButton = screen.getByRole('button', { name: /reset form/i });
    await user.click(resetButton);

    // Form should be cleared
    expect(nameInput).toHaveValue('');
  });

  it('should show project analysis when business type and project type are selected', async () => {
    render(<ClientForm />);
    const user = userEvent.setup();

    // Select business type
    await user.click(screen.getByLabelText(/business type/i));
    await user.click(screen.getByText('Enterprise'));

    // Select project type
    const websiteCheckbox = screen.getByRole('checkbox', { name: /website design/i });
    await user.click(websiteCheckbox);

    // Should show project analysis
    expect(screen.getByText(/project analysis/i)).toBeInTheDocument();
    expect(screen.getByText(/estimated complexity/i)).toBeInTheDocument();
    expect(screen.getByText(/priority level/i)).toBeInTheDocument();
  });

  it('should call custom onSubmit handler when provided', async () => {
    render(<ClientForm onSubmit={mockOnSubmit} />);
    const user = userEvent.setup();

    // Fill minimal form data
    await user.type(screen.getByLabelText(/contact person/i), 'John Doe');
    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/phone number/i), '+1234567890');
    await user.type(screen.getByLabelText(/project description/i), 'Valid description');

    const submitButton = screen.getByRole('button', { name: /submit request/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalled();
    });

    // Should not call the default API
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('should handle network errors gracefully', async () => {
    (global.fetch as jest.Mock).mockRejectedValue(new Error('Network error'));

    render(<ClientForm />);
    const user = userEvent.setup();

    // Fill minimal form data
    await user.type(screen.getByLabelText(/contact person/i), 'John Doe');
    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/phone number/i), '+1234567890');
    await user.type(screen.getByLabelText(/project description/i), 'Valid description');

    const submitButton = screen.getByRole('button', { name: /submit request/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument();
    });
  });
});