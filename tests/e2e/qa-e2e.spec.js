/**
 * QA End-to-End Testing Suite
 * Complete user journey validation with Playwright
 */

const { test, expect } = require('@playwright/test');

// QA Configuration
const QA_CONFIG = {
  timeout: 30000,
  retries: 2,
  baseURL: 'http://localhost:3000',
  viewport: { width: 1280, height: 720 },
  devices: ['Desktop Chrome', 'Mobile Safari', 'Desktop Firefox']
};

// Test utilities
class E2EQAFramework {
  constructor(page) {
    this.page = page;
    this.metrics = {
      startTime: 0,
      loadTime: 0,
      interactionTime: 0,
      errors: []
    };
  }

  async startTest(testName) {
    this.metrics.startTime = Date.now();
    console.log(`🚀 Starting E2E test: ${testName}`);
    
    // Setup error monitoring
    this.page.on('pageerror', error => {
      this.metrics.errors.push(error.message);
    });
    
    this.page.on('requestfailed', request => {
      this.metrics.errors.push(`Failed request: ${request.url()}`);
    });
  }

  async endTest(testName) {
    const endTime = Date.now();
    const totalTime = endTime - this.metrics.startTime;
    
    console.log(`✅ E2E test completed: ${testName}`);
    console.log(`⏱️  Total time: ${totalTime}ms`);
    
    // Validate zero errors
    if (this.metrics.errors.length > 0) {
      throw new Error(`Zero-error policy violated: ${this.metrics.errors.join(', ')}`);
    }
    
    // Performance validation
    if (totalTime > 10000) {
      throw new Error(`E2E test exceeded time limit: ${totalTime}ms > 10000ms`);
    }
  }

  async waitForLoadComplete() {
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForLoadState('domcontentloaded');
  }

  async takeScreenshot(name) {
    await this.page.screenshot({ 
      path: `tests/e2e/screenshots/${name}-${Date.now()}.png`,
      fullPage: true 
    });
  }

  async checkAccessibility() {
    // Basic accessibility checks
    const headings = await this.page.locator('h1, h2, h3, h4, h5, h6').count();
    const images = await this.page.locator('img').count();
    const imagesWithAlt = await this.page.locator('img[alt]').count();
    
    if (images > 0 && imagesWithAlt !== images) {
      console.warn(`⚠️ Accessibility warning: ${images - imagesWithAlt} images missing alt text`);
    }
    
    // Check for proper heading structure
    const h1Count = await this.page.locator('h1').count();
    if (h1Count !== 1) {
      console.warn(`⚠️ Accessibility warning: Page should have exactly 1 h1, found ${h1Count}`);
    }
  }
}

