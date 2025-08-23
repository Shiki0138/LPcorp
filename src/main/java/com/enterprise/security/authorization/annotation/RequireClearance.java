package com.enterprise.security.authorization.annotation;

import com.enterprise.security.rbac.model.enums.ClearanceLevel;
import java.lang.annotation.*;

/**
 * Annotation for method-level clearance checking
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireClearance {
    
    /**
     * Required minimum clearance level
     */
    ClearanceLevel value();
    
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
    String denyMessage() default "Access denied: insufficient clearance level";
}