package com.enterprise.security.integration.frontend;

import com.enterprise.security.authorization.engine.RbacAuthorizationEngine;
import com.enterprise.security.authorization.engine.AuthorizationRequest;
import com.enterprise.security.authorization.engine.AuthorizationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for frontend permission checking
 */
@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PermissionCheckController {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionCheckController.class);
    
    @Autowired
    private RbacAuthorizationEngine authorizationEngine;
    
    /**
     * Check single permission for current user
     */
    @GetMapping("/check")
    public ResponseEntity<PermissionCheckResponse> checkPermission(
            @RequestParam String resource,
            @RequestParam String action,
            @RequestParam(required = false) String resourceId,
            HttpServletRequest request) {
        
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(PermissionCheckResponse.denied("User not authenticated"));
            }
            
            AuthorizationRequest authRequest = AuthorizationRequest.builder()
                .userId(userId)
                .action(action)
                .resourceId(resourceId)
                .resourceType(resource)
                .clientIp(getClientIp(request))
                .build();
            
            AuthorizationResult result = authorizationEngine.authorize(authRequest);
            
            PermissionCheckResponse response = new PermissionCheckResponse();
            response.setGranted(result.isGranted());
            response.setReason(result.getReason());
            response.setResource(resource);
            response.setAction(action);
            response.setResourceId(resourceId);
            response.setTimestamp(result.getTimestamp());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking permission for resource: {}, action: {}", resource, action, e);
            return ResponseEntity.ok(PermissionCheckResponse.denied("Permission check failed"));
        }
    }
    
    /**
     * Check multiple permissions for current user
     */
    @PostMapping("/check/bulk")
    public ResponseEntity<Map<String, PermissionCheckResponse>> checkPermissions(
            @RequestBody BulkPermissionCheckRequest bulkRequest,
            HttpServletRequest request) {
        
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                Map<String, PermissionCheckResponse> responses = bulkRequest.getPermissions().stream()
                    .collect(Collectors.toMap(
                        req -> req.getResource() + ":" + req.getAction(),
                        req -> PermissionCheckResponse.denied("User not authenticated")
                    ));
                return ResponseEntity.ok(responses);
            }
            
            Map<String, PermissionCheckResponse> responses = new HashMap<>();
            String clientIp = getClientIp(request);
            
            for (SinglePermissionRequest permRequest : bulkRequest.getPermissions()) {
                String key = permRequest.getResource() + ":" + permRequest.getAction();
                if (permRequest.getResourceId() != null) {
                    key += ":" + permRequest.getResourceId();
                }
                
                AuthorizationRequest authRequest = AuthorizationRequest.builder()
                    .userId(userId)
                    .action(permRequest.getAction())
                    .resourceId(permRequest.getResourceId())
                    .resourceType(permRequest.getResource())
                    .clientIp(clientIp)
                    .build();
                
                AuthorizationResult result = authorizationEngine.authorize(authRequest);
                
                PermissionCheckResponse response = new PermissionCheckResponse();
                response.setGranted(result.isGranted());
                response.setReason(result.getReason());
                response.setResource(permRequest.getResource());
                response.setAction(permRequest.getAction());
                response.setResourceId(permRequest.getResourceId());
                response.setTimestamp(result.getTimestamp());
                
                responses.put(key, response);
            }
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error checking bulk permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get current user's roles
     */
    @GetMapping("/roles")
    public ResponseEntity<UserRolesResponse> getCurrentUserRoles() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(new UserRolesResponse(Collections.emptySet()));
            }
            
            // This would typically fetch from user service
            Set<String> roles = new HashSet<>(); // Placeholder
            
            UserRolesResponse response = new UserRolesResponse(roles);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting user roles", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get current user's effective permissions
     */
    @GetMapping("/effective")
    public ResponseEntity<UserPermissionsResponse> getCurrentUserPermissions() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(new UserPermissionsResponse(Collections.emptySet()));
            }
            
            Set<String> permissions = authorizationEngine.getUserPermissions(userId);
            
            UserPermissionsResponse response = new UserPermissionsResponse(permissions);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting user permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get UI permissions for current user (optimized for frontend)
     */
    @GetMapping("/ui")
    public ResponseEntity<UiPermissionsResponse> getUiPermissions(
            @RequestParam(required = false) String module) {
        
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(new UiPermissionsResponse(Collections.emptyMap()));
            }
            
            Map<String, Map<String, Boolean>> uiPermissions = buildUiPermissions(userId, module);
            
            UiPermissionsResponse response = new UiPermissionsResponse(uiPermissions);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting UI permissions for module: {}", module, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Check if current user has specific role
     */
    @GetMapping("/has-role")
    public ResponseEntity<Boolean> hasRole(@RequestParam String roleName) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(false);
            }
            
            boolean hasRole = authorizationEngine.hasRole(userId, roleName);
            return ResponseEntity.ok(hasRole);
            
        } catch (Exception e) {
            logger.error("Error checking role: {}", roleName, e);
            return ResponseEntity.ok(false);
        }
    }
    
    /**
     * Check if current user has specific permission
     */
    @GetMapping("/has-permission")
    public ResponseEntity<Boolean> hasPermission(@RequestParam String permissionName) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(false);
            }
            
            boolean hasPermission = authorizationEngine.hasPermission(userId, permissionName);
            return ResponseEntity.ok(hasPermission);
            
        } catch (Exception e) {
            logger.error("Error checking permission: {}", permissionName, e);
            return ResponseEntity.ok(false);
        }
    }
    
    // Helper methods
    
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
        } else if (auth.getPrincipal() instanceof String) {
            return (String) auth.getPrincipal();
        }
        
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private Map<String, Map<String, Boolean>> buildUiPermissions(String userId, String module) {
        Map<String, Map<String, Boolean>> uiPermissions = new HashMap<>();
        
        // Define common UI resources and actions
        Map<String, List<String>> uiResources = new HashMap<>();
        uiResources.put("user", Arrays.asList("view", "create", "edit", "delete", "manage_roles"));
        uiResources.put("role", Arrays.asList("view", "create", "edit", "delete", "assign"));
        uiResources.put("permission", Arrays.asList("view", "create", "edit", "delete"));
        uiResources.put("document", Arrays.asList("view", "create", "edit", "delete", "share", "export"));
        uiResources.put("report", Arrays.asList("view", "create", "edit", "delete", "export", "schedule"));
        uiResources.put("admin", Arrays.asList("access", "system_config", "audit_logs", "monitoring"));
        
        // Filter by module if specified
        Map<String, List<String>> targetResources = module != null && uiResources.containsKey(module) ?
            Map.of(module, uiResources.get(module)) : uiResources;
        
        // Check permissions for each resource and action
        for (Map.Entry<String, List<String>> entry : targetResources.entrySet()) {
            String resource = entry.getKey();
            Map<String, Boolean> actions = new HashMap<>();
            
            for (String action : entry.getValue()) {
                AuthorizationRequest request = new AuthorizationRequest(userId, action, null, resource);
                AuthorizationResult result = authorizationEngine.authorize(request);
                actions.put(action, result.isGranted());
            }
            
            uiPermissions.put(resource, actions);
        }
        
        return uiPermissions;
    }
    
    // Response classes
    
    public static class PermissionCheckResponse {
        private boolean granted;
        private String reason;
        private String resource;
        private String action;
        private String resourceId;
        private java.time.Instant timestamp;
        
        public static PermissionCheckResponse denied(String reason) {
            PermissionCheckResponse response = new PermissionCheckResponse();
            response.setGranted(false);
            response.setReason(reason);
            response.setTimestamp(java.time.Instant.now());
            return response;
        }
        
        // Getters and setters
        public boolean isGranted() { return granted; }
        public void setGranted(boolean granted) { this.granted = granted; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        
        public java.time.Instant getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.Instant timestamp) { this.timestamp = timestamp; }
    }
    
    public static class BulkPermissionCheckRequest {
        private List<SinglePermissionRequest> permissions;
        
        public List<SinglePermissionRequest> getPermissions() { return permissions; }
        public void setPermissions(List<SinglePermissionRequest> permissions) { this.permissions = permissions; }
    }
    
    public static class SinglePermissionRequest {
        private String resource;
        private String action;
        private String resourceId;
        
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    }
    
    public static class UserRolesResponse {
        private Set<String> roles;
        
        public UserRolesResponse(Set<String> roles) { this.roles = roles; }
        
        public Set<String> getRoles() { return roles; }
        public void setRoles(Set<String> roles) { this.roles = roles; }
    }
    
    public static class UserPermissionsResponse {
        private Set<String> permissions;
        
        public UserPermissionsResponse(Set<String> permissions) { this.permissions = permissions; }
        
        public Set<String> getPermissions() { return permissions; }
        public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    }
    
    public static class UiPermissionsResponse {
        private Map<String, Map<String, Boolean>> permissions;
        
        public UiPermissionsResponse(Map<String, Map<String, Boolean>> permissions) { 
            this.permissions = permissions; 
        }
        
        public Map<String, Map<String, Boolean>> getPermissions() { return permissions; }
        public void setPermissions(Map<String, Map<String, Boolean>> permissions) { this.permissions = permissions; }
    }
}