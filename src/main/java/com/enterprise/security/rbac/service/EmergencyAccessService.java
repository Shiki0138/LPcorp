package com.enterprise.security.rbac.service;

import com.enterprise.security.rbac.model.User;
import com.enterprise.security.rbac.model.EmergencyAccess;
import com.enterprise.security.rbac.model.enums.EmergencyAccessStatus;
import com.enterprise.security.rbac.repository.EmergencyAccessRepository;
import com.enterprise.security.authorization.engine.AuthorizationRequest;
import com.enterprise.security.authorization.engine.AuthorizationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing emergency access procedures
 */
@Service
@Transactional
public class EmergencyAccessService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmergencyAccessService.class);
    
    @Autowired
    private EmergencyAccessRepository emergencyAccessRepository;
    
    @Autowired
    private RbacUserService userService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    // Default emergency access duration
    private static final Duration DEFAULT_EMERGENCY_DURATION = Duration.ofHours(4);
    private static final Duration MAX_EMERGENCY_DURATION = Duration.ofDays(1);
    
    /**
     * Request emergency access for a user
     */
    public EmergencyAccess requestEmergencyAccess(String userId, String resourceId, String action, 
                                                 String justification, String requestedBy) {
        try {
            User user = userService.getUser(userId);
            if (user == null) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            
            // Check if user already has active emergency access
            List<EmergencyAccess> activeAccess = emergencyAccessRepository
                .findByUserIdAndStatusAndExpiresAtAfter(userId, EmergencyAccessStatus.ACTIVE, Instant.now());
            
            if (!activeAccess.isEmpty()) {
                throw new IllegalStateException("User already has active emergency access");
            }
            
            // Create emergency access request
            EmergencyAccess emergencyAccess = new EmergencyAccess();
            emergencyAccess.setId(UUID.randomUUID().toString());
            emergencyAccess.setUserId(userId);
            emergencyAccess.setResourceId(resourceId);
            emergencyAccess.setAction(action);
            emergencyAccess.setJustification(justification);
            emergencyAccess.setRequestedBy(requestedBy);
            emergencyAccess.setStatus(EmergencyAccessStatus.PENDING);
            emergencyAccess.setRequestedAt(Instant.now());
            emergencyAccess.setExpiresAt(Instant.now().plus(DEFAULT_EMERGENCY_DURATION));
            
            // Determine if auto-approval is possible
            if (canAutoApprove(user, resourceId, action)) {
                emergencyAccess.setStatus(EmergencyAccessStatus.ACTIVE);
                emergencyAccess.setApprovedBy("SYSTEM");
                emergencyAccess.setApprovedAt(Instant.now());
                logger.info("Emergency access auto-approved for user: {}", userId);
            } else {
                // Notify approvers
                notifyEmergencyAccessApprovers(emergencyAccess);
                logger.info("Emergency access request created for user: {}, awaiting approval", userId);
            }
            
            emergencyAccess = emergencyAccessRepository.save(emergencyAccess);
            
            // Audit the request
            auditService.logEmergencyAccessRequest(emergencyAccess);
            
            return emergencyAccess;
            
        } catch (Exception e) {
            logger.error("Error requesting emergency access for user: {}", userId, e);
            throw e;
        }
    }
    
    /**
     * Approve emergency access request
     */
    public EmergencyAccess approveEmergencyAccess(String emergencyAccessId, String approvedBy, 
                                                 Duration customDuration) {
        try {
            EmergencyAccess emergencyAccess = emergencyAccessRepository.findById(emergencyAccessId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency access request not found"));
            
            if (emergencyAccess.getStatus() != EmergencyAccessStatus.PENDING) {
                throw new IllegalStateException("Emergency access request is not pending approval");
            }
            
            // Update approval details
            emergencyAccess.setStatus(EmergencyAccessStatus.ACTIVE);
            emergencyAccess.setApprovedBy(approvedBy);
            emergencyAccess.setApprovedAt(Instant.now());
            
            // Apply custom duration if provided and within limits
            if (customDuration != null) {
                Duration actualDuration = customDuration.compareTo(MAX_EMERGENCY_DURATION) > 0 ?
                    MAX_EMERGENCY_DURATION : customDuration;
                emergencyAccess.setExpiresAt(Instant.now().plus(actualDuration));
            }
            
            emergencyAccess = emergencyAccessRepository.save(emergencyAccess);
            
            // Notify user of approval
            notificationService.notifyEmergencyAccessApproved(emergencyAccess);
            
            // Audit the approval
            auditService.logEmergencyAccessApproval(emergencyAccess);
            
            logger.info("Emergency access approved for user: {} by: {}", 
                       emergencyAccess.getUserId(), approvedBy);
            
            return emergencyAccess;
            
        } catch (Exception e) {
            logger.error("Error approving emergency access: {}", emergencyAccessId, e);
            throw e;
        }
    }
    
    /**
     * Reject emergency access request
     */
    public EmergencyAccess rejectEmergencyAccess(String emergencyAccessId, String rejectedBy, String reason) {
        try {
            EmergencyAccess emergencyAccess = emergencyAccessRepository.findById(emergencyAccessId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency access request not found"));
            
            if (emergencyAccess.getStatus() != EmergencyAccessStatus.PENDING) {
                throw new IllegalStateException("Emergency access request is not pending approval");
            }
            
            emergencyAccess.setStatus(EmergencyAccessStatus.REJECTED);
            emergencyAccess.setRejectedBy(rejectedBy);
            emergencyAccess.setRejectedAt(Instant.now());
            emergencyAccess.setRejectionReason(reason);
            
            emergencyAccess = emergencyAccessRepository.save(emergencyAccess);
            
            // Notify user of rejection
            notificationService.notifyEmergencyAccessRejected(emergencyAccess);
            
            // Audit the rejection
            auditService.logEmergencyAccessRejection(emergencyAccess);
            
            logger.info("Emergency access rejected for user: {} by: {}", 
                       emergencyAccess.getUserId(), rejectedBy);
            
            return emergencyAccess;
            
        } catch (Exception e) {
            logger.error("Error rejecting emergency access: {}", emergencyAccessId, e);
            throw e;
        }
    }
    
    /**
     * Revoke active emergency access
     */
    public void revokeEmergencyAccess(String emergencyAccessId, String revokedBy, String reason) {
        try {
            EmergencyAccess emergencyAccess = emergencyAccessRepository.findById(emergencyAccessId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency access not found"));
            
            if (emergencyAccess.getStatus() != EmergencyAccessStatus.ACTIVE) {
                throw new IllegalStateException("Emergency access is not active");
            }
            
            emergencyAccess.setStatus(EmergencyAccessStatus.REVOKED);
            emergencyAccess.setRevokedBy(revokedBy);
            emergencyAccess.setRevokedAt(Instant.now());
            emergencyAccess.setRevocationReason(reason);
            
            emergencyAccessRepository.save(emergencyAccess);
            
            // Notify user of revocation
            notificationService.notifyEmergencyAccessRevoked(emergencyAccess);
            
            // Audit the revocation
            auditService.logEmergencyAccessRevocation(emergencyAccess);
            
            logger.info("Emergency access revoked for user: {} by: {}", 
                       emergencyAccess.getUserId(), revokedBy);
            
        } catch (Exception e) {
            logger.error("Error revoking emergency access: {}", emergencyAccessId, e);
            throw e;
        }
    }
    
    /**
     * Check if user has active emergency access for given request
     */
    public boolean hasActiveEmergencyAccess(String userId, String resourceId, String action) {
        try {
            List<EmergencyAccess> activeAccess = emergencyAccessRepository
                .findByUserIdAndStatusAndExpiresAtAfter(userId, EmergencyAccessStatus.ACTIVE, Instant.now());
            
            return activeAccess.stream()
                .anyMatch(access -> 
                    (resourceId == null || resourceId.equals(access.getResourceId())) &&
                    (action == null || action.equals(access.getAction()) || "*".equals(access.getAction()))
                );
                
        } catch (Exception e) {
            logger.error("Error checking emergency access for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Get all emergency access requests for approval
     */
    public List<EmergencyAccess> getPendingEmergencyAccessRequests() {
        return emergencyAccessRepository.findByStatusOrderByRequestedAtDesc(EmergencyAccessStatus.PENDING);
    }
    
    /**
     * Get emergency access history for a user
     */
    public List<EmergencyAccess> getEmergencyAccessHistory(String userId) {
        return emergencyAccessRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }
    
    /**
     * Cleanup expired emergency access entries
     */
    @Transactional
    public void cleanupExpiredEmergencyAccess() {
        try {
            List<EmergencyAccess> expiredAccess = emergencyAccessRepository
                .findByStatusAndExpiresAtBefore(EmergencyAccessStatus.ACTIVE, Instant.now());
            
            for (EmergencyAccess access : expiredAccess) {
                access.setStatus(EmergencyAccessStatus.EXPIRED);
                emergencyAccessRepository.save(access);
                
                // Audit the expiration
                auditService.logEmergencyAccessExpired(access);
            }
            
            logger.info("Cleaned up {} expired emergency access entries", expiredAccess.size());
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired emergency access", e);
        }
    }
    
    /**
     * Evaluate emergency access authorization
     */
    public AuthorizationResult evaluateEmergencyAccess(AuthorizationRequest request) {
        try {
            if (!request.isEmergencyAccess()) {
                return AuthorizationResult.denied("Not an emergency access request");
            }
            
            boolean hasAccess = hasActiveEmergencyAccess(request.getUserId(), 
                                                        request.getResourceId(), 
                                                        request.getAction());
            
            if (hasAccess) {
                return AuthorizationResult.granted("Emergency access granted")
                    .withMetadata("emergency", true);
            } else {
                return AuthorizationResult.denied("No active emergency access found");
            }
            
        } catch (Exception e) {
            logger.error("Error evaluating emergency access", e);
            return AuthorizationResult.denied("Emergency access evaluation failed");
        }
    }
    
    // Helper methods
    
    private boolean canAutoApprove(User user, String resourceId, String action) {
        // Define criteria for auto-approval
        // For example: low-risk operations, senior staff, etc.
        
        // Check user clearance level
        if (user.getClearanceLevel().getLevel() < 2) {
            return false;
        }
        
        // Check if it's a read-only operation
        if ("read".equalsIgnoreCase(action) || "view".equalsIgnoreCase(action)) {
            return true;
        }
        
        // Check user roles
        boolean isSeniorStaff = user.getActiveRoles().stream()
            .anyMatch(role -> role.getName().contains("SENIOR") || role.getName().contains("MANAGER"));
        
        return isSeniorStaff;
    }
    
    private void notifyEmergencyAccessApprovers(EmergencyAccess emergencyAccess) {
        // Get list of approvers (security team, managers, etc.)
        List<String> approvers = getEmergencyAccessApprovers();
        
        for (String approver : approvers) {
            notificationService.notifyEmergencyAccessRequest(approver, emergencyAccess);
        }
    }
    
    private List<String> getEmergencyAccessApprovers() {
        // This would typically query for users with emergency access approval permissions
        return List.of("security-team@company.com", "it-manager@company.com");
    }
}