test.describe('QA E2E Testing Suite', () => {
  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(QA_CONFIG.viewport);
  });

  test.describe('Landing Page Journey', () => {
    test('should complete full landing page creation workflow', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Landing Page Creation');

      // Navigate to home page
      await page.goto('/');
      await qa.waitForLoadComplete();
      
      // Verify page loads correctly
      await expect(page).toHaveTitle(/LP制作/);
      await expect(page.locator('h1')).toBeVisible();
      
      // Check accessibility
      await qa.checkAccessibility();
      
      // Navigate to wizard
      await page.click('text=今すぐ始める');
      await qa.waitForLoadComplete();
      
      // Verify wizard page
      await expect(page.locator('[data-testid="wizard-form"]')).toBeVisible();
      
      // Fill wizard form
      await page.fill('[name="businessName"]', 'Test Business');
      await page.fill('[name="description"]', 'Test business description');
      await page.selectOption('[name="industry"]', 'technology');
      await page.selectOption('[name="target"]', 'b2b');
      
      // Submit form
      await page.click('button[type="submit"]');
      await qa.waitForLoadComplete();
      
      // Verify results page
      await expect(page.locator('[data-testid="lp-preview"]')).toBeVisible();
      
      // Take screenshot for verification
      await qa.takeScreenshot('lp-creation-complete');
      
      await qa.endTest('Landing Page Creation');
    });

    test('should handle form validation correctly', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Form Validation');

      await page.goto('/wizard');
      await qa.waitForLoadComplete();
      
      // Try to submit empty form
      await page.click('button[type="submit"]');
      
      // Verify validation messages appear
      await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
      
      // Fill required fields one by one and verify validation
      await page.fill('[name="businessName"]', 'Test');
      await page.fill('[name="description"]', 'Short desc');
      
      // Verify form becomes submittable
      const submitButton = page.locator('button[type="submit"]');
      await expect(submitButton).toBeEnabled();
      
      await qa.endTest('Form Validation');
    });
  });

  test.describe('Portfolio Showcase', () => {
    test('should navigate through portfolio examples', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Portfolio Navigation');

      await page.goto('/portfolio');
      await qa.waitForLoadComplete();
      
      // Verify portfolio page loads
      await expect(page.locator('h1')).toContainText('ポートフォリオ');
      
      // Test each portfolio item
      const portfolioItems = [
        'luxury-watch',
        'medical-clinic',
        'saas-project'
      ];
      
      for (const item of portfolioItems) {
        await page.click(`[data-testid="portfolio-${item}"]`);
        await qa.waitForLoadComplete();
        
        // Verify portfolio detail page
        await expect(page.locator('[data-testid="portfolio-detail"]')).toBeVisible();
        
        // Check accessibility
        await qa.checkAccessibility();
        
        // Take screenshot
        await qa.takeScreenshot(`portfolio-${item}`);
        
        // Go back to portfolio
        await page.goBack();
        await qa.waitForLoadComplete();
      }
      
      await qa.endTest('Portfolio Navigation');
    });
  });

  test.describe('Premium Features', () => {
    test('should display premium features correctly', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Premium Features');

      await page.goto('/premium-features');
      await qa.waitForLoadComplete();
      
      // Verify premium features page
      await expect(page.locator('h1')).toContainText('プレミアム機能');
      
      // Check feature cards are displayed
      const featureCards = await page.locator('[data-testid="feature-card"]').count();
      expect(featureCards).toBeGreaterThan(0);
      
      // Test feature interactions
      await page.hover('[data-testid="feature-card"]:first-child');
      
      // Check for call-to-action buttons
      await expect(page.locator('[data-testid="cta-button"]')).toBeVisible();
      
      await qa.endTest('Premium Features');
    });
  });

  test.describe('Analytics Dashboard', () => {
    test('should load analytics features correctly', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Analytics Dashboard');

      await page.goto('/analytics');
      await qa.waitForLoadComplete();
      
      // Verify analytics page
      await expect(page.locator('h1')).toContainText('アナリティクス');
      
      // Check for charts/visualizations
      await expect(page.locator('[data-testid="analytics-chart"]')).toBeVisible();
      
      // Test navigation to features
      await page.click('[data-testid="analytics-features-link"]');
      await qa.waitForLoadComplete();
      
      await expect(page.locator('h1')).toContainText('アナリティクス機能');
      
      await qa.endTest('Analytics Dashboard');
    });
  });

  test.describe('Cross-Device Testing', () => {
    QA_CONFIG.devices.forEach(deviceName => {
      test(`should work correctly on ${deviceName}`, async ({ page, context }) => {
        const qa = new E2EQAFramework(page);
        await qa.startTest(`Cross-Device: ${deviceName}`);

        // Set device-specific viewport if mobile
        if (deviceName.includes('Mobile')) {
          await page.setViewportSize({ width: 375, height: 667 });
        }
        
        await page.goto('/');
        await qa.waitForLoadComplete();
        
        // Test responsive design
        await expect(page.locator('h1')).toBeVisible();
        
        // Test mobile menu if applicable
        if (deviceName.includes('Mobile')) {
          const menuButton = page.locator('[data-testid="mobile-menu-button"]');
          if (await menuButton.isVisible()) {
            await menuButton.click();
            await expect(page.locator('[data-testid="mobile-menu"]')).toBeVisible();
          }
        }
        
        // Check basic functionality works
        await page.click('text=今すぐ始める');
        await qa.waitForLoadComplete();
        await expect(page.locator('[data-testid="wizard-form"]')).toBeVisible();
        
        await qa.takeScreenshot(`device-${deviceName.replace(/\s+/g, '-')}`);
        
        await qa.endTest(`Cross-Device: ${deviceName}`);
      });
    });
  });

  test.describe('Performance Validation', () => {
    test('should meet performance benchmarks', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Performance Validation');

      // Start performance monitoring
      const startTime = Date.now();
      
      await page.goto('/');
      
      // Measure initial page load
      await qa.waitForLoadComplete();
      const loadTime = Date.now() - startTime;
      
      console.log(`📊 Page load time: ${loadTime}ms`);
      
      // Performance assertions
      expect(loadTime).toBeLessThan(3000); // Should load in under 3 seconds
      
      // Test JavaScript performance
      const jsStartTime = Date.now();
      await page.evaluate(() => {
        // Simulate heavy computation
        const data = Array(10000).fill(0).map((_, i) => ({ id: i, value: Math.random() }));
        return data.filter(item => item.value > 0.5).length;
      });
      const jsTime = Date.now() - jsStartTime;
      
      console.log(`⚡ JavaScript execution time: ${jsTime}ms`);
      expect(jsTime).toBeLessThan(100); // Should execute in under 100ms
      
      await qa.endTest('Performance Validation');
    });
  });

  test.describe('Error Handling', () => {
    test('should handle network errors gracefully', async ({ page }) => {
      const qa = new E2EQAFramework(page);
      await qa.startTest('Error Handling');

      // Intercept network requests and simulate errors
      await page.route('**/api/**', route => {
        route.abort('failed');
      });
      
      await page.goto('/wizard');
      await qa.waitForLoadComplete();
      
      // Try to submit form (should handle API error gracefully)
      await page.fill('[name="businessName"]', 'Test Business');
      await page.fill('[name="description"]', 'Test description');
      await page.click('button[type="submit"]');
      
      // Verify error handling
      await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
      
      // Clear route interception
      await page.unroute('**/api/**');
      
      await qa.endTest('Error Handling');
    });
  });
});

// Global test configuration
test.use({
  timeout: QA_CONFIG.timeout,
  retries: QA_CONFIG.retries,
  screenshot: 'only-on-failure',
  video: 'retain-on-failure'
});

module.exports = { E2EQAFramework, QA_CONFIG };