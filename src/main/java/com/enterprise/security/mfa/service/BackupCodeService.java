package com.enterprise.security.mfa.service;

import com.enterprise.security.mfa.model.BackupCode;
import com.enterprise.security.mfa.repository.BackupCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BackupCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupCodeService.class);
    private static final int CODE_LENGTH = 8;
    private static final int DEFAULT_CODE_COUNT = 10;
    private static final int MIN_REMAINING_CODES = 2;
    
    @Autowired
    private BackupCodeRepository backupCodeRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generates new backup codes for a user
     */
    public List<String> generateBackupCodes(String userId) {
        return generateBackupCodes(userId, DEFAULT_CODE_COUNT);
    }
    
    /**
     * Generates specified number of backup codes for a user
     */
    public List<String> generateBackupCodes(String userId, int codeCount) {
        if (codeCount <= 0 || codeCount > 20) {
            throw new IllegalArgumentException("Code count must be between 1 and 20");
        }
        
        // Remove existing codes
        backupCodeRepository.deleteByUserId(userId);
        
        List<String> plainCodes = new ArrayList<>();
        List<BackupCode> encodedCodes = new ArrayList<>();
        
        for (int i = 0; i < codeCount; i++) {
            String plainCode = generateCode();
            String hashedCode = passwordEncoder.encode(plainCode);
            
            plainCodes.add(plainCode);
            encodedCodes.add(new BackupCode(userId, hashedCode));
        }
        
        // Save all codes
        backupCodeRepository.saveAll(encodedCodes);
        
        logger.info("Generated {} backup codes for user: {}", codeCount, userId);
        
        return plainCodes;
    }
    
    /**
     * Verifies a backup code and marks it as used
     */
    public boolean verifyAndUseBackupCode(String userId, String code, String ipAddress, String userAgent) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        String cleanCode = code.trim().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        // Get all unused codes for the user
        List<BackupCode> unusedCodes = backupCodeRepository.findByUserIdAndIsUsedFalse(userId);
        
        for (BackupCode backupCode : unusedCodes) {
            if (passwordEncoder.matches(cleanCode, backupCode.getHashedCode())) {
                // Mark as used
                backupCode.markAsUsed(ipAddress, userAgent);
                backupCodeRepository.save(backupCode);
                
                logger.info("Backup code used for user: {}", userId);
                
                // Check if running low on codes
                long remainingCodes = backupCodeRepository.countUnusedCodesByUserId(userId);
                if (remainingCodes <= MIN_REMAINING_CODES) {
                    logger.warn("User {} has only {} backup codes remaining", userId, remainingCodes);
                }
                
                return true;
            }
        }
        
        logger.warn("Invalid backup code attempt for user: {}", userId);
        return false;
    }
    
    /**
     * Checks if a backup code is valid without using it
     */
    public boolean isValidBackupCode(String userId, String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        String cleanCode = code.trim().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        List<BackupCode> unusedCodes = backupCodeRepository.findByUserIdAndIsUsedFalse(userId);
        
        return unusedCodes.stream()
            .anyMatch(backupCode -> passwordEncoder.matches(cleanCode, backupCode.getHashedCode()));
    }
    
    /**
     * Gets the count of remaining unused backup codes
     */
    public long getRemainingCodeCount(String userId) {
        return backupCodeRepository.countUnusedCodesByUserId(userId);
    }
    
    /**
     * Gets the count of used backup codes
     */
    public long getUsedCodeCount(String userId) {
        return backupCodeRepository.countUsedCodesByUserId(userId);
    }
    
    /**
     * Checks if user has any unused backup codes
     */
    public boolean hasUnusedCodes(String userId) {
        return backupCodeRepository.hasUnusedCodes(userId);
    }
    
    /**
     * Invalidates all backup codes for a user
     */
    public void invalidateAllCodes(String userId) {
        backupCodeRepository.deleteByUserId(userId);
        logger.info("Invalidated all backup codes for user: {}", userId);
    }
    
    /**
     * Regenerates backup codes (invalidates old ones and creates new ones)
     */
    public List<String> regenerateBackupCodes(String userId) {
        return regenerateBackupCodes(userId, DEFAULT_CODE_COUNT);
    }
    
    /**
     * Regenerates specified number of backup codes
     */
    public List<String> regenerateBackupCodes(String userId, int codeCount) {
        logger.info("Regenerating backup codes for user: {}", userId);
        return generateBackupCodes(userId, codeCount);
    }
    
    /**
     * Gets backup code usage statistics for a user
     */
    public BackupCodeStats getCodeStats(String userId) {
        long total = backupCodeRepository.countUnusedCodesByUserId(userId) + 
                    backupCodeRepository.countUsedCodesByUserId(userId);
        long used = backupCodeRepository.countUsedCodesByUserId(userId);
        long remaining = backupCodeRepository.countUnusedCodesByUserId(userId);
        
        return new BackupCodeStats(total, used, remaining);
    }
    
    /**
     * Validates backup code format
     */
    public boolean isValidCodeFormat(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        String cleanCode = code.trim().replaceAll("[^a-zA-Z0-9]", "");
        return cleanCode.length() == CODE_LENGTH && cleanCode.matches("[A-Z0-9]+");
    }
    
    /**
     * Formats a backup code for display (adds dashes)
     */
    public String formatCodeForDisplay(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return code;
        }
        
        // Format as XXXX-XXXX
        return code.substring(0, 4) + "-" + code.substring(4);
    }
    
    /**
     * Formats multiple backup codes for display
     */
    public List<String> formatCodesForDisplay(List<String> codes) {
        return codes.stream()
            .map(this::formatCodeForDisplay)
            .toList();
    }
    
    // Private helper methods
    
    private String generateCode() {
        // Use alphanumeric characters excluding confusing ones (0, O, 1, I, L)
        String chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(chars.length());
            code.append(chars.charAt(index));
        }
        
        return code.toString();
    }
    
    /**
     * Inner class for backup code statistics
     */
    public static class BackupCodeStats {
        private final long total;
        private final long used;
        private final long remaining;
        
        public BackupCodeStats(long total, long used, long remaining) {
            this.total = total;
            this.used = used;
            this.remaining = remaining;
        }
        
        public long getTotal() {
            return total;
        }
        
        public long getUsed() {
            return used;
        }
        
        public long getRemaining() {
            return remaining;
        }
        
        public double getUsagePercentage() {
            return total > 0 ? (double) used / total * 100 : 0;
        }
        
        public boolean isRunningLow() {
            return remaining <= MIN_REMAINING_CODES;
        }
        
        public boolean hasNoCodesRemaining() {
            return remaining == 0;
        }
    }
}