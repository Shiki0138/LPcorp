/**
 * Jest Global Setup for QA Testing
 * Enterprise test environment configuration
 */

// Performance monitoring
const startTime = Date.now();
let testCount = 0;
let failedTests = 0;

// Global test utilities
global.QA_FRAMEWORK = {
  startTime,
  testCount: 0,
  failedTests: 0,
  performance: {
    slowTests: [],
    memoryLeaks: [],
    failedAssertions: []
  }
};

// Enhanced assertions
expect.extend({
  toBeWithinPerformanceThreshold(received, threshold) {
    const pass = received <= threshold;
    
    if (!pass) {
      global.QA_FRAMEWORK.performance.slowTests.push({
        duration: received,
        threshold,
        test: expect.getState().currentTestName
      });
    }
    
    return {
      message: () => `Expected ${received}ms to be within performance threshold of ${threshold}ms`,
      pass
    };
  },
  
  toBeSecure(received) {
    const securityPatterns = [
      /<script/i,
      /javascript:/i,
      /on\w+=/i,
      /DROP TABLE/i,
      /'; DELETE/i,
      /\|\| wget/i
    ];
    
    const isSecure = !securityPatterns.some(pattern => pattern.test(received));
    
    if (!isSecure) {
      global.QA_FRAMEWORK.performance.failedAssertions.push({
        type: 'SECURITY',
        value: received,
        test: expect.getState().currentTestName
      });
    }
    
    return {
      message: () => `Expected "${received}" to be secure (no malicious patterns)`,
      pass: isSecure
    };
  },
  
  toHaveValidHTML(received) {
    const isValidHTML = /<html[\s\S]*<\/html>/i.test(received) || 
                       /<div[\s\S]*<\/div>/i.test(received) ||
                       /^[^<]*$/.test(received); // Plain text is OK
    
    return {
      message: () => `Expected valid HTML structure`,
      pass: isValidHTML
    };
  },
  
  toBeAccessible(received) {
    // Basic accessibility checks
    const hasHeading = /<h[1-6]/i.test(received);
    const hasAltText = !/<img(?![^>]*alt=)/i.test(received); // No img without alt
    const hasValidStructure = !/<div role="button">/i.test(received); // No div buttons
    
    const isAccessible = hasHeading && hasAltText && hasValidStructure;
    
    return {
      message: () => `Expected content to meet accessibility standards`,
      pass: isAccessible
    };
  }
});

// Performance monitoring hooks
beforeEach(() => {
  global.QA_FRAMEWORK.testCount++;
  global.QA_FRAMEWORK.currentTest = {
    name: expect.getState().currentTestName,
    startTime: Date.now(),
    memoryBefore: process.memoryUsage().heapUsed
  };
});

afterEach(() => {
  const currentTest = global.QA_FRAMEWORK.currentTest;
  const endTime = Date.now();
  const memoryAfter = process.memoryUsage().heapUsed;
  
  const duration = endTime - currentTest.startTime;
  const memoryDiff = memoryAfter - currentTest.memoryBefore;
  
  // Log slow tests
  if (duration > 1000) {
    console.warn(`⚠️  Slow test detected: ${currentTest.name} (${duration}ms)`);
    global.QA_FRAMEWORK.performance.slowTests.push({
      name: currentTest.name,
      duration,
      timestamp: new Date().toISOString()
    });
  }
  
  // Log potential memory leaks
  if (memoryDiff > 10 * 1024 * 1024) { // 10MB
    console.warn(`⚠️  Potential memory leak: ${currentTest.name} (+${(memoryDiff / 1024 / 1024).toFixed(2)}MB)`);
    global.QA_FRAMEWORK.performance.memoryLeaks.push({
      name: currentTest.name,
      memoryIncrease: memoryDiff,
      timestamp: new Date().toISOString()
    });
  }
});

