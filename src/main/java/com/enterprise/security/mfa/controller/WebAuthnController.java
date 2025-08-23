package com.enterprise.security.mfa.controller;

import com.enterprise.security.mfa.service.WebAuthnService;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/mfa/webauthn")
public class WebAuthnController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebAuthnController.class);
    
    @Autowired
    private WebAuthnService webAuthnService;
    
    /**
     * Starts WebAuthn registration for platform authenticators (biometrics)
     */
    @PostMapping("/register/platform/start")
    public ResponseEntity<PublicKeyCredentialCreationOptions> startPlatformRegistration(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody WebAuthnRegistrationRequest request) {
        
        try {
            PublicKeyCredentialCreationOptions options = webAuthnService.startRegistration(
                user.getUsername(),
                user.getUsername(),
                request.getDisplayName(),
                true // requireResidentKey for platform authenticators
            );
            
            logger.info("Started WebAuthn platform registration for user: {}", user.getUsername());
            
            return ResponseEntity.ok(options);
            
        } catch (Exception e) {
            logger.error("Error starting WebAuthn platform registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Starts WebAuthn registration for cross-platform authenticators (security keys)
     */
    @PostMapping("/register/cross-platform/start")
    public ResponseEntity<PublicKeyCredentialCreationOptions> startCrossPlatformRegistration(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody WebAuthnRegistrationRequest request) {
        
        try {
            PublicKeyCredentialCreationOptions options = webAuthnService.startRegistration(
                user.getUsername(),
                user.getUsername(),
                request.getDisplayName(),
                false // don't require resident key for cross-platform
            );
            
            logger.info("Started WebAuthn cross-platform registration for user: {}", user.getUsername());
            
            return ResponseEntity.ok(options);
            
        } catch (Exception e) {
            logger.error("Error starting WebAuthn cross-platform registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Finishes WebAuthn registration
     */
    @PostMapping("/register/finish")
    public ResponseEntity<WebAuthnRegistrationResponse> finishRegistration(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody WebAuthnRegistrationFinishRequest request) {
        
        try {
            var registrationResult = webAuthnService.finishRegistration(
                user.getUsername(),
                request.getDeviceName(),
                request.getCredential()
            );
            
            WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse();
            response.setSuccess(registrationResult.isSuccess());
            response.setCredentialId(request.getCredential().getId().getBase64Url());
            response.setDeviceName(request.getDeviceName());
            
            if (registrationResult.isSuccess()) {
                response.setAttestationTrusted(registrationResult.isAttestationTrusted());
                response.setSignatureCount(registrationResult.getSignatureCount());
                
                logger.info("WebAuthn registration completed successfully for user: {}", user.getUsername());
            } else {
                logger.warn("WebAuthn registration failed for user: {}", user.getUsername());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (RegistrationFailedException e) {
            logger.error("WebAuthn registration failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error finishing WebAuthn registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Starts WebAuthn authentication
     */
    @PostMapping("/authenticate/start")
    public ResponseEntity<PublicKeyCredentialRequestOptions> startAuthentication(
            @RequestParam String userId) {
        
        try {
            PublicKeyCredentialRequestOptions options = webAuthnService.startAuthentication(userId);
            
            logger.info("Started WebAuthn authentication for user: {}", userId);
            
            return ResponseEntity.ok(options);
            
        } catch (Exception e) {
            logger.error("Error starting WebAuthn authentication: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Finishes WebAuthn authentication
     */
    @PostMapping("/authenticate/finish")
    public ResponseEntity<WebAuthnAuthenticationResponse> finishAuthentication(
            @RequestParam String userId,
            @RequestBody WebAuthnAuthenticationFinishRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            var assertionResult = webAuthnService.finishAuthentication(userId, request.getCredential());
            
            WebAuthnAuthenticationResponse response = new WebAuthnAuthenticationResponse();
            response.setSuccess(assertionResult.isSuccess());
            response.setCredentialId(request.getCredential().getId().getBase64Url());
            
            if (assertionResult.isSuccess()) {
                response.setSignatureCount(assertionResult.getSignatureCount());
                response.setUserVerified(assertionResult.isUserVerified());
                response.setAuthenticationToken(generateAuthToken(userId));
                
                // Handle trusted device if requested
                if (request.isRememberDevice() && request.getDeviceFingerprint() != null) {
                    response.setTrustedDevice(true);
                }
                
                logger.info("WebAuthn authentication completed successfully for user: {}", userId);
            } else {
                logger.warn("WebAuthn authentication failed for user: {}", userId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (AssertionFailedException e) {
            logger.error("WebAuthn authentication failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createAuthErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error finishing WebAuthn authentication: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gets user's WebAuthn devices
     */
    @GetMapping("/devices")
    public ResponseEntity<?> getUserWebAuthnDevices(
            @AuthenticationPrincipal UserDetails user) {
        
        try {
            var devices = webAuthnService.getUserWebAuthnDevices(user.getUsername());
            return ResponseEntity.ok(devices);
            
        } catch (Exception e) {
            logger.error("Error getting WebAuthn devices: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Removes a WebAuthn device
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Void> removeWebAuthnDevice(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String deviceId) {
        
        try {
            boolean removed = webAuthnService.removeDevice(user.getUsername(), deviceId);
            
            if (removed) {
                logger.info("WebAuthn device removed for user: {}", user.getUsername());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error removing WebAuthn device: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Checks WebAuthn support in browser
     */
    @GetMapping("/support")
    public ResponseEntity<WebAuthnSupportResponse> checkWebAuthnSupport(
            HttpServletRequest request) {
        
        String userAgent = request.getHeader("User-Agent");
        
        WebAuthnSupportResponse response = new WebAuthnSupportResponse();
        response.setSupported(isWebAuthnSupported(userAgent));
        response.setPlatformAuthenticatorSupported(isPlatformAuthenticatorSupported(userAgent));
        response.setCrossPlatformAuthenticatorSupported(true); // Generally supported
        response.setUserAgent(userAgent);
        
        return ResponseEntity.ok(response);
    }
    
    // Private helper methods
    
    private String generateAuthToken(String userId) {
        // Implementation depends on JWT/token service
        return "webauthn-auth-token-" + java.util.UUID.randomUUID().toString();
    }
    
    private boolean isWebAuthnSupported(String userAgent) {
        // Basic user agent detection for WebAuthn support
        if (userAgent == null) return false;
        
        return userAgent.contains("Chrome/") ||
               userAgent.contains("Firefox/") ||
               userAgent.contains("Safari/") ||
               userAgent.contains("Edge/");
    }
    
    private boolean isPlatformAuthenticatorSupported(String userAgent) {
        // Platform authenticator support detection
        if (userAgent == null) return false;
        
        return (userAgent.contains("Chrome/") && !userAgent.contains("Mobile")) ||
               (userAgent.contains("Safari/") && userAgent.contains("Mac")) ||
               userAgent.contains("Edge/");
    }
    
    private WebAuthnRegistrationResponse createErrorResponse(String message) {
        WebAuthnRegistrationResponse response = new WebAuthnRegistrationResponse();
        response.setSuccess(false);
        response.setErrorMessage(message);
        return response;
    }
    
    private WebAuthnAuthenticationResponse createAuthErrorResponse(String message) {
        WebAuthnAuthenticationResponse response = new WebAuthnAuthenticationResponse();
        response.setSuccess(false);
        response.setErrorMessage(message);
        return response;
    }
    
    // Request/Response DTOs
    
    public static class WebAuthnRegistrationRequest {
        private String displayName;
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
    
    public static class WebAuthnRegistrationFinishRequest {
        private String deviceName;
        private PublicKeyCredential<AuthenticatorAttestationResponse> credential;
        
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public PublicKeyCredential<AuthenticatorAttestationResponse> getCredential() { return credential; }
        public void setCredential(PublicKeyCredential<AuthenticatorAttestationResponse> credential) { this.credential = credential; }
    }
    
    public static class WebAuthnRegistrationResponse {
        private boolean success;
        private String credentialId;
        private String deviceName;
        private boolean attestationTrusted;
        private long signatureCount;
        private String errorMessage;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getCredentialId() { return credentialId; }
        public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public boolean isAttestationTrusted() { return attestationTrusted; }
        public void setAttestationTrusted(boolean attestationTrusted) { this.attestationTrusted = attestationTrusted; }
        public long getSignatureCount() { return signatureCount; }
        public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class WebAuthnAuthenticationFinishRequest {
        private PublicKeyCredential<AuthenticatorAssertionResponse> credential;
        private boolean rememberDevice;
        private String deviceFingerprint;
        
        public PublicKeyCredential<AuthenticatorAssertionResponse> getCredential() { return credential; }
        public void setCredential(PublicKeyCredential<AuthenticatorAssertionResponse> credential) { this.credential = credential; }
        public boolean isRememberDevice() { return rememberDevice; }
        public void setRememberDevice(boolean rememberDevice) { this.rememberDevice = rememberDevice; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
        public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    }
    
    public static class WebAuthnAuthenticationResponse {
        private boolean success;
        private String credentialId;
        private long signatureCount;
        private boolean userVerified;
        private String authenticationToken;
        private boolean trustedDevice;
        private String errorMessage;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getCredentialId() { return credentialId; }
        public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
        public long getSignatureCount() { return signatureCount; }
        public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }
        public boolean isUserVerified() { return userVerified; }
        public void setUserVerified(boolean userVerified) { this.userVerified = userVerified; }
        public String getAuthenticationToken() { return authenticationToken; }
        public void setAuthenticationToken(String authenticationToken) { this.authenticationToken = authenticationToken; }
        public boolean isTrustedDevice() { return trustedDevice; }
        public void setTrustedDevice(boolean trustedDevice) { this.trustedDevice = trustedDevice; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class WebAuthnSupportResponse {
        private boolean supported;
        private boolean platformAuthenticatorSupported;
        private boolean crossPlatformAuthenticatorSupported;
        private String userAgent;
        
        // Getters and setters
        public boolean isSupported() { return supported; }
        public void setSupported(boolean supported) { this.supported = supported; }
        public boolean isPlatformAuthenticatorSupported() { return platformAuthenticatorSupported; }
        public void setPlatformAuthenticatorSupported(boolean platformAuthenticatorSupported) { this.platformAuthenticatorSupported = platformAuthenticatorSupported; }
        public boolean isCrossPlatformAuthenticatorSupported() { return crossPlatformAuthenticatorSupported; }
        public void setCrossPlatformAuthenticatorSupported(boolean crossPlatformAuthenticatorSupported) { this.crossPlatformAuthenticatorSupported = crossPlatformAuthenticatorSupported; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }
}