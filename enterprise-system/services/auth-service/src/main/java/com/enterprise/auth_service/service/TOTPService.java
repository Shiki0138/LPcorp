package com.enterprise.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Time-based One-Time Password (TOTP) service for MFA
 */
@Service
@Slf4j
public class TOTPService {

    private static final String ISSUER = "Enterprise Auth Service";
    private static final int SECRET_SIZE = 32;
    private static final int CODE_LENGTH = 6;
    private static final int TIME_STEP = 30; // seconds
    private static final int WINDOW = 1; // Allow 1 time window before/after for clock skew
    private static final String ALGORITHM = "HmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new TOTP secret for a user
     */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        secureRandom.nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }

    /**
     * Generate QR code URI for TOTP setup
     */
    public String generateQRCodeUri(String email, String secret) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA256&digits=%d&period=%d",
            ISSUER, email, secret, ISSUER, CODE_LENGTH, TIME_STEP
        );
    }

    /**
     * Verify a TOTP code against a secret
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }

        try {
            long currentTime = getCurrentTimeWindow();
            
            // Check current time window and adjacent windows for clock skew tolerance
            for (int i = -WINDOW; i <= WINDOW; i++) {
                String calculatedCode = generateCode(secret, currentTime + i);
                if (calculatedCode.equals(code)) {
                    log.debug("TOTP code verified successfully (window offset: {})", i);
                    return true;
                }
            }
            
            log.debug("TOTP code verification failed");
            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }

    /**
     * Generate backup codes for account recovery
     */
    public List<String> generateBackupCodes(int count) {
        List<String> backupCodes = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            byte[] buffer = new byte[8];
            secureRandom.nextBytes(buffer);
            
            // Convert to numeric string
            StringBuilder code = new StringBuilder();
            for (byte b : buffer) {
                code.append(String.format("%02d", Math.abs(b) % 100));
            }
            
            // Format as XXX-XXX-XXX
            String formattedCode = code.substring(0, 8);
            formattedCode = formattedCode.substring(0, 3) + "-" + 
                           formattedCode.substring(3, 6) + "-" + 
                           formattedCode.substring(6, 8) + 
                           String.format("%01d", secureRandom.nextInt(10));
            
            backupCodes.add(formattedCode);
        }
        
        return backupCodes;
    }

    /**
     * Generate current TOTP code for testing/display purposes
     */
    public String getCurrentCode(String secret) {
        try {
            return generateCode(secret, getCurrentTimeWindow());
        } catch (Exception e) {
            log.error("Error generating current TOTP code", e);
            return null;
        }
    }

    /**
     * Get remaining seconds until next code generation
     */
    public int getSecondsUntilNextCode() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        return TIME_STEP - (int)(currentTimeSeconds % TIME_STEP);
    }

    private long getCurrentTimeWindow() {
        return System.currentTimeMillis() / 1000 / TIME_STEP;
    }

    private String generateCode(String secret, long timeWindow) {
        try {
            byte[] secretBytes = new Base32().decode(secret);
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeWindow).array();
            
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);
            
            // Dynamic truncation
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24) |
                        ((hash[offset + 1] & 0xFF) << 16) |
                        ((hash[offset + 2] & 0xFF) << 8) |
                        (hash[offset + 3] & 0xFF);
            
            int otp = binary % (int) Math.pow(10, CODE_LENGTH);
            return String.format("%0" + CODE_LENGTH + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }

    /**
     * Validate secret format
     */
    public boolean isValidSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return false;
        }
        
        try {
            Base32 base32 = new Base32();
            byte[] decoded = base32.decode(secret);
            return decoded.length >= 16; // Minimum 128 bits
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a provisioning URI for QR code generation
     */
    public String generateProvisioningUri(String accountName, String secret, String issuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA256&digits=%d&period=%d",
            issuer != null ? issuer : ISSUER,
            accountName,
            secret,
            issuer != null ? issuer : ISSUER,
            CODE_LENGTH,
            TIME_STEP
        );
    }
}