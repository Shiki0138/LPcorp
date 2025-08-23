package com.enterprise.security.mfa;

import com.enterprise.security.mfa.model.*;
import com.enterprise.security.mfa.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.mfa.sms.provider=mock",
    "app.mfa.webauthn.rp-id=test.example.com",
    "app.mfa.webauthn.rp-name=Test App"
})
@Transactional
public class MfaIntegrationTest {
    
    @Autowired
    private MfaService mfaService;
    
    @Autowired
    private TOTPService totpService;
    
    @Autowired
    private BackupCodeService backupCodeService;
    
    @Autowired
    private SMSService smsService;
    
    @Autowired
    private EmailService emailService;
    
    private final String testUserId = "test-user-123";
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        // In a real implementation, you'd clean up repositories
    }
    
    @Test
    void testCompleteM FAFlow() {
        // Test initial state - no MFA enabled
        assertFalse(mfaService.isMfaEnabled(testUserId));
        
        // Test TOTP enrollment
        String totpSecret = totpService.generateSecret();
        assertTrue(totpService.isValidSecret(totpSecret));
        
        String currentCode = totpService.generateCurrentCode(totpSecret);
        assertTrue(totpService.verifyCode(totpSecret, currentCode));
        
        // Test backup codes
        var backupCodes = backupCodeService.generateBackupCodes(testUserId);
        assertEquals(10, backupCodes.size());
        assertTrue(backupCodeService.hasUnusedCodes(testUserId));
        
        // Test backup code verification
        String firstCode = backupCodes.get(0);
        assertTrue(backupCodeService.verifyAndUseBackupCode(
            testUserId, firstCode, "127.0.0.1", "test-agent"));
        
        // Code should not be reusable
        assertFalse(backupCodeService.verifyAndUseBackupCode(
            testUserId, firstCode, "127.0.0.1", "test-agent"));
        
        assertEquals(9, backupCodeService.getRemainingCodeCount(testUserId));
    }
    
    @Test
    void testTOTPServiceFunctionality() {
        // Test secret generation
        String secret = totpService.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.length() > 0);
        assertTrue(totpService.isValidSecret(secret));
        
        // Test TOTP URI generation
        String uri = totpService.generateTOTPUri("test@example.com", secret);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("test@example.com"));
        assertTrue(uri.contains(secret));
        
        // Test code generation and verification
        String code = totpService.generateCurrentCode(secret);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
        
        // Code should be valid
        assertTrue(totpService.verifyCode(secret, code));
        
        // Invalid code should fail
        assertFalse(totpService.verifyCode(secret, "000000"));
        assertFalse(totpService.verifyCode(secret, "invalid"));
        
        // Test time window
        assertTrue(totpService.getRemainingSeconds() > 0);
        assertTrue(totpService.getRemainingSeconds() <= 30);
    }
    
    @Test
    void testBackupCodeService() {
        // Test code generation
        var codes = backupCodeService.generateBackupCodes(testUserId, 5);
        assertEquals(5, codes.size());
        
        // All codes should be unique
        assertEquals(5, codes.stream().distinct().count());
        
        // All codes should be valid format
        codes.forEach(code -> {
            assertTrue(backupCodeService.isValidCodeFormat(code));
            assertEquals(8, code.length());
        });
        
        // Test formatted display
        var formattedCodes = backupCodeService.formatCodesForDisplay(codes);
        formattedCodes.forEach(code -> {
            assertTrue(code.contains("-"));
            assertEquals(9, code.length()); // XXXX-XXXX
        });
        
        // Test statistics
        var stats = backupCodeService.getCodeStats(testUserId);
        assertEquals(5, stats.getTotal());
        assertEquals(0, stats.getUsed());
        assertEquals(5, stats.getRemaining());
        assertFalse(stats.isRunningLow());
        
        // Use some codes
        String firstCode = codes.get(0);
        assertTrue(backupCodeService.verifyAndUseBackupCode(
            testUserId, firstCode, "127.0.0.1", "test-agent"));
        
        // Check updated statistics
        stats = backupCodeService.getCodeStats(testUserId);
        assertEquals(5, stats.getTotal());
        assertEquals(1, stats.getUsed());
        assertEquals(4, stats.getRemaining());
        
        // Test regeneration
        var newCodes = backupCodeService.regenerateBackupCodes(testUserId, 3);
        assertEquals(3, newCodes.size());
        
        // Old codes should be invalid
        assertFalse(backupCodeService.verifyAndUseBackupCode(
            testUserId, codes.get(1), "127.0.0.1", "test-agent"));
    }
    
    @Test
    void testSMSService() {
        // Test phone number validation
        assertTrue(smsService.isValidPhoneNumber("+1234567890"));
        assertTrue(smsService.isValidPhoneNumber("+441234567890"));
        assertFalse(smsService.isValidPhoneNumber("1234567890")); // No country code
        assertFalse(smsService.isValidPhoneNumber("invalid"));
        assertFalse(smsService.isValidPhoneNumber(""));
        
        // Test phone number normalization
        assertEquals("+1234567890", smsService.normalizePhoneNumber("1234567890"));
        assertEquals("+1234567890", smsService.normalizePhoneNumber("+1234567890"));
        assertEquals("+1234567890", smsService.normalizePhoneNumber("(123) 456-7890"));
        
        // Test code generation
        String code = smsService.generateCode();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
        
        // Test SMS sending (mock mode)
        String phoneNumber = "+1234567890";
        assertTrue(smsService.sendVerificationCode(phoneNumber, code, "127.0.0.1"));
        
        // Test rate limiting
        assertEquals(4, smsService.getRemainingAttempts(phoneNumber)); // After one send
    }
    
    @Test
    void testEmailService() {
        // Test email validation
        assertTrue(emailService.isValidEmail("test@example.com"));
        assertTrue(emailService.isValidEmail("user.name+tag@domain.co.uk"));
        assertFalse(emailService.isValidEmail("invalid-email"));
        assertFalse(emailService.isValidEmail("@example.com"));
        assertFalse(emailService.isValidEmail("test@"));
        
        // Test email normalization
        assertEquals("test@example.com", emailService.normalizeEmail("  TEST@EXAMPLE.COM  "));
        assertEquals("user@domain.com", emailService.normalizeEmail("User@Domain.Com"));
        
        // Test code generation
        String numericCode = emailService.generateCode();
        assertEquals(6, numericCode.length());
        assertTrue(numericCode.matches("\\d{6}"));
        
        String alphaCode = emailService.generateAlphanumericCode(8);
        assertEquals(8, alphaCode.length());
        assertTrue(alphaCode.matches("[A-Z0-9]+"));
        
        // Test email sending would require mock mail sender setup
        // In integration tests, you might want to use a test mail server
        
        // Test rate limiting
        String email = "test@example.com";
        assertEquals(5, emailService.getRemainingAttempts(email)); // Initial attempts
    }
    
    @Test
    void testMfaEnrollmentStatus() {
        // Initial status - no MFA
        var status = mfaService.getEnrollmentStatus(testUserId);
        assertFalse(status.isHasMfaEnabled());
        assertFalse(status.isHasStrongMfa());
        assertFalse(status.isHasBackupCodes());
        assertFalse(status.isFullySetup());
        
        // Recommendations should suggest setup
        var recommendations = status.getRecommendations();
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("strong MFA")));
        
        // After generating backup codes
        backupCodeService.generateBackupCodes(testUserId);
        status = mfaService.getEnrollmentStatus(testUserId);
        assertTrue(status.isHasBackupCodes());
        
        // Simulate TOTP device enrollment (in real test, you'd use repositories)
        // This would require setting up the full Spring context with repositories
    }
    
    @Test
    void testSecurityConstraints() {
        // Test that secrets are properly validated
        assertFalse(totpService.isValidSecret(""));
        assertFalse(totpService.isValidSecret("invalid"));
        assertFalse(totpService.isValidSecret("short"));
        
        // Test that backup codes follow security requirements
        var codes = backupCodeService.generateBackupCodes(testUserId);
        codes.forEach(code -> {
            // Should not contain confusing characters
            assertFalse(code.contains("0"));
            assertFalse(code.contains("O"));
            assertFalse(code.contains("1"));
            assertFalse(code.contains("I"));
            assertFalse(code.contains("L"));
            
            // Should be uppercase alphanumeric
            assertTrue(code.matches("[2-9A-HJ-NP-Z]+"));
        });
        
        // Test rate limiting works
        String phoneNumber = "+1234567890";
        String testCode = "123456";
        
        // First send should work
        assertTrue(smsService.sendVerificationCode(phoneNumber, testCode, "127.0.0.1"));
        assertEquals(4, smsService.getRemainingAttempts(phoneNumber));
        
        // Continue sending until rate limited
        for (int i = 0; i < 4; i++) {
            smsService.sendVerificationCode(phoneNumber, testCode, "127.0.0.1");
        }
        
        // Should be rate limited now
        assertEquals(0, smsService.getRemainingAttempts(phoneNumber));
    }
    
    @Test
    void testErrorHandling() {
        // Test invalid inputs
        assertThrows(IllegalArgumentException.class, () -> {
            backupCodeService.generateBackupCodes(testUserId, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            backupCodeService.generateBackupCodes(testUserId, 25);
        });
        
        // Test null/empty inputs
        assertFalse(totpService.verifyCode(null, "123456"));
        assertFalse(totpService.verifyCode("secret", null));
        assertFalse(totpService.verifyCode("secret", ""));
        
        assertFalse(backupCodeService.verifyAndUseBackupCode(testUserId, null, "127.0.0.1", "test"));
        assertFalse(backupCodeService.verifyAndUseBackupCode(testUserId, "", "127.0.0.1", "test"));
        
        // Test invalid formats
        assertFalse(totpService.verifyCode("secret", "12345")); // Too short
        assertFalse(totpService.verifyCode("secret", "1234567")); // Too long
        assertFalse(totpService.verifyCode("secret", "abc123")); // Non-numeric
    }
}