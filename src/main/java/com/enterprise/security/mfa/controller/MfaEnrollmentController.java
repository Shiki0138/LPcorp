package com.enterprise.security.mfa.controller;

import com.enterprise.security.mfa.model.*;
import com.enterprise.security.mfa.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mfa/enrollment")
public class MfaEnrollmentController {
    
    private static final Logger logger = LoggerFactory.getLogger(MfaEnrollmentController.class);
    
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
    
    /**
     * Gets MFA enrollment status
     */
    @GetMapping("/status")
    public ResponseEntity<MfaService.MfaEnrollmentStatus> getEnrollmentStatus(
            @AuthenticationPrincipal UserDetails user) {
        
        MfaService.MfaEnrollmentStatus status = mfaService.getEnrollmentStatus(user.getUsername());
        return ResponseEntity.ok(status);
    }
    
    /**
     * Gets available MFA methods
     */
    @GetMapping("/methods")
    public ResponseEntity<List<MfaDevice>> getAvailableMethods(
            @AuthenticationPrincipal UserDetails user) {
        
        List<MfaDevice> methods = mfaService.getAvailableMfaMethods(user.getUsername());
        return ResponseEntity.ok(methods);
    }
    
    // TOTP Enrollment
    
    /**
     * Starts TOTP enrollment
     */
    @PostMapping("/totp/start")
    public ResponseEntity<TOTPEnrollmentResponse> startTOTPEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody TOTPEnrollmentRequest request) {
        
        try {
            String secret = totpService.generateSecret();
            String qrUri = totpService.generateTOTPUri(user.getUsername(), secret);
            String qrCode = totpService.generateQRCodeImage(qrUri);
            
            // Store secret temporarily (should be encrypted in production)
            // Implementation depends on session/cache storage
            
            TOTPEnrollmentResponse response = new TOTPEnrollmentResponse();
            response.setSecret(secret);
            response.setQrCodeUri(qrUri);
            response.setQrCodeImage(qrCode);
            response.setDeviceName(request.getDeviceName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting TOTP enrollment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Completes TOTP enrollment
     */
    @PostMapping("/totp/complete")
    public ResponseEntity<MfaDevice> completeTOTPEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody TOTPVerificationRequest request) {
        
        try {
            // Verify TOTP code
            if (!totpService.verifyCode(request.getSecret(), request.getCode())) {
                return ResponseEntity.badRequest().build();
            }
            
            // Create and save device
            MfaDevice device = new MfaDevice(
                user.getUsername(),
                MfaType.TOTP,
                request.getDeviceName()
            );
            
            // Encrypt and store secret (implementation depends on encryption service)
            device.setEncryptedSecret(request.getSecret()); // TODO: Encrypt
            device.markAsVerified();
            
            // Save device (would use repository in real implementation)
            // deviceRepository.save(device);
            
            logger.info("TOTP enrollment completed for user: {}", user.getUsername());
            
            return ResponseEntity.ok(device);
            
        } catch (Exception e) {
            logger.error("Error completing TOTP enrollment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // SMS Enrollment
    
    /**
     * Starts SMS enrollment
     */
    @PostMapping("/sms/start")
    public ResponseEntity<SMSEnrollmentResponse> startSMSEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody SMSEnrollmentRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String phoneNumber = smsService.normalizePhoneNumber(request.getPhoneNumber());
            
            if (!smsService.isValidPhoneNumber(phoneNumber)) {
                return ResponseEntity.badRequest().build();
            }
            
            String code = smsService.generateCode();
            String ipAddress = getClientIpAddress(httpRequest);
            
            boolean sent = smsService.sendVerificationCode(phoneNumber, code, ipAddress);
            
            if (!sent) {
                return ResponseEntity.badRequest().build();
            }
            
            // Store code temporarily for verification
            // Implementation depends on session/cache storage
            
            SMSEnrollmentResponse response = new SMSEnrollmentResponse();
            response.setPhoneNumber(phoneNumber);
            response.setCodeSent(true);
            response.setRemainingAttempts(smsService.getRemainingAttempts(phoneNumber));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting SMS enrollment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Completes SMS enrollment
     */
    @PostMapping("/sms/complete")
    public ResponseEntity<MfaDevice> completeSMSEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody SMSVerificationRequest request) {
        
        try {
            // Verify SMS code (implementation depends on how codes are stored)
            // if (!verifySMSCode(request.getPhoneNumber(), request.getCode())) {
            //     return ResponseEntity.badRequest().build();
            // }
            
            // Create and save device
            MfaDevice device = new MfaDevice(
                user.getUsername(),
                MfaType.SMS,
                request.getDeviceName()
            );
            
            device.setPhoneNumber(request.getPhoneNumber());
            device.markAsVerified();
            
            // Save device (would use repository in real implementation)
            // deviceRepository.save(device);
            
            logger.info("SMS enrollment completed for user: {}", user.getUsername());
            
            return ResponseEntity.ok(device);
            
        } catch (Exception e) {
            logger.error("Error completing SMS enrollment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Email Enrollment
    
    /**
     * Starts Email enrollment
     */
    @PostMapping("/email/start")
    public ResponseEntity<EmailEnrollmentResponse> startEmailEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody EmailEnrollmentRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String email = emailService.normalizeEmail(request.getEmail());
            
            if (!emailService.isValidEmail(email)) {
                return ResponseEntity.badRequest().build();
            }
            
            String code = emailService.generateCode();
            String ipAddress = getClientIpAddress(httpRequest);
            
            boolean sent = emailService.sendVerificationCode(email, code, ipAddress);
            
            if (!sent) {
                return ResponseEntity.badRequest().build();
            }
            
            // Store code temporarily for verification
            // Implementation depends on session/cache storage
            
            EmailEnrollmentResponse response = new EmailEnrollmentResponse();
            response.setEmail(email);
            response.setCodeSent(true);
            response.setRemainingAttempts(emailService.getRemainingAttempts(email));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting email enrollment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Completes Email enrollment
     */
    @PostMapping("/email/complete")
    public ResponseEntity<MfaDevice> completeEmailEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody EmailVerificationRequest request) {
        
        try {
            // Verify email code (implementation depends on how codes are stored)
            // if (!verifyEmailCode(request.getEmail(), request.getCode())) {
            //     return ResponseEntity.badRequest().build();
            // }
            
            // Create and save device
            MfaDevice device = new MfaDevice(
                user.getUsername(),
                MfaType.EMAIL,
                request.getDeviceName()
            );
            
            device.setEmail(request.getEmail());
            device.markAsVerified();
            
            // Save device (would use repository in real implementation)
            // deviceRepository.save(device);
            
            logger.info("Email enrollment completed for user: {}", user.getUsername());
            
            return ResponseEntity.ok(device);
            
        } catch (Exception e) {
            logger.error("Error completing email enrollment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Backup Codes
    
    /**
     * Generates backup codes
     */
    @PostMapping("/backup-codes/generate")
    public ResponseEntity<BackupCodesResponse> generateBackupCodes(
            @AuthenticationPrincipal UserDetails user) {
        
        try {
            List<String> codes = backupCodeService.generateBackupCodes(user.getUsername());
            List<String> formattedCodes = backupCodeService.formatCodesForDisplay(codes);
            
            BackupCodesResponse response = new BackupCodesResponse();
            response.setCodes(formattedCodes);
            response.setCount(codes.size());
            response.setGenerated(true);
            
            logger.info("Backup codes generated for user: {}", user.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating backup codes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gets backup code statistics
     */
    @GetMapping("/backup-codes/stats")
    public ResponseEntity<BackupCodeService.BackupCodeStats> getBackupCodeStats(
            @AuthenticationPrincipal UserDetails user) {
        
        BackupCodeService.BackupCodeStats stats = backupCodeService.getCodeStats(user.getUsername());
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Removes MFA device
     */
    @DeleteMapping("/device/{deviceId}")
    public ResponseEntity<Void> removeMfaDevice(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID deviceId) {
        
        boolean removed = mfaService.removeMfaDevice(user.getUsername(), deviceId);
        
        if (removed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Utility methods
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    // Request/Response DTOs
    
    public static class TOTPEnrollmentRequest {
        private String deviceName;
        
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class TOTPEnrollmentResponse {
        private String secret;
        private String qrCodeUri;
        private String qrCodeImage;
        private String deviceName;
        
        // Getters and setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getQrCodeUri() { return qrCodeUri; }
        public void setQrCodeUri(String qrCodeUri) { this.qrCodeUri = qrCodeUri; }
        public String getQrCodeImage() { return qrCodeImage; }
        public void setQrCodeImage(String qrCodeImage) { this.qrCodeImage = qrCodeImage; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class TOTPVerificationRequest {
        private String secret;
        private String code;
        private String deviceName;
        
        // Getters and setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class SMSEnrollmentRequest {
        private String phoneNumber;
        private String deviceName;
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class SMSEnrollmentResponse {
        private String phoneNumber;
        private boolean codeSent;
        private int remainingAttempts;
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public boolean isCodeSent() { return codeSent; }
        public void setCodeSent(boolean codeSent) { this.codeSent = codeSent; }
        public int getRemainingAttempts() { return remainingAttempts; }
        public void setRemainingAttempts(int remainingAttempts) { this.remainingAttempts = remainingAttempts; }
    }
    
    public static class SMSVerificationRequest {
        private String phoneNumber;
        private String code;
        private String deviceName;
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class EmailEnrollmentRequest {
        private String email;
        private String deviceName;
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class EmailEnrollmentResponse {
        private String email;
        private boolean codeSent;
        private int remainingAttempts;
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isCodeSent() { return codeSent; }
        public void setCodeSent(boolean codeSent) { this.codeSent = codeSent; }
        public int getRemainingAttempts() { return remainingAttempts; }
        public void setRemainingAttempts(int remainingAttempts) { this.remainingAttempts = remainingAttempts; }
    }
    
    public static class EmailVerificationRequest {
        private String email;
        private String code;
        private String deviceName;
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
    
    public static class BackupCodesResponse {
        private List<String> codes;
        private int count;
        private boolean generated;
        
        // Getters and setters
        public List<String> getCodes() { return codes; }
        public void setCodes(List<String> codes) { this.codes = codes; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public boolean isGenerated() { return generated; }
        public void setGenerated(boolean generated) { this.generated = generated; }
    }
}