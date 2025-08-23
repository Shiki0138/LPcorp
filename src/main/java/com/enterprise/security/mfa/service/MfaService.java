package com.enterprise.security.mfa.service;

import com.enterprise.security.mfa.model.*;
import com.enterprise.security.mfa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MfaService {
    
    private static final Logger logger = LoggerFactory.getLogger(MfaService.class);
    
    @Autowired
    private MfaDeviceRepository deviceRepository;
    
    @Autowired
    private MfaChallengeRepository challengeRepository;
    
    @Autowired
    private TrustedDeviceRepository trustedDeviceRepository;
    
    @Autowired
    private TOTPService totpService;
    
    @Autowired
    private WebAuthnService webAuthnService;
    
    @Autowired
    private SMSService smsService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private BackupCodeService backupCodeService;
    
    @Value("${app.mfa.challenge-validity-minutes:5}")
    private int challengeValidityMinutes;
    
    @Value("${app.mfa.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${app.mfa.trusted-device-days:30}")
    private int trustedDeviceDays;
    
    @Value("${app.mfa.require-mfa:true}")
    private boolean requireMfa;
    
    /**
     * Checks if user has MFA enabled
     */
    public boolean isMfaEnabled(String userId) {
        return deviceRepository.hasVerifiedDevices(userId);
    }
    
    /**
     * Checks if user requires MFA for authentication
     */
    public boolean requiresMfa(String userId, String deviceFingerprint) {
        if (!requireMfa) {
            return false;
        }
        
        // Check if device is trusted
        if (deviceFingerprint != null && isTrustedDevice(userId, deviceFingerprint)) {
            return false;
        }
        
        // Check if user has MFA devices
        return isMfaEnabled(userId);
    }
    
    /**
     * Gets available MFA methods for user
     */
    public List<MfaDevice> getAvailableMfaMethods(String userId) {
        return deviceRepository.findVerifiedDevicesByUserId(userId);
    }
    
    /**
     * Gets recommended MFA method (most secure available)
     */
    public Optional<MfaDevice> getRecommendedMfaMethod(String userId) {
        List<MfaType> preferredOrder = Arrays.asList(
            MfaType.WEBAUTHN_PLATFORM,
            MfaType.WEBAUTHN_CROSS_PLATFORM,
            MfaType.TOTP,
            MfaType.SMS,
            MfaType.EMAIL
        );
        
        return deviceRepository.findPreferredDevicesByTypes(userId, preferredOrder)
            .stream()
            .findFirst();
    }
    
    /**
     * Initiates MFA challenge
     */
    public MfaChallenge initiateMfaChallenge(String userId, UUID deviceId, String sessionId, 
                                           String ipAddress, String userAgent) {
        Optional<MfaDevice> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty() || !deviceOpt.get().isActive() || !deviceOpt.get().isVerified()) {
            throw new IllegalArgumentException("Invalid or inactive MFA device");
        }
        
        MfaDevice device = deviceOpt.get();
        if (!device.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Device does not belong to user");
        }
        
        // Clean up existing challenges for this user/session
        challengeRepository.markChallengesAsUsed(userId, sessionId, LocalDateTime.now());
        
        // Create new challenge based on device type
        MfaChallenge challenge = createChallenge(device, sessionId, ipAddress, userAgent);
        challenge = challengeRepository.save(challenge);
        
        // Send challenge if needed (SMS/Email)
        if (device.getType() == MfaType.SMS) {
            sendSmsChallenge(device, challenge.getExpectedResponse(), ipAddress);
        } else if (device.getType() == MfaType.EMAIL) {
            sendEmailChallenge(device, challenge.getExpectedResponse(), ipAddress);
        }
        
        logger.info("MFA challenge initiated for user: {} with device type: {}", userId, device.getType());
        
        return challenge;
    }
    
    /**
     * Verifies MFA challenge response
     */
    public boolean verifyMfaChallenge(UUID challengeId, String response, String deviceFingerprint) {
        Optional<MfaChallenge> challengeOpt = challengeRepository.findByIdAndIsUsedFalse(challengeId);
        if (challengeOpt.isEmpty()) {
            return false;
        }
        
        MfaChallenge challenge = challengeOpt.get();
        if (!challenge.isValid()) {
            return false;
        }
        
        // Check attempt count
        if (challenge.getAttemptCount() >= maxAttempts) {
            return false;
        }
        
        challenge.incrementAttemptCount();
        
        Optional<MfaDevice> deviceOpt = deviceRepository.findById(challenge.getDeviceId());
        if (deviceOpt.isEmpty()) {
            return false;
        }
        
        MfaDevice device = deviceOpt.get();
        boolean verified = false;
        
        try {
            switch (device.getType()) {
                case TOTP:
                    verified = verifyTotpResponse(device, response);
                    break;
                case SMS:
                case EMAIL:
                    verified = challenge.getExpectedResponse().equals(response);
                    break;
                case BACKUP_CODES:
                    verified = backupCodeService.verifyAndUseBackupCode(
                        device.getUserId(), response, challenge.getIpAddress(), challenge.getUserAgent());
                    break;
                // WebAuthn is handled separately
                default:
                    verified = false;
            }
            
            if (verified) {
                challenge.markAsUsed();
                device.markAsUsed();
                
                // Save trusted device if requested
                if (deviceFingerprint != null && !deviceFingerprint.isEmpty()) {
                    saveTrustedDevice(device.getUserId(), deviceFingerprint, challenge.getIpAddress(), challenge.getUserAgent());
                }
                
                logger.info("MFA challenge verified successfully for user: {}", device.getUserId());
            } else {
                logger.warn("MFA challenge verification failed for user: {}", device.getUserId());
            }
            
        } catch (Exception e) {
            logger.error("Error verifying MFA challenge: {}", e.getMessage(), e);
            verified = false;
        } finally {
            challengeRepository.save(challenge);
            deviceRepository.save(device);
        }
        
        return verified;
    }
    
    /**
     * Verifies backup code
     */
    public boolean verifyBackupCode(String userId, String code, String ipAddress, String userAgent, 
                                  String deviceFingerprint) {
        boolean verified = backupCodeService.verifyAndUseBackupCode(userId, code, ipAddress, userAgent);
        
        if (verified) {
            // Save trusted device if requested
            if (deviceFingerprint != null && !deviceFingerprint.isEmpty()) {
                saveTrustedDevice(userId, deviceFingerprint, ipAddress, userAgent);
            }
            
            logger.info("Backup code verified successfully for user: {}", userId);
        }
        
        return verified;
    }
    
    /**
     * Gets MFA enrollment status
     */
    public MfaEnrollmentStatus getEnrollmentStatus(String userId) {
        List<MfaDevice> devices = deviceRepository.findByUserIdAndIsActiveTrue(userId);
        boolean hasBackupCodes = backupCodeService.hasUnusedCodes(userId);
        
        Map<MfaType, Long> deviceCounts = devices.stream()
            .filter(MfaDevice::isVerified)
            .collect(Collectors.groupingBy(MfaDevice::getType, Collectors.counting()));
        
        boolean hasStrongMfa = deviceCounts.containsKey(MfaType.TOTP) || 
                              deviceCounts.containsKey(MfaType.WEBAUTHN_PLATFORM) ||
                              deviceCounts.containsKey(MfaType.WEBAUTHN_CROSS_PLATFORM);
        
        return new MfaEnrollmentStatus(
            !devices.isEmpty(),
            hasStrongMfa,
            hasBackupCodes,
            deviceCounts,
            backupCodeService.getRemainingCodeCount(userId)
        );
    }
    
    /**
     * Removes MFA device
     */
    public boolean removeMfaDevice(String userId, UUID deviceId) {
        Optional<MfaDevice> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty() || !deviceOpt.get().getUserId().equals(userId)) {
            return false;
        }
        
        MfaDevice device = deviceOpt.get();
        device.setActive(false);
        deviceRepository.save(device);
        
        logger.info("MFA device removed for user: {} type: {}", userId, device.getType());
        
        return true;
    }
    
    /**
     * Checks if device is trusted
     */
    public boolean isTrustedDevice(String userId, String deviceFingerprint) {
        return trustedDeviceRepository
            .findValidTrustedDevice(userId, deviceFingerprint, LocalDateTime.now())
            .isPresent();
    }
    
    /**
     * Gets trusted devices for user
     */
    public List<TrustedDevice> getTrustedDevices(String userId) {
        return trustedDeviceRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    /**
     * Revokes trusted device
     */
    public boolean revokeTrustedDevice(String userId, UUID deviceId, String revokedBy) {
        Optional<TrustedDevice> deviceOpt = trustedDeviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty() || !deviceOpt.get().getUserId().equals(userId)) {
            return false;
        }
        
        TrustedDevice device = deviceOpt.get();
        device.revoke(revokedBy, "User revoked");
        trustedDeviceRepository.save(device);
        
        logger.info("Trusted device revoked for user: {}", userId);
        
        return true;
    }
    
    /**
     * Revokes all trusted devices for user
     */
    public void revokeAllTrustedDevices(String userId, String revokedBy) {
        trustedDeviceRepository.revokeAllUserDevices(userId, LocalDateTime.now(), revokedBy, "All devices revoked");
        logger.info("All trusted devices revoked for user: {}", userId);
    }
    
    /**
     * Cleanup expired challenges and trusted devices
     */
    public void cleanupExpiredData() {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up expired challenges
        challengeRepository.deleteExpiredChallenges(now.minusHours(24));
        
        // Deactivate expired trusted devices
        trustedDeviceRepository.deactivateExpiredDevices(now);
        
        // Clean up rate limiting data
        smsService.cleanupExpiredEntries();
        emailService.cleanupExpiredEntries();
        
        logger.info("Cleaned up expired MFA data");
    }
    
    // Private helper methods
    
    private MfaChallenge createChallenge(MfaDevice device, String sessionId, String ipAddress, String userAgent) {
        String challenge = null;
        String expectedResponse = null;
        
        switch (device.getType()) {
            case TOTP:
                challenge = "TOTP_CHALLENGE";
                // TOTP doesn't need expected response as it's time-based
                break;
            case SMS:
                challenge = "SMS_CHALLENGE";
                expectedResponse = smsService.generateCode();
                break;
            case EMAIL:
                challenge = "EMAIL_CHALLENGE";
                expectedResponse = emailService.generateCode();
                break;
            case BACKUP_CODES:
                challenge = "BACKUP_CODE_CHALLENGE";
                // Backup codes don't need expected response as they're pre-generated
                break;
            default:
                throw new IllegalArgumentException("Unsupported MFA type: " + device.getType());
        }
        
        MfaChallenge mfaChallenge = new MfaChallenge(
            device.getUserId(),
            device.getId(),
            device.getType(),
            challenge,
            challengeValidityMinutes
        );
        
        mfaChallenge.setExpectedResponse(expectedResponse);
        mfaChallenge.setSessionId(sessionId);
        mfaChallenge.setIpAddress(ipAddress);
        mfaChallenge.setUserAgent(userAgent);
        
        return mfaChallenge;
    }
    
    private void sendSmsChallenge(MfaDevice device, String code, String ipAddress) {
        if (device.getPhoneNumber() != null) {
            smsService.sendVerificationCode(device.getPhoneNumber(), code, ipAddress);
        }
    }
    
    private void sendEmailChallenge(MfaDevice device, String code, String ipAddress) {
        if (device.getEmail() != null) {
            emailService.sendVerificationCode(device.getEmail(), code, ipAddress);
        }
    }
    
    private boolean verifyTotpResponse(MfaDevice device, String response) {
        if (device.getEncryptedSecret() == null) {
            return false;
        }
        
        // Decrypt secret (implementation depends on encryption service)
        String secret = device.getEncryptedSecret(); // TODO: Decrypt
        
        return totpService.verifyCode(secret, response);
    }
    
    private void saveTrustedDevice(String userId, String deviceFingerprint, String ipAddress, String userAgent) {
        // Check if already exists
        Optional<TrustedDevice> existing = trustedDeviceRepository
            .findValidTrustedDevice(userId, deviceFingerprint, LocalDateTime.now());
        
        if (existing.isPresent()) {
            existing.get().markAsUsed();
            trustedDeviceRepository.save(existing.get());
        } else {
            TrustedDevice trustedDevice = new TrustedDevice(
                userId,
                deviceFingerprint,
                "Trusted Device",
                trustedDeviceDays
            );
            trustedDevice.setIpAddress(ipAddress);
            trustedDevice.setUserAgent(userAgent);
            trustedDeviceRepository.save(trustedDevice);
        }
    }
    
    /**
     * MFA enrollment status class
     */
    public static class MfaEnrollmentStatus {
        private final boolean hasMfaEnabled;
        private final boolean hasStrongMfa;
        private final boolean hasBackupCodes;
        private final Map<MfaType, Long> deviceCounts;
        private final long remainingBackupCodes;
        
        public MfaEnrollmentStatus(boolean hasMfaEnabled, boolean hasStrongMfa, boolean hasBackupCodes,
                                 Map<MfaType, Long> deviceCounts, long remainingBackupCodes) {
            this.hasMfaEnabled = hasMfaEnabled;
            this.hasStrongMfa = hasStrongMfa;
            this.hasBackupCodes = hasBackupCodes;
            this.deviceCounts = deviceCounts;
            this.remainingBackupCodes = remainingBackupCodes;
        }
        
        // Getters
        public boolean isHasMfaEnabled() { return hasMfaEnabled; }
        public boolean isHasStrongMfa() { return hasStrongMfa; }
        public boolean isHasBackupCodes() { return hasBackupCodes; }
        public Map<MfaType, Long> getDeviceCounts() { return deviceCounts; }
        public long getRemainingBackupCodes() { return remainingBackupCodes; }
        
        public boolean isFullySetup() {
            return hasMfaEnabled && hasStrongMfa && hasBackupCodes;
        }
        
        public List<String> getRecommendations() {
            List<String> recommendations = new ArrayList<>();
            
            if (!hasStrongMfa) {
                recommendations.add("Set up a strong MFA method (TOTP or WebAuthn)");
            }
            if (!hasBackupCodes) {
                recommendations.add("Generate backup codes for account recovery");
            }
            if (remainingBackupCodes > 0 && remainingBackupCodes <= 2) {
                recommendations.add("Generate new backup codes (running low)");
            }
            
            return recommendations;
        }
    }
}