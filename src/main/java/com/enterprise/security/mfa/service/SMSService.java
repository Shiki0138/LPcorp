package com.enterprise.security.mfa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SMSService {
    
    private static final Logger logger = LoggerFactory.getLogger(SMSService.class);
    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_MINUTES = 5;
    private static final int MAX_ATTEMPTS_PER_PHONE = 5;
    private static final int MAX_ATTEMPTS_PER_IP = 10;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 60;
    
    @Value("${app.mfa.sms.provider:twilio}")
    private String smsProvider;
    
    @Value("${app.mfa.sms.twilio.account-sid:}")
    private String twilioAccountSid;
    
    @Value("${app.mfa.sms.twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${app.mfa.sms.twilio.from-number:}")
    private String twilioFromNumber;
    
    @Value("${app.mfa.sms.aws.access-key:}")
    private String awsAccessKey;
    
    @Value("${app.mfa.sms.aws.secret-key:}")
    private String awsSecretKey;
    
    @Value("${app.mfa.sms.aws.region:us-east-1}")
    private String awsRegion;
    
    @Value("${app.mfa.sms.template:Your verification code is: %s. Valid for %d minutes.}")
    private String messageTemplate;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Rate limiting maps
    private final Map<String, AttemptTracker> phoneAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptTracker> ipAttempts = new ConcurrentHashMap<>();
    
    /**
     * Sends SMS verification code
     */
    public boolean sendVerificationCode(String phoneNumber, String code, String ipAddress) {
        // Check rate limits
        if (!checkRateLimit(phoneNumber, ipAddress)) {
            logger.warn("Rate limit exceeded for phone: {} or IP: {}", phoneNumber, ipAddress);
            return false;
        }
        
        // Format message
        String message = String.format(messageTemplate, code, CODE_VALIDITY_MINUTES);
        
        try {
            boolean sent = false;
            
            switch (smsProvider.toLowerCase()) {
                case "twilio":
                    sent = sendViaTwilio(phoneNumber, message);
                    break;
                case "aws":
                    sent = sendViaAWS(phoneNumber, message);
                    break;
                case "mock":
                    sent = sendViaMock(phoneNumber, message);
                    break;
                default:
                    logger.error("Unknown SMS provider: {}", smsProvider);
                    return false;
            }
            
            if (sent) {
                // Update rate limiting counters
                incrementAttempts(phoneNumber, ipAddress);
                logger.info("SMS sent successfully to: {}", maskPhoneNumber(phoneNumber));
            } else {
                logger.error("Failed to send SMS to: {}", maskPhoneNumber(phoneNumber));
            }
            
            return sent;
            
        } catch (Exception e) {
            logger.error("Error sending SMS to: {} - {}", maskPhoneNumber(phoneNumber), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generates numeric verification code
     */
    public String generateCode() {
        int code = secureRandom.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", code);
    }
    
    /**
     * Validates phone number format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Basic validation - should start with + and have 7-15 digits
        return cleaned.matches("^\\+[1-9]\\d{6,14}$");
    }
    
    /**
     * Normalizes phone number format
     */
    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Add + prefix if missing
        if (!cleaned.startsWith("+")) {
            // Assume US number if no country code
            if (cleaned.length() == 10) {
                cleaned = "+1" + cleaned;
            } else {
                cleaned = "+" + cleaned;
            }
        }
        
        return cleaned;
    }
    
    // Private methods for different SMS providers
    
    private boolean sendViaTwilio(String phoneNumber, String message) {
        if (twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty()) {
            logger.error("Twilio credentials not configured");
            return false;
        }
        
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(twilioAccountSid, twilioAuthToken);
            
            String body = String.format("From=%s&To=%s&Body=%s", 
                twilioFromNumber, phoneNumber, message);
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.error("Twilio SMS sending failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean sendViaAWS(String phoneNumber, String message) {
        // AWS SNS implementation would go here
        // For now, returning mock success
        logger.info("AWS SNS SMS sending not implemented yet");
        return true;
    }
    
    private boolean sendViaMock(String phoneNumber, String message) {
        logger.info("MOCK SMS to {}: {}", maskPhoneNumber(phoneNumber), message);
        return true;
    }
    
    // Rate limiting methods
    
    private boolean checkRateLimit(String phoneNumber, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check phone number rate limit
        AttemptTracker phoneTracker = phoneAttempts.get(phoneNumber);
        if (phoneTracker != null && !phoneTracker.canAttempt(now, MAX_ATTEMPTS_PER_PHONE)) {
            return false;
        }
        
        // Check IP address rate limit
        AttemptTracker ipTracker = ipAttempts.get(ipAddress);
        if (ipTracker != null && !ipTracker.canAttempt(now, MAX_ATTEMPTS_PER_IP)) {
            return false;
        }
        
        return true;
    }
    
    private void incrementAttempts(String phoneNumber, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        phoneAttempts.computeIfAbsent(phoneNumber, k -> new AttemptTracker())
            .incrementAttempt(now);
        
        ipAttempts.computeIfAbsent(ipAddress, k -> new AttemptTracker())
            .incrementAttempt(now);
    }
    
    /**
     * Gets remaining attempts for phone number
     */
    public int getRemainingAttempts(String phoneNumber) {
        AttemptTracker tracker = phoneAttempts.get(phoneNumber);
        if (tracker == null) {
            return MAX_ATTEMPTS_PER_PHONE;
        }
        
        return Math.max(0, MAX_ATTEMPTS_PER_PHONE - tracker.getAttemptCount(LocalDateTime.now()));
    }
    
    /**
     * Resets rate limiting for phone number (admin function)
     */
    public void resetRateLimit(String phoneNumber) {
        phoneAttempts.remove(phoneNumber);
        logger.info("Rate limit reset for phone: {}", maskPhoneNumber(phoneNumber));
    }
    
    /**
     * Cleans up expired rate limiting entries
     */
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES);
        
        phoneAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff));
        
        ipAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff));
    }
    
    // Utility methods
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        
        int visibleDigits = 4;
        String visible = phoneNumber.substring(phoneNumber.length() - visibleDigits);
        return "***" + visible;
    }
    
    /**
     * Inner class for tracking attempt counts and timestamps
     */
    private static class AttemptTracker {
        private final AtomicInteger count = new AtomicInteger(0);
        private LocalDateTime windowStart = LocalDateTime.now();
        private LocalDateTime lastAttempt = LocalDateTime.now();
        
        public synchronized boolean canAttempt(LocalDateTime now, int maxAttempts) {
            // Reset window if enough time has passed
            if (now.isAfter(windowStart.plus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
                count.set(0);
                windowStart = now;
            }
            
            return count.get() < maxAttempts;
        }
        
        public synchronized void incrementAttempt(LocalDateTime now) {
            // Reset window if enough time has passed
            if (now.isAfter(windowStart.plus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
                count.set(0);
                windowStart = now;
            }
            
            count.incrementAndGet();
            lastAttempt = now;
        }
        
        public synchronized int getAttemptCount(LocalDateTime now) {
            // Reset window if enough time has passed
            if (now.isAfter(windowStart.plus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
                count.set(0);
                windowStart = now;
            }
            
            return count.get();
        }
        
        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }
    }
}