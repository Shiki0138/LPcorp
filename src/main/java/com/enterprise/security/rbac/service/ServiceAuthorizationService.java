package com.enterprise.security.rbac.service;

import com.enterprise.security.authorization.engine.RbacAuthorizationEngine;
import com.enterprise.security.authorization.engine.AuthorizationRequest;
import com.enterprise.security.authorization.engine.AuthorizationResult;
import com.enterprise.security.rbac.model.ServiceAccount;
import com.enterprise.security.rbac.repository.ServiceAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Service for handling service-to-service authorization
 */
@Service
@Transactional
public class ServiceAuthorizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceAuthorizationService.class);
    
    @Autowired
    private RbacAuthorizationEngine authorizationEngine;
    
    @Autowired
    private ServiceAccountRepository serviceAccountRepository;
    
    @Autowired
    private RbacUserService userService;
    
    /**
     * Authorize service-to-service request
     */
    public AuthorizationResult authorizeServiceRequest(String serviceId, String targetService, 
                                                       String operation, Map<String, Object> context) {
        try {
            logger.debug("Authorizing service request: {} -> {} for operation: {}", 
                        serviceId, targetService, operation);
            
            // Get service account
            Optional<ServiceAccount> serviceAccountOpt = serviceAccountRepository.findById(serviceId);
            if (serviceAccountOpt.isEmpty()) {
                return AuthorizationResult.denied("Service account not found: " + serviceId);
            }
            
            ServiceAccount serviceAccount = serviceAccountOpt.get();
            
            // Check if service account is active
            if (!serviceAccount.isActive()) {
                return AuthorizationResult.denied("Service account is not active: " + serviceId);
            }
            
            // Check if service account is expired
            if (serviceAccount.getExpiresAt() != null && serviceAccount.getExpiresAt().isBefore(Instant.now())) {
                return AuthorizationResult.denied("Service account has expired: " + serviceId);
            }
            
            // Create authorization request for the service user
            AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .userId(serviceAccount.getUserId())
                .action(operation)
                .resourceId(targetService)
                .resourceType("service")
                .context("service_call", true)
                .context("source_service", serviceId)
                .context("target_service", targetService)
                .build();
            
            // Add any additional context
            if (context != null) {
                context.forEach(authRequest::withContext);
            }
            
            // Perform authorization
            AuthorizationResult result = authorizationEngine.authorize(authRequest);
            
            // Update service account last used timestamp
            if (result.isGranted()) {
                serviceAccount.setLastUsedAt(Instant.now());
                serviceAccountRepository.save(serviceAccount);
            }
            
            // Add service-specific metadata
            result.withMetadata("service_id", serviceId)
                  .withMetadata("target_service", targetService)
                  .withMetadata("service_call", true);
            
            logger.debug("Service authorization result for {}: {}", serviceId, 
                        result.isGranted() ? "GRANTED" : "DENIED");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error authorizing service request: {} -> {}", serviceId, targetService, e);
            return AuthorizationResult.denied("Service authorization failed");
        }
    }
    
    /**
     * Validate service-to-service API key
     */
    public Optional<ServiceAccount> validateServiceApiKey(String apiKey) {
        try {
            // Hash the API key to match stored hash
            String hashedKey = hashApiKey(apiKey);
            
            Optional<ServiceAccount> serviceAccountOpt = serviceAccountRepository.findByApiKeyHash(hashedKey);
            
            if (serviceAccountOpt.isEmpty()) {
                return Optional.empty();
            }
            
            ServiceAccount serviceAccount = serviceAccountOpt.get();
            
            // Check if service account is active and not expired
            if (!serviceAccount.isActive() || 
                (serviceAccount.getExpiresAt() != null && serviceAccount.getExpiresAt().isBefore(Instant.now()))) {
                return Optional.empty();
            }
            
            // Update last used timestamp
            serviceAccount.setLastUsedAt(Instant.now());
            serviceAccountRepository.save(serviceAccount);
            
            return Optional.of(serviceAccount);
            
        } catch (Exception e) {
            logger.error("Error validating service API key", e);
            return Optional.empty();
        }
    }
    
    /**
     * Create a new service account
     */
    public ServiceAccount createServiceAccount(String serviceName, String description, 
                                             String tenantId, String createdBy, 
                                             Set<String> allowedServices) {
        try {
            // Create underlying user account for the service
            String serviceUserId = userService.createServiceUser(serviceName, tenantId, createdBy);
            
            // Generate API key
            String apiKey = generateApiKey();
            String hashedApiKey = hashApiKey(apiKey);
            
            // Create service account
            ServiceAccount serviceAccount = new ServiceAccount();
            serviceAccount.setId(UUID.randomUUID().toString());
            serviceAccount.setServiceName(serviceName);
            serviceAccount.setDescription(description);
            serviceAccount.setTenantId(tenantId);
            serviceAccount.setUserId(serviceUserId);
            serviceAccount.setApiKeyHash(hashedApiKey);
            serviceAccount.setActive(true);
            serviceAccount.setCreatedBy(createdBy);
            serviceAccount.setAllowedServices(allowedServices);
            serviceAccount.setExpiresAt(Instant.now().plusSeconds(365 * 24 * 3600)); // 1 year
            
            serviceAccount = serviceAccountRepository.save(serviceAccount);
            
            // Set the plain API key for return (only time it's available)
            serviceAccount.setPlainApiKey(apiKey);
            
            logger.info("Created service account: {} for tenant: {}", serviceName, tenantId);
            
            return serviceAccount;
            
        } catch (Exception e) {
            logger.error("Error creating service account: {}", serviceName, e);
            throw new RuntimeException("Failed to create service account", e);
        }
    }
    
    /**
     * Rotate service account API key
     */
    public String rotateServiceAccountApiKey(String serviceAccountId, String rotatedBy) {
        try {
            Optional<ServiceAccount> serviceAccountOpt = serviceAccountRepository.findById(serviceAccountId);
            if (serviceAccountOpt.isEmpty()) {
                throw new IllegalArgumentException("Service account not found");
            }
            
            ServiceAccount serviceAccount = serviceAccountOpt.get();
            
            // Generate new API key
            String newApiKey = generateApiKey();
            String hashedApiKey = hashApiKey(newApiKey);
            
            // Update service account
            serviceAccount.setApiKeyHash(hashedApiKey);
            serviceAccount.setLastRotatedAt(Instant.now());
            serviceAccount.setLastRotatedBy(rotatedBy);
            
            serviceAccountRepository.save(serviceAccount);
            
            logger.info("Rotated API key for service account: {} by: {}", 
                       serviceAccount.getServiceName(), rotatedBy);
            
            return newApiKey;
            
        } catch (Exception e) {
            logger.error("Error rotating service account API key: {}", serviceAccountId, e);
            throw new RuntimeException("Failed to rotate service account API key", e);
        }
    }
    
    /**
     * Check if service is allowed to call target service
     */
    public boolean isServiceCallAllowed(String sourceServiceId, String targetService) {
        try {
            Optional<ServiceAccount> serviceAccountOpt = serviceAccountRepository.findById(sourceServiceId);
            if (serviceAccountOpt.isEmpty()) {
                return false;
            }
            
            ServiceAccount serviceAccount = serviceAccountOpt.get();
            
            // Check if service is active
            if (!serviceAccount.isActive()) {
                return false;
            }
            
            // Check allowed services
            Set<String> allowedServices = serviceAccount.getAllowedServices();
            if (allowedServices.isEmpty()) {
                return true; // No restrictions
            }
            
            return allowedServices.contains(targetService) || allowedServices.contains("*");
            
        } catch (Exception e) {
            logger.error("Error checking service call permission: {} -> {}", 
                        sourceServiceId, targetService, e);
            return false;
        }
    }
    
    /**
     * Get service accounts for a tenant
     */
    public List<ServiceAccount> getServiceAccountsForTenant(String tenantId) {
        try {
            return serviceAccountRepository.findByTenantId(tenantId);
        } catch (Exception e) {
            logger.error("Error getting service accounts for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Disable service account
     */
    public void disableServiceAccount(String serviceAccountId, String disabledBy) {
        try {
            Optional<ServiceAccount> serviceAccountOpt = serviceAccountRepository.findById(serviceAccountId);
            if (serviceAccountOpt.isEmpty()) {
                throw new IllegalArgumentException("Service account not found");
            }
            
            ServiceAccount serviceAccount = serviceAccountOpt.get();
            serviceAccount.setActive(false);
            serviceAccount.setDisabledAt(Instant.now());
            serviceAccount.setDisabledBy(disabledBy);
            
            serviceAccountRepository.save(serviceAccount);
            
            logger.info("Disabled service account: {} by: {}", 
                       serviceAccount.getServiceName(), disabledBy);
            
        } catch (Exception e) {
            logger.error("Error disabling service account: {}", serviceAccountId, e);
            throw new RuntimeException("Failed to disable service account", e);
        }
    }
    
    /**
     * Get service call statistics
     */
    public ServiceCallStatistics getServiceCallStatistics(String serviceAccountId, 
                                                          Instant fromTime, Instant toTime) {
        try {
            // This would typically query a service call log table
            // For now, return a placeholder implementation
            ServiceCallStatistics stats = new ServiceCallStatistics();
            stats.setServiceAccountId(serviceAccountId);
            stats.setFromTime(fromTime);
            stats.setToTime(toTime);
            stats.setTotalCalls(0L);
            stats.setSuccessfulCalls(0L);
            stats.setFailedCalls(0L);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting service call statistics for: {}", serviceAccountId, e);
            return new ServiceCallStatistics();
        }
    }
    
    // Helper methods
    
    private String generateApiKey() {
        // Generate a secure random API key
        byte[] keyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(keyBytes);
        return "svc_" + Base64.getEncoder().encodeToString(keyBytes)
                                          .replaceAll("[/+=]", "")
                                          .substring(0, 32);
    }
    
    private String hashApiKey(String apiKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
    
    /**
     * Service call statistics
     */
    public static class ServiceCallStatistics {
        private String serviceAccountId;
        private Instant fromTime;
        private Instant toTime;
        private Long totalCalls;
        private Long successfulCalls;
        private Long failedCalls;
        private Map<String, Long> callsByService = new HashMap<>();
        private Map<String, Long> callsByOperation = new HashMap<>();
        
        // Getters and setters
        public String getServiceAccountId() { return serviceAccountId; }
        public void setServiceAccountId(String serviceAccountId) { this.serviceAccountId = serviceAccountId; }
        
        public Instant getFromTime() { return fromTime; }
        public void setFromTime(Instant fromTime) { this.fromTime = fromTime; }
        
        public Instant getToTime() { return toTime; }
        public void setToTime(Instant toTime) { this.toTime = toTime; }
        
        public Long getTotalCalls() { return totalCalls; }
        public void setTotalCalls(Long totalCalls) { this.totalCalls = totalCalls; }
        
        public Long getSuccessfulCalls() { return successfulCalls; }
        public void setSuccessfulCalls(Long successfulCalls) { this.successfulCalls = successfulCalls; }
        
        public Long getFailedCalls() { return failedCalls; }
        public void setFailedCalls(Long failedCalls) { this.failedCalls = failedCalls; }
        
        public Map<String, Long> getCallsByService() { return callsByService; }
        public void setCallsByService(Map<String, Long> callsByService) { this.callsByService = callsByService; }
        
        public Map<String, Long> getCallsByOperation() { return callsByOperation; }
        public void setCallsByOperation(Map<String, Long> callsByOperation) { this.callsByOperation = callsByOperation; }
    }
}