// Test failure tracking
const originalIt = global.it;
global.it = (name, fn, timeout) => {
  return originalIt(name, async (...args) => {
    try {
      await fn(...args);
    } catch (error) {
      global.QA_FRAMEWORK.failedTests++;
      console.error(`❌ Test failed: ${name}`);
      console.error(error.message);
      throw error;
    }
  }, timeout);
};

// Global setup for mocks
global.mockAPIResponse = (data, status = 200, delay = 0) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        status,
        ok: status >= 200 && status < 300,
        json: () => Promise.resolve(data),
        text: () => Promise.resolve(JSON.stringify(data))
      });
    }, delay);
  });
};

global.mockNetworkError = (message = 'Network Error') => {
  return Promise.reject(new Error(message));
};

// Security testing utilities
global.MALICIOUS_PAYLOADS = {
  XSS: [
    '<script>alert("xss")</script>',
    '<img src="x" onerror="alert(1)">',
    'javascript:alert("xss")',
    '<svg onload="alert(1)">'
  ],
  SQL_INJECTION: [
    "'; DROP TABLE users; --",
    "' OR '1'='1",
    "' UNION SELECT * FROM passwords --"
  ],
  COMMAND_INJECTION: [
    '; rm -rf /',
    '| cat /etc/passwd',
    '&& whoami'
  ]
};

// Test data generators
global.generateTestData = {
  user: (overrides = {}) => ({
    id: Math.floor(Math.random() * 1000),
    name: 'Test User',
    email: 'test@example.com',
    role: 'user',
    ...overrides
  }),
  
  business: (overrides = {}) => ({
    name: 'Test Business',
    description: 'Test business description',
    industry: 'technology',
    target: 'b2b',
    ...overrides
  }),
  
  maliciousInput: () => {
    const categories = Object.keys(global.MALICIOUS_PAYLOADS);
    const category = categories[Math.floor(Math.random() * categories.length)];
    const payloads = global.MALICIOUS_PAYLOADS[category];
    return payloads[Math.floor(Math.random() * payloads.length)];
  }
};

// Environment setup
process.env.NODE_ENV = 'test';
process.env.QA_MODE = 'true';
process.env.LOG_LEVEL = 'error'; // Reduce noise during tests

// Console override for cleaner test output
const originalConsoleError = console.error;
console.error = (...args) => {
  // Only show errors that are not part of expected test failures
  const message = args[0];
  if (typeof message === 'string' && message.includes('Test failed:')) {
    return; // Skip our test failure logs
  }
  originalConsoleError.apply(console, args);
};

// Cleanup function
process.on('exit', () => {
  const endTime = Date.now();
  const totalTime = endTime - startTime;
  
  console.log('\n📊 QA Test Suite Summary:');
  console.log(`⏱️  Total time: ${totalTime}ms`);
  console.log(`✅ Tests run: ${global.QA_FRAMEWORK.testCount}`);
  console.log(`❌ Failed tests: ${global.QA_FRAMEWORK.failedTests}`);
  console.log(`🐌 Slow tests: ${global.QA_FRAMEWORK.performance.slowTests.length}`);
  console.log(`💾 Memory leaks: ${global.QA_FRAMEWORK.performance.memoryLeaks.length}`);
  
  if (global.QA_FRAMEWORK.performance.slowTests.length > 0) {
    console.log('\n🐌 Slow Tests:');
    global.QA_FRAMEWORK.performance.slowTests.forEach(test => {
      console.log(`  - ${test.name}: ${test.duration}ms`);
    });
  }
  
  if (global.QA_FRAMEWORK.performance.memoryLeaks.length > 0) {
    console.log('\n💾 Potential Memory Leaks:');
    global.QA_FRAMEWORK.performance.memoryLeaks.forEach(leak => {
      console.log(`  - ${leak.name}: +${(leak.memoryIncrease / 1024 / 1024).toFixed(2)}MB`);
    });
  }
});

// Unhandled rejection tracking
process.on('unhandledRejection', (reason, promise) => {
  console.error('🚨 Unhandled Promise Rejection:', reason);
  global.QA_FRAMEWORK.failedTests++;
});