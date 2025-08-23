package com.enterprise.security.management.controller;

import com.enterprise.security.rbac.model.*;
import com.enterprise.security.rbac.service.*;
import com.enterprise.security.management.dto.*;
import com.enterprise.security.authorization.engine.RbacAuthorizationEngine;
import com.enterprise.security.authorization.annotation.RequirePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for RBAC management operations
 */
@RestController
@RequestMapping("/api/rbac")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RbacManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(RbacManagementController.class);
    
    @Autowired
    private RbacUserService userService;
    
    @Autowired
    private RbacRoleService roleService;
    
    @Autowired
    private RbacPermissionService permissionService;
    
    @Autowired
    private RbacResourceService resourceService;
    
    @Autowired
    private RbacAuthorizationEngine authorizationEngine;
    
    // ==================== USER MANAGEMENT ====================
    
    /**
     * Get all users with pagination
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'read')")
    public ResponseEntity<Page<UserDto>> getUsers(Pageable pageable,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String department,
                                                  @RequestParam(required = false) String status) {
        try {
            Page<User> users = userService.getUsers(pageable, search, department, status);
            Page<UserDto> userDtos = users.map(this::convertToUserDto);
            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            logger.error("Error retrieving users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#userId, 'user', 'read')")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        try {
            User user = userService.getUserWithPermissions(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(convertToUserDto(user));
        } catch (Exception e) {
            logger.error("Error retrieving user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create new user
     */
    @PostMapping("/users")
    @RequirePermission(resource = "user", action = "create")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            User user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToUserDto(user));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update user
     */
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#userId, 'user', 'update')")
    public ResponseEntity<UserDto> updateUser(@PathVariable String userId,
                                            @Valid @RequestBody UpdateUserRequest request) {
        try {
            User user = userService.updateUser(userId, request);
            return ResponseEntity.ok(convertToUserDto(user));
        } catch (Exception e) {
            logger.error("Error updating user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Assign role to user
     */
    @PostMapping("/users/{userId}/roles")
    @RequirePermission(resource = "user", action = "assign_role")
    public ResponseEntity<Void> assignRole(@PathVariable String userId,
                                         @Valid @RequestBody AssignRoleRequest request) {
        try {
            userService.assignRole(userId, request.getRoleId(), request.getAssignedBy(), 
                                 request.getExpiresAt(), request.getContext());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error assigning role to user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Remove role from user
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @RequirePermission(resource = "user", action = "remove_role")
    public ResponseEntity<Void> removeRole(@PathVariable String userId,
                                         @PathVariable String roleId) {
        try {
            userService.removeRole(userId, roleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing role from user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Grant direct permission to user
     */
    @PostMapping("/users/{userId}/permissions")
    @RequirePermission(resource = "user", action = "grant_permission")
    public ResponseEntity<Void> grantPermission(@PathVariable String userId,
                                              @Valid @RequestBody GrantPermissionRequest request) {
        try {
            userService.grantDirectPermission(userId, request.getPermissionId(), 
                                            request.getGrantedBy(), request.getExpiresAt(),
                                            request.getResourceId(), request.getConstraints());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error granting permission to user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user's effective permissions
     */
    @GetMapping("/users/{userId}/effective-permissions")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#userId, 'user', 'read')")
    public ResponseEntity<Set<String>> getUserPermissions(@PathVariable String userId) {
        try {
            Set<String> permissions = authorizationEngine.getUserPermissions(userId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            logger.error("Error retrieving user permissions: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== ROLE MANAGEMENT ====================
    
    /**
     * Get all roles
     */
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('role', 'read')")
    public ResponseEntity<Page<RoleDto>> getRoles(Pageable pageable,
                                                @RequestParam(required = false) String search,
                                                @RequestParam(required = false) String category) {
        try {
            Page<Role> roles = roleService.getRoles(pageable, search, category);
            Page<RoleDto> roleDtos = roles.map(this::convertToRoleDto);
            return ResponseEntity.ok(roleDtos);
        } catch (Exception e) {
            logger.error("Error retrieving roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get role by ID
     */
    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#roleId, 'role', 'read')")
    public ResponseEntity<RoleDto> getRole(@PathVariable String roleId) {
        try {
            Role role = roleService.getRoleWithPermissions(roleId);
            if (role == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(convertToRoleDto(role));
        } catch (Exception e) {
            logger.error("Error retrieving role: {}", roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create new role
     */
    @PostMapping("/roles")
    @RequirePermission(resource = "role", action = "create")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        try {
            Role role = roleService.createRole(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToRoleDto(role));
        } catch (Exception e) {
            logger.error("Error creating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update role
     */
    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#roleId, 'role', 'update')")
    public ResponseEntity<RoleDto> updateRole(@PathVariable String roleId,
                                            @Valid @RequestBody UpdateRoleRequest request) {
        try {
            Role role = roleService.updateRole(roleId, request);
            return ResponseEntity.ok(convertToRoleDto(role));
        } catch (Exception e) {
            logger.error("Error updating role: {}", roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Assign permission to role
     */
    @PostMapping("/roles/{roleId}/permissions")
    @RequirePermission(resource = "role", action = "assign_permission")
    public ResponseEntity<Void> assignPermissionToRole(@PathVariable String roleId,
                                                      @Valid @RequestBody AssignPermissionRequest request) {
        try {
            roleService.assignPermission(roleId, request.getPermissionId(), 
                                       request.getAssignedBy(), request.getConstraints());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error assigning permission to role: {}", roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Set role hierarchy
     */
    @PostMapping("/roles/{roleId}/parents")
    @RequirePermission(resource = "role", action = "manage_hierarchy")
    public ResponseEntity<Void> setRoleParents(@PathVariable String roleId,
                                             @Valid @RequestBody SetRoleParentsRequest request) {
        try {
            roleService.setParentRoles(roleId, request.getParentRoleIds());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error setting role parents: {}", roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== PERMISSION MANAGEMENT ====================
    
    /**
     * Get all permissions
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('permission', 'read')")
    public ResponseEntity<Page<PermissionDto>> getPermissions(Pageable pageable,
                                                            @RequestParam(required = false) String search,
                                                            @RequestParam(required = false) String resourceType) {
        try {
            Page<Permission> permissions = permissionService.getPermissions(pageable, search, resourceType);
            Page<PermissionDto> permissionDtos = permissions.map(this::convertToPermissionDto);
            return ResponseEntity.ok(permissionDtos);
        } catch (Exception e) {
            logger.error("Error retrieving permissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create new permission
     */
    @PostMapping("/permissions")
    @RequirePermission(resource = "permission", action = "create")
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        try {
            Permission permission = permissionService.createPermission(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToPermissionDto(permission));
        } catch (Exception e) {
            logger.error("Error creating permission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== RESOURCE MANAGEMENT ====================
    
    /**
     * Register a new resource
     */
    @PostMapping("/resources")
    @RequirePermission(resource = "resource", action = "create")
    public ResponseEntity<ResourceDto> createResource(@Valid @RequestBody CreateResourceRequest request) {
        try {
            Resource resource = resourceService.createResource(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToResourceDto(resource));
        } catch (Exception e) {
            logger.error("Error creating resource", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== AUTHORIZATION TESTING ====================
    
    /**
     * Test authorization for specific user/resource/action
     */
    @PostMapping("/test-authorization")
    @RequirePermission(resource = "system", action = "test_authorization")
    public ResponseEntity<AuthorizationTestResult> testAuthorization(
            @Valid @RequestBody AuthorizationTestRequest request) {
        try {
            var authRequest = com.enterprise.security.authorization.engine.AuthorizationRequest.builder()
                .userId(request.getUserId())
                .action(request.getAction())
                .resourceId(request.getResourceId())
                .resourceType(request.getResourceType())
                .clientIp(request.getClientIp())
                .build();
            
            var result = authorizationEngine.authorize(authRequest);
            
            AuthorizationTestResult testResult = new AuthorizationTestResult();
            testResult.setGranted(result.isGranted());
            testResult.setReason(result.getReason());
            testResult.setPermissionSource(result.getPermissionSource());
            testResult.setEvaluationTimeMs(result.getEvaluationTimeMs());
            
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            logger.error("Error testing authorization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Bulk authorization test
     */
    @PostMapping("/test-authorization/bulk")
    @RequirePermission(resource = "system", action = "test_authorization")
    public ResponseEntity<Map<String, Boolean>> testBulkAuthorization(
            @Valid @RequestBody BulkAuthorizationTestRequest request) {
        try {
            Map<String, com.enterprise.security.authorization.engine.AuthorizationResult> results = 
                authorizationEngine.authorizeMultiple(request.getUserId(), request.getAction(), 
                                                     request.getResourceIds());
            
            Map<String, Boolean> simplifiedResults = results.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isGranted()));
            
            return ResponseEntity.ok(simplifiedResults);
        } catch (Exception e) {
            logger.error("Error testing bulk authorization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    private UserDto convertToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setStatus(user.getStatus().name());
        dto.setDepartmentId(user.getDepartmentId());
        dto.setJobTitle(user.getJobTitle());
        dto.setClearanceLevel(user.getClearanceLevel().name());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        
        // Convert roles
        dto.setRoles(user.getActiveRoles().stream()
            .map(role -> {
                RoleDto roleDto = new RoleDto();
                roleDto.setId(role.getId());
                roleDto.setName(role.getName());
                roleDto.setDescription(role.getDescription());
                return roleDto;
            })
            .collect(java.util.stream.Collectors.toSet()));
        
        return dto;
    }
    
    private RoleDto convertToRoleDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setRoleType(role.getRoleType().name());
        dto.setStatus(role.getStatus().name());
        dto.setHierarchyLevel(role.getHierarchyLevel());
        dto.setCreatedAt(role.getCreatedAt());
        
        // Convert permissions
        dto.setPermissions(role.getDirectPermissions().stream()
            .map(permission -> {
                PermissionDto permDto = new PermissionDto();
                permDto.setId(permission.getId());
                permDto.setName(permission.getName());
                permDto.setResourceType(permission.getResourceType());
                permDto.setAction(permission.getAction());
                return permDto;
            })
            .collect(java.util.stream.Collectors.toSet()));
        
        return dto;
    }
    
    private PermissionDto convertToPermissionDto(Permission permission) {
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDescription(permission.getDescription());
        dto.setResourceType(permission.getResourceType());
        dto.setAction(permission.getAction());
        dto.setCategory(permission.getCategory());
        dto.setPermissionType(permission.getPermissionType().name());
        dto.setScope(permission.getScope().name());
        dto.setRiskLevel(permission.getRiskLevel().name());
        dto.setCreatedAt(permission.getCreatedAt());
        return dto;
    }
    
    private ResourceDto convertToResourceDto(Resource resource) {
        ResourceDto dto = new ResourceDto();
        dto.setId(resource.getId());
        dto.setResourceType(resource.getResourceType());
        dto.setIdentifier(resource.getIdentifier());
        dto.setName(resource.getName());
        dto.setDescription(resource.getDescription());
        dto.setOwnerId(resource.getOwnerId());
        dto.setDepartmentId(resource.getDepartmentId());
        dto.setClassification(resource.getClassification() != null ? resource.getClassification().name() : null);
        dto.setCreatedAt(resource.getCreatedAt());
        return dto;
    }
}