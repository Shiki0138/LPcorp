package com.enterprise.security.integration.database;

import java.lang.annotation.*;

/**
 * Annotation to mark repository methods that should apply row-level security
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApplyRowLevelSecurity {
    
    /**
     * Types of filters to apply
     */
    FilterType[] value() default {FilterType.TENANT, FilterType.DEPARTMENT, FilterType.CLASSIFICATION};
    
    /**
     * Whether to apply ownership filter
     */
    boolean includeOwnership() default true;
    
    /**
     * Whether to apply project access filter
     */
    boolean includeProjectAccess() default false;
    
    /**
     * Custom filter names to apply
     */
    String[] customFilters() default {};
    
    enum FilterType {
        TENANT,
        DEPARTMENT,
        CLASSIFICATION,
        OWNERSHIP,
        PROJECT_ACCESS
    }
}