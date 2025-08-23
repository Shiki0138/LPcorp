package com.enterprise.security.rbac.config;

import com.enterprise.security.authorization.handler.RbacAuthorizationAspect;
import com.enterprise.security.authorization.evaluator.RbacPermissionEvaluator;
import com.enterprise.security.integration.database.RowLevelSecurityAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main security configuration for RBAC framework
 */
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
@EnableAspectJAutoProxy
@EnableCaching
@EnableAsync
@EnableScheduling
public class RbacSecurityConfiguration extends GlobalMethodSecurityConfiguration {
    
    @Autowired
    private RbacPermissionEvaluator rbacPermissionEvaluator;
    
    /**
     * Configure method security expression handler with custom permission evaluator
     */
    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(rbacPermissionEvaluator);
        return handler;
    }
    
    /**
     * RBAC authorization aspect bean
     */
    @Bean
    public RbacAuthorizationAspect rbacAuthorizationAspect() {
        return new RbacAuthorizationAspect();
    }
    
    /**
     * Row-level security aspect bean
     */
    @Bean
    public RowLevelSecurityAspect rowLevelSecurityAspect() {
        return new RowLevelSecurityAspect();
    }
    
    /**
     * Permission cache configuration
     */
    @Bean
    public org.springframework.cache.CacheManager rbacCacheManager() {
        org.springframework.cache.concurrent.ConcurrentMapCacheManager cacheManager = 
            new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "permissions", "roles", "userPermissions", "userRoles", "resources"
        ));
        return cacheManager;
    }
    
    /**
     * Task executor for async operations
     */
    @Bean
    public java.util.concurrent.Executor rbacTaskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = 
            new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rbac-async-");
        executor.initialize();
        return executor;
    }
}