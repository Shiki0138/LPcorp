package com.enterprise.security.mfa.controller;

import com.enterprise.security.mfa.model.*;
import com.enterprise.security.mfa.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/mfa/auth")
public class MfaAuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(MfaAuthenticationController.class);
    
    @Autowired
    private MfaService mfaService;
    
    @Autowired
    private WebAuthnService webAuthnService;
    
    @Autowired
    private BackupCodeService backupCodeService;
    
    /**
     * Checks if MFA is required for user
     */
    @GetMapping("/required")
    public ResponseEntity<MfaRequiredResponse> isMfaRequired(
            @RequestParam String userId,
            @RequestParam(required = false) String deviceFingerprint) {
        
        boolean required = mfaService.requiresMfa(userId, deviceFingerprint);
        List<MfaDevice> availableMethods = mfaService.getAvailableMfaMethods(userId);
        Optional<MfaDevice> recommended = mfaService.getRecommendedMfaMethod(userId);
        
        MfaRequiredResponse response = new MfaRequiredResponse();
        response.setRequired(required);
        response.setAvailableMethods(availableMethods);
        response.setRecommendedMethod(recommended.orElse(null));
        response.setHasBackupCodes(backupCodeService.hasUnusedCodes(userId));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Initiates MFA challenge
     */
    @PostMapping("/challenge/initiate")
    public ResponseEntity<MfaChallengeResponse> initiateMfaChallenge(
            @RequestBody MfaChallengeRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String sessionId = httpRequest.getSession().getId();
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            MfaChallenge challenge = mfaService.initiateMfaChallenge(
                request.getUserId(),
                request.getDeviceId(),
                sessionId,
                ipAddress,
                userAgent
            );
            
            MfaChallengeResponse response = new MfaChallengeResponse();
            response.setChallengeId(challenge.getId());
            response.setType(challenge.getType());
            response.setExpiresAt(challenge.getExpiresAt());
            response.setRemainingMinutes(challenge.getRemainingMinutes());
            
            // For WebAuthn, include the challenge data
            if (challenge.getType().isWebAuthn()) {
                // WebAuthn challenge data would be included here
                response.setWebAuthnChallenge(challenge.getChallenge());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error initiating MFA challenge: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Verifies MFA challenge
     */
    @PostMapping("/challenge/verify")
    public ResponseEntity<MfaVerificationResponse> verifyMfaChallenge(
            @RequestBody MfaVerificationRequest request) {
        
        try {
            boolean verified = mfaService.verifyMfaChallenge(
                request.getChallengeId(),
                request.getResponse(),
                request.getDeviceFingerprint()
            );
            
            MfaVerificationResponse response = new MfaVerificationResponse();
            response.setVerified(verified);
            
            if (verified) {
                // Generate authentication token or update session
                response.setAuthenticationToken(generateAuthToken(request));
                response.setTrustedDevice(request.getRememberDevice());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying MFA challenge: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Verifies backup code
     */
    @PostMapping("/backup-code/verify")
    public ResponseEntity<MfaVerificationResponse> verifyBackupCode(
            @RequestBody BackupCodeVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean verified = mfaService.verifyBackupCode(
                request.getUserId(),
                request.getCode(),
                ipAddress,
                userAgent,
                request.getDeviceFingerprint()
            );
            
            MfaVerificationResponse response = new MfaVerificationResponse();
            response.setVerified(verified);
            
            if (verified) {
                response.setAuthenticationToken(generateBackupCodeAuthToken(request));
                response.setTrustedDevice(request.getRememberDevice());
                
                // Check remaining backup codes
                long remaining = backupCodeService.getRemainingCodeCount(request.getUserId());
                response.setRemainingBackupCodes(remaining);
                
                if (remaining <= 2) {
                    response.setWarning("Running low on backup codes. Generate new ones.");
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying backup code: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // WebAuthn specific endpoints
    
    /**
     * Starts WebAuthn authentication
     */
    @PostMapping("/webauthn/authenticate/start")
    public ResponseEntity<?> startWebAuthnAuthentication(
            @RequestParam String userId) {
        
        try {
            var requestOptions = webAuthnService.startAuthentication(userId);
            return ResponseEntity.ok(requestOptions);
            
        } catch (Exception e) {
            logger.error("Error starting WebAuthn authentication: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Finishes WebAuthn authentication
     */
    @PostMapping("/webauthn/authenticate/finish")
    public ResponseEntity<MfaVerificationResponse> finishWebAuthnAuthentication(
            @RequestParam String userId,
            @RequestBody Object credential) { // PublicKeyCredential in real implementation
        
        try {
            // var result = webAuthnService.finishAuthentication(userId, credential);
            
            MfaVerificationResponse response = new MfaVerificationResponse();
            // response.setVerified(result.isSuccess());
            response.setVerified(true); // Mock for now
            
            if (response.isVerified()) {
                response.setAuthenticationToken("webauthn-auth-token");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error finishing WebAuthn authentication: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gets trusted devices
     */
    @GetMapping("/trusted-devices")
    public ResponseEntity<List<TrustedDevice>> getTrustedDevices(
            @RequestParam String userId) {
        
        List<TrustedDevice> devices = mfaService.getTrustedDevices(userId);
        return ResponseEntity.ok(devices);
    }
    
    /**
     * Revokes trusted device
     */
    @DeleteMapping("/trusted-devices/{deviceId}")
    public ResponseEntity<Void> revokeTrustedDevice(
            @RequestParam String userId,
            @PathVariable UUID deviceId) {
        
        boolean revoked = mfaService.revokeTrustedDevice(userId, deviceId, "User requested");
        
        if (revoked) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Revokes all trusted devices
     */
    @DeleteMapping("/trusted-devices/all")
    public ResponseEntity<Void> revokeAllTrustedDevices(
            @RequestParam String userId) {
        
        mfaService.revokeAllTrustedDevices(userId, "User requested");
        return ResponseEntity.ok().build();
    }
    
    // Private helper methods
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private String generateAuthToken(MfaVerificationRequest request) {
        // Implementation depends on JWT/token service
        return "mfa-auth-token-" + UUID.randomUUID().toString();
    }
    
    private String generateBackupCodeAuthToken(BackupCodeVerificationRequest request) {
        // Implementation depends on JWT/token service
        return "backup-code-auth-token-" + UUID.randomUUID().toString();
    }
    
    // Request/Response DTOs
    
    public static class MfaRequiredResponse {
        private boolean required;
        private List<MfaDevice> availableMethods;
        private MfaDevice recommendedMethod;
        private boolean hasBackupCodes;
        
        // Getters and setters
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public List<MfaDevice> getAvailableMethods() { return availableMethods; }
        public void setAvailableMethods(List<MfaDevice> availableMethods) { this.availableMethods = availableMethods; }
        public MfaDevice getRecommendedMethod() { return recommendedMethod; }
        public void setRecommendedMethod(MfaDevice recommendedMethod) { this.recommendedMethod = recommendedMethod; }
        public boolean isHasBackupCodes() { return hasBackupCodes; }
        public void setHasBackupCodes(boolean hasBackupCodes) { this.hasBackupCodes = hasBackupCodes; }
    }
    
    public static class MfaChallengeRequest {
        private String userId;
        private UUID deviceId;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public UUID getDeviceId() { return deviceId; }
        public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
    }
    
    public static class MfaChallengeResponse {
        private UUID challengeId;
        private MfaType type;
        private java.time.LocalDateTime expiresAt;
        private int remainingMinutes;
        private String webAuthnChallenge;
        
        // Getters and setters
        public UUID getChallengeId() { return challengeId; }
        public void setChallengeId(UUID challengeId) { this.challengeId = challengeId; }
        public MfaType getType() { return type; }
        public void setType(MfaType type) { this.type = type; }
        public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public int getRemainingMinutes() { return remainingMinutes; }
        public void setRemainingMinutes(int remainingMinutes) { this.remainingMinutes = remainingMinutes; }
        public String getWebAuthnChallenge() { return webAuthnChallenge; }
        public void setWebAuthnChallenge(String webAuthnChallenge) { this.webAuthnChallenge = webAuthnChallenge; }
    }
    
    public static class MfaVerificationRequest {
        private UUID challengeId;
        private String response;
        private String deviceFingerprint;
        private boolean rememberDevice;
        
        // Getters and setters
        public UUID getChallengeId() { return challengeId; }
        public void setChallengeId(UUID challengeId) { this.challengeId = challengeId; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
        public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
        public boolean isRememberDevice() { return rememberDevice; }
        public void setRememberDevice(boolean rememberDevice) { this.rememberDevice = rememberDevice; }
    }
    
    public static class MfaVerificationResponse {
        private boolean verified;
        private String authenticationToken;
        private boolean trustedDevice;
        private long remainingBackupCodes;
        private String warning;
        
        // Getters and setters
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getAuthenticationToken() { return authenticationToken; }
        public void setAuthenticationToken(String authenticationToken) { this.authenticationToken = authenticationToken; }
        public boolean isTrustedDevice() { return trustedDevice; }
        public void setTrustedDevice(boolean trustedDevice) { this.trustedDevice = trustedDevice; }
        public long getRemainingBackupCodes() { return remainingBackupCodes; }
        public void setRemainingBackupCodes(long remainingBackupCodes) { this.remainingBackupCodes = remainingBackupCodes; }
        public String getWarning() { return warning; }
        public void setWarning(String warning) { this.warning = warning; }
    }
    
    public static class BackupCodeVerificationRequest {
        private String userId;
        private String code;
        private String deviceFingerprint;
        private boolean rememberDevice;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
        public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
        public boolean isRememberDevice() { return rememberDevice; }
        public void setRememberDevice(boolean rememberDevice) { this.rememberDevice = rememberDevice; }
    }
}