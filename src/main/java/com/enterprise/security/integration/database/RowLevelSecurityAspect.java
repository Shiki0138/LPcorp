package com.enterprise.security.integration.database;

import com.enterprise.security.authorization.engine.RbacAuthorizationEngine;
import com.enterprise.security.rbac.model.User;
import com.enterprise.security.rbac.service.RbacUserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Aspect for implementing row-level security using database filters
 */
@Aspect
@Component
public class RowLevelSecurityAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RowLevelSecurityAspect.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private RbacAuthorizationEngine authorizationEngine;
    
    @Autowired
    private RbacUserService userService;
    
    /**
     * Apply row-level security to repository find methods
     */
    @Around("execution(* com.enterprise.*.repository.*Repository.find*(..)) && @annotation(com.enterprise.security.integration.database.ApplyRowLevelSecurity)")
    public Object applyRowLevelSecurityToFind(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        String userId = extractUserId(auth);
        User user = userService.getUserWithPermissions(userId);
        
        if (user == null) {
            throw new SecurityException("User not found");
        }
        
        // Enable row-level security filters
        enableRowLevelSecurityFilters(user);
        
        try {
            return joinPoint.proceed();
        } finally {
            // Disable filters after query execution
            disableRowLevelSecurityFilters();
        }
    }
    
    /**
     * Apply row-level security to JPA queries
     */
    @Around("execution(* javax.persistence.EntityManager.createQuery(..))||" +
            "execution(* javax.persistence.EntityManager.createNamedQuery(..))")
    public Object applyRowLevelSecurityToQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return joinPoint.proceed();
        }
        
        String userId = extractUserId(auth);
        User user = userService.getUserWithPermissions(userId);
        
        if (user == null) {
            return joinPoint.proceed();
        }
        
        Object result = joinPoint.proceed();
        
        // Apply security parameters to queries
        if (result instanceof Query) {
            Query query = (Query) result;
            applySecurityParametersToQuery(query, user);
        }
        
        return result;
    }
    
    /**
     * Enable Hibernate filters for row-level security
     */
    private void enableRowLevelSecurityFilters(User user) {
        // Enable tenant filter
        if (user.getTenantId() != null) {
            org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", user.getTenantId());
        }
        
        // Enable department filter if user doesn't have cross-department access
        if (user.getDepartmentId() != null && !hasCrossDepartmentAccess(user)) {
            org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
            session.enableFilter("departmentFilter")
                   .setParameter("departmentId", user.getDepartmentId());
        }
        
        // Enable data classification filter
        org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
        session.enableFilter("classificationFilter")
               .setParameter("maxClassificationLevel", user.getClearanceLevel().getLevel());
        
        // Enable ownership filter for resources owned by user
        session.enableFilter("ownershipFilter")
               .setParameter("userId", user.getId());
        
        // Enable project access filter
        List<String> accessibleProjects = getAccessibleProjects(user);
        if (!accessibleProjects.isEmpty()) {
            session.enableFilter("projectAccessFilter")
                   .setParameterList("projectIds", accessibleProjects);
        }
        
        logger.debug("Enabled row-level security filters for user: {}", user.getUsername());
    }
    
    /**
     * Disable all row-level security filters
     */
    private void disableRowLevelSecurityFilters() {
        org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
        session.disableFilter("tenantFilter");
        session.disableFilter("departmentFilter");
        session.disableFilter("classificationFilter");
        session.disableFilter("ownershipFilter");
        session.disableFilter("projectAccessFilter");
        
        logger.debug("Disabled row-level security filters");
    }
    
    /**
     * Apply security parameters to JPA queries
     */
    private void applySecurityParametersToQuery(Query query, User user) {
        try {
            // Set tenant parameter if query contains it
            if (queryContainsParameter(query, "tenantId")) {
                query.setParameter("tenantId", user.getTenantId());
            }
            
            // Set department parameter if needed
            if (queryContainsParameter(query, "departmentId") && user.getDepartmentId() != null) {
                query.setParameter("departmentId", user.getDepartmentId());
            }
            
            // Set user ID parameter
            if (queryContainsParameter(query, "currentUserId")) {
                query.setParameter("currentUserId", user.getId());
            }
            
            // Set clearance level parameter
            if (queryContainsParameter(query, "maxClearanceLevel")) {
                query.setParameter("maxClearanceLevel", user.getClearanceLevel().getLevel());
            }
            
            logger.debug("Applied security parameters to query for user: {}", user.getUsername());
            
        } catch (Exception e) {
            logger.warn("Could not apply security parameters to query", e);
        }
    }
    
    /**
     * Check if query contains a specific parameter
     */
    private boolean queryContainsParameter(Query query, String parameterName) {
        try {
            // This is a simplified check - in reality, you'd parse the query string
            // or use JPA metamodel to check for parameters
            String queryString = query.toString();
            return queryString.contains(":" + parameterName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if user has cross-department access
     */
    private boolean hasCrossDepartmentAccess(User user) {
        return authorizationEngine.hasPermission(user.getId(), "cross_department_access");
    }
    
    /**
     * Get list of projects user has access to
     */
    private List<String> getAccessibleProjects(User user) {
        // This would typically query the database for projects the user can access
        // based on their roles and permissions
        return List.of(); // Placeholder
    }
    
    /**
     * Extract user ID from authentication
     */
    private String extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
        } else if (auth.getPrincipal() instanceof String) {
            return (String) auth.getPrincipal();
        }
        return null;
    }
}