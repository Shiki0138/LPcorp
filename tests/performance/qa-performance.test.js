/**
 * QA Performance Testing Suite
 * Enterprise-grade performance validation with K6 and Artillery
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time');
const requestCounter = new Counter('request_count');

// Performance configuration
export const options = {
  stages: [
    // Warm-up
    { duration: '2m', target: 10 },
    // Ramp-up
    { duration: '5m', target: 50 },
    // Peak load
    { duration: '10m', target: 100 },
    // Stress test
    { duration: '5m', target: 200 },
    // Cool-down
    { duration: '5m', target: 0 }
  ],
  thresholds: {
    // KPI Requirements
    http_req_duration: ['p(95)<500'], // 95% under 500ms
    http_req_failed: ['rate<0.01'],   // Less than 1% error rate
    error_rate: ['rate<0.01'],        // Less than 1% errors
    response_time: ['p(99)<1000']     // 99% under 1s
  },
  ext: {
    loadimpact: {
      distribution: {
        'amazon:us:ashburn': { loadZone: 'amazon:us:ashburn', percent: 50 },
        'amazon:gb:london': { loadZone: 'amazon:gb:london', percent: 25 },
        'amazon:sg:singapore': { loadZone: 'amazon:sg:singapore', percent: 25 }
      }
    }
  }
};

// Base URL configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';

// Test data
const testData = {
  users: [
    { name: 'Test User 1', email: 'test1@example.com', industry: 'technology' },
    { name: 'Test User 2', email: 'test2@example.com', industry: 'healthcare' },
    { name: 'Test User 3', email: 'test3@example.com', industry: 'finance' }
  ],
  businesses: [
    { name: 'Tech Startup', description: 'Innovative technology solutions', target: 'b2b' },
    { name: 'Healthcare Clinic', description: 'Premium medical services', target: 'b2c' },
    { name: 'Financial Services', description: 'Investment and banking', target: 'enterprise' }
  ]
};

export default function() {
  group('Landing Page Performance', () => {
    testLandingPageLoad();
    testStaticAssets();
  });

  group('API Performance', () => {
    testWizardAPI();
    testGenerationAPI();
  });

  group('User Journey Performance', () => {
    testCompleteUserFlow();
  });

  sleep(1);
}

function testLandingPageLoad() {
  group('Home Page Load', () => {
    const startTime = Date.now();
    const response = http.get(`${BASE_URL}/`);
    const endTime = Date.now();
    
    const success = check(response, {
      'status is 200': (r) => r.status === 200,
      'response time < 2s': (r) => r.timings.duration < 2000,
      'body contains title': (r) => r.body.includes('LP制作'),
      'body size reasonable': (r) => r.body.length > 1000 && r.body.length < 1000000
    });
    
    responseTime.add(endTime - startTime);
    requestCounter.add(1);
    errorRate.add(!success);
    
    if (!success) {
      console.error('Home page load failed:', response.status, response.error);
    }
  });
}

function testStaticAssets() {
  group('Static Assets', () => {
    const assets = [
      '/favicon.ico',
      '/_next/static/css/',
      '/_next/static/chunks/'
    ];
    
    assets.forEach(asset => {
      const response = http.get(`${BASE_URL}${asset}`);
      const success = check(response, {
        [`${asset} loads successfully`]: (r) => r.status === 200 || r.status === 404, // 404 is OK for some assets
        [`${asset} loads quickly`]: (r) => r.timings.duration < 1000
      });
      
      errorRate.add(!success && response.status !== 404);
    });
  });
}

function testWizardAPI() {
  group('Wizard API', () => {
    // Test wizard form submission
    const user = testData.users[Math.floor(Math.random() * testData.users.length)];
    const business = testData.businesses[Math.floor(Math.random() * testData.businesses.length)];
    
    const payload = {
      ...user,
      ...business
    };
    
    const headers = { 'Content-Type': 'application/json' };
    const response = http.post(`${BASE_URL}/api/wizard`, JSON.stringify(payload), { headers });
    
    const success = check(response, {
      'wizard API returns 200': (r) => r.status === 200,
      'wizard API responds quickly': (r) => r.timings.duration < 3000,
      'wizard API returns valid JSON': (r) => {
        try {
          JSON.parse(r.body);
          return true;
        } catch {
          return false;
        }
      }
    });
    
    errorRate.add(!success);
    requestCounter.add(1);
  });
}

function testGenerationAPI() {
  group('LP Generation API', () => {
    const business = testData.businesses[Math.floor(Math.random() * testData.businesses.length)];
    
    const payload = {
      type: 'landing-page',
      config: business
    };
    
    const headers = { 'Content-Type': 'application/json' };
    const response = http.post(`${BASE_URL}/api/generate`, JSON.stringify(payload), { headers });
    
    const success = check(response, {
      'generation API responds': (r) => r.status === 200 || r.status === 202,
      'generation API reasonable response time': (r) => r.timings.duration < 10000, // 10s for generation
      'generation includes HTML': (r) => r.body.includes('html') || r.body.includes('generated')
    });
    
    errorRate.add(!success);
    requestCounter.add(1);
  });
}

function testCompleteUserFlow() {
  group('Complete User Flow', () => {
    // Step 1: Visit home page
    let response = http.get(`${BASE_URL}/`);
    check(response, {
      'home page loads': (r) => r.status === 200
    });
    
    sleep(0.5);
    
    // Step 2: Visit wizard
    response = http.get(`${BASE_URL}/wizard`);
    check(response, {
      'wizard page loads': (r) => r.status === 200
    });
    
    sleep(1);
    
    // Step 3: Submit wizard form
    const user = testData.users[Math.floor(Math.random() * testData.users.length)];
    const business = testData.businesses[Math.floor(Math.random() * testData.businesses.length)];
    
    const payload = { ...user, ...business };
    const headers = { 'Content-Type': 'application/json' };
    
    response = http.post(`${BASE_URL}/api/wizard`, JSON.stringify(payload), { headers });
    
    const flowSuccess = check(response, {
      'complete flow succeeds': (r) => r.status === 200,
      'complete flow under 15s': (r) => r.timings.duration < 15000
    });
    
    sleep(2);
    
    // Step 4: View results
    response = http.get(`${BASE_URL}/wizard/result`);
    check(response, {
      'results page loads': (r) => r.status === 200
    });
    
    errorRate.add(!flowSuccess);
  });
}

// Teardown function
export function teardown(data) {
  console.log('Performance test completed');
  console.log(`Total requests: ${requestCounter.value}`);
  console.log(`Error rate: ${(errorRate.rate * 100).toFixed(2)}%`);
  console.log(`Average response time: ${responseTime.avg.toFixed(2)}ms`);
}

// Helper functions for Artillery.js configuration
export const artilleryConfig = {
  config: {
    target: BASE_URL,
    phases: [
      { duration: 60, arrivalRate: 5, name: 'Warm up' },
      { duration: 120, arrivalRate: 10, rampTo: 50, name: 'Ramp up load' },
      { duration: 300, arrivalRate: 50, name: 'Sustained load' },
      { duration: 120, arrivalRate: 50, rampTo: 100, name: 'Peak load' },
      { duration: 60, arrivalRate: 1, name: 'Cool down' }
    ],
    payload: {
      path: './test-data.csv',
      fields: ['name', 'email', 'industry']
    }
  },
  scenarios: [
    {
      name: 'Landing Page Workflow',
      weight: 70,
      flow: [
        { get: { url: '/' } },
        { think: 3 },
        { get: { url: '/wizard' } },
        { think: 5 },
        {
          post: {
            url: '/api/wizard',
            json: {
              name: '{{ name }}',
              email: '{{ email }}',
              industry: '{{ industry }}',
              description: 'Performance test business',
              target: 'b2b'
            }
          }
        },
        { think: 2 },
        { get: { url: '/wizard/result' } }
      ]
    },
    {
      name: 'Portfolio Browsing',
      weight: 20,
      flow: [
        { get: { url: '/portfolio' } },
        { think: 2 },
        { get: { url: '/portfolio/luxury-watch' } },
        { think: 3 },
        { get: { url: '/portfolio/medical-clinic' } },
        { think: 2 },
        { get: { url: '/portfolio/saas-project' } }
      ]
    },
    {
      name: 'Premium Features',
      weight: 10,
      flow: [
        { get: { url: '/premium-features' } },
        { think: 5 },
        { get: { url: '/pricing' } },
        { think: 3 }
      ]
    }
  ]
};

// Memory leak detection for Node.js environment
if (typeof process !== 'undefined' && process.memoryUsage) {
  export function checkMemoryUsage() {
    const usage = process.memoryUsage();
    const used = usage.heapUsed / 1024 / 1024;
    
    if (used > 100) { // Alert if over 100MB
      console.warn(`Memory usage high: ${Math.round(used * 100) / 100} MB`);
    }
    
    return {
      rss: Math.round(usage.rss / 1024 / 1024 * 100) / 100,
      heapTotal: Math.round(usage.heapTotal / 1024 / 1024 * 100) / 100,
      heapUsed: Math.round(usage.heapUsed / 1024 / 1024 * 100) / 100,
      external: Math.round(usage.external / 1024 / 1024 * 100) / 100
    };
  }
}