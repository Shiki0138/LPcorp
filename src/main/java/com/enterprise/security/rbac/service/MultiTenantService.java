package com.enterprise.security.rbac.service;

import com.enterprise.security.rbac.model.*;
import com.enterprise.security.rbac.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for multi-tenant isolation and management
 */
@Service
@Transactional
public class MultiTenantService {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantService.class);
    
    @Autowired
    private RbacUserRepository userRepository;
    
    @Autowired
    private RbacRoleRepository roleRepository;
    
    @Autowired
    private RbacPermissionRepository permissionRepository;
    
    @Autowired
    private RbacResourceRepository resourceRepository;
    
    /**
     * Get all users for a specific tenant
     */
    public List<User> getTenantUsers(String tenantId) {
        try {
            return userRepository.findByTenantIdAndStatusNot(tenantId, 
                com.enterprise.security.rbac.model.enums.UserStatus.DELETED);
        } catch (Exception e) {
            logger.error("Error getting users for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all roles for a specific tenant
     */
    public List<Role> getTenantRoles(String tenantId) {
        try {
            return roleRepository.findByTenantIdAndStatusNot(tenantId,
                com.enterprise.security.rbac.model.enums.RoleStatus.INACTIVE);
        } catch (Exception e) {
            logger.error("Error getting roles for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all permissions for a specific tenant
     */
    public List<Permission> getTenantPermissions(String tenantId) {
        try {
            return permissionRepository.findByTenantIdAndStatusNot(tenantId,
                com.enterprise.security.rbac.model.enums.PermissionStatus.INACTIVE);
        } catch (Exception e) {
            logger.error("Error getting permissions for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all resources for a specific tenant
     */
    public List<Resource> getTenantResources(String tenantId) {
        try {
            return resourceRepository.findByTenantIdAndStatusNot(tenantId,
                com.enterprise.security.rbac.model.enums.ResourceStatus.DELETED);
        } catch (Exception e) {
            logger.error("Error getting resources for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Check if user belongs to specified tenant
     */
    public boolean userBelongsToTenant(String userId, String tenantId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            return user.isPresent() && tenantId.equals(user.get().getTenantId());
        } catch (Exception e) {
            logger.error("Error checking user tenant membership: {} for tenant: {}", userId, tenantId, e);
            return false;
        }
    }
    
    /**
     * Check if role belongs to specified tenant
     */
    public boolean roleBelongsToTenant(String roleId, String tenantId) {
        try {
            Optional<Role> role = roleRepository.findById(roleId);
            return role.isPresent() && tenantId.equals(role.get().getTenantId());
        } catch (Exception e) {
            logger.error("Error checking role tenant membership: {} for tenant: {}", roleId, tenantId, e);
            return false;
        }
    }
    
    /**
     * Check if permission belongs to specified tenant
     */
    public boolean permissionBelongsToTenant(String permissionId, String tenantId) {
        try {
            Optional<Permission> permission = permissionRepository.findById(permissionId);
            return permission.isPresent() && tenantId.equals(permission.get().getTenantId());
        } catch (Exception e) {
            logger.error("Error checking permission tenant membership: {} for tenant: {}", 
                        permissionId, tenantId, e);
            return false;
        }
    }
    
    /**
     * Check if resource belongs to specified tenant
     */
    public boolean resourceBelongsToTenant(String resourceId, String tenantId) {
        try {
            Optional<Resource> resource = resourceRepository.findById(resourceId);
            return resource.isPresent() && tenantId.equals(resource.get().getTenantId());
        } catch (Exception e) {
            logger.error("Error checking resource tenant membership: {} for tenant: {}", 
                        resourceId, tenantId, e);
            return false;
        }
    }
    
    /**
     * Validate tenant isolation for user role assignment
     */
    public boolean validateTenantIsolationForRoleAssignment(String userId, String roleId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            Optional<Role> role = roleRepository.findById(roleId);
            
            if (user.isEmpty() || role.isEmpty()) {
                return false;
            }
            
            // Both user and role must belong to the same tenant
            return user.get().getTenantId().equals(role.get().getTenantId());
            
        } catch (Exception e) {
            logger.error("Error validating tenant isolation for role assignment: user={}, role={}", 
                        userId, roleId, e);
            return false;
        }
    }
    
    /**
     * Validate tenant isolation for permission grant
     */
    public boolean validateTenantIsolationForPermissionGrant(String userId, String permissionId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            Optional<Permission> permission = permissionRepository.findById(permissionId);
            
            if (user.isEmpty() || permission.isEmpty()) {
                return false;
            }
            
            // Both user and permission must belong to the same tenant
            return user.get().getTenantId().equals(permission.get().getTenantId());
            
        } catch (Exception e) {
            logger.error("Error validating tenant isolation for permission grant: user={}, permission={}", 
                        userId, permissionId, e);
            return false;
        }
    }
    
    /**
     * Validate tenant isolation for resource access
     */
    public boolean validateTenantIsolationForResourceAccess(String userId, String resourceId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            Optional<Resource> resource = resourceRepository.findById(resourceId);
            
            if (user.isEmpty() || resource.isEmpty()) {
                return false;
            }
            
            // Both user and resource must belong to the same tenant
            return user.get().getTenantId().equals(resource.get().getTenantId());
            
        } catch (Exception e) {
            logger.error("Error validating tenant isolation for resource access: user={}, resource={}", 
                        userId, resourceId, e);
            return false;
        }
    }
    
    /**
     * Get tenant statistics
     */
    public TenantStatistics getTenantStatistics(String tenantId) {
        try {
            TenantStatistics stats = new TenantStatistics();
            stats.setTenantId(tenantId);
            
            // Count entities for this tenant
            stats.setUserCount(userRepository.countByTenantId(tenantId));
            stats.setRoleCount(roleRepository.countByTenantId(tenantId));
            stats.setPermissionCount(permissionRepository.countByTenantId(tenantId));
            stats.setResourceCount(resourceRepository.countByTenantId(tenantId));
            
            // Get active counts
            stats.setActiveUserCount(userRepository.countByTenantIdAndStatus(tenantId, 
                com.enterprise.security.rbac.model.enums.UserStatus.ACTIVE));
            stats.setActiveRoleCount(roleRepository.countByTenantIdAndStatus(tenantId,
                com.enterprise.security.rbac.model.enums.RoleStatus.ACTIVE));
            stats.setActivePermissionCount(permissionRepository.countByTenantIdAndStatus(tenantId,
                com.enterprise.security.rbac.model.enums.PermissionStatus.ACTIVE));
            stats.setActiveResourceCount(resourceRepository.countByTenantIdAndStatus(tenantId,
                com.enterprise.security.rbac.model.enums.ResourceStatus.ACTIVE));
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting tenant statistics for tenant: {}", tenantId, e);
            return new TenantStatistics(tenantId);
        }
    }
    
    /**
     * Get all tenant IDs in the system
     */
    public Set<String> getAllTenantIds() {
        try {
            Set<String> tenantIds = new HashSet<>();
            tenantIds.addAll(userRepository.findAllTenantIds());
            tenantIds.addAll(roleRepository.findAllTenantIds());
            tenantIds.addAll(permissionRepository.findAllTenantIds());
            tenantIds.addAll(resourceRepository.findAllTenantIds());
            return tenantIds;
        } catch (Exception e) {
            logger.error("Error getting all tenant IDs", e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Validate cross-tenant operation is allowed
     */
    public boolean isCrossTenantOperationAllowed(String operatorUserId, String targetTenantId, String operation) {
        try {
            Optional<User> operator = userRepository.findById(operatorUserId);
            if (operator.isEmpty()) {
                return false;
            }
            
            User operatorUser = operator.get();
            
            // System administrators can perform cross-tenant operations
            boolean isSystemAdmin = operatorUser.getActiveRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()) || 
                                 "SUPER_ADMIN".equals(role.getName()));
            
            if (isSystemAdmin) {
                return true;
            }
            
            // Tenant administrators can perform operations within their tenant
            if (operatorUser.getTenantId().equals(targetTenantId)) {
                boolean isTenantAdmin = operatorUser.getActiveRoles().stream()
                    .anyMatch(role -> "TENANT_ADMIN".equals(role.getName()));
                return isTenantAdmin;
            }
            
            // Cross-tenant operations require special permissions
            return operatorUser.getDirectPermissions().stream()
                .anyMatch(permission -> ("cross_tenant_" + operation).equals(permission.getName()));
            
        } catch (Exception e) {
            logger.error("Error validating cross-tenant operation: operator={}, tenant={}, operation={}", 
                        operatorUserId, targetTenantId, operation, e);
            return false;
        }
    }
    
    /**
     * Clone roles and permissions from one tenant to another
     */
    public void cloneTenantRbacStructure(String sourceTenantId, String targetTenantId, String createdBy) {
        try {
            logger.info("Cloning RBAC structure from tenant {} to tenant {}", sourceTenantId, targetTenantId);
            
            // Clone permissions first
            List<Permission> sourcePermissions = getTenantPermissions(sourceTenantId);
            Map<String, String> permissionMapping = new HashMap<>();
            
            for (Permission sourcePermission : sourcePermissions) {
                Permission clonedPermission = clonePermissionForTenant(sourcePermission, targetTenantId, createdBy);
                permissionRepository.save(clonedPermission);
                permissionMapping.put(sourcePermission.getId(), clonedPermission.getId());
            }
            
            // Clone roles and map permissions
            List<Role> sourceRoles = getTenantRoles(sourceTenantId);
            Map<String, String> roleMapping = new HashMap<>();
            
            for (Role sourceRole : sourceRoles) {
                Role clonedRole = cloneRoleForTenant(sourceRole, targetTenantId, createdBy, permissionMapping);
                roleRepository.save(clonedRole);
                roleMapping.put(sourceRole.getId(), clonedRole.getId());
            }
            
            logger.info("Successfully cloned {} permissions and {} roles from tenant {} to tenant {}", 
                       sourcePermissions.size(), sourceRoles.size(), sourceTenantId, targetTenantId);
            
        } catch (Exception e) {
            logger.error("Error cloning RBAC structure from tenant {} to tenant {}", 
                        sourceTenantId, targetTenantId, e);
            throw new RuntimeException("Failed to clone tenant RBAC structure", e);
        }
    }
    
    // Helper methods
    
    private Permission clonePermissionForTenant(Permission source, String targetTenantId, String createdBy) {
        Permission cloned = new Permission();
        cloned.setName(source.getName());
        cloned.setDescription(source.getDescription());
        cloned.setResourceType(source.getResourceType());
        cloned.setAction(source.getAction());
        cloned.setCategory(source.getCategory());
        cloned.setTenantId(targetTenantId);
        cloned.setPermissionType(source.getPermissionType());
        cloned.setScope(source.getScope());
        cloned.setStatus(source.getStatus());
        cloned.setAttributeConstraints(source.getAttributeConstraints());
        cloned.setResourceConstraints(source.getResourceConstraints());
        cloned.setConditionExpression(source.getConditionExpression());
        cloned.setMinimumClassification(source.getMinimumClassification());
        cloned.setRequiredClearanceLevel(source.getRequiredClearanceLevel());
        cloned.setRiskLevel(source.getRiskLevel());
        cloned.setComplianceTag(source.getComplianceTag());
        cloned.setAuditRequired(source.getAuditRequired());
        cloned.setApprovalRequired(source.getApprovalRequired());
        cloned.setCreatedBy(createdBy);
        return cloned;
    }
    
    private Role cloneRoleForTenant(Role source, String targetTenantId, String createdBy, 
                                  Map<String, String> permissionMapping) {
        Role cloned = new Role();
        cloned.setName(source.getName());
        cloned.setDescription(source.getDescription());
        cloned.setRoleType(source.getRoleType());
        cloned.setStatus(source.getStatus());
        cloned.setTenantId(targetTenantId);
        cloned.setHierarchyLevel(source.getHierarchyLevel());
        cloned.setMaxUsers(source.getMaxUsers());
        cloned.setMaxConcurrentSessions(source.getMaxConcurrentSessions());
        cloned.setRequiresApproval(source.getRequiresApproval());
        cloned.setApproverRoleId(source.getApproverRoleId());
        cloned.setCreatedBy(createdBy);
        
        // Note: Permission assignments would need to be handled separately
        // after the role is saved, using the permissionMapping
        
        return cloned;
    }
    
    /**
     * Tenant statistics class
     */
    public static class TenantStatistics {
        private String tenantId;
        private long userCount;
        private long roleCount;
        private long permissionCount;
        private long resourceCount;
        private long activeUserCount;
        private long activeRoleCount;
        private long activePermissionCount;
        private long activeResourceCount;
        
        public TenantStatistics() {}
        
        public TenantStatistics(String tenantId) {
            this.tenantId = tenantId;
        }
        
        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }
        
        public long getRoleCount() { return roleCount; }
        public void setRoleCount(long roleCount) { this.roleCount = roleCount; }
        
        public long getPermissionCount() { return permissionCount; }
        public void setPermissionCount(long permissionCount) { this.permissionCount = permissionCount; }
        
        public long getResourceCount() { return resourceCount; }
        public void setResourceCount(long resourceCount) { this.resourceCount = resourceCount; }
        
        public long getActiveUserCount() { return activeUserCount; }
        public void setActiveUserCount(long activeUserCount) { this.activeUserCount = activeUserCount; }
        
        public long getActiveRoleCount() { return activeRoleCount; }
        public void setActiveRoleCount(long activeRoleCount) { this.activeRoleCount = activeRoleCount; }
        
        public long getActivePermissionCount() { return activePermissionCount; }
        public void setActivePermissionCount(long activePermissionCount) { this.activePermissionCount = activePermissionCount; }
        
        public long getActiveResourceCount() { return activeResourceCount; }
        public void setActiveResourceCount(long activeResourceCount) { this.activeResourceCount = activeResourceCount; }
    }
}