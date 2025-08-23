/**
 * Playwright Configuration for E2E Testing
 * Enterprise-grade browser testing configuration
 */

const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  // Test directory
  testDir: '../../tests/e2e',
  
  // Global test timeout
  timeout: 30 * 1000,
  
  // Test configuration
  expect: {
    timeout: 5000
  },
  
  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: !!process.env.CI,
  
  // Retry on CI only
  retries: process.env.CI ? 2 : 0,
  
  // Opt out of parallel tests on CI
  workers: process.env.CI ? 1 : undefined,
  
  // Reporter configuration
  reporter: [
    ['html', { 
      outputFolder: '../../coverage/playwright-report',
      open: 'never'
    }],
    ['json', { 
      outputFile: '../../coverage/playwright-results.json' 
    }],
    ['junit', { 
      outputFile: '../../coverage/playwright-junit.xml' 
    }],
    ['list']
  ],
  
  // Shared settings for all the projects below
  use: {
    // Base URL for tests
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    
    // Collect trace when retrying failed tests
    trace: 'on-first-retry',
    
    // Record video on failure
    video: 'retain-on-failure',
    
    // Take screenshot on failure
    screenshot: 'only-on-failure',
    
    // Accept downloads
    acceptDownloads: true,
    
    // Ignore HTTPS errors
    ignoreHTTPSErrors: false,
    
    // Viewport
    viewport: { width: 1280, height: 720 },
    
    // User agent
    userAgent: 'QA-Test-Suite/1.0 (Enterprise Testing)'
  },
  
  // Configure projects for major browsers
  projects: [
    // Desktop browsers
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    
    // Mobile browsers
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
    
    // Tablet browsers
    {
      name: 'iPad',
      use: { ...devices['iPad Pro'] },
    },
    
    // High DPI
    {
      name: 'High DPI',
      use: {
        ...devices['Desktop Chrome'],
        deviceScaleFactor: 2,
      },
    },
    
    // Dark mode
    {
      name: 'Dark Mode',
      use: {
        ...devices['Desktop Chrome'],
        colorScheme: 'dark',
      },
    },
    
    // Slow network
    {
      name: 'Slow 3G',
      use: {
        ...devices['Desktop Chrome'],
        connectionType: 'slow-3g',
      },
    }
  ],
  
  // Global setup/teardown
  globalSetup: '../../config/testing/playwright.global-setup.js',
  globalTeardown: '../../config/testing/playwright.global-teardown.js',
  
  // Run your local dev server before starting the tests
  webServer: process.env.CI ? undefined : {
    command: 'npm run dev',
    port: 3000,
    timeout: 120 * 1000,
    reuseExistingServer: !process.env.CI,
  },
  
  // Output directory
  outputDir: '../../coverage/playwright-results',
  
  // Test match patterns
  testMatch: [
    '**/*.spec.js',
    '**/*.e2e.js'
  ],
  
  // Test ignore patterns
  testIgnore: [
    '**/node_modules/**',
    '**/coverage/**'
  ]
});