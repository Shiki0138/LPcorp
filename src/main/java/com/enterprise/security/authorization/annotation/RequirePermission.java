package com.enterprise.security.authorization.annotation;

import java.lang.annotation.*;

/**
 * Annotation for method-level permission checking
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * Resource type required for the permission
     */
    String resource();
    
    /**
     * Action required for the permission
     */
    String action();
    
    /**
     * Optional specific resource ID (SpEL expression supported)
     */
    String resourceId() default "";
    
    /**
     * Optional additional conditions (SpEL expression)
     */
    String condition() default "";
    
    /**
     * Whether to allow access for emergency cases
     */
    boolean allowEmergencyAccess() default false;
    
    /**
     * Error message when access is denied
     */
    String denyMessage() default "Access denied: insufficient permissions";
}