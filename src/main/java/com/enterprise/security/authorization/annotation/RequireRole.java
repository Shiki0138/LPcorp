package com.enterprise.security.authorization.annotation;

import java.lang.annotation.*;

/**
 * Annotation for method-level role checking
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    
    /**
     * Required role names (any of these roles will grant access)
     */
    String[] value();
    
    /**
     * Whether all roles are required (AND) or any role (OR)
     * Default is OR (any role grants access)
     */
    boolean requireAll() default false;
    
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
    String denyMessage() default "Access denied: insufficient roles";
}