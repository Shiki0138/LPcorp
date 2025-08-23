package com.enterprise.security.authorization.engine;

import com.enterprise.security.rbac.model.*;
import com.enterprise.security.rbac.model.enums.*;
import com.enterprise.security.rbac.service.*;
import com.enterprise.security.authorization.evaluator.AttributeEvaluator;
import com.enterprise.security.authorization.evaluator.ContextEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main RBAC authorization engine with comprehensive permission evaluation
 */
@Component
public class RbacAuthorizationEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(RbacAuthorizationEngine.class);
    
    @Autowired
    private RbacUserService userService;
    
    @Autowired
    private RbacPermissionService permissionService;
    
    @Autowired
    private RbacResourceService resourceService;
    
    @Autowired
    private AttributeEvaluator attributeEvaluator;
    
    @Autowired
    private ContextEvaluator contextEvaluator;
    
    @Autowired
    private PermissionCache permissionCache;
    
    /**
     * Main authorization check method
     */
    public AuthorizationResult authorize(AuthorizationRequest request) {
        try {
            logger.debug("Authorizing user {} for action {} on resource {}", 
                request.getUserId(), request.getAction(), request.getResourceId());
            
            // Get user with all permissions
            User user = userService.getUserWithPermissions(request.getUserId());
            if (user == null) {
                return AuthorizationResult.denied("User not found");
            }
            
            // Check if user is active and not locked
            if (!isUserActiveAndAccessible(user)) {
                return AuthorizationResult.denied("User account is not accessible");
            }
            
            // Get resource if specified
            Resource resource = null;
            if (request.getResourceId() != null) {
                resource = resourceService.getResource(request.getResourceId());
                if (resource != null && !resource.isAccessible()) {
                    return AuthorizationResult.denied("Resource is not accessible");
                }
            }
            
            // Evaluate permissions
            AuthorizationResult result = evaluatePermissions(user, request, resource);
            
            // Log authorization decision
            logAuthorizationDecision(request, result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Authorization error for user {} on resource {}", 
                request.getUserId(), request.getResourceId(), e);
            return AuthorizationResult.denied("Authorization evaluation failed");
        }
    }
    
    /**
     * Bulk authorization check for multiple resources
     */
    public Map<String, AuthorizationResult> authorizeMultiple(String userId, String action, List<String> resourceIds) {
        Map<String, AuthorizationResult> results = new HashMap<>();
        
        for (String resourceId : resourceIds) {
            AuthorizationRequest request = new AuthorizationRequest(userId, action, resourceId);
            results.put(resourceId, authorize(request));
        }
        
        return results;
    }
    
    /**
     * Check if user has a specific permission
     */
    @Cacheable(value = "permissions", key = "#userId + ':' + #permissionName")
    public boolean hasPermission(String userId, String permissionName) {
        try {
            User user = userService.getUserWithPermissions(userId);
            if (user == null || !isUserActiveAndAccessible(user)) {
                return false;
            }
            
            return hasDirectPermission(user, permissionName) || 
                   hasRoleBasedPermission(user, permissionName) ||
                   hasDelegatedPermission(user, permissionName);
            
        } catch (Exception e) {
            logger.error("Error checking permission {} for user {}", permissionName, userId, e);
            return false;
        }
    }
    
    /**
     * Check if user has a specific role
     */
    @Cacheable(value = "roles", key = "#userId + ':' + #roleName")
    public boolean hasRole(String userId, String roleName) {
        try {
            User user = userService.getUserWithPermissions(userId);
            if (user == null || !isUserActiveAndAccessible(user)) {
                return false;
            }
            
            return user.getActiveRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
            
        } catch (Exception e) {
            logger.error("Error checking role {} for user {}", roleName, userId, e);
            return false;
        }
    }
    
    /**
     * Get all effective permissions for a user
     */
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(String userId) {
        try {
            User user = userService.getUserWithPermissions(userId);
            if (user == null || !isUserActiveAndAccessible(user)) {
                return Collections.emptySet();
            }
            
            Set<String> permissions = new HashSet<>();
            
            // Add direct permissions
            permissions.addAll(getDirectPermissions(user));
            
            // Add role-based permissions
            permissions.addAll(getRoleBasedPermissions(user));
            
            // Add delegated permissions
            permissions.addAll(getDelegatedPermissions(user));
            
            return permissions;
            
        } catch (Exception e) {
            logger.error("Error getting permissions for user {}", userId, e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Main permission evaluation logic
     */
    private AuthorizationResult evaluatePermissions(User user, AuthorizationRequest request, Resource resource) {
        // Check direct permissions first
        AuthorizationResult directResult = checkDirectPermissions(user, request, resource);
        if (directResult.isGranted()) {
            return directResult;
        }
        
        // Check role-based permissions
        AuthorizationResult roleResult = checkRoleBasedPermissions(user, request, resource);
        if (roleResult.isGranted()) {
            return roleResult;
        }
        
        // Check delegated permissions
        AuthorizationResult delegatedResult = checkDelegatedPermissions(user, request, resource);
        if (delegatedResult.isGranted()) {
            return delegatedResult;
        }
        
        // Check emergency access
        if (request.isEmergencyAccess() && isEmergencyAccessAllowed(user, request)) {
            return AuthorizationResult.granted("Emergency access granted");
        }
        
        return AuthorizationResult.denied("No matching permissions found");
    }
    
    /**
     * Check direct user permissions
     */
    private AuthorizationResult checkDirectPermissions(User user, AuthorizationRequest request, Resource resource) {
        for (UserPermissionGrant grant : user.getDirectPermissionGrants()) {
            if (!grant.isActive() || !grant.isEffective()) {
                continue;
            }
            
            Permission permission = grant.getPermission();
            if (!permission.isActive()) {
                continue;
            }
            
            if (matchesPermission(permission, request, resource)) {
                // Evaluate constraints and attributes
                AuthorizationResult constraintResult = evaluateConstraints(user, permission, grant.getConstraints(), request, resource);
                if (constraintResult.isGranted()) {
                    return AuthorizationResult.granted("Direct permission: " + permission.getName());
                }
            }
        }
        
        return AuthorizationResult.denied("No matching direct permissions");
    }
    
    /**
     * Check role-based permissions
     */
    private AuthorizationResult checkRoleBasedPermissions(User user, AuthorizationRequest request, Resource resource) {
        for (UserRoleAssignment assignment : user.getRoleAssignments()) {
            if (!assignment.isActive() || !assignment.isEffective()) {
                continue;
            }
            
            Role role = assignment.getRole();
            if (!role.isActive()) {
                continue;
            }
            
            // Check all permissions in the role (including inherited)
            for (Permission permission : role.getAllPermissions()) {
                if (!permission.isActive()) {
                    continue;
                }
                
                if (matchesPermission(permission, request, resource)) {
                    // Evaluate constraints and attributes
                    AuthorizationResult constraintResult = evaluateConstraints(user, permission, null, request, resource);
                    if (constraintResult.isGranted()) {
                        return AuthorizationResult.granted("Role permission: " + role.getName() + "." + permission.getName());
                    }
                }
            }
        }
        
        return AuthorizationResult.denied("No matching role permissions");
    }
    
    /**
     * Check delegated permissions
     */
    private AuthorizationResult checkDelegatedPermissions(User user, AuthorizationRequest request, Resource resource) {
        for (UserDelegation delegation : user.getReceivedDelegations()) {
            if (!delegation.isActive() || !delegation.isEffective()) {
                continue;
            }
            
            if (delegation.isFullDelegation()) {
                // Full delegation - check delegator's permissions
                User delegator = delegation.getDelegator();
                AuthorizationResult delegatorResult = evaluatePermissions(delegator, request, resource);
                if (delegatorResult.isGranted()) {
                    return AuthorizationResult.granted("Delegated from: " + delegator.getUsername());
                }
            } else {
                // Partial delegation - check specific permissions
                for (Permission permission : delegation.getDelegatedPermissions()) {
                    if (matchesPermission(permission, request, resource)) {
                        AuthorizationResult constraintResult = evaluateConstraints(user, permission, delegation.getConstraints(), request, resource);
                        if (constraintResult.isGranted()) {
                            return AuthorizationResult.granted("Delegated permission: " + permission.getName());
                        }
                    }
                }
            }
        }
        
        return AuthorizationResult.denied("No matching delegated permissions");
    }
    
    /**
     * Check if permission matches the request
     */
    private boolean matchesPermission(Permission permission, AuthorizationRequest request, Resource resource) {
        // Match resource type
        if (!"*".equals(permission.getResourceType()) && resource != null && 
            !permission.getResourceType().equalsIgnoreCase(resource.getResourceType())) {
            return false;
        }
        
        // Match action
        if (!"*".equals(permission.getAction()) && 
            !permission.getAction().equalsIgnoreCase(request.getAction())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Evaluate permission constraints and attributes
     */
    private AuthorizationResult evaluateConstraints(User user, Permission permission, String additionalConstraints, 
                                                   AuthorizationRequest request, Resource resource) {
        
        // Check clearance level
        if (permission.getRequiredClearanceLevel() != null && 
            !user.getClearanceLevel().canAccess(permission.getRequiredClearanceLevel())) {
            return AuthorizationResult.denied("Insufficient clearance level");
        }
        
        // Check data classification
        if (resource != null && permission.getMinimumClassification() != null && 
            resource.getClassification() != null &&
            !user.getClearanceLevel().canAccess(mapClassificationToClearance(resource.getClassification()))) {
            return AuthorizationResult.denied("Insufficient data classification clearance");
        }
        
        // Check time restrictions
        if (permission.getTimeRestriction() != null && 
            !permission.getTimeRestriction().isCurrentlyAllowed()) {
            return AuthorizationResult.denied("Access not allowed at current time");
        }
        
        // Check geographic restrictions
        if (permission.getGeographicRestriction() != null && 
            !isGeographicallyAllowed(user, permission.getGeographicRestriction(), request)) {
            return AuthorizationResult.denied("Access not allowed from current location");
        }
        
        // Evaluate ABAC attributes
        if (permission.getAttributeConstraints() != null) {
            boolean attributeMatch = attributeEvaluator.evaluate(user, resource, permission.getAttributeConstraints());
            if (!attributeMatch) {
                return AuthorizationResult.denied("Attribute constraints not satisfied");
            }
        }
        
        // Evaluate custom conditions
        if (permission.getConditionExpression() != null) {
            boolean conditionMet = contextEvaluator.evaluateCondition(user, resource, request, permission.getConditionExpression());
            if (!conditionMet) {
                return AuthorizationResult.denied("Custom conditions not satisfied");
            }
        }
        
        return AuthorizationResult.granted("All constraints satisfied");
    }
    
    // Helper methods
    
    private boolean isUserActiveAndAccessible(User user) {
        return user.isActive() && !user.isLocked();
    }
    
    private boolean hasDirectPermission(User user, String permissionName) {
        return user.getDirectPermissionGrants().stream()
            .filter(UserPermissionGrant::isActive)
            .anyMatch(grant -> grant.getPermission().getName().equals(permissionName));
    }
    
    private boolean hasRoleBasedPermission(User user, String permissionName) {
        return user.getActiveRoles().stream()
            .flatMap(role -> role.getAllPermissions().stream())
            .anyMatch(permission -> permission.getName().equals(permissionName));
    }
    
    private boolean hasDelegatedPermission(User user, String permissionName) {
        return user.getReceivedDelegations().stream()
            .filter(UserDelegation::isActive)
            .anyMatch(delegation -> 
                delegation.getDelegatedPermissions().stream()
                    .anyMatch(permission -> permission.getName().equals(permissionName)));
    }
    
    private Set<String> getDirectPermissions(User user) {
        return user.getDirectPermissionGrants().stream()
            .filter(UserPermissionGrant::isActive)
            .map(grant -> grant.getPermission().getName())
            .collect(Collectors.toSet());
    }
    
    private Set<String> getRoleBasedPermissions(User user) {
        return user.getActiveRoles().stream()
            .flatMap(role -> role.getAllPermissions().stream())
            .map(Permission::getName)
            .collect(Collectors.toSet());
    }
    
    private Set<String> getDelegatedPermissions(User user) {
        return user.getReceivedDelegations().stream()
            .filter(UserDelegation::isActive)
            .flatMap(delegation -> delegation.getDelegatedPermissions().stream())
            .map(Permission::getName)
            .collect(Collectors.toSet());
    }
    
    private boolean isEmergencyAccessAllowed(User user, AuthorizationRequest request) {
        // Implementation for emergency access logic
        return false; // Placeholder
    }
    
    private boolean isGeographicallyAllowed(User user, GeographicRestriction restriction, AuthorizationRequest request) {
        if (request.getClientIp() != null && !restriction.isIpAllowed(request.getClientIp())) {
            return false;
        }
        
        if (request.getCountryCode() != null && !restriction.isCountryAllowed(request.getCountryCode())) {
            return false;
        }
        
        return true;
    }
    
    private ClearanceLevel mapClassificationToClearance(DataClassification classification) {
        switch (classification) {
            case PUBLIC: return ClearanceLevel.PUBLIC;
            case INTERNAL: return ClearanceLevel.STANDARD;
            case CONFIDENTIAL: return ClearanceLevel.CONFIDENTIAL;
            case RESTRICTED: return ClearanceLevel.SECRET;
            case TOP_SECRET: return ClearanceLevel.TOP_SECRET;
            default: return ClearanceLevel.STANDARD;
        }
    }
    
    private void logAuthorizationDecision(AuthorizationRequest request, AuthorizationResult result) {
        if (logger.isInfoEnabled()) {
            logger.info("Authorization {} for user {} on resource {} with action {}: {}",
                result.isGranted() ? "GRANTED" : "DENIED",
                request.getUserId(),
                request.getResourceId(),
                request.getAction(),
                result.getReason());
        }
    }
}