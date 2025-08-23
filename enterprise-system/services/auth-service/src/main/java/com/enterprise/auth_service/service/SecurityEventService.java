package com.enterprise.auth_service.service;

import com.enterprise.auth_service.entity.SecurityEvent;
import com.enterprise.auth_service.repository.SecurityEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Service for managing security events and audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;
    private final ObjectMapper objectMapper;

    public void logLoginSuccess(String username, String clientId, String ipAddress, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("LOGIN_SUCCESS")
            .username(username)
            .clientId(clientId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .success(true)
            .severity("LOW")
            .riskScore(1)
            .build();
        
        securityEventRepository.save(event);
        log.info("Login success event logged for user: {}", username);
    }

    public void logLoginFailure(String username, String clientId, String ipAddress, String userAgent, 
                               String errorCode, String errorMessage) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("LOGIN_FAILURE")
            .username(username)
            .clientId(clientId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .severity("MEDIUM")
            .riskScore(calculateRiskScore(username, ipAddress))
            .build();
        
        securityEventRepository.save(event);
        log.warn("Login failure event logged for user: {} with error: {}", username, errorCode);
    }

    public void logTokenGeneration(String username, String clientId, String tokenType, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("TOKEN_GENERATED")
            .username(username)
            .clientId(clientId)
            .ipAddress(ipAddress)
            .success(true)
            .action(tokenType + "_generation")
            .severity("LOW")
            .riskScore(1)
            .build();
        
        securityEventRepository.save(event);
    }

    public void logTokenIntrospection(String clientId, String tokenType, String ipAddress, boolean successful) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("TOKEN_INTROSPECTION")
            .clientId(clientId)
            .ipAddress(ipAddress)
            .success(successful)
            .action(tokenType + "_introspection")
            .severity("LOW")
            .riskScore(1)
            .build();
        
        securityEventRepository.save(event);
    }

    public void logUnauthorizedAccess(String username, String clientId, String resource, String ipAddress, 
                                     String userAgent, String errorMessage) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("UNAUTHORIZED_ACCESS")
            .username(username)
            .clientId(clientId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .resource(resource)
            .success(false)
            .errorMessage(errorMessage)
            .severity("HIGH")
            .riskScore(calculateRiskScore(username, ipAddress) + 3)
            .build();
        
        securityEventRepository.save(event);
        log.warn("Unauthorized access attempt by user: {} to resource: {}", username, resource);
    }

    public void logSuspiciousActivity(String username, String activityType, String ipAddress, 
                                     String userAgent, Map<String, Object> additionalData) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                .eventType("SUSPICIOUS_ACTIVITY")
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .action(activityType)
                .severity("HIGH")
                .riskScore(calculateRiskScore(username, ipAddress) + 5)
                .additionalData(objectMapper.writeValueAsString(additionalData))
                .build();
            
            securityEventRepository.save(event);
            log.error("Suspicious activity detected: {} by user: {}", activityType, username);
        } catch (Exception e) {
            log.error("Failed to log suspicious activity", e);
        }
    }

    public void logBruteForceAttempt(String username, String ipAddress, long attemptCount) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("BRUTE_FORCE_ATTEMPT")
            .username(username)
            .ipAddress(ipAddress)
            .success(false)
            .errorMessage("Brute force attempt detected with " + attemptCount + " failed attempts")
            .severity("CRITICAL")
            .riskScore(8)
            .build();
        
        securityEventRepository.save(event);
        log.error("Brute force attempt detected for user: {} from IP: {} with {} attempts", 
            username, ipAddress, attemptCount);
    }

    public void logAccountLocked(String username, String ipAddress, String reason) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("ACCOUNT_LOCKED")
            .username(username)
            .ipAddress(ipAddress)
            .success(false)
            .errorMessage("Account locked: " + reason)
            .severity("HIGH")
            .riskScore(6)
            .build();
        
        securityEventRepository.save(event);
        log.warn("Account locked for user: {} due to: {}", username, reason);
    }

    public void logMfaEvent(String username, String mfaType, boolean successful, String ipAddress) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("MFA_" + (successful ? "SUCCESS" : "FAILURE"))
            .username(username)
            .ipAddress(ipAddress)
            .success(successful)
            .action(mfaType + "_authentication")
            .severity(successful ? "LOW" : "MEDIUM")
            .riskScore(successful ? 1 : 3)
            .build();
        
        securityEventRepository.save(event);
    }

    public void logRateLimitExceeded(String clientId, String ipAddress, String endpoint) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType("RATE_LIMIT_EXCEEDED")
            .clientId(clientId)
            .ipAddress(ipAddress)
            .resource(endpoint)
            .success(false)
            .errorMessage("Rate limit exceeded")
            .severity("MEDIUM")
            .riskScore(4)
            .build();
        
        securityEventRepository.save(event);
        log.warn("Rate limit exceeded for client: {} from IP: {} on endpoint: {}", clientId, ipAddress, endpoint);
    }

    public boolean isAccountUnderAttack(String username) {
        Instant since = Instant.now().minus(15, ChronoUnit.MINUTES);
        long failedAttempts = securityEventRepository.countFailedAttempts(username, "LOGIN_FAILURE", since);
        return failedAttempts >= 5;
    }

    public boolean isIpSuspicious(String ipAddress) {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long failedAttempts = securityEventRepository.countFailedAttemptsByIp(ipAddress, "LOGIN_FAILURE", since);
        return failedAttempts >= 10;
    }

    public void logSecurityEvent(String eventType, String username, String clientId, 
                                 String ipAddress, String userAgent, boolean success, 
                                 String resource, String errorCode) {
        SecurityEvent event = SecurityEvent.builder()
            .eventType(eventType)
            .username(username)
            .clientId(clientId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .success(success)
            .resource(resource)
            .errorCode(errorCode)
            .severity(success ? "LOW" : "MEDIUM")
            .riskScore(calculateRiskScore(username, ipAddress))
            .build();
        
        securityEventRepository.save(event);
        log.info("Security event logged: {} for user: {} success: {}", eventType, username, success);
    }

    private int calculateRiskScore(String username, String ipAddress) {
        int score = 1; // Base score
        
        // Check recent failed attempts for this user
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long userFailedAttempts = securityEventRepository.countFailedAttempts(username, "LOGIN_FAILURE", since);
        score += Math.min(userFailedAttempts / 2, 3);
        
        // Check recent failed attempts from this IP
        long ipFailedAttempts = securityEventRepository.countFailedAttemptsByIp(ipAddress, "LOGIN_FAILURE", since);
        score += Math.min(ipFailedAttempts / 5, 3);
        
        return Math.min(score, 10); // Cap at 10
    }
}