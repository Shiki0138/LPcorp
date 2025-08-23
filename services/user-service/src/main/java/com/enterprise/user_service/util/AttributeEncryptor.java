package com.enterprise.user_service.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter for encrypting/decrypting PII data
 * Uses AES-256-GCM for encryption with secure random initialization vectors
 */
@Converter
@Component
@Slf4j
public class AttributeEncryptor implements AttributeConverter<String, String> {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    
    @Value("${user-service.encryption.key:}")
    private String encryptionKey;
    
    @Value("${user-service.encryption.enabled:true}")
    private boolean encryptionEnabled;
    
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
                log.warn("No encryption key provided, generating temporary key. This should not be used in production!");
                try {
                    KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                    keyGenerator.init(256);
                    secretKey = keyGenerator.generateKey();
                } catch (Exception e) {
                    log.error("Failed to generate encryption key", e);
                    throw new RuntimeException("Failed to generate encryption key", e);
                }
            } else {
                // In production, this should come from a secure key management service
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            }
        }
        return secretKey;
    }
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!encryptionEnabled || attribute == null || attribute.trim().isEmpty()) {
            return attribute;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Generate random IV for each encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            log.error("Failed to encrypt attribute", e);
            // In production, you might want to handle this differently
            // For now, we'll throw an exception to prevent storing unencrypted data
            throw new RuntimeException("Failed to encrypt sensitive data", e);
        }
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!encryptionEnabled || dbData == null || dbData.trim().isEmpty()) {
            return dbData;
        }
        
        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(dbData);
            
            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                log.warn("Invalid encrypted data length");
                return dbData; // Return as-is if not properly encrypted (backward compatibility)
            }
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Failed to decrypt attribute", e);
            // In production, you might want to handle this more gracefully
            // For now, return the original data (might be unencrypted legacy data)
            return dbData;
        }
    }
    
    /**
     * Utility method to hash sensitive data for search purposes
     * Uses SHA-256 with salt for secure hashing
     */
    public static String hashForSearch(String value, String salt) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(value.toLowerCase().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to hash value for search", e);
            throw new RuntimeException("Failed to hash value", e);
        }
    }
    
    /**
     * Utility method to mask sensitive data for logging/display
     */
    public static String maskSensitiveData(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        }
        
        // Show first 2 and last 2 characters, mask the rest
        return value.substring(0, 2) + "*".repeat(value.length() - 4) + value.substring(value.length() - 2);
    }
    
    /**
     * Utility method to mask email addresses
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
    
    /**
     * Utility method to mask phone numbers
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        
        // Remove all non-digit characters for masking
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        
        if (digitsOnly.length() <= 4) {
            return "*".repeat(phoneNumber.length());
        }
        
        // Show last 4 digits
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        String masked = phoneNumber.replaceAll("[0-9]", "*");
        
        // Replace last 4 asterisks with actual digits
        for (int i = 0; i < 4; i++) {
            int lastAsterisk = masked.lastIndexOf("*");
            if (lastAsterisk != -1) {
                masked = masked.substring(0, lastAsterisk) + 
                        lastFour.charAt(3 - i) + 
                        masked.substring(lastAsterisk + 1);
            }
        }
        
        return masked;
    }
}